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

package moe.lymia.multiverse.core

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.{IOUtils, res}

import scala.xml.XML

case class MPPatchLuaOverride(injectBefore: Seq[String] = Seq(), injectAfter: Seq[String] = Seq())
object MPPatchDLC {
  val DLC_UUID      = UUID.fromString("df74f698-2343-11e6-89c4-8fef6d8f889e")
  val DLC_VERSION   = 1

  private val luaPatchList = Map(
    "mainmenu.lua"          -> MPPatchLuaOverride(injectAfter = Seq("after_mainmenu.lua")),
    "modsmenu.lua"          -> MPPatchLuaOverride(injectAfter = Seq("after_modsmenu.lua")),
    "stagingroom.lua"       -> MPPatchLuaOverride(injectBefore = Seq("intercept_bIsModding.lua","before_stagingroom.lua")),
    "multiplayerselect.lua" -> MPPatchLuaOverride(injectBefore = Seq("before_multiplayerselect.lua")),
    "mpgamesetupscreen.lua" -> MPPatchLuaOverride(injectAfter = Seq("after_mpgamesetupscreen.lua"))
  )

  private val runtimeFileNames = Seq("mppatch_runtime.lua", "mppatch_utils.lua", "mppatch_modutils.lua",
                                     "mppatch_uiutils.lua")
  private val uiFileNames = Seq("mppatch_multiplayerproxy")
  private val newFiles = runtimeFileNames.map(x =>
    x -> res.loadResource(s"patch/lib/$x")
  ).toMap ++ uiFileNames.flatMap(x => Seq(
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

  private val textFiles = Seq("text_en_US.xml").flatMap(x =>
    getXmlFile("patch/xml/"+x).child
  )

  private def prepareList(map: Map[String, String]) = map.mapValues(_.getBytes(StandardCharsets.UTF_8))
  private def findPathTargets(base: Path, platform: Platform, path: String*) =
    findPatchTargets(platform.resolve(base, platform.assetsPath +: path: _*), luaPatchList)
  def generateBaseDLC(civBaseDirectory: Path, platform: Platform) = {
    val patchedFileList = findPathTargets(civBaseDirectory, platform, "UI")
    DLCData(DLCManifest(DLC_UUID, DLC_VERSION, 1, "MPPatch", "MPPatch"),
            DLCGameplay(globalTextData = textFiles,
                        uiOnlyFiles = prepareList(patchedFileList ++ newFiles),
                        uiSkins = Seq(DLCUISkin("MPPatch", "BaseGame", "Common", false, Map(), None))))
  }
}
