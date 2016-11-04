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

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.regex.Pattern

import moe.lymia.mppatch.util.DataSource
import moe.lymia.mppatch.util.XMLUtils._

import scala.xml.{Node, XML}

case class RenameFile(filename: String, renameTo: String)
case class WriteConfig(filename: String, section: String)
case class AdditionalFile(filename: String, source: String, isExecutable: Boolean)
case class WriteDLC(source: String, target: String, textData: String)
case class Package(name: String, dependencies: Set[String],
                   renames: Seq[RenameFile], installBinary: Seq[String], writeConfig: Seq[WriteConfig],
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
            (xml \ "InstallBinary" ).map(loadFilename),
            (xml \ "WriteConfig"   ).map(x => WriteConfig(loadFilename(x), getAttribute(x, "Section"))),
            (xml \ "AdditionalFile").map(loadAdditionalFile),
            (xml \ "WriteDLC"      ).map(loadWriteDLC),
            (xml \ "SetFlag"       ).map(x => getAttribute(x, "Name")).toSet)
}

case class InstallScript(steamId: Int, assetsPath: String, checkFor: Set[String], packages: Map[String, Package],
                         leftoverFilter: Seq[String], versionFrom: String) {
  private lazy val leftoverRegex = leftoverFilter.map(x => Pattern.compile(x))
  def isLeftoverFile(x: String) = leftoverRegex.exists(_.matcher(x).matches())

  def loadPackage(name: String) = packages.getOrElse(name, sys.error("no such package "+name))
  @annotation.tailrec final def loadPackages(toLoad: Set[String], packages: Set[Package] = Set()): Set[Package] = {
    val loaded = packages.map(_.name)
    if(loaded == toLoad) packages
    else {
      val newPackages = (toLoad -- loaded).map(loadPackage)
      loadPackages(toLoad ++ newPackages.flatMap(_.dependencies), packages ++ newPackages)
    }
  }
}
object InstallScript {
  def loadFromXML(xml: Node) =
    InstallScript(getNodeText(xml, "SteamId").toInt,
                  (xml \ "AssetsPath"    ).map(loadFilename).head,
                  (xml \ "CheckFor"      ).map(loadFilename).toSet,
                  (xml \ "Package"       ).map(Package.loadFromXML).map(x => x.name -> x).toMap,
                  (xml \ "LeftoverFilter").map(x => getAttribute(x, "Regex")),
                  (xml \ "VersionFrom"   ).map(loadFilename).head)
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

case class OutputFile(filename: String, data: Array[Byte], isExecutable: Boolean = false)
case class PackageSetLoader(loader: PatchLoader, packages: Seq[Package]) {
  lazy val renames = packages.flatMap(_.renames)
  def getFiles(basePath: Path, versionName: String): Seq[OutputFile] = {
    val configFileBody =
      "; This file was automatically generated by the MPPatch installer. Do not edit it manually.\n"+
      packages.flatMap(_.setFlags).toSet.map((x: String) => s"$x=true").mkString("\n")
    lazy val nativePatch =
      loader.loadVersion(loader.getNativePatch(versionName).getOrElse(sys.error("Unknown version.")))

    val configFiles = packages.flatMap(_.writeConfig).map(x =>
      OutputFile(x.filename, s"[${x.section}]\n$configFileBody".getBytes(StandardCharsets.UTF_8)))
    val additionalFiles = packages.flatMap(_.additionalFile).map(x =>
      OutputFile(x.filename, loader.source.loadBinaryResource(x.source), x.isExecutable))
    val binaryFiles = packages.flatMap(_.installBinary).map(x => OutputFile(x, nativePatch))

    val assetPath = loader.script.assetsPath
    val assets = loader.platform.resolve(basePath, assetPath)
    val dlcFiles = packages.flatMap(_.writeDLC).flatMap { x =>
      val uiPatch = loader.loadUIPatch(x.source)
      val dlc = new UIPatchLoader(loader.source, uiPatch).generateBaseDLC(assets, loader.platform)
      val dlcMap = DLCDataWriter.writeDLC(s"$assetPath/${loader.platform.mapPath(x.target)}",
                                          Some(s"$assetPath/${loader.platform.mapPath(x.textData)}"),
                                          dlc, loader.platform)
      dlcMap.map(x => OutputFile(x._1, x._2))
    }

    dlcFiles ++ binaryFiles ++ additionalFiles ++ configFiles
  }
}
class PatchLoader(val source: DataSource, val platform: Platform) {
  lazy val data   = PatchManifest.loadFromXML(XML.loadString(source.loadResource("manifest.xml")))
  lazy val script = data.installScripts.get(platform.platformName).map(x =>
    InstallScript.loadFromXML(source.loadXML(x))).getOrElse(sys.error("Unknown platform"))

  def loadUIPatch(x: String) = UIPatch.loadFromXML(source.loadXML(x))
  def isLeftoverFile(x: String) = script.isLeftoverFile(x)
  def loadPackages(toLoad: Set[String]) = PackageSetLoader(this, script.loadPackages(toLoad).toSeq)

  private val versionMap = data.nativePatches.map(x => (x.platform, x.version) -> x).toMap
  def getNativePatch(versionName: String) =
    versionMap.get((platform.platformName, versionName))
  def nativePatchExists(versionName: String) =
    versionMap.contains((platform.platformName, versionName))
  def loadVersion(patch: NativePatch) = source.loadBinaryResource(patch.path)
}