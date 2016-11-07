/*
 * Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
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

import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{Files, Path}

import moe.lymia.mppatch.util.common.Crypto
import moe.lymia.mppatch.util.{FileLock, IOUtils, XMLUtils}

import scala.collection.JavaConverters._
import scala.xml.Node

private object PathNames {
  val patchStateFilename = "mppatch_install_state.xml"
  val patchLockFilename  = ".mppatch_installer_lock"
}

sealed trait PatchStatus
object PatchStatus {
  case class NotInstalled(isKnownVersion: Boolean) extends PatchStatus

  case object Installed       extends PatchStatus
  case object CanUninstall    extends PatchStatus
  case object PackageChange   extends PatchStatus
  case object NeedsUpdate     extends PatchStatus
  case object TargetUpdated   extends PatchStatus
  case object FilesCorrupted  extends PatchStatus
  case object NeedsCleanup    extends PatchStatus
  case object NeedsValidation extends PatchStatus
}

private case class PatchFile(path: String, expectedSha256: String)
private case class RenameData(replacedFile: PatchFile, originalFile: PatchFile)
private case class PatchState(versionFrom: String, sha256: String,
                              packages: Set[String], renames: Seq[RenameData],
                              additionalFiles: Seq[PatchFile], additionalDirectories: Seq[String],
                              installedVersion: String, installedTimestamp: Long) {
  lazy val expectedFiles =
    (additionalFiles.map(_.path) ++ renames.map(_.replacedFile.path) ++ renames.map(_.originalFile.path)).toSet
  lazy val replacementTargets = renames.map(_.replacedFile.path).toSet
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
    RenameData(unserializePatchFile((node \ "ReplacedFile" \ "PatchFile").head),
               unserializePatchFile((node \ "OriginalFile" \ "PatchFile").head))
  def unserialize(xml: Node) =
    if(XMLUtils.getAttribute(xml, "version") != formatVersion) None
    else Some(PatchState(XMLUtils.getNodeText(xml, "VersionFrom"), XMLUtils.getNodeText(xml, "Sha256"),
                         (xml \ "Packages"              \ "Package"   ).map(_.text               ).toSet,
                         (xml \ "Renames"               \ "RenameData").map(unserializeRenameData),
                         (xml \ "AdditionalFiles"       \ "PatchFile" ).map(unserializePatchFile ),
                         (xml \ "AdditionalDirectories" \ "Directory" ).map(_.text               ),
                         XMLUtils.getNodeText(xml, "InstalledVersion"),
                         XMLUtils.getNodeText(xml, "InstalledTimestamp").toLong))
}

class PatchInstaller(val basePath: Path, val loader: PatchLoader, platform: Platform, log: String => Unit = println) {
  import PathNames._

  private val patchStatePath = basePath.resolve(patchStateFilename)
  private val patchLockPath  = basePath.resolve(patchLockFilename)

  private val syncLock = new Object
  private var fileLock: FileLock = _
  private var manualLock = false

  def isLockAcquired = syncLock synchronized { fileLock != null }
  def acquireLock(manualLock: Boolean = true) = syncLock synchronized {
    if(fileLock != null) {
      if(!this.manualLock && manualLock) this.manualLock = true
      true
    } else IOUtils.lock(patchLockPath) match {
      case Some(lock) =>
        fileLock = lock
        this.manualLock = manualLock
        true
      case None =>
        false
    }
  }
  def releaseLock() = syncLock synchronized {
    if(fileLock != null) {
      fileLock.release()
      fileLock = null
      manualLock = false
    }
  }
  private def lock[T](f: => T) = syncLock synchronized {
    if(!acquireLock(false)) sys.error("Could not acquire lock.")
    val out = f
    if(!manualLock) releaseLock()
    out
  }

  private def validatePatchFile(file: PatchFile) = {
    val path = basePath.resolve(file.path)
    Files.exists(path) && Files.isRegularFile(path) &&
      Crypto.sha256_hex(Files.readAllBytes(path)) == file.expectedSha256
  }
  private def isVersionKnown(path: String) =
    loader.nativePatchExists(Crypto.sha256_hex(Files.readAllBytes(basePath.resolve(path))))
  private def loadPatchState() = try {
    if(Files.exists(patchStatePath)) PatchState.unserialize(IOUtils.readXML(patchStatePath))
    else None
  } catch {
    case e: Exception =>
      System.err.println("Error encountered while deserializing patch state.")
      e.printStackTrace()

      None
  }

  def installedVersion = loadPatchState().map(_.installedVersion)
  def isDowngrade      = loadPatchState().fold(0L)(_.installedTimestamp) > loader.data.timestamp

  private def intCheckPatchStatus(packages: Set[String]): PatchStatus = {
    val detectedPatchFiles = IOUtils.listFileNames(basePath).filter(loader.isLeftoverFile)

    if(!Files.exists(patchStatePath)) return {
      if(!Files.exists(basePath.resolve(loader.script.versionFrom))) PatchStatus.NeedsValidation
      else if(detectedPatchFiles.nonEmpty) PatchStatus.NeedsCleanup
      else PatchStatus.NotInstalled(isVersionKnown(loader.script.versionFrom))
    }

    loadPatchState() match {
      case Some(patchState) =>
        if((detectedPatchFiles.toSet -- patchState.expectedFiles).nonEmpty) PatchStatus.NeedsCleanup
        else {
          val originalFilesOK = patchState.renames.map(_.originalFile).forall(validatePatchFile)
          if(!patchState.renames.map(_.replacedFile).forall(x => Files.exists(basePath.resolve(x.path))) ||
             !originalFilesOK ||
             !patchState.nonreplacementFiles.forall(validatePatchFile)) {
            if(originalFilesOK) PatchStatus.FilesCorrupted else PatchStatus.NeedsValidation
          } else if(!patchState.renames.map(_.replacedFile).forall(validatePatchFile)) {
            PatchStatus.TargetUpdated
          } else loader.getNativePatch(patchState.sha256) match {
            case Some(version) =>
              if(patchState.installedTimestamp != loader.data.timestamp ||
                 patchState.installedVersion != loader.data.patchVersion)
                PatchStatus.NeedsUpdate
              else if(patchState.packages != packages) PatchStatus.PackageChange
              else PatchStatus.Installed
            case None => PatchStatus.CanUninstall
          }
        }
      case None => PatchStatus.NeedsCleanup
    }
  }

  private def installPatch(packages: Set[String]) = {
    val targetVersion = Crypto.sha256_hex(Files.readAllBytes(basePath.resolve(loader.script.versionFrom)))
    val packageLoader = loader.loadPackages(packages)
    val newFiles = packageLoader.getFiles(basePath, targetVersion)

    for(rename <- packageLoader.renames)
      Files.move(basePath.resolve(rename.filename), basePath.resolve(rename.renameTo))
    val patchNewFiles = for(OutputFile(name, data, executable) <- newFiles) yield {
      val path = basePath.resolve(name)
      val newDirs = for(parentName <- name.split("/").inits.toList.reverse.init.map(_.mkString("/"))) yield {
        val parentPath = basePath.resolve(parentName)
        if(!Files.exists(parentPath)) {
          Files.createDirectories(parentPath)
          Seq(parentName)
        } else Seq()
      }
      Files.write(path, data)
      if(executable) Files.setPosixFilePermissions(path,
        (Files.getPosixFilePermissions(path).asScala + OWNER_EXECUTE + GROUP_EXECUTE + OTHERS_EXECUTE).asJava)
      (PatchFile(name, Crypto.sha256_hex(data)), newDirs.flatten)
    }

    def patchFileFromPath(path: String) =
      PatchFile(path, Crypto.sha256_hex(Files.readAllBytes(basePath.resolve(path))))
    val renameData = for(rename <- packageLoader.renames) yield
      RenameData(patchFileFromPath(rename.filename), patchFileFromPath(rename.renameTo))

    val state = PatchState(loader.script.versionFrom, targetVersion, packages, renameData,
                           patchNewFiles.map(_._1), patchNewFiles.flatMap(_._2),
                           loader.data.patchVersion, loader.data.timestamp)
    IOUtils.writeXML(patchStatePath, PatchState.serialize(state))
  }

  private def uninstallPatch() = {
    loadPatchState() match {
      case None => sys.error("could not load patch state")
      case Some(patchState) =>
        for(RenameData(replacement, original) <- patchState.renames)
          if(validatePatchFile(original) && !validatePatchFile(replacement) &&
             Files.exists(basePath.resolve(replacement.path)) &&
             Files.isRegularFile(basePath.resolve(replacement.path))) {
            Files.delete(basePath.resolve(original.path))
            Files.move(basePath.resolve(replacement.path), basePath.resolve(original.path))
          }
        for(PatchFile(name, _) <- patchState.additionalFiles) Files.deleteIfExists(basePath.resolve(name))
        for(RenameData(replacement, original) <- patchState.renames)
          Files.move(basePath.resolve(original.path), basePath.resolve(replacement.path))
        for(name <- patchState.additionalDirectories.reverse) Files.deleteIfExists(basePath.resolve(name))
        Files.delete(patchStatePath)
    }
  }

  def checkPatchStatus(packages: Set[String]) = lock { intCheckPatchStatus(packages) }
  def cleanupPatch() = lock { }
  def safeUpdate(packages: Set[String]) = lock {
    intCheckPatchStatus(packages) match {
      case PatchStatus.Installed | PatchStatus.PackageChange | PatchStatus.NeedsUpdate |
           PatchStatus.FilesCorrupted =>
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
      case PatchStatus.Installed | PatchStatus.PackageChange | PatchStatus.NeedsUpdate |
           PatchStatus.CanUninstall | PatchStatus.TargetUpdated | PatchStatus.FilesCorrupted =>
        uninstallPatch()
      case _ => sys.error("cannot safely uninstall")
    }
  }
}