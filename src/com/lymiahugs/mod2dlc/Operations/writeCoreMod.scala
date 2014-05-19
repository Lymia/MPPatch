/*
 * Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.lymiahugs.mod2dlc.Operations

import java.io.File
import java.util.UUID
import com.lymiahugs.mod2dlc.util.{FileOperationLogger, DLCKey, ParseDatabaseUpdate}
import com.lymiahugs.mod2dlc.data.LuaFrag

object writeCoreMod {
  val patchList = Seq(
    "UI/InGame/CityView/CityView.lua" ->
      Seq("DLC/Expansion2/UI/InGame/CityView/CityView.lua",
          "DLC/Expansion/UI/InGame/CityView/CityView.lua",
          "UI/InGame/CityView/CityView.lua"),
    "UI/InGame/InGame.lua" ->
      Seq("DLC/Expansion2/UI/InGame/InGame.lua",
          "DLC/Expansion/UI/InGame/InGame.lua",
          "UI/InGame/InGame.lua"),
    "UI/InGame/LeaderHead/LeaderHeadRoot.lua" ->
      Seq("DLC/Expansion2/UI/InGame/LeaderHead/LeaderHeadRoot.lua",
          "DLC/Expansion/UI/InGame/LeaderHead/LeaderHeadRoot.lua",
          "UI/InGame/LeaderHead/LeaderHeadRoot.lua")
  )

  val mod2dlc_uuid = UUID.fromString("3d2df716-2c91-454f-8a5d-c21cfded78f8")
  def apply(target: File, assetDir: File, languageFile: File)
           (implicit logger: FileOperationLogger) =
    logger.logException {
      import logger._
      if(!target.exists) target.mkdirs()

      // TODO: Note if new expansions have been installed. Might be worth checking that for error detection.

      writeFile(new File(target, "Gameplay/Lua/m2d_core.lua"), LuaFrag.core_library, " core library")
      generateLanguageFile("Mod2DLC Core", "Mod2DLC Core",
        mod2dlc_uuid.toString.replace("-", "").toUpperCase, languageFile)
      generateXML(
        <Civ5Package>
          <GUID>{"{"+mod2dlc_uuid+"}"}</GUID>
          <SteamApp>99999</SteamApp>
          <Version>1</Version>
          <Ownership>FREE</Ownership>
          <Priority>250</Priority>
          <PTags>
            <Tag>Version</Tag>
            <Tag>Ownership</Tag>
          </PTags>
          <Key>{DLCKey.key(mod2dlc_uuid, Seq(99999), 1, "FREE")}</Key>

          <Name>
            <Value language="en_US">Mod2DLC Core</Value>
          </Name>
          <Description>
            <Value language="en_US">Mod2DLC Core</Value>
          </Description>
        </Civ5Package>, new File(target, "Mod2DLC.Civ5Pkg"), "Civ5Pkg File")

      for((patchTarget, sourceList) <- patchList) {
        val sourceFile = sourceList.find(x => new File(assetDir, x).exists).get
        writeFile(new File(target, patchTarget), LuaFrag.core_entrypoint_hook+readFile(new File(assetDir, sourceFile)),
          " patch for entry points")
      }
    }
}
