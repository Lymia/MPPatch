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
import java.util.{Locale, UUID}

import moe.lymia.mppatch.util.{IOUtils, res}
import moe.lymia.mppatch.util.XMLUtils._

import scala.xml.{Node, XML}


trait PatchFileSource {
  def loadResource(name: String): String
}
object ResourceSource extends PatchFileSource {
  override def loadResource(name: String): String = res.loadResource("patch/files/"+name)
}

case class LuaOverride(fileName: String, injectBefore: Seq[String] = Seq(), injectAfter: Seq[String] = Seq())
case class PatchManifest(dlcManifest: DLCManifest, patchVersion: String,
                         luaPatches: Seq[LuaOverride], libraryFileNames: Seq[String],
                         newScreenFileNames: Seq[String], textFileNames: Seq[String])
object PatchManifest {
  def readDLCManifest(node: Node) =
    DLCManifest(UUID.fromString(getAttribute(node, "UUID")),
                getAttribute(node, "Version").toInt, getAttribute(node, "Priority").toInt,
                getAttribute(node, "ShortName"), getAttribute(node, "Name"))
  def loadFilename(node: Node) = getAttribute(node, "Filename")
  def loadLuaOverride(node: Node) =
    LuaOverride(loadFilename(node),
                (node \ "InjectBefore").map(loadFilename), (node \ "InjectAfter").map(loadFilename))
  def loadFromXML(xml: Node) = {
    val manifestVersion = getAttribute(xml, "ManifestVersion")
    if(manifestVersion != "0") sys.error("Unknown ManifestVersion: "+manifestVersion)
    PatchManifest(readDLCManifest((xml \ "Info").head), getAttribute(xml, "PatchVersion"),
                              (xml \ "Hook"    ).map(loadLuaOverride),
                              (xml \ "Include" ).map(loadFilename),
                              (xml \ "Screen"  ).map(loadFilename),
                              (xml \ "TextData").map(loadFilename))
  }

  val resPatchData = loadFromXML(XML.loadString(res.loadResource("patch/manifest.xml")))
}

class PatchLoader(data: PatchManifest, source: PatchFileSource) {
  def manifest = data.dlcManifest

  lazy val luaPatchList = data.luaPatches.map(x => x.fileName.toLowerCase -> x).toMap
  lazy val libraryFiles = data.libraryFileNames.map(x =>
    x -> source.loadResource(s"$x")
  ).toMap
  val newScreenFiles = data.newScreenFileNames.flatMap(x => Seq(
    s"$x.lua" -> source.loadResource(s"$x.lua"),
    s"$x.xml" -> source.loadResource(s"$x.xml")
  )).toMap
  val textFiles = data.textFileNames.map(x =>
    x -> XML.loadString(source.loadResource(x))
  ).toMap

  private def loadWrapper(str: Seq[String]) =
    if(str.isEmpty) "" else {
      "--- BEGIN INJECTED MPPATCH CODE ---\n\n"+
      str.mkString("\n")+
      "\n--- END INJECTED MPPATCH CODE ---"
    }
  private def getLuaFragment(path: String) = {
    val code = source.loadResource(path)
    "-- source file: "+path+"\n\n"+
    code+(if(!code.endsWith("\n")) "\n" else "")
  }
  def patchFile(path: Path) = {
    val fileName = path.getFileName.toString
    luaPatchList.get(fileName.toLowerCase(Locale.ENGLISH)) match {
      case Some(patch) =>
        val runtime      = "include \"mppatch_runtime.lua\"\n"
        val injectBefore = runtime +: patch.injectBefore.map(getLuaFragment)
        val contents     = IOUtils.readFileAsString(path)
        val injectAfter  = patch.injectAfter.map(getLuaFragment)
        val finalFile    = loadWrapper(injectBefore)+"\n\n"+contents+"\n\n"+loadWrapper(injectAfter)
        Some(finalFile)
      case None =>
        None
    }
  }
}