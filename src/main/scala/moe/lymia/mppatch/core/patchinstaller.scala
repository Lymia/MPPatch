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

import moe.lymia.mppatch.platform.Platform
import moe.lymia.mppatch.util.{Crypto, IOUtils, XMLUtils}

import scala.collection.JavaConversions._
import scala.xml.Node

object PathNames {
  val PATCH_STATE_FILENAME = "mppatch_install_state.xml"
  val PATCH_LOCK_FILENAME  = ".mppatch_install_lock"
}

sealed trait PatchStatus
object PatchStatus {
  case object Installed extends PatchStatus
  case object NeedsUpdate extends PatchStatus
  case class NotInstalled(isKnownVersion: Boolean) extends PatchStatus
  case class TargetUpdated(knownVersion: Boolean) extends PatchStatus

  case object NeedsCleanup extends PatchStatus
}

private case class PatchFile(path: String, expectedSha1: String)
private case class PatchState(patchVersionSha1: String, replacementTarget: PatchFile, originalFile: PatchFile,
                              additionalFiles: Seq[PatchFile],
                              installedVersion: String, installedTimestamp: Long,
                              dlcInstallPath: String, textDataInstallPath: String) {
  lazy val expectedFiles = additionalFiles.map(_.path).toSet + originalFile.path
}
private object PatchState {
  private val formatVersion = "0"

  def serializePatchFile(file: PatchFile) = <PatchFile path={file.path} expectedSha1={file.expectedSha1}/>
  def serialize(state: PatchState) = <PatchState version={formatVersion}>
    <PatchVersionSha1>{state.patchVersionSha1}</PatchVersionSha1>
    <ReplacementTarget>{serializePatchFile(state.replacementTarget)}</ReplacementTarget>
    <OriginalFile>{serializePatchFile(state.originalFile)}</OriginalFile>
    <AdditionalFiles>{state.additionalFiles.map(serializePatchFile)}</AdditionalFiles>
    <InstalledVersion>{state.installedVersion}</InstalledVersion>
    <InstalledTimestamp>{state.installedTimestamp}</InstalledTimestamp>
    <DLCInstallPath>{state.dlcInstallPath}</DLCInstallPath>
    <TextDataInstallPath>{state.textDataInstallPath}</TextDataInstallPath>
  </PatchState>

  def unserializePatchFile(node: Node) =
    PatchFile(XMLUtils.getAttribute(node, "path"), XMLUtils.getAttribute(node, "expectedSha1"))
  def unserialize(xml: Node) =
    if(XMLUtils.getAttribute(xml, "version") != formatVersion) None
    else Some(PatchState(XMLUtils.getNodeText(xml, "PatchVersionSha1"),
                         unserializePatchFile((xml \ "ReplacementTarget" \ "PatchFile").head),
                         unserializePatchFile((xml \ "OriginalFile"      \ "PatchFile").head),
                         (xml \ "AdditionalFiles" \ "PatchFile").map(unserializePatchFile),
                         XMLUtils.getNodeText(xml, "InstalledVersion"),
                         XMLUtils.getNodeText(xml, "InstalledTimestamp").toLong,
                         XMLUtils.getNodeText(xml, "DLCInstallPath"),
                         XMLUtils.getNodeText(xml, "TextDataInstallPath")))
}

case class PatchInstalledFile(name: String, data: Array[Byte], executable: Boolean = false)
trait PatchPlatformInfo {
  val replacementTarget: String
  def replacementNewName(versionName: String): String
  def patchInstallTarget(versionName: String): String

  def patchReplacementFile(versionName: String): Option[PatchInstalledFile]
  def additionalFiles(versionName: String): Seq[PatchInstalledFile]

  def findInstalledFiles(list: Seq[String]): Seq[String]
}

class PatchInstaller(val basePath: Path, loader: PatchLoader, platform: Platform, log: String => Unit = println) {
  def resolve(path: String) = basePath.resolve(path)

  private val patchStatePath     = resolve(PathNames.PATCH_STATE_FILENAME)
  private val patchLockPath      = resolve(PathNames.PATCH_LOCK_FILENAME)
  private val platformInfo       = platform.patchInfo

  private val dlcInstallPath     = "DLC/MpPatch"
  private val dlcTextPath        = "Gameplay/XML/NewText/mppatch_textdata.xml"

  private def lock[T](f: => T) = IOUtils.withLock(patchLockPath)(f)

  private def validatePatchFile(file: PatchFile) = {
    val path = resolve(file.path)
    Files.exists(path) && Files.isRegularFile(path) && Crypto.sha1_hex(Files.readAllBytes(path)) == file.expectedSha1
  }
  private def isVersionKnown(path: String) =
    loader.nativePatchExists(platform.platformName, Crypto.sha1_hex(Files.readAllBytes(resolve(path))))
  private def getVersion(path: String) =
    loader.getNativePatch(platform.platformName, Crypto.sha1_hex(Files.readAllBytes(resolve(path))))
  private def loadPatchState() = try {
    PatchState.unserialize(IOUtils.readXML(patchStatePath))
  } catch {
    case e: Exception =>
      System.err.println("Error encountered while deserializing patch state.")
      System.err.println()
      e.printStackTrace()

      None
  }

