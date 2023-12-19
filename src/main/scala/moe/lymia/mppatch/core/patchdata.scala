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
import moe.lymia.mppatch.util.{EncodingUtils, PropertiesSource, VersionInfo}
import play.api.libs.json.{Json, OFormat}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.xml.Node

case class JsonPatchManifest(installScripts: Seq[String])
case class JsonInstallScript(
    game: String,
    gameType: String,
    platform: String,
    steamId: Int,
    assetsPath: String,
    checkFor: Set[String],
    hashFrom: String,
    supportedHashes: Map[String, String],
    packages: Map[String, JsonPackage],
    cleanup: JsonCleanup
)
case class JsonCleanup(renames: Seq[JsonRename], checkFile: Seq[String])
case class JsonPackage(
    depends: Option[Set[String]],
    renameFile: Option[Seq[JsonRename]],
    writeFile: Option[Seq[JsonWriteFile]],
    writeConfig: Option[Seq[String]],
    writeDlc: Option[Seq[JsonWriteDLC]],
    enableFeature: Option[Set[String]]
)
case class JsonRename(from: String, to: String)
case class JsonWriteFile(from: String, to: String, exec: Option[Boolean])
case class JsonWriteDLC(from: String, to: String, textData: String)

object JsonFormats {
  implicit lazy val PatchManifest: OFormat[JsonPatchManifest] = Json.format[JsonPatchManifest]
  implicit lazy val InstallScript: OFormat[JsonInstallScript] = Json.format[JsonInstallScript]
  implicit lazy val Cleanup: OFormat[JsonCleanup]             = Json.format[JsonCleanup]
  implicit lazy val Package: OFormat[JsonPackage]             = Json.format[JsonPackage]
  implicit lazy val Rename: OFormat[JsonRename]               = Json.format[JsonRename]
  implicit lazy val WriteFile: OFormat[JsonWriteFile]         = Json.format[JsonWriteFile]
  implicit lazy val WriteDLC: OFormat[JsonWriteDLC]           = Json.format[JsonWriteDLC]
}
import JsonFormats.*

private object PackageUtils {
  def loadPackage(script: JsonInstallScript, name: String) =
    script.packages.getOrElse(name, sys.error(f"no such package $name"))

  @annotation.tailrec
  final def loadPackages(
      script: JsonInstallScript,
      toLoad: Set[String],
      packages: Seq[(String, JsonPackage)] = Seq()
  ): Seq[JsonPackage] = {
    val loaded = packages.map(_._1).toSet
    if (loaded == toLoad) packages.map(_._2)
    else {
      val newPackages = (toLoad -- loaded).map(x => (x, loadPackage(script, x)))
      loadPackages(script, toLoad ++ newPackages.flatMap(_._2.depends.getOrElse(Set())), packages ++ newPackages)
    }
  }
}

/** Main loader for the game's patch packages.
  *
  * @param source
  *   Where to load patch from.
  */
class PatchPackage(val source: DataSource) {
  source.loadResource("manifest_version.txt").strip() match {
    case "1.0" => // ok
    case unk   => sys.error(f"Unknown .mppatch-pkg version: $unk")
  }

  /** The manifest for this patch. */
  lazy val patchManifest =
    EncodingUtils.unwrapJson(Json.fromJson[JsonPatchManifest](Json.parse(source.loadResource("manifest.json"))))

  /** The version information stored in this patch. */
  lazy val versionInfo = new VersionInfo(new PropertiesSource(source.loadResource("version.properties")))

  /** The list of install scripts available in this package. */
  lazy val scripts =
    patchManifest.installScripts.map(x =>
      EncodingUtils.unwrapJson(Json.fromJson[JsonInstallScript](Json.parse(source.loadResource(x))))
    )

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
class InstallScript private[core] (val pkg: PatchPackage, val script: JsonInstallScript) {
  val platform = Platform.forName(script.platform).get

  lazy val source      = pkg.source
  lazy val versionInfo = pkg.versionInfo
  lazy val cleanup     = script.cleanup

  def makeFileSet(packages: Set[String], sha256: String) =
    InstallFileSet(this, sha256, PackageUtils.loadPackages(script, packages))
  def supportedHash(hash: String) = script.supportedHashes.contains(hash)
}

/** A set of files which to be added to a Civilization directory. */
case class InstallFileSet private[core] (script: InstallScript, sha256: String, packages: Seq[JsonPackage]) {
  lazy val renames = packages.flatMap(_.renameFile.getOrElse(Seq()))
  def getFiles(basePath: Path, versionName: String): Seq[OutputFile] = {
    val configFileBody =
      f"""# Generated by MPPatch. Do not edit manually.
         |bin_sha256 = "$sha256"
         |features = [${packages
          .flatMap(_.enableFeature.getOrElse(Set()))
          .map(x => "\"" + x + "\"")
          .distinct
          .sorted
          .mkString(",")}]
         |""".stripMargin

    val configFiles = packages
      .flatMap(_.writeConfig.getOrElse(Seq()))
      .map(x => OutputFile(x, configFileBody.getBytes(StandardCharsets.UTF_8)))
    val additionalFiles = packages
      .flatMap(_.writeFile.getOrElse(Seq()))
      .map(x => OutputFile(x.to, script.source.loadBinaryResource(x.from), x.exec.getOrElse(false)))

    val assetPath = script.script.assetsPath
    val assets    = script.platform.resolve(basePath, assetPath)
    val dlcFiles = packages.flatMap(_.writeDlc.getOrElse(Seq())).flatMap { x =>
      val uiPatch = script.pkg.loadUIPatch(x.from)
      val dlc     = new CivDlcBuilder(script.source, uiPatch).generateBaseDLC(assets, script.platform)
      val dlcMap = DLCDataWriter.writeDLC(
        s"$assetPath/${script.platform.mapPath(x.to)}",
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
