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

case class MPPatchLuaOverride(injectBefore: Seq[String] = Seq(), injectAfter: Seq[String] = Seq())
object MPPatchDLC {
  val DLC_UUID          = UUID.fromString("df74f698-2343-11e6-89c4-8fef6d8f889e")
  val DLC_VERSION       = 1
  val DLC_UPDATEVERSION = 1

  private val luaPatchList = Map(
    "contentswitch.lua"     -> MPPatchLuaOverride(injectAfter  = Seq("after_contentswitch.lua")),
    "mainmenu.lua"          -> MPPatchLuaOverride(injectAfter  = Seq("after_mainmenu.lua")),
    "modsmenu.lua"          -> MPPatchLuaOverride(injectAfter  = Seq("after_modsmenu.lua")),
    "joiningroom.lua"       -> MPPatchLuaOverride(injectAfter  = Seq("after_joiningroom.lua")),
    "stagingroom.lua"       -> MPPatchLuaOverride(injectBefore = Seq("before_stagingroom.lua"),
                                                  injectAfter  = Seq("after_stagingroom.lua")),
    "multiplayerselect.lua" -> MPPatchLuaOverride(injectBefore = Seq("before_multiplayerselect.lua")),
    "lobby.lua"             -> MPPatchLuaOverride(injectBefore = Seq("intercept_bIsModding.lua")),
    "mpgamesetupscreen.lua" -> MPPatchLuaOverride(injectBefore = Seq("intercept_bIsModding.lua")),
    "mpgameoptions.lua"     -> MPPatchLuaOverride(injectAfter  = Seq("after_mpgameoptions.lua"))
  )

  private val libraryFileNames = Seq("mppatch_runtime.lua", "mppatch_utils.lua", "mppatch_modutils.lua",
                                     "mppatch_uiutils.lua", "mppatch_debug.lua")
  private val libraryFiles = libraryFileNames.map(x =>
    x -> res.loadResource(s"patch/lib/$x")
  ).toMap

  private val newScreenFileNames = Seq("mppatch_multiplayerproxy")
  private val newScreenFiles = newScreenFileNames.flatMap(x => Seq(
    s"$x.lua" -> res.loadResource(s"patch/ui/$x.lua"),
    s"$x.xml" -> res.loadResource(s"patch/ui/$x.xml")
  )).toMap

  private def loadWrapper(str: Seq[String]) =
    if(str.isEmpty) "" else {
      "--- BEGIN INJECTED MPPATCH CODE ---\n\n"+
      str.mkString("\n")+
      "\n--- END INJECTED MPPATCH CODE ---"
    }
  private def getLuaFragment(path: String) = {
    val code = res.loadResource("patch/hooks/"+path)
    "-- source file: patch/hooks/"+path+"\n\n"+
    code+(if(!code.endsWith("\n")) "\n" else "")
  }
  private def getXmlFile(path: String) = XML.load(res.getResource(path))
  private def findPatchTargets(path: Path, patches: Map[String, MPPatchLuaOverride]): Map[String, String] =
    IOUtils.listFiles(path).flatMap { file =>
      val fileName = file.getFileName.toString
      if(Files.isDirectory(file)) findPatchTargets(file, patches)
      else patches.get(fileName.toLowerCase(Locale.ENGLISH)) match {
        case Some(patch) =>
          val runtime      = "include \"mppatch_runtime.lua\"\n"
          val injectBefore = runtime +: patch.injectBefore.map(getLuaFragment)
          val contents     = IOUtils.readFileAsString(file)
          val injectAfter  = patch.injectAfter.map(getLuaFragment)
          val finalFile    = loadWrapper(injectBefore)+"\n\n"+contents+"\n\n"+loadWrapper(injectAfter)
          Seq(fileName -> finalFile)
        case None =>
          Seq()
      }
    }.toMap

  private val textFiles = Seq("text_en_US.xml").map(x =>
    x -> getXmlFile("patch/xml/"+x)
  ).toMap

  private def prepareList(map: Map[String, String]) = map.mapValues(_.getBytes(StandardCharsets.UTF_8))
  private def findPathTargets(base: Path, platform: Platform, path: String*) =
    findPatchTargets(platform.resolve(base, platform.assetsPath +: path: _*), luaPatchList)
  def generateBaseDLC(civBaseDirectory: Path, platform: Platform) = {
    DLCData(DLCManifest(DLC_UUID, DLC_VERSION, 1, "MPPatch", "MPPatch"),
            DLCGameplay(textData = textFiles,
                        uiFiles = Map(
                          "LuaPatches" -> prepareList(findPathTargets(civBaseDirectory, platform, "UI")),
                          "Runtime"    -> prepareList(libraryFiles),
                          "Screens"    -> prepareList(newScreenFiles)
                        ),
                        uiSkins = Seq(DLCUISkin("MPPatch", "BaseGame", "Common"))))
  }
}
