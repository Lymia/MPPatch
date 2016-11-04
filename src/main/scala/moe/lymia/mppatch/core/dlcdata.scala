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
import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import moe.lymia.mppatch.util.{DataSource, IOUtils}
import moe.lymia.mppatch.util.XMLUtils._

import scala.xml.{Node, XML}

case class LuaOverride(filename: String, includes: Seq[String],
                       injectBefore: Seq[String] = Seq(), injectAfter: Seq[String] = Seq())
case class FileWithSource(filename: String, source: String)
case class UIPatch(dlcManifest: DLCManifest,
                   luaPatches: Seq[LuaOverride], libraryFiles: Seq[FileWithSource],
                   newScreenFileNames: Seq[FileWithSource], textFileNames: Seq[String])
object UIPatch {
  def readDLCManifest(node: Node) =
    DLCManifest(UUID.fromString(getAttribute(node, "UUID")),
                getAttribute(node, "Version").toInt, getAttribute(node, "Priority").toInt,
                getAttribute(node, "ShortName"), getAttribute(node, "Name"))
  def loadLuaOverride(node: Node) =
    LuaOverride(loadFilename(node), (node \ "Include").map(loadFilename),
                (node \ "InjectBefore").map(loadFilename), (node \ "InjectAfter").map(loadFilename))
  def loadFileWithSource(node: Node) =
    FileWithSource(loadFilename(node), (node \ "@Source").text)
  def loadFromXML(xml: Node) =
    UIPatch(readDLCManifest((xml \ "Info").head),
            (xml \ "Hook"         ).map(loadLuaOverride),
            (xml \ "Include"      ).map(loadFileWithSource),
            (xml \ "Screen"       ).map(loadFileWithSource),
            (xml \ "TextData"     ).map(loadFilename))
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
    x -> XML.loadString(source.loadResource(x))
  ).toMap

  private def loadWrapper(str: Seq[String], pf: String = "", sf: String = "") =
    if(str.isEmpty) "" else {
      pf+"--- BEGIN INJECTED MPPATCH CODE ---\n\n"+
      str.mkString("\n")+
      "\n--- END INJECTED MPPATCH CODE ---"+sf
    }
  private def getLuaFragment(path: String) = {
    val code = source.loadResource(path)
    "-- source file: "+path+"\n\n"+
    code+(if(!code.endsWith("\n")) "\n" else "")
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
  private def prepareList(map: Map[String, String]) = map.mapValues(_.getBytes(StandardCharsets.UTF_8))
  private def findPathTargets(assetsPath: Path, platform: Platform, path: String*) =
    findPatchTargets(platform.resolve(assetsPath, path: _*))
  def generateBaseDLC(assetsPath: Path, platform: Platform) = {
    DLCData(patch.dlcManifest,
            DLCGameplay(textData = textFiles,
                        uiFiles = Map(
                          "LuaPatches" -> prepareList(findPathTargets(assetsPath, platform, "UI")),
                          "Runtime"    -> prepareList(libraryFiles),
                          "Screens"    -> prepareList(newScreenFiles)
                        ),
                        uiSkins = Seq(DLCUISkin("MPPatch", "BaseGame", "Common"))))
  }
}