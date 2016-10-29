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

import moe.lymia.mppatch.util.IOUtils

object MPPatchDLC {
  val DLC_UPDATEVERSION = 1

  private def findPatchTargets(path: Path, loader: PatchLoader): Map[String, String] =
    IOUtils.listFiles(path).flatMap { file =>
      if(Files.isDirectory(file)) findPatchTargets(file, loader)
      else loader.patchFile(file).fold(Seq[(String, String)]())(x => Seq(file.getFileName.toString -> x))
    }.toMap
  private def prepareList(map: Map[String, String]) = map.mapValues(_.getBytes(StandardCharsets.UTF_8))
  private def findPathTargets(base: Path, loader: PatchLoader, platform: Platform, path: String*) =
    findPatchTargets(platform.resolve(base, platform.assetsPath +: path: _*), loader)
  def generateBaseDLC(civBaseDirectory: Path, loader: PatchLoader, platform: Platform) = {
    DLCData(loader.data.dlcManifest,
            DLCGameplay(textData = loader.textFiles,
                        uiFiles = Map(
                          "LuaPatches" -> prepareList(findPathTargets(civBaseDirectory, loader, platform, "UI")),
                          "Runtime"    -> prepareList(loader.libraryFiles),
                          "Screens"    -> prepareList(loader.newScreenFiles)
                        ),
                        uiSkins = Seq(DLCUISkin("MPPatch", "BaseGame", "Common"))))
  }
}
