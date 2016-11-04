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

  val dlcInstallPath     = "DLC/MpPatch"
  val dlcTextPath        = "Gameplay/XML/NewText/mppatch_textdata.xml"
}

sealed trait PatchStatus
object PatchStatus {
  case class NotInstalled(isKnownVersion: Boolean) extends PatchStatus
  case class TargetUpdated(isKnownVersion: Boolean) extends PatchStatus

  case object Installed       extends PatchStatus
  case object CanUninstall    extends PatchStatus
  case object NeedsUpdate     extends PatchStatus
  case object NeedsRepair     extends PatchStatus
  case object NeedsCleanup    extends PatchStatus
  case object NeedsValidation extends PatchStatus
}

private case class PatchFile(path: String, expectedSha256: String)
private case class PatchState(replacementTarget: PatchFile, originalFile: PatchFile,
                              additionalFiles: Seq[PatchFile],
                              installedVersion: String, installedTimestamp: Long,
                              dlcInstallPath: String, textDataInstallPath: String) {
  lazy val expectedFiles = additionalFiles.map(_.path).toSet + originalFile.path
}
private object PatchState {
  private val formatVersion = "0"

  def serializePatchFile(file: PatchFile) = <PatchFile path={file.path} expectedSha256={file.expectedSha256}/>
  def serialize(state: PatchState) = <PatchState version={formatVersion}>
    <ReplacementTarget>{serializePatchFile(state.replacementTarget)}</ReplacementTarget>
    <OriginalFile>{serializePatchFile(state.originalFile)}</OriginalFile>
    <AdditionalFiles>{state.additionalFiles.map(serializePatchFile)}</AdditionalFiles>
    <InstalledVersion>{state.installedVersion}</InstalledVersion>
    <InstalledTimestamp>{state.installedTimestamp}</InstalledTimestamp>
    <DLCInstallPath>{state.dlcInstallPath}</DLCInstallPath>
    <TextDataInstallPath>{state.textDataInstallPath}</TextDataInstallPath>
  </PatchState>

  def unserializePatchFile(node: Node) =
    PatchFile(XMLUtils.getAttribute(node, "path"), XMLUtils.getAttribute(node, "expectedSha256"))
  def unserialize(xml: Node) =
    if(XMLUtils.getAttribute(xml, "version") != formatVersion) None
    else Some(PatchState(unserializePatchFile((xml \ "ReplacementTarget" \ "PatchFile").head),
                         unserializePatchFile((xml \ "OriginalFile"      \ "PatchFile").head),
                         (xml \ "AdditionalFiles" \ "PatchFile").map(unserializePatchFile),
                         XMLUtils.getNodeText(xml, "InstalledVersion"),
                         XMLUtils.getNodeText(xml, "InstalledTimestamp").toLong,
                         XMLUtils.getNodeText(xml, "DLCInstallPath"),
                         XMLUtils.getNodeText(xml, "TextDataInstallPath")))
}

class PatchInstaller(val basePath: Path, val loader: PatchLoader, platform: Platform, log: String => Unit = println) {
  import PathNames._

  private def resolve(path: String) = basePath.resolve(path)

  private val patchStatePath = resolve(patchStateFilename)
  private val patchLockPath  = resolve(patchLockFilename)
  private val installScript  = loader.loadInstallScript(platform.platformName).get

  private val syncLock = new Object
  private var fileLock: FileLock = _
  private var manualLock = false

