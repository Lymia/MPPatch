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

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Files
import moe.lymia.mppatch.util.io.*
import moe.lymia.mppatch.util.io.XMLUtils.*

import java.io.File
import scala.xml.{Node, XML}

case class XmlPackage(
    name: String,
    dependencies: Set[String],
    renames: Seq[XmlRenameFile],
    installBinary: Seq[String],
    writeConfig: Seq[XmlWriteConfig],
    additionalFile: Seq[XmlAdditionalFile],
    writeDLC: Seq[XmlWriteDLC],
    setFlags: Set[String]
)
object XmlPackage {
  def loadAdditionalFile(xml: Node) =
    XmlAdditionalFile(loadPath(xml), loadSource(xml), getBoolAttribute(xml, "SetExecutable"))
  def loadWriteDLC(xml: Node) =
    XmlWriteDLC(loadSource(xml), getAttribute(xml, "DLCData"), getAttribute(xml, "TextData"))
  def loadFromXML(xml: Node) =
    XmlPackage(
      getAttribute(xml, "Name"),
      getOptionalAttribute(xml, "Depends").fold(Set[String]())(_.split(",").toSet),
      (xml \ "RenameFile").map(XmlRenameFile.loadFromXML),
      (xml \ "InstallBinary").map(loadPath),
      (xml \ "WriteConfig").map(x => XmlWriteConfig(loadPath(x), getAttribute(x, "Section"))),
      (xml \ "AdditionalFile").map(loadAdditionalFile),
      (xml \ "WriteDLC").map(loadWriteDLC),
      (xml \ "SetFlag").map(x => getAttribute(x, "Name")).toSet
    )
}

case class XmlRenameFile(filename: String, renameTo: String)
object XmlRenameFile {
  def loadFromXML(xml: Node) = XmlRenameFile(loadPath(xml), getAttribute(xml, "RenameTo"))
}

case class XmlWriteConfig(filename: String, section: String)
case class XmlAdditionalFile(filename: String, source: String, isExecutable: Boolean)
case class XmlWriteDLC(source: String, target: String, textData: String)

case class XmlCleanupData(rename: Seq[XmlRenameFile], checkFile: Seq[String])
object XmlCleanupData {
  def loadFromXML(xml: Node) =
    XmlCleanupData((xml \ "RenameIfExists").map(XmlRenameFile.loadFromXML), (xml \ "CheckFile").map(loadPath))
}

case class XmlInstallScript(
    steamId: Int,
    assetsPath: String,
    checkFor: Set[String],
    packages: Map[String, XmlPackage],
    cleanupData: XmlCleanupData,
    versionFrom: String
) {
  def loadPackage(name: String) = packages.getOrElse(name, sys.error("no such package " + name))

  @annotation.tailrec
  final def loadPackages(toLoad: Set[String], packages: Set[XmlPackage] = Set()): Set[XmlPackage] = {
    val loaded = packages.map(_.name)
    if (loaded == toLoad) packages
    else {
      val newPackages = (toLoad -- loaded).map(loadPackage)
      loadPackages(toLoad ++ newPackages.flatMap(_.dependencies), packages ++ newPackages)
    }
  }
}
object XmlInstallScript {
  def loadFromXML(xml: Node) =
    XmlInstallScript(
      getNodeText(xml, "SteamId").toInt,
      (xml \ "AssetsPath").map(loadPath).head,
      (xml \ "CheckFor").map(loadPath).toSet,
      (xml \ "Package").map(XmlPackage.loadFromXML).map(x => x.name -> x).toMap,
      (xml \ "Cleanup").map(XmlCleanupData.loadFromXML).head,
      (xml \ "VersionFrom").map(loadPath).head
    )
}

case class XmlPatchManifest(
    patchVersion: String,
    timestamp: Long,
    nativePatches: Seq[XmlNativePatch],
    installScripts: Map[String, String]
)
object XmlPatchManifest {
  def loadNativePatch(xml: Node) =
    XmlNativePatch(getAttribute(xml, "Platform"), getAttribute(xml, "Sha256"), loadSource(xml))
  def loadFromXML(xml: Node) = {
    val manifestVersion = getAttribute(xml, "ManifestVersion")
    if (manifestVersion != "0") sys.error("Unknown ManifestVersion: " + manifestVersion)
    XmlPatchManifest(
      getAttribute(xml, "PatchVersion"),
      getAttribute(xml, "Timestamp").toLong,
      (xml \ "NativePatch").map(loadNativePatch),
      (xml \ "InstallScript").map(x => getAttribute(x, "Platform") -> loadSource(x)).toMap
    )
  }
}

