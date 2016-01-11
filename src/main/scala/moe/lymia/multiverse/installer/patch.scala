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

package moe.lymia.multiverse.installer

import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.{Files, Path}

import moe.lymia.multiverse.data.{PatchData, PatchVersion, PathNames}
import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.{IOUtils, Crypto, XMLUtils}

import scala.xml.{XML, Node}
import scala.collection.JavaConversions._

sealed trait PatchStatus
object PatchStatus {
  case class Installed(isDebug: Boolean) extends PatchStatus
  case object RequiresUpdate extends PatchStatus
  case object UnknownVersionInstalled extends PatchStatus
  case class NotInstalled(isKnownVersion: Boolean) extends PatchStatus

  case object StateCorrupted extends PatchStatus
  case object FatalIncompatibility extends PatchStatus
  case class TargetUpdated(knownVersion: Boolean) extends PatchStatus
  case class PatchCorrupted(fatal: Boolean, originalUpdated: Boolean,
                            originalVersionKnown: Boolean) extends PatchStatus
}

case class PatchFile(path: String, expectedSha1: String)
case class PatchState(patchVersionSha1: String, originalFileSha1: String,
                      replacementTarget: PatchFile, originalFile: PatchFile,
                      additionalFiles: Seq[PatchFile])
object PatchState {
  def serializePatchFile(file: PatchFile) =
    <PatchFile path={file.path} sha1={file.expectedSha1}/>
  def unserializePatchFile(node: Node) =
    PatchFile(XMLUtils.getAttribute(node, "path"), XMLUtils.getAttribute(node, "sha1"))

  def serialize(state: PatchState) =
    <PatchState stateVersion="1">
      <VersionInfo patchSha1={state.patchVersionSha1} originalSha1={state.originalFileSha1}/>
      <ReplacementTarget>{serializePatchFile(state.replacementTarget)}</ReplacementTarget>
      <OriginalFile>{serializePatchFile(state.originalFile)}</OriginalFile>
      <InstalledFile>{state.additionalFiles.map(serializePatchFile)}</InstalledFile>
    </PatchState>
  def unserialize(node: Node): Option[PatchState] = ??? // TODO Write unserialize code. Needs data validation.
}

case class PatchInstalledFile(name: String, data: Array[Byte], executable: Boolean = false)
trait PatchPlatformInfo {
  val replacementTarget: String
  def replacementNewName(versionName: String): String
  def patchInstallTarget(versionName: String): String

  def patchReplacementFile(versionName: String): Option[PatchInstalledFile]
  def additionalFiles(versionName: String): Seq[PatchInstalledFile]

  // TODO Installation sanity check so we can infer when we might have a damaged installation.
}

class PatchInstaller(basePath: Path, platform: Platform) {
  private def resolve(path: String) = basePath.resolve(path)

  val patchStatePath = resolve(PathNames.PATCH_STATE_FILENAME)
  val platformInfo   = platform.patchInfo

  private def validatePatchFile(file: PatchFile) = {
    val path = resolve(file.path)
    Files.exists(path) && Files.isRegularFile(path) && Crypto.sha1_hex(Files.readAllBytes(path)) == file.expectedSha1
  }
  private def isVersionKnown(path: String) =
    PatchVersion.exists(platform.platformName, Crypto.sha1_hex(Files.readAllBytes(resolve(path))))
  private def getVersion(path: String) =
    PatchVersion.get(platform.platformName, Crypto.sha1_hex(Files.readAllBytes(resolve(path))))
  private def loadPatchState() = try {
    PatchState.unserialize(XML.load(patchStatePath.toUri.toURL))
  } catch {
    case _: Exception => None
  }

  def checkPatchStatus() =
    if(Files.exists(patchStatePath)) loadPatchState() match {
      case Some(patchState) =>
        if(patchState.replacementTarget.path != platformInfo.replacementTarget)
          PatchStatus.FatalIncompatibility
        else {
          val originalFileStatus = validatePatchFile(patchState.originalFile)
          if(!patchState.additionalFiles.forall(validatePatchFile) || !originalFileStatus)
            PatchStatus.PatchCorrupted(originalFileStatus,
                                       validatePatchFile(patchState.replacementTarget),
                                       isVersionKnown(patchState.replacementTarget.path))
          else if(!validatePatchFile(patchState.replacementTarget))
            PatchStatus.TargetUpdated(isVersionKnown(patchState.replacementTarget.path))
          else PatchVersion.get(platform.platformName, patchState.originalFileSha1) match {
            case Some(version) =>
              if(version.patch.sha1 == patchState.patchVersionSha1)
                PatchStatus.Installed(isDebug = false)
              else if(version.debugPatch.sha1 == patchState.patchVersionSha1)
                PatchStatus.Installed(isDebug = true)
              else PatchStatus.RequiresUpdate
            case None => PatchStatus.UnknownVersionInstalled
          }
        }
      case None => PatchStatus.StateCorrupted
    } else PatchStatus.NotInstalled(isVersionKnown(platformInfo.replacementTarget))

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

      val state = PatchState(patchData.sha1, version.version, replacementTarget,
                             PatchFile(platformInfo.replacementNewName(version.version), version.version),
                             additional)
      IOUtils.writeXML(patchStatePath, PatchState.serialize(state))
  }

  private def uninstallPatch() = loadPatchState() match {
    case None => sys.error("could not load patch state")
    case Some(patchState) =>
      for(PatchFile(name, _) <- patchState.additionalFiles) Files.delete(resolve(name))
      Files.delete(resolve(patchState.replacementTarget.path))
      Files.move(resolve(patchState.originalFile.path), resolve(patchState.replacementTarget.path))
  }

  def safeUpdate(debug: Boolean) = checkPatchStatus() match {
    case PatchStatus.Installed(`debug`) =>
    case PatchStatus.Installed(_) | PatchStatus.RequiresUpdate | PatchStatus.TargetUpdated(true) =>
      uninstallPatch()
      installPatch(debug)
    case PatchStatus.NotInstalled(true) =>
      installPatch(debug)
    case _ => sys.error("cannot safely update")
  }
}