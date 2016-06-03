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

object DlcUUID {
  val BASE_DLC_UUID = UUID.fromString("df74f698-2343-11e6-89c4-8fef6d8f889e")
}

case class MPPatchLuaOverride(libraries: Seq[String] = Seq(),
                              injectBefore: Seq[String] = Seq(), injectAfter: Seq[String] = Seq()) {
  lazy val beforePaths = libraries.map("lib/"+_) ++ injectBefore.map("ui/"+_)
  lazy val afterPaths  = injectAfter.map("ui/"+_)
}
object MPPatch {
  private val UISkin_BaseGame      = DLCUISkin("BaseGame"         , "BaseGame"  , "Common", false, Map())
  private val UISkin_GodsAndKings  = DLCUISkin("Expansion1Primary", "Expansion1", "Common", false, Map())
  private val UISkin_BraveNewWorld = DLCUISkin("Expansion2Primary", "Expansion2", "Common", false, Map())

  private val luaPatchList = Map(
    "mainmenu.lua"    -> MPPatchLuaOverride(injectAfter = Seq("after_mainmenu.lua")),
    "modsmenu.lua"    -> MPPatchLuaOverride(injectAfter = Seq("after_modsmenu.lua")),
    "stagingroom.lua" -> MPPatchLuaOverride(injectBefore = Seq("intercept_bIsModding.lua", "before_stagingroom.lua"))
  )
  private val runtimeFiles = Map(
    "mppatch_runtime.lua" -> res.loadResource("patch/lib/mppatch_runtime.lua")
  )

  private def loadWrapper(str: Seq[String]) =
    if(str.isEmpty) "" else {
      "--- BEGIN INJECTED MPPATCH CODE ---\n\n"+
      str.mkString("\n")+
      "\n--- END INJECTED MPPATCH CODE ---\n\n"
    }
  private def getLuaFragment(path: String) = {
    val code = res.loadResource("patch/"+path)
    "-- source file: patch/"+path+"\n\n"+
    code+(if(!code.endsWith("\n")) "\n" else "")
  }
  private def findPatchTargets(path: Path, patches: Map[String, MPPatchLuaOverride]): Map[String, String] =
    IOUtils.listFiles(path).flatMap { file =>
      val fileName = file.getFileName.toString
      if(Files.isDirectory(file)) findPatchTargets(file, patches)
      else patches.get(fileName.toLowerCase(Locale.ENGLISH)) match {
        case Some(patch) =>
          val runtime      = "include \"mppatch_runtime.lua\"\n"
          val injectBefore = runtime +: patch.beforePaths.map(getLuaFragment)
          val contents     = IOUtils.readFileAsString(file)
          val injectAfter  = patch.afterPaths.map(getLuaFragment)
          val finalFile    = loadWrapper(injectBefore)+contents+loadWrapper(injectAfter)
          Seq(fileName -> finalFile)
        case None =>
          Seq()
      }
    }.toMap

  private def prepareList(map: Map[String, String]) = map.mapValues(_.getBytes(StandardCharsets.UTF_8))
  def generateBaseDLC(civBaseDirectory: Path, platform: Platform) = {
    val patchedFileList =
      findPatchTargets(platform.resolve(civBaseDirectory, platform.assetsPath, "ui"), luaPatchList)
    DLCData(DLCManifest(DlcUUID.BASE_DLC_UUID, 1, 300, "MPPatch", "MPPatch"),
            DLCGameplay(Nil, Nil, Nil, prepareList(runtimeFiles), prepareList(patchedFileList),
                        Seq(UISkin_BraveNewWorld)))
  }
}
