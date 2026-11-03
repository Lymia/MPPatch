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

import moe.lymia.mppatch.util.{EncodingUtils, LuaUtils}
import moe.lymia.mppatch.util.io.*
import moe.lymia.mppatch.util.io.XMLUtils.*
import toml.Codec
import toml.Value
import toml.derivation.auto.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}
import scala.xml.{Node, XML}

case class LuaSoftHook(id: String, includes: Seq[String], inject: Seq[String])

case class LuaOverrideInject(source: String, inline: Boolean)

case class LuaOverride(
  filename: String,
  includes: Seq[String],
  injectBefore: Seq[LuaOverrideInject] = Seq(),
  injectAfter: Seq[LuaOverrideInject] = Seq()
)

case class FileWithSource(filename: String, source: String)

case class SoftHookInfo(namespace: String, infoTarget: String, patchPrefix: String)

case class UIPatch(
  dlcManifest: DLCManifest,
  softHookInfo: SoftHookInfo,
  luaPatches: Seq[LuaOverride],
  luaSoftHooks: Seq[LuaSoftHook],
  libraryFiles: Seq[FileWithSource],
  newScreenFileNames: Seq[FileWithSource],
  textFileNames: Seq[String]
)

object UIPatch {
  def parseUiPatch(source: String) = {
    val info = EncodingUtils.unwrapToml(toml.Toml.parseAs[TomlDlcInfo](source))
    UIPatch(
      dlcManifest = DLCManifest(
        UUID.fromString(info.metadata.uuid),
        info.metadata.version,
        info.metadata.priority,
        info.metadata.short_name,
        info.metadata.name,
      ),
      softHookInfo = SoftHookInfo(
        namespace = info.paths.soft_hook_namespace,
        infoTarget = info.paths.soft_hook_filename,
        patchPrefix = info.paths.patch_prefix,
      ),
      luaPatches = info.hook.map((name, hook) => LuaOverride(
        name,
        hook.includes,
        hook.inject.map(parseTomlInjection),
        hook.inject_after.map(parseTomlInjection),
      )).toSeq,
      luaSoftHooks = info.softhook.map((name, softhook) =>
        LuaSoftHook(name, softhook.inject, softhook.inject_after)
      ).toSeq,
      libraryFiles = info.include.map((key, value) => FileWithSource(key, value)).toSeq,
      newScreenFileNames = info.screen.map((key, screen) => FileWithSource(key, screen.source)).toSeq,
      textFileNames = info.paths.text_data,
    )
  }

  def parseTomlInjection(name: toml.Value) = name match {
    case Value.Str(value) => LuaOverrideInject(value, false)
    case Value.Tbl(values) =>
      // TODO: This is a bad parser, but IDC, since this is temporary.
      val source = values("source") match {
        case Value.Str(value) => value
        case _ => ???
      }
      val inline = values("inline") match {
        case Value.Bool(value) => value
        case _ => ???
      }
      LuaOverrideInject(source, inline)
    case _ => ???
  }

  case class TomlMetadata(
    uuid: String,
    version: Int,
    priority: Int,
    name: String,
    short_name: String,
  )

  case class TomlPaths(
    soft_hook_namespace: String,
    soft_hook_filename: String,
    patch_prefix: String,
    text_data: List[String],
  )

  case class TomlHook(
    includes: List[String],
    inject: List[toml.Value],
    inject_after: List[toml.Value],
  )

  case class TomlSoftHook(
    includes: List[String],
    inject: List[String],
    inject_after: List[String],
  )

  case class TomlScreen(
    source: String,
  )

  case class TomlDlcInfo(
    metadata: TomlMetadata,
    paths: TomlPaths,
    include: Map[String, String],
    hook: Map[String, TomlHook],
    softhook: Map[String, TomlSoftHook],
    screen: Map[String, TomlScreen],
  )

  implicit val rawValueCodec: Codec[Value] = Codec { (value, _, _) => Right(value) }
}

class CivDlcBuilder(source: DataSource, patch: UIPatch) {
  private lazy val luaPatchList = patch.luaPatches.map(x => x.filename.toLowerCase(Locale.ENGLISH) -> x).toMap
  private lazy val libraryFiles = patch.libraryFiles.map(x => x.filename -> source.loadResource(x.source)).toMap
  private lazy val newScreenFiles = patch.newScreenFileNames
    .flatMap(x =>
      Seq(
        s"${x.filename}.lua" -> source.loadResource(s"${x.source}.lua"),
        s"${x.filename}.xml" -> source.loadResource(s"${x.source}.xml")
      )
    )
    .toMap
  private lazy val textFiles =
    patch.textFileNames.map(x => x.replace("/", "_") -> XML.loadString(source.loadResource(x))).toMap

