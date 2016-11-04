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

import java.nio.file.Path
import java.util.regex.Pattern
import java.util.{Locale, UUID}

import moe.lymia.mppatch.util.{DataSource, IOUtils}
import moe.lymia.mppatch.util.XMLUtils._

import scala.xml.{Node, XML}

case class AdditionalFile(filename: String, source: String, isExecutable: Boolean)
case class InstallScript(replacementTarget: String, renameTo: String, patchTarget: String,
                         additionalFiles: Seq[AdditionalFile], leftoverFilter: Seq[String]) {
  lazy val leftoverRegex = leftoverFilter.map(x => Pattern.compile(x))
  def isLeftoverFile(x: String) = leftoverRegex.exists(_.matcher(x).matches())
}
object InstallScript {
  def loadAdditionalFile(xml: Node) =
    AdditionalFile(loadFilename(xml), getAttribute(xml, "Source"), getBoolAttribute(xml, "SetExecutable"))
  def loadFromXML(xml: Node) =
    InstallScript(loadFilename((xml \ "ReplacementTarget").head),
                  loadFilename((xml \ "RenameTo"         ).head),
                  loadFilename((xml \ "InstallBinary"    ).head),
                  (xml \ "AdditionalFile").map(loadAdditionalFile),
                  (xml \ "LeftoverFilter").map(x => getAttribute(x, "Regex")))
}

case class NativePatch(platform: String, version: String, path: String)
case class PatchManifest(patchVersion: String, timestamp: Long, uiPatch: String,
                         nativePatches: Seq[NativePatch], installScripts: Map[String, String])
object PatchManifest {
  def loadNativePatch(node: Node) =
    NativePatch(getAttribute(node, "Platform"), getAttribute(node, "Version"), getAttribute(node, "Filename"))
  def loadInstallScript(node: Node) =
    getAttribute(node, "Platform") -> loadFilename(node)
  def loadFromXML(xml: Node) = {
    val manifestVersion = getAttribute(xml, "ManifestVersion")
    if(manifestVersion != "0") sys.error("Unknown ManifestVersion: "+manifestVersion)
    PatchManifest(getAttribute(xml, "PatchVersion"), getAttribute(xml, "Timestamp").toLong,
                  (xml \ "UIPatch"      ).map(loadFilename).head,
                  (xml \ "NativePatch"  ).map(loadNativePatch),
                  (xml \ "InstallScript").map(loadInstallScript).toMap)
  }
}

class PatchLoader(val source: DataSource) {
  val data  = PatchManifest.loadFromXML(XML.loadString(source.loadResource("manifest.xml")))
  val patch = UIPatch.loadFromXML(XML.loadString(source.loadResource(data.uiPatch)))

  def loadInstallScript(name: String) =
    data.installScripts.get(name).map(x => InstallScript.loadFromXML(XML.loadString(source.loadResource(x))))

  val versionMap = data.nativePatches.map(x => (x.platform, x.version) -> x).toMap
  def getNativePatch(targetPlatform: String, versionName: String) =
    versionMap.get((targetPlatform, versionName))
  def nativePatchExists(targetPlatform: String, versionName: String) =
    versionMap.contains((targetPlatform, versionName))
  def loadVersion(patch: NativePatch) = source.loadBinaryResource(patch.path)
}