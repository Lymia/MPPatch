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

import java.nio.file.{Files, Path}

import moe.lymia.multiverse.data.{PatchVersion, PathNames}
import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.{Crypto, XMLUtils}

import scala.xml.{XML, Node}

sealed trait PatchStatus
object PatchStatus {
  case class Installed(isDebug: Boolean) extends PatchStatus
  case object RequiresUpdate extends PatchStatus
  case object UnknownVersionInstalled extends PatchStatus
  case class NotInstalled(canInstall: Boolean) extends PatchStatus

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

trait PatchPlatformInfo {
  val replacementTarget: String
  def replacementNewName(versionName: String): String
  def patchInstallTarget(versionName: String): String
  def additionalFiles(versionName: String): Seq[(String, Array[Byte])]

  // TODO Installation sanity check so we can infer when we might have a damaged installation.
}

class PatchInstaller(basePath: Path, platform: Platform) {
  val patchStatePath = platform.resolve(basePath, PathNames.PATCH_STATE_FILENAME)
  val platformInfo   = platform.patchInfo

  private def validatePatchFile(file: PatchFile) = {
    val path = platform.resolve(basePath, file.path)
    Files.exists(path) && Files.isRegularFile(path) && Crypto.sha1_hex(Files.readAllBytes(path)) == file.expectedSha1
  }
  private def isVersionKnown(path: String) =
    PatchVersion.exists(platform.platformName, Crypto.sha1_hex(Files.readAllBytes(platform.resolve(basePath, path))))
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
                PatchStatus.Installed(false)
              else if(version.debugPatch.sha1 == patchState.patchVersionSha1)
                PatchStatus.Installed(true)
              else PatchStatus.RequiresUpdate
            case None => PatchStatus.UnknownVersionInstalled
          }
        }
      case None => PatchStatus.StateCorrupted
    } else PatchStatus.NotInstalled(isVersionKnown(platformInfo.replacementTarget))
}