case class XmlNativePatch(platform: String, sha256: String, source: String)

/** Main loader for the game's patch packages.
  *
  * @param source
  *   Where to load patch from.
  */
class PatchPackage(val source: DataSource) {

  /** The manifest for this patch. */
  lazy val patchManifest = XmlPatchManifest.loadFromXML(XML.loadString(source.loadResource("manifest.xml")))

  /** The list of install scripts available in this package. */
  lazy val scripts =
    patchManifest.installScripts.view.mapValues(x => XmlInstallScript.loadFromXML(source.loadXML(x))).toMap

  /** Detects the platform a Civilization installation directory is for. */
  def detectInstallationPlatform(root: Path): Option[InstallScript] = {
    val possible = for (
      (platformStr, script) <- scripts;
      platform              <- Platform.forName(platformStr)
      if script.checkFor.forall(x => Files.exists(root.resolve(x)))
    ) yield new InstallScript(this, platform, script)
    possible.headOption
  }

  /** Loads a UI patch from the package. */
  def loadUIPatch(x: String) = UIPatch.loadFromXML(source.loadXML(x))

  private[core] val versionMap         = patchManifest.nativePatches.map(x => (x.platform, x.sha256) -> x).toMap
  def loadPatch(patch: XmlNativePatch) = source.loadBinaryResource(patch.source)
}

/** A specific installation script for a specific version of Civilization. */
class InstallScript private[core] (val pkg: PatchPackage, val platform: Platform, val script: XmlInstallScript) {
  lazy val source        = pkg.source
  lazy val patchManifest = pkg.patchManifest
  lazy val cleanup       = script.cleanupData

  def nativePatchForHash(versionName: String) =
    pkg.versionMap.get((platform.platformName, versionName))

  def nativePatchExists(versionName: String) = nativePatchForHash(versionName).isDefined

  def makeFileSet(packages: Set[String]) = InstallFileSet(this, script.loadPackages(packages).toSeq)
}

/** A set of files which to be added to a Civilization directory. */
case class InstallFileSet private[core] (script: InstallScript, packages: Seq[XmlPackage]) {
  lazy val renames = packages.flatMap(_.renames)
  def getFiles(basePath: Path, versionName: String): Seq[OutputFile] = {
    val configFileBody =
      "; This file was automatically generated by the MPPatch installer. Do not edit it manually.\n" +
        packages.flatMap(_.setFlags).toSet.map((x: String) => s"$x=true").mkString("\n")
    lazy val nativePatch =
      script.pkg.loadPatch(script.nativePatchForHash(versionName).getOrElse(sys.error("Unknown version.")))

    val configFiles = packages
      .flatMap(_.writeConfig)
      .map(x => OutputFile(x.filename, s"[${x.section}]\n$configFileBody".getBytes(StandardCharsets.UTF_8)))
    val additionalFiles = packages
      .flatMap(_.additionalFile)
      .map(x => OutputFile(x.filename, script.source.loadBinaryResource(x.source), x.isExecutable))
    val binaryFiles = packages.flatMap(_.installBinary).map(x => OutputFile(x, nativePatch))

    val assetPath = script.script.assetsPath
    val assets    = script.platform.resolve(basePath, assetPath)
    val dlcFiles = packages.flatMap(_.writeDLC).flatMap { x =>
      val uiPatch = script.pkg.loadUIPatch(x.source)
      val dlc     = new UIPatchLoader(script.source, uiPatch).generateBaseDLC(assets, script.platform)
      val dlcMap = DLCDataWriter.writeDLC(
        s"$assetPath/${script.platform.mapPath(x.target)}",
        Some(s"$assetPath/${script.platform.mapPath(x.textData)}"),
        dlc,
        script.platform
      )
      dlcMap.map(x => OutputFile(x._1, x._2))
    }

    dlcFiles ++ binaryFiles ++ additionalFiles ++ configFiles
  }
}
case class OutputFile(filename: String, data: Array[Byte], isExecutable: Boolean = false)
