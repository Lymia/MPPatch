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

package moe.lymia.multiverse.core.generator

import java.nio.file.{Files, Path}
import java.util.UUID

import moe.lymia.multiverse.core.data._
import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.res.LuaCode

object DlcUUID {
  val BASE_DLC_UUID           = UUID.fromString("aa75946c-7fca-4166-874c-de18ecd39162")
  val MOD_UUID_NAMESPACE      = UUID.fromString("28b620c3-93a8-4b4d-9cee-58bc71640a58")
  val MOD_PACK_UUID_NAMESPACE = UUID.fromString("0b3e3322-2dee-45c1-9f4c-091136c7cf29")
}

object BaseDLC {
  val patchList = Seq(
    "InstanceManager.lua"   -> "UI/InstanceManager.lua",
    "GameplayUtilities.lua" -> "Gameplay/Lua/GameplayUtilities.lua")
  def generateBaseDLC(civBaseDirectory: Path, platform: Platform) = {
    val patchedFileList = (for((file, realPath) <- patchList) yield {
      val targetPath = platform.resolve(civBaseDirectory, platform.assetsPath, realPath)
      (file, ImportFromMemory(LuaCode.core_entrypoint_hook.getBytes("UTF8") ++ Files.readAllBytes(targetPath)))
    }).toMap
    DLCData(DLCManifest(DlcUUID.BASE_DLC_UUID, 1, 250,
                        "Multiverse - Base DLC", "Multiverse - Base DLC"),
            DLCGameplay(Nil, Nil, Nil, patchedFileList ++ LuaCode.core_library.mapValues(ImportFromMemory), Nil))
  }
}
