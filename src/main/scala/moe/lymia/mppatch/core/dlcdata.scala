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

import moe.lymia.mppatch.util.LuaUtils
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}
import moe.lymia.mppatch.util.io._
import moe.lymia.mppatch.util.io.XMLUtils._

import scala.xml.{Node, XML}

case class LuaSoftHook(id: String, includes: Seq[String], inject: Seq[String])
case class LuaOverrideInject(source: String, inline: Boolean)
case class LuaOverride(filename: String, includes: Seq[String],
                       injectBefore: Seq[LuaOverrideInject] = Seq(), injectAfter: Seq[LuaOverrideInject] = Seq())
case class FileWithSource(filename: String, source: String)
case class SoftHookInfo(namespace: String, infoTarget: String, patchPrefix: String)
case class UIPatch(dlcManifest: DLCManifest, softHookInfo: SoftHookInfo,
                   luaPatches: Seq[LuaOverride], luaSoftHooks: Seq[LuaSoftHook], libraryFiles: Seq[FileWithSource],
                   newScreenFileNames: Seq[FileWithSource], textFileNames: Seq[String])
object UIPatch {
  def loadFilename(node: Node) = getAttribute(node, "Filename")
  def readDLCManifest(node: Node) =
    DLCManifest(UUID.fromString(getAttribute(node, "UUID")),
                getAttribute(node, "Version").toInt, getAttribute(node, "Priority").toInt,
                getAttribute(node, "ShortName"), getAttribute(node, "Name"))
  def loadInject(node: Node) = LuaOverrideInject(loadSource(node), getBoolAttribute(node, "Inline"))
  def loadLuaOverride(node: Node) =
    LuaOverride(loadFilename(node), (node \ "Include").map(loadFilename),
                (node \ "InjectBefore").map(loadInject), (node \ "InjectAfter").map(loadInject))
  def loadLuaSoftHook(node: Node) =
    LuaSoftHook(getAttribute(node, "ScreenID"), (node \ "Include").map(loadFilename),
                (node \ "Inject").map(loadSource))
  def loadInclude(node: Node) = FileWithSource(loadFilename(node), getAttribute(node, "Source"))
  def loadScreen(node: Node) = FileWithSource(getAttribute(node, "Name"), getAttribute(node, "Source"))
  def loadSoftHookInfo(node: Node) =
    SoftHookInfo(getAttribute(node, "Namespace"), getAttribute(node, "Filename"), getAttribute(node, "PatchPrefix"))
  def loadFromXML(xml: Node) =
    UIPatch(readDLCManifest((xml \ "Info").head),
            loadSoftHookInfo((xml \ "SoftHookInfo").head),
            (xml \ "Hook"        ).map(loadLuaOverride),
            (xml \ "SoftHook"    ).map(loadLuaSoftHook),
            (xml \ "Include"     ).map(loadInclude),
            (xml \ "Screen"      ).map(loadScreen),
            (xml \ "TextData"    ).map(loadSource))
}

class UIPatchLoader(source: DataSource, patch: UIPatch) {
  private lazy val luaPatchList = patch.luaPatches.map(x => x.filename.toLowerCase(Locale.ENGLISH) -> x).toMap
  private lazy val libraryFiles = patch.libraryFiles.map(x =>
    x.filename -> source.loadResource(x.source)
  ).toMap
  private lazy val newScreenFiles = patch.newScreenFileNames.flatMap(x => Seq(
    s"${x.filename}.lua" -> source.loadResource(s"${x.source}.lua"),
    s"${x.filename}.xml" -> source.loadResource(s"${x.source}.xml")
  )).toMap
  private lazy val textFiles = patch.textFileNames.map(x =>
    x.replace("/", "_") -> XML.loadString(source.loadResource(x))
  ).toMap

  private lazy val patchFileList =
    (patch.luaSoftHooks.flatMap(_.inject) ++
     patch.luaPatches.flatMap(x => (x.injectAfter ++ x.injectBefore).filter(!_.inline).map(_.source))).distinct
  private lazy val patchFileMap = patchFileList.zipWithIndex.map(x =>
    x._1 -> s"${patch.softHookInfo.patchPrefix}${x._2}_${x._1.split("/").last}").toMap

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
    if(str.isEmpty) "" else {
      pf+"--- BEGIN INJECTED MPPATCH CODE ---\n\n"+
      str.mkString("\n")+
      "\n--- END INJECTED MPPATCH CODE ---"+sf
    }
  private def getLuaFragment(inject: LuaOverrideInject) = {
    if(!inject.inline) {
      s"include ${LuaUtils.quote(patchFileMap(inject.source))}\n"
    } else {
      val code = source.loadResource(inject.source)
      s"-- source file: ${inject.source} --\n\n"+
      code+(if(!code.endsWith("\n")) "\n" else "")+
      s"\n-- end source file: ${inject.source} --\n"
    }
  }
  private def patchFile(path: Path) = {
    val fileName = path.getFileName.toString
    luaPatchList.get(fileName.toLowerCase(Locale.ENGLISH)) match {
      case Some(patchData) =>
        val runtime      = s"${patchData.includes.map(x => s"include [[$x]]").mkString("\n")}\n"
        val injectBefore = runtime +: patchData.injectBefore.map(getLuaFragment)
        val contents     = IOUtils.readFileAsString(path)
        val injectAfter  = patchData.injectAfter.map(getLuaFragment)
        val finalFile    = loadWrapper(injectBefore, sf = "\n\n")+contents+loadWrapper(injectAfter, pf = "\n\n")
        Some(finalFile)
      case None =>
        None
    }
  }

  private def findPatchTargets(path: Path): Map[String, String] =
    IOUtils.listFiles(path).flatMap { file =>
      if(Files.isDirectory(file)) findPatchTargets(file)
      else patchFile(file).fold(Seq[(String, String)]())(x => Seq(file.getFileName.toString -> x))
    }.toMap
  private def prepareList(map: Map[String, String]) =
    map.view.mapValues(_.getBytes(StandardCharsets.UTF_8)).toMap
  private def findPathTargets(assetsPath: Path, platform: Platform, path: String*) =
    findPatchTargets(platform.resolve(assetsPath, path: _*))
  def generateBaseDLC(assetsPath: Path, platform: Platform) = {
    DLCData(patch.dlcManifest,
            DLCGameplay(textData = textFiles,
                        uiFiles = Map(
                          "LuaOverrides" -> prepareList(findPathTargets(assetsPath, platform, "UI")),
                          "Runtime"      -> prepareList(libraryFiles),
                          "Patches"      -> prepareList(softHookFiles),
                          "Screens"      -> prepareList(newScreenFiles)
                        ),
                        uiSkins = Seq(DLCUISkin("MPPatch", "BaseGame", "Common"))))
  }
}