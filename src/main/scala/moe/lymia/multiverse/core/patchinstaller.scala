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

package moe.lymia.multiverse.core

import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{Files, Path}

import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.res.{PatchData, PatchVersion, VersionInfo}
import moe.lymia.multiverse.util.{Crypto, IOUtils, XMLUtils}

import scala.collection.JavaConversions._
import scala.xml.Node

object PathNames {
  val PATCH_STATE_FILENAME = "mppatch_install_state.xml"
  val PATCH_LOCK_FILENAME  = ".mppatch_install_lock"
}

sealed trait PatchStatus
object PatchStatus {
  case class Installed(isDebug: Boolean) extends PatchStatus
  case object RequiresUpdate extends PatchStatus
  case object UnknownVersionInstalled extends PatchStatus
  case class NotInstalled(isKnownVersion: Boolean) extends PatchStatus

  case object TargetCorrupted extends PatchStatus
  case object FatalIncompatibility extends PatchStatus
  case class TargetUpdated(knownVersion: Boolean) extends PatchStatus
  case class PatchCorrupted(originalFileOK: Boolean) extends PatchStatus

  sealed trait StateError extends PatchStatus
  case object StateCorrupted extends StateError
  case object StateIsDirty extends StateError
  case object FoundLeftoverFiles extends StateError
}

private case class PatchFile(path: String, expectedSha1: String)
private case class PatchState(patchVersionSha1: String, replacementTarget: PatchFile, originalFile: PatchFile,
                              additionalFiles: Seq[PatchFile],
                              dlcUpdateVersion: Int, dlcInstallPath: String, textDataInstallPath: String) {
  lazy val expectedFiles = additionalFiles.map(_.path).toSet + originalFile.path
}
private object PatchState {
  private val formatVersion = s"mppatch-v${VersionInfo.versionString}"

  def serializePatchFile(file: PatchFile) = <PatchFile path={file.path} expectedSha1={file.expectedSha1}/>
  def serialize(state: PatchState) = <PatchState version={formatVersion}>
    <PatchVersionSha1>{state.patchVersionSha1}</PatchVersionSha1>
    <ReplacementTarget>{serializePatchFile(state.replacementTarget)}</ReplacementTarget>
    <OriginalFile>{serializePatchFile(state.originalFile)}</OriginalFile>
    <AdditionalFiles>{state.additionalFiles.map(serializePatchFile)}</AdditionalFiles>
    <DLCUpdateVersion>{state.dlcUpdateVersion}</DLCUpdateVersion>
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
                         XMLUtils.getNodeText(xml, "DLCUpdateVersion").toInt,
                         XMLUtils.getNodeText(xml, "DLCInstallPath"),
                         XMLUtils.getNodeText(xml, "TextDataInstallPath")))
}

sealed trait PatchActionStatus
object PatchActionStatus {
  case object CanLaunch   extends PatchActionStatus
  case object NeedsUpdate extends PatchActionStatus
  case object FatalError  extends PatchActionStatus
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

class PatchInstaller(basePath: Path, platform: Platform, log: String => Unit = println _) {
  private def resolve(path: String) = basePath.resolve(path)

  private val patchStatePath     = resolve(PathNames.PATCH_STATE_FILENAME)
  private val patchLockPath      = resolve(PathNames.PATCH_LOCK_FILENAME)
  private val platformInfo       = platform.patchInfo

  private val dlcInstallPath     = "DLC/MpPatch"
  private val dlcTextPath        = "Gameplay/XML/NewText/mppatch_textdata.xml"

  private def lock[T](f: => T) = {
    try {
      IOUtils.withLock(patchLockPath)(f)
    } finally {
      Files.deleteIfExists(patchLockPath)
    }
  }

  private def validatePatchFile(file: PatchFile) = {
    val path = resolve(file.path)
    Files.exists(path) && Files.isRegularFile(path) && Crypto.sha1_hex(Files.readAllBytes(path)) == file.expectedSha1
  }
  private def isVersionKnown(path: String) =
    PatchVersion.exists(platform.platformName, Crypto.sha1_hex(Files.readAllBytes(resolve(path))))
  private def getVersion(path: String) =
    PatchVersion.get(platform.platformName, Crypto.sha1_hex(Files.readAllBytes(resolve(path))))
  private def loadPatchState() = try {
    PatchState.unserialize(IOUtils.readXML(patchStatePath))
  } catch {
    case e: Exception =>
      System.err.println("Error encountered while deserializing patch state.")
      System.err.println()
      e.printStackTrace()

      None
  }