  def isLockAcquired = syncLock synchronized {
    fileLock != null
  }
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
    val path = resolve(file.path)
    Files.exists(path) && Files.isRegularFile(path) &&
      Crypto.sha256_hex(Files.readAllBytes(path)) == file.expectedSha256
  }
  private def isVersionKnown(path: String) =
    loader.nativePatchExists(platform.platformName, Crypto.sha256_hex(Files.readAllBytes(resolve(path))))
  private def getVersion(path: String) =
    loader.getNativePatch(platform.platformName, Crypto.sha256_hex(Files.readAllBytes(resolve(path))))
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

  private def intCheckPatchStatus(): PatchStatus = {
    val detectedPatchFiles = IOUtils.listFileNames(basePath).filter(installScript.isLeftoverFile)

    if(!Files.exists(patchStatePath)) return {
      if(!Files.exists(resolve(installScript.replacementTarget))) PatchStatus.NeedsValidation
      else if(detectedPatchFiles.nonEmpty) PatchStatus.NeedsCleanup
      else PatchStatus.NotInstalled(isVersionKnown(installScript.replacementTarget))
    }

    loadPatchState() match {
      case Some(patchState) =>
        if((detectedPatchFiles.toSet -- patchState.expectedFiles).nonEmpty) PatchStatus.NeedsCleanup
        else {
          if(!Files.exists(resolve(patchState.replacementTarget.path)) ||
             !patchState.additionalFiles.forall(validatePatchFile) ||
             !validatePatchFile(patchState.originalFile) ||
             !Files.exists(resolve(patchState.dlcInstallPath)) ||
             !Files.exists(resolve(patchState.textDataInstallPath)))
            PatchStatus.NeedsCleanup
          else if(!validatePatchFile(patchState.replacementTarget))
            PatchStatus.TargetUpdated(isVersionKnown(patchState.replacementTarget.path))
          else loader.getNativePatch(platform.platformName, patchState.originalFile.expectedSha256) match {
            case Some(version) =>
              if(patchState.installedTimestamp != loader.data.timestamp ||
                 patchState.installedVersion != loader.data.patchVersion ||
                 patchState.replacementTarget.path != installScript.replacementTarget)
                PatchStatus.NeedsUpdate
              else PatchStatus.Installed
            case None => PatchStatus.CanUninstall
          }
        }
      case None => PatchStatus.NeedsCleanup
    }
  }

  private def installFile(name: String, data: Array[Byte], executable: Boolean = false) = {
    val path = resolve(name)
    Files.write(path, data)
    if(executable) Files.setPosixFilePermissions(path,
      (Files.getPosixFilePermissions(path).asScala + OWNER_EXECUTE + GROUP_EXECUTE + OTHERS_EXECUTE).asJava)
    PatchFile(name, Crypto.sha256_hex(data))
  }

  // TODO: Make sure this is atomic enough
  private def installPatch() = getVersion(installScript.replacementTarget) match {
    case None => sys.error("attempt to install patch on unknown version")
    case Some(patchData) =>
      val patchInstallTarget = resolve(installScript.renameTo)
      Files.move(resolve(installScript.replacementTarget), patchInstallTarget)
      val patchTarget = installFile(installScript.patchTarget, loader.loadVersion(patchData))
      val additionalMap = installScript.additionalFiles.map { file =>
        file.filename -> installFile(file.filename, loader.source.loadBinaryResource(file.source), file.isExecutable)
      }.toMap

      val assets = resolve(platform.assetsPath)
      DLCDataWriter.writeDLC(assets.resolve(platform.mapPath(dlcInstallPath)),
                             Some(assets.resolve(platform.mapPath(dlcTextPath))),
                             PatchDLCGenerator.generateBaseDLC(basePath, loader, platform), platform)

      val state = PatchState(if(installScript.patchTarget == installScript.replacementTarget) patchTarget
                             else additionalMap(installScript.replacementTarget),
                             PatchFile(installScript.renameTo, patchData.version),
                             additionalMap.values.toSeq :+ patchTarget,
                             loader.data.patchVersion, loader.data.timestamp,
                             platform.assetsPath + "/" + platform.mapPath(dlcInstallPath),
                             platform.assetsPath + "/" + platform.mapPath(dlcTextPath))
      IOUtils.writeXML(patchStatePath, PatchState.serialize(state))
  }

  private def uninstallPatch(targetUpdated: Boolean) = {

    loadPatchState() match {
      case None => sys.error("could not load patch state")
      case Some(patchState) =>
        if(targetUpdated) {
          Files.delete(resolve(patchState.originalFile.path))
          Files.move(resolve(patchState.replacementTarget.path), resolve(patchState.originalFile.path))
        }
        for(PatchFile(name, _) <- patchState.additionalFiles) Files.deleteIfExists(resolve(name))

        Files.deleteIfExists(resolve(patchState.replacementTarget.path))
        Files.move(resolve(patchState.originalFile.path), resolve(patchState.replacementTarget.path))

        IOUtils.deleteDirectory(resolve(patchState.dlcInstallPath))
        IOUtils.deleteDirectory(resolve(patchState.textDataInstallPath))
        Files.delete(patchStatePath)
    }
  }

  def checkPatchStatus() = lock { intCheckPatchStatus() }
  def cleanupPatch() = lock { }
  def safeUpdate() = lock {
    intCheckPatchStatus() match {
      case PatchStatus.Installed | PatchStatus.NeedsUpdate =>
        uninstallPatch(false)
        installPatch()
      case PatchStatus.TargetUpdated(true) =>
        uninstallPatch(true)
        installPatch()
      case PatchStatus.NotInstalled(true) =>
        installPatch()
      case _ => sys.error("cannot safely update")
    }
  }
  def safeUninstall() = lock {
    intCheckPatchStatus() match {
      case PatchStatus.NotInstalled(_) =>
        // do nothing
      case PatchStatus.Installed | PatchStatus.NeedsUpdate =>
        uninstallPatch(false)
      case PatchStatus.TargetUpdated(_) =>
        uninstallPatch(true)
      case _ => sys.error("cannot safely uninstall")
    }
  }
}