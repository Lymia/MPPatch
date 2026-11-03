/*
 * Copyright (c) 2015-2026 Rimi Kanokawa <rimi@rimin.moe>
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
import toml.derivation.auto.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.xml.Node

case class JsonPatchManifest(installScripts: List[String])

case class JsonInstallScript(
  metadata: JsonMetadata,
  packages: Map[String, JsonPackage],
  cleanup: JsonCleanup,
)

case class JsonMetadata(
  game: String,
  platform: String,
  steam_id: Int,
  assets_path: String,
  check_for: List[String],
  hash_from: String,
  supported_hashes: List[String],
)

case class JsonCleanup(renames: List[JsonRename], check_file: List[String])

case class JsonPackage(
  depends: Option[List[String]],
  rename_file: Option[List[JsonRename]],
  write_file: Option[List[JsonWriteFile]],
  write_config: Option[List[String]],
  write_dlc: Option[List[JsonWriteDLC]],
  enable_feature: Option[List[String]],
)

case class JsonRename(from: String, to: String)

case class JsonWriteFile(from: String, to: String, exec: Option[Boolean])

case class JsonWriteDLC(from: String, to: String, text_data: String)

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
 * Where to load patch from.
 */
class PatchPackage(val source: DataSource) {
  source.loadResource("manifest_version.txt").strip() match {
    case "2.0" => // ok
    case unk => sys.error(f"Unknown .mppatch-pkg version: $unk")
  }

  /** The manifest for this patch. */
  lazy val patchManifest =
    EncodingUtils.unwrapToml(toml.Toml.parseAs[JsonPatchManifest](source.loadResource("manifest.toml")))

  /** The version information stored in this patch. */
  lazy val versionInfo = new VersionInfo(new PropertiesSource(source.loadResource("version.properties")))

  /** The list of install scripts available in this package. */
  lazy val scripts =
    patchManifest.installScripts.map(x =>
      EncodingUtils.unwrapToml(toml.Toml.parseAs[JsonInstallScript](source.loadResource(x)))
    )

  /** Detects the platform a Civilization installation directory is for. */
  def detectInstallationPlatform(root: Path): Option[InstallScript] = {
    val possible = for (
      script <- scripts;
      if script.metadata.check_for.forall(x => Files.exists(root.resolve(x)))
    ) yield new InstallScript(this, script)
    possible.headOption
  }

  /** Loads a UI patch from the package. */
  def loadUIPatch(x: String) = UIPatch.parseUiPatch(source.loadResource(x))
}

/** A specific installation script for a specific version of Civilization. */
class InstallScript private[core](val pkg: PatchPackage, val script: JsonInstallScript) {
  val platform = Platform.forName(script.metadata.platform).get

  lazy val source = pkg.source
  lazy val versionInfo = pkg.versionInfo
  lazy val cleanup = script.cleanup

  def makeFileSet(packages: Set[String], sha256: String) =
    InstallFileSet(this, sha256, PackageUtils.loadPackages(script, packages))

  def supportedHash(hash: String) = script.metadata.supported_hashes.contains(hash)
}

/** A set of files which to be added to a Civilization directory. */
case class InstallFileSet private[core](script: InstallScript, sha256: String, packages: Seq[JsonPackage]) {
  lazy val renames = packages.flatMap(_.rename_file.getOrElse(Seq()))

  def getFiles(basePath: Path, versionName: String): Seq[OutputFile] = {
    val packagesStr = packages.flatMap(_.enable_feature.getOrElse(Set())).distinct.sorted.mkString(",")
    val configFileBody = f"$sha256\n$packagesStr"

    val configFiles = packages
      .flatMap(_.write_config.getOrElse(Seq()))
      .map(x => OutputFile(x, configFileBody.getBytes(StandardCharsets.UTF_8)))
    val additionalFiles = packages
      .flatMap(_.write_file.getOrElse(Seq()))
      .map(x => OutputFile(x.to, script.source.loadBinaryResource(x.from), x.exec.getOrElse(false)))

    val assetPath = script.script.metadata.assets_path
    val assets = script.platform.resolve(basePath, assetPath)
    val dlcFiles = packages.flatMap(_.write_dlc.getOrElse(Seq())).flatMap { x =>
      val uiPatch = script.pkg.loadUIPatch(x.from)
      val dlc = new CivDlcBuilder(script.source, uiPatch).generateBaseDLC(assets, script.platform)
      val dlcMap = DLCDataWriter.writeDLC(
        s"$assetPath/${script.platform.mapPath(x.to)}",
        Some(s"$assetPath/${script.platform.mapPath(x.text_data)}"),
        dlc,
        script.platform
      )
      dlcMap.map(x => OutputFile(x._1, x._2))
    }

    dlcFiles ++ additionalFiles ++ configFiles
  }
}

case class OutputFile(filename: String, data: Array[Byte], isExecutable: Boolean = false)