  private lazy val patchFileList =
    (patch.luaSoftHooks.flatMap(_.inject) ++
      patch.luaPatches.flatMap(x => (x.injectAfter ++ x.injectBefore).filter(!_.inline).map(_.source))).distinct
  private lazy val patchFileMap = patchFileList.zipWithIndex
    .map(x => x._1 -> s"${patch.softHookInfo.patchPrefix}${x._2}_${x._1.split("/").last}")
    .toMap

  private lazy val softHookInfoFragments = patch.luaSoftHooks.map { x =>
    val subtableName = s"${patch.softHookInfo.namespace}[${LuaUtils.quote(x.id)}]"
    s"""$subtableName = {}
       |$subtableName.include = {${x.includes.map(LuaUtils.quote).mkString(", ")}}
       |$subtableName.inject = {${x.inject.map(patchFileMap).map(LuaUtils.quote).mkString(", ")}}
     """.stripMargin
  }
  private lazy val softHookInfo = {
    val tableName = patch.softHookInfo.namespace
    s"""if not $tableName then
       |  $tableName = {}
       |
       |  ${softHookInfoFragments.map(_.replace("\n", "\n  ")).mkString("\n  ").trim}
       |end
     """.stripMargin
  }
  private lazy val softHookFiles = patchFileMap.map(x => x._2 -> source.loadResource(x._1)) ++ Map(
    patch.softHookInfo.infoTarget -> softHookInfo
  )

  private def loadWrapper(str: Seq[String], pf: String = "", sf: String = "") =
    if (str.isEmpty) ""
    else {
      pf + "--- BEGIN INJECTED MPPATCH CODE ---\n\n" +
        str.mkString("\n") +
        "\n--- END INJECTED MPPATCH CODE ---" + sf
    }

  private def getLuaFragment(inject: LuaOverrideInject) =
    if (!inject.inline) {
      s"include ${LuaUtils.quote(patchFileMap(inject.source))}\n"
    } else {
      val code = source.loadResource(inject.source)
      s"-- source file: ${inject.source} --\n\n" +
        code + (if (!code.endsWith("\n")) "\n" else "") +
        s"\n-- end source file: ${inject.source} --\n"
    }

  private def patchFile(path: Path) = {
    val fileName = path.getFileName.toString
    luaPatchList.get(fileName.toLowerCase(Locale.ENGLISH)) match {
      case Some(patchData) =>
        val runtime = s"${patchData.includes.map(x => s"include [[$x]]").mkString("\n")}\n"
        val injectBefore = runtime +: patchData.injectBefore.map(getLuaFragment)
        val contents = IOUtils.readFileAsString(path)
        val injectAfter = patchData.injectAfter.map(getLuaFragment)
        val finalFile = loadWrapper(injectBefore, sf = "\n\n") + contents + loadWrapper(injectAfter, pf = "\n\n")
        Some(finalFile)
      case None =>
        None
    }
  }

  private def findPatchTargets(path: Path): Map[String, String] =
    IOUtils
      .listFiles(path)
      .flatMap { file =>
        if (Files.isDirectory(file)) findPatchTargets(file)
        else patchFile(file).fold(Seq[(String, String)]())(x => Seq(file.getFileName.toString -> x))
      }
      .toMap

  private def prepareList(map: Map[String, String]) =
    map.view.mapValues(_.getBytes(StandardCharsets.UTF_8)).toMap

  private def findPathTargets(assetsPath: Path, platform: Platform, path: String*) =
    findPatchTargets(platform.resolve(assetsPath, path *))

  def generateBaseDLC(assetsPath: Path, platform: Platform) =
    DLCData(
      patch.dlcManifest,
      DLCGameplay(
        textData = textFiles,
        uiFiles = Map(
          "LuaOverrides" -> prepareList(findPathTargets(assetsPath, platform, "UI")),
          "Runtime" -> prepareList(libraryFiles),
          "Patches" -> prepareList(softHookFiles),
          "Screens" -> prepareList(newScreenFiles)
        ),
        uiSkins = Seq(DLCUISkin("MPPatch", "BaseGame", "Common"))
      )
    )
}
