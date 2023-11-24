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

import moe.lymia.mppatch.util.io.*
import moe.lymia.mppatch.util.io.XMLUtils.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.xml.{Node, XML}

case class XmlPatchManifest(
    patchVersion: String,
    timestamp: Long,
    installScripts: Seq[String]
)
object XmlPatchManifest {
  def loadFromXML(xml: Node) = {
    val manifestVersion = getAttribute(xml, "ManifestVersion")
    if (manifestVersion != "1") sys.error("Unknown ManifestVersion: " + manifestVersion)
    XmlPatchManifest(
      getAttribute(xml, "PatchVersion"),
      getAttribute(xml, "Timestamp").toLong,
      (xml \ "InstallScript").map(loadSource)
    )
  }
}

case class XmlInstallScript(
    platform: String,
    steamId: Int,
    assetsPath: String,
    checkFor: Set[String],
    hashFrom: String,
    supportedHashes: Set[String],
    packages: Map[String, XmlPackage],
    cleanupData: XmlCleanupData
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
      getAttribute(xml, "Platform"),
      getNodeText(xml, "SteamId").toInt,
      (xml \ "AssetsPath").map(loadPath).head,
      (xml \ "CheckFor").map(loadPath).toSet,
      (xml \ "HashFrom").map(loadPath).head,
      (xml \ "SupportedHash").map(loadHash).toSet,
      (xml \ "Package").map(XmlPackage.loadFromXML).map(x => x.name -> x).toMap,
      (xml \ "Cleanup").map(XmlCleanupData.loadFromXML).head
    )
}

case class XmlPackage(
    name: String,
    dependencies: Set[String],
    renames: Seq[XmlRenameFile],
    writeConfig: Seq[String],
    additionalFile: Seq[XmlAdditionalFile],
    writeDLC: Seq[XmlWriteDLC],
    enableFeature: Set[String]
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
      (xml \ "WriteConfig").map(loadPath),
      (xml \ "AdditionalFile").map(loadAdditionalFile),
      (xml \ "WriteDLC").map(loadWriteDLC),
      (xml \ "EnableFeature").map(x => getAttribute(x, "Name")).toSet
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

/** Main loader for the game's patch packages.
  *
  * @param source
  *   Where to load patch from.
  */
class PatchPackage(val source: DataSource) {

  /** The manifest for this patch. */
  lazy val patchManifest = XmlPatchManifest.loadFromXML(XML.loadString(source.loadResource("manifest.xml")))

  /** The list of install scripts available in this package. */
  lazy val scripts = patchManifest.installScripts.map(x => XmlInstallScript.loadFromXML(source.loadXML(x)))

  /** Detects the platform a Civilization installation directory is for. */
  def detectInstallationPlatform(root: Path): Option[InstallScript] = {
    val possible = for (
      script <- scripts;
      if script.checkFor.forall(x => Files.exists(root.resolve(x)))
    ) yield new InstallScript(this, script)
    possible.headOption
  }

  /** Loads a UI patch from the package. */
  def loadUIPatch(x: String) = UIPatch.loadFromXML(source.loadXML(x))
}

/** A specific installation script for a specific version of Civilization. */
class InstallScript private[core] (val pkg: PatchPackage, val script: XmlInstallScript) {
  val platform = Platform.forName(script.platform).get

  lazy val source        = pkg.source
  lazy val patchManifest = pkg.patchManifest
  lazy val cleanup       = script.cleanupData

  def makeFileSet(packages: Set[String], sha256: String) =
    InstallFileSet(this, sha256, script.loadPackages(packages).toSeq)

  def supportedHash(hash: String) =
    script.supportedHashes.contains(hash)
}

/** A set of files which to be added to a Civilization directory. */
case class InstallFileSet private[core] (script: InstallScript, sha256: String, packages: Seq[XmlPackage]) {
  lazy val renames = packages.flatMap(_.renames)
  def getFiles(basePath: Path, versionName: String): Seq[OutputFile] = {
    val configFileBody =
      f"""# Generated by MPPatch. Do not edit manually.
         |bin_sha256 = "$sha256"
         |features = [${packages.flatMap(_.enableFeature).map(x => "\"" + x + "\"").distinct.sorted.mkString(",")}]
         |""".stripMargin

    val configFiles = packages
      .flatMap(_.writeConfig)
      .map(x => OutputFile(x, configFileBody.getBytes(StandardCharsets.UTF_8)))
    val additionalFiles = packages
      .flatMap(_.additionalFile)
      .map(x => OutputFile(x.filename, script.source.loadBinaryResource(x.source), x.isExecutable))

    val assetPath = script.script.assetsPath
    val assets    = script.platform.resolve(basePath, assetPath)
    val dlcFiles = packages.flatMap(_.writeDLC).flatMap { x =>
      val uiPatch = script.pkg.loadUIPatch(x.source)
      val dlc     = new CivDlcBuilder(script.source, uiPatch).generateBaseDLC(assets, script.platform)
      val dlcMap = DLCDataWriter.writeDLC(
        s"$assetPath/${script.platform.mapPath(x.target)}",
        Some(s"$assetPath/${script.platform.mapPath(x.textData)}"),
        dlc,
        script.platform
      )
      dlcMap.map(x => OutputFile(x._1, x._2))
    }

    dlcFiles ++ additionalFiles ++ configFiles
  }
}
case class OutputFile(filename: String, data: Array[Byte], isExecutable: Boolean = false)
