/*
 * Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package moe.lymia.mppatch.core

import moe.lymia.mppatch.util.io.*
import moe.lymia.mppatch.util.{EncodingUtils, Logger, SimpleLogger}

import java.nio.file.attribute.PosixFilePermission.*
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.xml.Node

private object PathNames {
  val patchStateFilename = "mppatch_install_state.xml"
  val patchLockFilename  = ".mppatch_installer_lock"
}

enum PatchStatus {
  case NotInstalled(isKnownVersion: Boolean)
  case Installed
  case CanUninstall
  case PackageChange
  case NeedsUpdate
  case FilesValidated
  case TargetUpdated
  case UnknownUpdate
  case FilesCorrupted
  case NeedsCleanup
  case NeedsValidation
}

private case class PatchFile(path: String, expectedSha256: String)
private case class RenameData(replacedFile: PatchFile, originalFile: PatchFile)
private case class PatchState(
    versionFrom: String,
    sha256: String,
    packages: Set[String],
    renames: Seq[RenameData],
    additionalFiles: Seq[PatchFile],
    additionalDirectories: Seq[String],
    installedVersion: String,
    installedTimestamp: Long
) {
  lazy val expectedPaths =
    (additionalFiles.map(_.path) ++ renames.map(_.replacedFile.path) ++ renames.map(_.originalFile.path) ++
      additionalDirectories).toSet
  lazy val replacementTargets  = renames.map(_.replacedFile.path).toSet
  lazy val nonreplacementFiles = additionalFiles.filter(x => !replacementTargets.contains(x.path))
}
private object PatchState {
  private val formatVersion = "0"

  def serializePatchFile(file: PatchFile) = <PatchFile path={file.path} expectedSha256={file.expectedSha256}/>
  def serializeRenameData(renameData: RenameData) =
    <RenameData>
      <ReplacedFile>{serializePatchFile(renameData.replacedFile)}</ReplacedFile>
      <OriginalFile>{serializePatchFile(renameData.originalFile)}</OriginalFile>
    </RenameData>
  def serialize(state: PatchState) =
    <PatchState version={formatVersion}>
      <VersionFrom>{state.versionFrom}</VersionFrom>
      <Sha256>{state.sha256}</Sha256>
      <Packages>{state.packages.map(x => <Package>{x}</Package>)}</Packages>
      <Renames>{state.renames.map(serializeRenameData)}</Renames>
      <AdditionalFiles>{state.additionalFiles.map(serializePatchFile)}</AdditionalFiles>
      <AdditionalDirectories>{state.additionalDirectories.map(x => <Directory>{x}</Directory>)}</AdditionalDirectories>
      <InstalledVersion>{state.installedVersion}</InstalledVersion>
      <InstalledTimestamp>{state.installedTimestamp}</InstalledTimestamp>
    </PatchState>

  def unserializePatchFile(node: Node) =
    PatchFile(XMLUtils.getAttribute(node, "path"), XMLUtils.getAttribute(node, "expectedSha256"))
  def unserializeRenameData(node: Node) =
    RenameData(
      unserializePatchFile((node \ "ReplacedFile" \ "PatchFile").head),
      unserializePatchFile((node \ "OriginalFile" \ "PatchFile").head)
    )
  def unserialize(xml: Node) =
    if (XMLUtils.getAttribute(xml, "version") != formatVersion) None
    else
      Some(
        PatchState(
          XMLUtils.getNodeText(xml, "VersionFrom"),
          XMLUtils.getNodeText(xml, "Sha256"),
          (xml \ "Packages" \ "Package").map(_.text).toSet,
          (xml \ "Renames" \ "RenameData").map(unserializeRenameData),
          (xml \ "AdditionalFiles" \ "PatchFile").map(unserializePatchFile),
          (xml \ "AdditionalDirectories" \ "Directory").map(_.text),
          XMLUtils.getNodeText(xml, "InstalledVersion"),
          XMLUtils.getNodeText(xml, "InstalledTimestamp").toLong
        )
      )
}

class PatchInstaller(val basePath: Path, val install: InstallScript, platform: Platform, log: Logger = SimpleLogger) {
  import PathNames.*

  private val patchStatePath = basePath.resolve(patchStateFilename)
  private val patchLockPath  = basePath.resolve(patchLockFilename)

  private val syncLock                   = new Object
  private var fileLock: Option[FileLock] = None
  private var manualLock                 = false

  def isLockAcquired = syncLock synchronized fileLock.isDefined
  def acquireLock(manualLock: Boolean = true) = syncLock synchronized {
    fileLock match {
      case Some(_) =>
        if (!this.manualLock && manualLock) this.manualLock = true
        true
      case None =>
        IOUtils.lock(patchLockPath) match {
          case Some(lock) =>
            fileLock = Some(lock)
            this.manualLock = manualLock
            true
          case None =>
            false
        }
    }
  }
  def releaseLock() = syncLock synchronized {
    fileLock.foreach { lock =>
      lock.release()
      fileLock = None
      manualLock = false
    }
  }
  private def lock[T](f: => T) = syncLock synchronized {
    if (!acquireLock(false)) sys.error("Could not acquire lock.")
    val out = f
    if (!manualLock) releaseLock()
    out
  }

  private def validatePatchFile(pathName: String, expectedSha256: String): Boolean = {
    val path = basePath.resolve(pathName)
    if (!Files.exists(path)) {
      log.warn(s"- File $pathName is missing")
      false
    } else if (!Files.isRegularFile(path)) {
      log.warn(s"- File $pathName is not a regular file")
      false
    } else {
      val sha256 = EncodingUtils.sha256_hex(Files.readAllBytes(path))
      if (sha256 != expectedSha256) {
        log.warn(s"- File $pathName failed to validate. (Actual sha256: $sha256, expected: $expectedSha256")
        false
      } else {
        log.info(s"- File $pathName successflly validated.")
        true
      }
    }
  }
  private def validatePatchFile(file: PatchFile): Boolean = validatePatchFile(file.path, file.expectedSha256)

  private def isVersionKnown(path: String) =
    install.supportedHash(EncodingUtils.sha256_hex(Files.readAllBytes(basePath.resolve(path))))
  private def loadPatchState() = try
    if (Files.exists(patchStatePath))
      PatchState.unserialize(IOUtils.readXML(patchStatePath))
    else None
  catch {
    case e: Exception =>
      log.error("Error encountered while deserializing patch state.", e)
      None
  }

  def installedVersion = loadPatchState().map(_.installedVersion)
  def isDowngrade      = loadPatchState().fold(0L)(_.installedTimestamp) > install.versionInfo.buildDate.getTime

  private def intCheckPatchStatus(packages: Set[String]) = {
    log.info("Checking patch status...")

    def body() = {
      val leftoverFiles = install.cleanup.checkFile.filter(x => Files.exists(basePath.resolve(x)))

      if (!Files.exists(patchStatePath)) {
        if (!Files.exists(basePath.resolve(install.script.hashFrom))) PatchStatus.NeedsValidation
        else if (leftoverFiles.nonEmpty) PatchStatus.NeedsCleanup
        else PatchStatus.NotInstalled(isVersionKnown(install.script.hashFrom))
      } else
        loadPatchState() match {
          case Some(patchState) =>
            val leftoverSet = leftoverFiles.toSet -- patchState.expectedPaths
            if (leftoverSet.nonEmpty) {
              log.info(s"- Leftover files: [${leftoverSet.mkString(", ")}]")
              PatchStatus.NeedsCleanup
            } else {
              val originalFilesOK = patchState.renames.map(_.originalFile).forall(validatePatchFile)
              if (
                !patchState.renames.map(_.replacedFile).forall(x => Files.exists(basePath.resolve(x.path))) ||
                !originalFilesOK ||
                !patchState.nonreplacementFiles.forall(validatePatchFile)
              ) {
                if (originalFilesOK) PatchStatus.FilesCorrupted else PatchStatus.NeedsValidation
              } else if (!patchState.renames.map(_.replacedFile).forall(validatePatchFile)) {
                if (validatePatchFile(patchState.versionFrom, patchState.sha256)) PatchStatus.FilesValidated
                else if (isVersionKnown(patchState.versionFrom)) PatchStatus.TargetUpdated
                else PatchStatus.UnknownUpdate
              } else if (install.supportedHash(patchState.sha256)) {
                if (
                  patchState.installedTimestamp != install.versionInfo.buildDate.getTime ||
                  patchState.installedVersion != install.versionInfo.versionString
                ) PatchStatus.NeedsUpdate
                else if (patchState.packages != packages) PatchStatus.PackageChange
                else PatchStatus.Installed
              } else {
                PatchStatus.CanUninstall
              }
            }
          case None => PatchStatus.NeedsCleanup
        }
    }

    val ret = body()
    log.info("Status: " + ret)
    ret
  }

  private def installPatch(packages: Set[String]) = {
    log.info("Installing patch...")

    val targetVersion = EncodingUtils.sha256_hex(Files.readAllBytes(basePath.resolve(install.script.hashFrom)))
    log.info(s"- Target version: $targetVersion")

    val newFileSet = install.makeFileSet(packages, targetVersion)
    val newFiles   = newFileSet.getFiles(basePath, targetVersion)

    for (rename <- newFileSet.renames) {
      log.info(s"- Renaming ${rename.from} -> ${rename.to}")
      Files.move(basePath.resolve(rename.from), basePath.resolve(rename.to))
    }
    val patchNewFiles = for (OutputFile(name, data, executable) <- newFiles) yield {
      val path = basePath.resolve(name)
      val newDirs = for (parentName <- name.split("/").inits.toList.reverse.init.map(_.mkString("/"))) yield {
        val parentPath = basePath.resolve(parentName)
        if (!Files.exists(parentPath)) {
          log.info(s"- Creating directory ${parentPath.toString}")
          Files.createDirectories(parentPath)
          Seq(parentName)
        } else Seq()
      }
      log.info(s"- Installing $name (size: ${data.length}, executable: $executable)")
      Files.write(path, data)
      if (executable)
        Files.setPosixFilePermissions(
          path,
          (Files.getPosixFilePermissions(path).asScala.toSet + OWNER_EXECUTE + GROUP_EXECUTE + OTHERS_EXECUTE).asJava
        )
      (PatchFile(name, EncodingUtils.sha256_hex(data)), newDirs.flatten)
    }

    def patchFileFromPath(path: String) =
      PatchFile(path, EncodingUtils.sha256_hex(Files.readAllBytes(basePath.resolve(path))))
    val renameData =
      for (rename <- newFileSet.renames)
        yield RenameData(patchFileFromPath(rename.from), patchFileFromPath(rename.to))

    log.info("- Writing patch state")
    val state = PatchState(
      install.script.hashFrom,
      targetVersion,
      packages,
      renameData,
      patchNewFiles.map(_._1),
      patchNewFiles.flatMap(_._2),
      install.versionInfo.versionString,
      install.versionInfo.buildDate.getTime
    )
    IOUtils.writeXML(patchStatePath, PatchState.serialize(state))
  }

  private def uninstallPatch() = {
    log.info("Uninstalling patch...")
    loadPatchState() match {
      case None => sys.error("could not load patch state")
      case Some(patchState) =>
        for (RenameData(replacement, original) <- patchState.renames)
          if (
            validatePatchFile(original) && !validatePatchFile(replacement) &&
            Files.exists(basePath.resolve(replacement.path)) &&
            Files.isRegularFile(basePath.resolve(replacement.path))
          ) {
            log.info(s"- Target $original updated, renaming $replacement -> $original")
            Files.delete(basePath.resolve(original.path))
            Files.move(basePath.resolve(replacement.path), basePath.resolve(original.path))
          }
        for (PatchFile(name, _) <- patchState.additionalFiles) if (Files.exists(basePath.resolve(name))) {
          log.info(s"- Deleting $name")
          IOUtils.deleteDirectory(basePath.resolve(name))
        } else log.warn(s"- File $name is missing.")
        for (RenameData(replacement, original) <- patchState.renames) {
          val originalPath    = basePath.resolve(original.path)
          val replacementPath = basePath.resolve(replacement.path)

          if (Files.exists(replacementPath)) {
            log.info(s"- Deleting leftover file ${replacement.path}")
            Files.delete(replacementPath)
          }
          if (Files.exists(originalPath)) {
            log.info(s"- Renaming ${original.path} -> ${replacement.path}")
            Files.move(originalPath, replacementPath)
          } else log.info(s"- File ${original.path} is missing.")
        }
        for (name <- patchState.additionalDirectories.reverse) if (Files.exists(basePath.resolve(name))) {
          log.info(s"- Deleting directory $name")
          IOUtils.deleteDirectory(basePath.resolve(name))
        }

        log.info("- Deleting patch state")
        IOUtils.deleteDirectory(patchStatePath)
    }
  }

  def checkPatchStatus(packages: Set[String]) = lock(intCheckPatchStatus(packages))
  def cleanupPatch() = lock {
    loadPatchState() match {
      case None    =>
      case Some(_) => uninstallPatch()
    }
    log.info("Cleaning up remaining files...")
    for (JsonRename(from, to) <- install.cleanup.renames) {
      log.info(s"- Renaming $from -> $to...")
      if (Files.exists(basePath.resolve(from))) {
        if (Files.exists(basePath.resolve(to))) IOUtils.deleteDirectory(basePath.resolve(to))
        Files.move(basePath.resolve(from), basePath.resolve(to))
      }
    }
    for (file <- install.cleanup.checkFile) {
      log.info(s"- Deleting $file...")
      IOUtils.deleteDirectory(basePath.resolve(file))
    }
    log.info("- Cleaning up patch state file")
    IOUtils.deleteDirectory(patchStatePath)
  }
  def safeUpdate(packages: Set[String]) = lock {
    intCheckPatchStatus(packages) match {
      case PatchStatus.Installed | PatchStatus.PackageChange | PatchStatus.NeedsUpdate | PatchStatus.FilesCorrupted |
          PatchStatus.TargetUpdated | PatchStatus.FilesValidated =>
        uninstallPatch()
        installPatch(packages)
      case PatchStatus.NotInstalled(true) =>
        installPatch(packages)
      case _ => sys.error("cannot safely update")
    }
  }
  def safeUninstall() = lock {
    intCheckPatchStatus(Set()) match {
      case PatchStatus.NotInstalled(_) =>
      // do nothing
      case PatchStatus.Installed | PatchStatus.PackageChange | PatchStatus.NeedsUpdate | PatchStatus.CanUninstall |
          PatchStatus.UnknownUpdate | PatchStatus.FilesCorrupted | PatchStatus.TargetUpdated |
          PatchStatus.FilesValidated =>
        uninstallPatch()
      case _ => sys.error("cannot safely uninstall")
    }
  }
}
