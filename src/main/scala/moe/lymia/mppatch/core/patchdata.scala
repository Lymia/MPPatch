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

import java.util.regex.Pattern

import moe.lymia.mppatch.util.DataSource
import moe.lymia.mppatch.util.XMLUtils._

import scala.xml.{Node, XML}

case class RenameFile(filename: String, renameTo: String)
case class InstallBinary(filename: String, checkVersion: String)
case class WriteConfig(filename: String, section: String)
case class AdditionalFile(filename: String, source: String, isExecutable: Boolean)
case class WriteDLC(source: String, target: String, textData: String)
case class Package(name: String, dependencies: Set[String],
                   renames: Seq[RenameFile], installBinary: Seq[InstallBinary], writeConfig: Seq[WriteConfig],
                   additionalFile: Seq[AdditionalFile], writeDLC: Seq[WriteDLC], setFlags: Set[String])
object Package {
  def loadAdditionalFile(xml: Node) =
    AdditionalFile(loadFilename(xml), getAttribute(xml, "Source"), getBoolAttribute(xml, "SetExecutable"))
  def loadWriteDLC(xml: Node) =
    WriteDLC(getAttribute(xml, "Source"), getAttribute(xml, "Target"), getAttribute(xml, "TextData"))
  def loadFromXML(xml: Node) =
    Package(getAttribute(xml, "Name"),
            getOptionalAttribute(xml, "Depends").fold(Set[String]())(_.split(",").toSet),
            (xml \ "RenameFile"    ).map(x => RenameFile(loadFilename(x), getAttribute(x, "RenameTo"))),
            (xml \ "InstallBinary" ).map(x => InstallBinary(loadFilename(xml), getAttribute(xml, "CheckVersion"))),
            (xml \ "WriteConfig"   ).map(x => WriteConfig(loadFilename(xml), getAttribute(xml, "Section"))),
            (xml \ "AdditionalFile").map(loadAdditionalFile),
            (xml \ "WriteDLC"      ).map(loadWriteDLC),
            (xml \ "SetFlag"       ).map(x => getAttribute(x, "Name")).toSet)
}

case class PackageSet(packages: Set[Package]) {
  lazy val renames        = packages.flatMap(_.renames       )
  lazy val installBianry  = packages.flatMap(_.installBinary )
  lazy val writeConfig    = packages.flatMap(_.writeConfig   )
  lazy val additionalFile = packages.flatMap(_.additionalFile)
  lazy val writeDLC       = packages.flatMap(_.writeDLC      )
  lazy val setFlags       = packages.flatMap(_.setFlags      )
}
case class InstallScript(steamId: Int, assetsPath: String, checkFor: Set[String], packages: Map[String, Package],
                         leftoverFilter: Seq[String]) {
  private lazy val leftoverRegex = leftoverFilter.map(x => Pattern.compile(x))
  def isLeftoverFile(x: String) = leftoverRegex.exists(_.matcher(x).matches())

  def loadPackage(name: String) = packages.getOrElse(name, sys.error("no such package "+name))
  @annotation.tailrec final def loadPackages(toLoad: Set[String], packages: Set[Package] = Set()): PackageSet = {
    val loaded = packages.map(_.name)
    if(loaded == toLoad) PackageSet(packages)
    else {
      val newPackages = (toLoad -- loaded).map(loadPackage)
      loadPackages(toLoad ++ newPackages.flatMap(_.dependencies), packages ++ newPackages)
    }
  }
}
object InstallScript {
  def loadFromXML(xml: Node) =
    InstallScript(getNodeText(xml, "SteamId").toInt,
                  (xml \ "AssetsPath").map(loadFilename).head,
                  (xml \ "CheckFor").map(loadFilename).toSet,
                  (xml \ "Package").map(Package.loadFromXML).map(x => x.name -> x).toMap,
                  (xml \ "LeftoverFilter").map(x => getAttribute(x, "Regex")))
}

case class NativePatch(platform: String, version: String, path: String)
case class PatchManifest(patchVersion: String, timestamp: Long,
                         nativePatches: Seq[NativePatch], installScripts: Map[String, String])
object PatchManifest {
  def loadNativePatch(xml: Node) =
    NativePatch(getAttribute(xml, "Platform"), getAttribute(xml, "Version"), getAttribute(xml, "Filename"))
  def loadFromXML(xml: Node) = {
    val manifestVersion = getAttribute(xml, "ManifestVersion")
    if(manifestVersion != "0") sys.error("Unknown ManifestVersion: "+manifestVersion)
    PatchManifest(getAttribute(xml, "PatchVersion"), getAttribute(xml, "Timestamp").toLong,
                  (xml \ "NativePatch"  ).map(loadNativePatch),
                  (xml \ "InstallScript").map(x => getAttribute(x, "Platform") -> loadFilename(x)).toMap)
  }
}

class PatchLoader(val source: DataSource, platform: Platform) {
  lazy val data   = PatchManifest.loadFromXML(XML.loadString(source.loadResource("manifest.xml")))
  lazy val script =
    data.installScripts.get(platform.platformName).map(x => InstallScript.loadFromXML(source.loadXML(x)))

  private val versionMap = data.nativePatches.map(x => (x.platform, x.version) -> x).toMap
  def getNativePatch(targetPlatform: String, versionName: String) =
    versionMap.get((targetPlatform, versionName))
  def nativePatchExists(targetPlatform: String, versionName: String) =
    versionMap.contains((targetPlatform, versionName))
  def loadVersion(patch: NativePatch) = source.loadBinaryResource(patch.path)
}