  def checkPatchStatus() = {
    val detectedPatchFiles = platformInfo.findInstalledFiles(IOUtils.listFileNames(basePath))

    if(Files.exists(patchLockPath)) PatchStatus.StateIsDirty
    else if(Files.exists(patchStatePath)) loadPatchState() match {
      case Some(patchState) =>
        if(patchState.replacementTarget.path != platformInfo.replacementTarget)
          PatchStatus.FatalIncompatibility
        else {
          val originalFileStatus = validatePatchFile(patchState.originalFile)

          if((detectedPatchFiles.toSet -- patchState.expectedFiles).nonEmpty)
            PatchStatus.FoundLeftoverFiles
          else if(!Files.exists(resolve(patchState.replacementTarget.path)) ||
                  !patchState.additionalFiles.forall(validatePatchFile) || !originalFileStatus ||
                  !Files.exists(resolve(patchState.dlcInstallPath)) ||
                  !Files.exists(resolve(patchState.textDataInstallPath)))
            PatchStatus.PatchCorrupted(originalFileStatus)
          else if(!validatePatchFile(patchState.replacementTarget))
            PatchStatus.TargetUpdated(isVersionKnown(patchState.replacementTarget.path))
          else PatchVersion.get(platform.platformName, patchState.originalFile.expectedSha1) match {
            case Some(version) =>
              if(patchState.dlcUpdateVersion != MPPatchDLC.DLC_UPDATEVERSION)
                PatchStatus.RequiresUpdate
              else if(version.patch.sha1 == patchState.patchVersionSha1)
                PatchStatus.Installed(isDebug = false)
              else if(version.debugPatch.sha1 == patchState.patchVersionSha1)
                PatchStatus.Installed(isDebug = true)
              else PatchStatus.RequiresUpdate
            case None => PatchStatus.UnknownVersionInstalled
          }
        }
      case None => PatchStatus.StateCorrupted
    } else if(!Files.exists(resolve(platformInfo.replacementTarget))) PatchStatus.TargetCorrupted
    else if(detectedPatchFiles.nonEmpty) PatchStatus.FoundLeftoverFiles
    else PatchStatus.NotInstalled(isVersionKnown(platformInfo.replacementTarget))
  }

  private def installFile(file: PatchInstalledFile) = {
    val PatchInstalledFile(name, data, executable) = file
    val path = resolve(name)
    Files.write(path, data)
    if(executable) Files.setPosixFilePermissions(path,
      Files.getPosixFilePermissions(path) + OWNER_EXECUTE + GROUP_EXECUTE + OTHERS_EXECUTE)
    PatchFile(name, Crypto.sha1_hex(data))
  }
  private def installFromPatchData(target: String, patch: PatchData) =
    PatchInstalledFile(target, patch.fileData)

  // TODO: Make sure this is atomic enough
  private def installPatch(debug: Boolean) = getVersion(platformInfo.replacementTarget) match {
    case None => sys.error("attempt to install patch on unknown version")
    case Some(version) =>
      val patchData = if(debug) version.debugPatch else version.patch

      lock {
        val patchInstallTarget = resolve(platformInfo.replacementNewName(version.version))
        Files.move(resolve(platformInfo.replacementTarget), patchInstallTarget)
        val patchTarget = installFromPatchData(platformInfo.patchInstallTarget(version.version), patchData)
        val (replacementTarget, patchAdditional) = platformInfo.patchReplacementFile(version.version) match {
          case Some(replacementFile) =>
            assert(replacementFile.name != platformInfo.patchInstallTarget(version.version))
            assert(replacementFile.name == platformInfo.replacementTarget)
            (installFile(replacementFile), Seq(installFile(patchTarget)))
          case None =>
            assert(platformInfo.patchInstallTarget(version.version) == platformInfo.replacementTarget)
            (installFile(patchTarget), Seq())
        }
        val additional = patchAdditional ++ (
          for(file <- platformInfo.additionalFiles(version.version)) yield installFile(file)
        )

        DLCDataWriter.writeDLC(resolve(platform.assetsPath).resolve(dlcInstallPath),
                               Some(resolve(platform.assetsPath).resolve(dlcTextPath)),
                               MPPatchDLC.generateBaseDLC(basePath, platform), platform)

        val state = PatchState(patchData.sha1, replacementTarget,
                               PatchFile(platformInfo.replacementNewName(version.version), version.version),
                               additional, MPPatchDLC.DLC_UPDATEVERSION, dlcInstallPath, dlcTextPath)
        IOUtils.writeXML(patchStatePath, PatchState.serialize(state))
      }
  }

  private def uninstallPatch(targetUpdated: Boolean) = loadPatchState() match {
    case None => sys.error("could not load patch state")
    case Some(patchState) =>
      lock {
        for(PatchFile(name, _) <- patchState.additionalFiles) Files.delete(resolve(name))
        if(!targetUpdated) {
          IOUtils.deleteDirectory(resolve(patchState.replacementTarget.path))
          Files.move(resolve(patchState.originalFile.path), resolve(patchState.replacementTarget.path))
        } else {
          IOUtils.deleteDirectory(resolve(patchState.originalFile.path))
        }
        IOUtils.deleteDirectory(resolve(patchState.textDataInstallPath))
        IOUtils.deleteDirectory(resolve(patchState.dlcInstallPath))
        Files.delete(patchStatePath)
      }
  }

  def actionStatus(debug: Boolean) = checkPatchStatus() match {
    case PatchStatus.Installed(`debug`) =>
      PatchActionStatus.CanLaunch
    case PatchStatus.Installed(_) | PatchStatus.RequiresUpdate | PatchStatus.NotInstalled(true) |
         PatchStatus.TargetUpdated(true) =>
      PatchActionStatus.NeedsUpdate
    case _ =>
      PatchActionStatus.FatalError
  }

  def safeUpdate(debug: Boolean) = checkPatchStatus() match {
    case PatchStatus.Installed(_) | PatchStatus.RequiresUpdate =>
      uninstallPatch(false)
      installPatch(debug)
    case PatchStatus.TargetUpdated(true) =>
      uninstallPatch(true)
      installPatch(debug)
    case PatchStatus.NotInstalled(true) =>
      installPatch(debug)
    case _ => sys.error("cannot safely update")
  }
  def safeUninstall() = checkPatchStatus() match {
    case PatchStatus.NotInstalled(_) =>
      // do nothing
    case PatchStatus.Installed(_) | PatchStatus.RequiresUpdate =>
      uninstallPatch(false)
    case PatchStatus.TargetUpdated(true) =>
      uninstallPatch(true)
    case _ => sys.error("cannot safely uninstall")
  }
}