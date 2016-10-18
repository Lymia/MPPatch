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

import moe.lymia.mppatch.platform.Platform
import moe.lymia.mppatch.util.{IOUtils, res}

import scala.xml.XML

trait MPPatchFileSource {
  def loadHook   (name: String): String
  def loadLibrary(name: String): String
  def loadScreen (name: String): String
  def loadText   (name: String): String
}
object ResourceSource extends MPPatchFileSource {
  override def loadHook   (name: String): String = res.loadResource("patch/hooks/"+name)
  override def loadLibrary(name: String): String = res.loadResource("patch/lib/"+name)
  override def loadScreen (name: String): String = res.loadResource("patch/screen/"+name)
  override def loadText   (name: String): String = res.loadResource("patch/text/"+name)
}

case class MPPatchLuaOverride(fileName: String, injectBefore: Seq[String] = Seq(), injectAfter: Seq[String] = Seq())
case class MPPatchData(luaPatches: Seq[MPPatchLuaOverride], libraryFileNames: Seq[String],
                       newScreenFileNames: Seq[String], textFileNames: Seq[String])

class MPPatchLoader(data: MPPatchData, source: MPPatchFileSource) {
  lazy val luaPatchList = data.luaPatches.map(x => x.fileName.toLowerCase -> x).toMap
  lazy val libraryFiles = data.libraryFileNames.map(x =>
    x -> source.loadLibrary(s"$x")
  ).toMap
  val newScreenFiles = data.newScreenFileNames.flatMap(x => Seq(
    s"$x.lua" -> source.loadScreen(s"$x.lua"),
    s"$x.xml" -> source.loadScreen(s"$x.xml")
  )).toMap
  val textFiles = data.textFileNames.map(x =>
    x -> XML.loadString(source.loadText(x))
  ).toMap

  private def loadWrapper(str: Seq[String]) =
    if(str.isEmpty) "" else {
      "--- BEGIN INJECTED MPPATCH CODE ---\n\n"+
      str.mkString("\n")+
      "\n--- END INJECTED MPPATCH CODE ---"
    }
  private def getLuaFragment(path: String) = {
    val code = source.loadHook(path)
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

object MPPatchDLC {
  val DLC_UUID          = UUID.fromString("df74f698-2343-11e6-89c4-8fef6d8f889e")
  val DLC_VERSION       = 1
  val DLC_UPDATEVERSION = 1

  private val luaPatchList = Seq(
    MPPatchLuaOverride("contentswitch.lua"     , injectAfter  = Seq("after_contentswitch.lua")),
    MPPatchLuaOverride("mainmenu.lua"          , injectAfter  = Seq("after_mainmenu.lua")),
    MPPatchLuaOverride("modsmenu.lua"          , injectAfter  = Seq("after_modsmenu.lua")),
    MPPatchLuaOverride("joiningroom.lua"       , injectAfter  = Seq("after_joiningroom.lua")),
    MPPatchLuaOverride("stagingroom.lua"       , injectBefore = Seq("before_stagingroom.lua"),
                                                 injectAfter  = Seq("after_stagingroom.lua")),
    MPPatchLuaOverride("multiplayerselect.lua" , injectBefore = Seq("before_multiplayerselect.lua")),
    MPPatchLuaOverride("lobby.lua"             , injectBefore = Seq("intercept_bIsModding.lua")),
    MPPatchLuaOverride("mpgamesetupscreen.lua" , injectBefore = Seq("intercept_bIsModding.lua")),
    MPPatchLuaOverride("mpgameoptions.lua"     , injectAfter  = Seq("after_mpgameoptions.lua"))
  )
  private val libraryFileNames = Seq("mppatch_runtime.lua", "mppatch_utils.lua", "mppatch_modutils.lua",
                                     "mppatch_uiutils.lua", "mppatch_debug.lua")
  private val newScreenFileNames = Seq("mppatch_multiplayerproxy")
  private val textFileNames = Seq("text_en_US.xml")
  private val patchData = MPPatchData(luaPatchList, libraryFileNames, newScreenFileNames, textFileNames)

  private def findPatchTargets(path: Path, loader: MPPatchLoader): Map[String, String] =
    IOUtils.listFiles(path).flatMap { file =>
      if(Files.isDirectory(file)) findPatchTargets(file, loader)
      else loader.patchFile(file).fold(Seq[(String, String)]())(x => Seq(file.getFileName.toString -> x))
    }.toMap
  private def prepareList(map: Map[String, String]) = map.mapValues(_.getBytes(StandardCharsets.UTF_8))
  private def findPathTargets(base: Path, loader: MPPatchLoader, platform: Platform, path: String*) =
    findPatchTargets(platform.resolve(base, platform.assetsPath +: path: _*), loader)
  def generateBaseDLC(civBaseDirectory: Path, platform: Platform) = {
    val loader = new MPPatchLoader(patchData, ResourceSource)
    DLCData(DLCManifest(DLC_UUID, DLC_VERSION, 1, "MPPatch", "MPPatch"),
            DLCGameplay(textData = loader.textFiles,
                        uiFiles = Map(
                          "LuaPatches" -> prepareList(findPathTargets(civBaseDirectory, loader, platform, "UI")),
                          "Runtime"    -> prepareList(loader.libraryFiles),
                          "Screens"    -> prepareList(loader.newScreenFiles)
                        ),
                        uiSkins = Seq(DLCUISkin("MPPatch", "BaseGame", "Common"))))
  }
}