  def checkPatchStatus(): PatchStatus = {
    val detectedPatchFiles = platformInfo.findInstalledFiles(IOUtils.listFileNames(basePath))

    if(!Files.exists(patchStatePath)) return {
      if(!Files.exists(resolve(platformInfo.replacementTarget))) PatchStatus.NeedsCleanup
      else if(detectedPatchFiles.nonEmpty) PatchStatus.NeedsCleanup
      else PatchStatus.NotInstalled(isVersionKnown(platformInfo.replacementTarget))
    }

    loadPatchState() match {
      case Some(patchState) =>
        if((detectedPatchFiles.toSet -- patchState.expectedFiles).nonEmpty) PatchStatus.NeedsCleanup
        else if(patchState.replacementTarget.path != platformInfo.replacementTarget) PatchStatus.NeedsUpdate
        else {
          val originalFileStatus = validatePatchFile(patchState.originalFile)

          if(!Files.exists(resolve(patchState.replacementTarget.path)) ||
                  !patchState.additionalFiles.forall(validatePatchFile) || !originalFileStatus ||
                  !Files.exists(resolve(patchState.dlcInstallPath)) ||
                  !Files.exists(resolve(patchState.textDataInstallPath)))
            PatchStatus.NeedsCleanup
          else if(!validatePatchFile(patchState.replacementTarget))
            PatchStatus.TargetUpdated(isVersionKnown(patchState.replacementTarget.path))
          else loader.getNativePatch(platform.platformName, patchState.originalFile.expectedSha1) match {
            case Some(version) =>
              if(patchState.installedTimestamp != loader.data.timestamp ||
                 patchState.installedVersion != loader.data.patchVersion ||
                 version.sha1 != patchState.patchVersionSha1)
                PatchStatus.NeedsUpdate
              else PatchStatus.Installed
            case None => PatchStatus.NeedsCleanup
          }
        }
      case None => PatchStatus.NeedsCleanup
    }
  }

  private def installFile(file: PatchInstalledFile) = {
    val PatchInstalledFile(name, data, executable) = file
    val path = resolve(name)
    Files.write(path, data)
    if(executable) Files.setPosixFilePermissions(path,
      Files.getPosixFilePermissions(path) + OWNER_EXECUTE + GROUP_EXECUTE + OTHERS_EXECUTE)
    PatchFile(name, Crypto.sha1_hex(data))
  }
  private def installFromPatchData(target: String, patch: NativePatch) =
    PatchInstalledFile(target, loader.loadVersion(patch))

  // TODO: Make sure this is atomic enough
  private def installPatch() = getVersion(platformInfo.replacementTarget) match {
    case None => sys.error("attempt to install patch on unknown version")
    case Some(patchData) =>
      lock {
        val patchInstallTarget = resolve(platformInfo.replacementNewName(patchData.version))
        Files.move(resolve(platformInfo.replacementTarget), patchInstallTarget)
        val patchTarget = installFromPatchData(platformInfo.patchInstallTarget(patchData.version), patchData)
        val (replacementTarget, patchAdditional) = platformInfo.patchReplacementFile(patchData.version) match {
          case Some(replacementFile) =>
            assert(replacementFile.name != platformInfo.patchInstallTarget(patchData.version))
            assert(replacementFile.name == platformInfo.replacementTarget)
            (installFile(replacementFile), Seq(installFile(patchTarget)))
          case None =>
            assert(platformInfo.patchInstallTarget(patchData.version) == platformInfo.replacementTarget)
            (installFile(patchTarget), Seq())
        }
        val additional = patchAdditional ++ (
          for(file <- platformInfo.additionalFiles(patchData.version)) yield installFile(file)
        )

        val assets = resolve(platform.assetsPath)
        DLCDataWriter.writeDLC(assets.resolve(platform.mapPath(dlcInstallPath)),
                               Some(assets.resolve(platform.mapPath(dlcTextPath))),
                               MPPatchDLC.generateBaseDLC(basePath, loader, platform), platform)

        val state = PatchState(patchData.sha1, replacementTarget,
                               PatchFile(platformInfo.replacementNewName(patchData.version), patchData.version),
                               additional, loader.data.patchVersion, loader.data.timestamp,
                               platform.assetsPath + "/" + platform.mapPath(dlcInstallPath),
                               platform.assetsPath + "/" + platform.mapPath(dlcTextPath))
        IOUtils.writeXML(patchStatePath, PatchState.serialize(state))
      }
  }

  private def uninstallPatch(targetUpdated: Boolean, ignoreError: Boolean = false) = {
    def checkError[T](v: => T): Unit =
      if(ignoreError) try {
        v
      } catch {
        case e: Exception => log("Error while uninstalling patch: "+e.toString)
      } else v
    loadPatchState() match {
      case None =>
        if(!ignoreError) sys.error("could not load patch state")
      case Some(patchState) =>
        lock {
          for(PatchFile(name, _) <- patchState.additionalFiles) Files.delete(resolve(name))
          if(!targetUpdated) {
            checkError(IOUtils.deleteDirectory(resolve(patchState.replacementTarget.path)))
            checkError(Files.move(resolve(patchState.originalFile.path), resolve(patchState.replacementTarget.path)))
          } else {
            checkError(IOUtils.deleteDirectory(resolve(patchState.originalFile.path)))
          }
          checkError(IOUtils.deleteDirectory(resolve(patchState.textDataInstallPath)))
          checkError(IOUtils.deleteDirectory(resolve(patchState.dlcInstallPath)))
          checkError(Files.delete(patchStatePath))
        }
    }
  }

  def cleanupPatch() = {

  }

  def safeUpdate() = checkPatchStatus() match {
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
  def safeUninstall() = checkPatchStatus() match {
    case PatchStatus.NotInstalled(_) =>
      // do nothing
    case PatchStatus.Installed | PatchStatus.NeedsUpdate =>
      uninstallPatch(false)
    case PatchStatus.TargetUpdated(_) =>
      uninstallPatch(true)
    case _ => sys.error("cannot safely uninstall")
  }
}