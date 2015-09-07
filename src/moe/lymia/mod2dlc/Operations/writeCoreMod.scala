/*
 * Copyright (c) 2015 Lymia Alusyia <lymia@lymiahugs.com>
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

package moe.lymia.mod2dlc.Operations

import java.io.File
import java.util.UUID
import com.lymiahugs.mod2dlc.util.FileOperationLogger
import moe.lymia.mod2dlc.data.LuaCode
import moe.lymia.mod2dlc.util.DLCKey

object writeCoreMod {
  val patchList = Seq(
    "UI/InstanceManager.lua" ->
      Seq("UI/InstanceManager.lua"),
    "Gameplay/Lua/GameplayUtilities.lua" ->
      Seq("Gameplay/Lua/GameplayUtilities.lua"))

  val mod2dlc_uuid = UUID.fromString("3d2df716-2c91-454f-8a5d-c21cfded78f8")
  def apply(target: File, assetDir: File, languageFile: File)
           (implicit logger: FileOperationLogger) =
    logger.logException {
      import logger._
      if(!target.exists) target.mkdirs()

      // TODO: Note if new expansions have been installed. Might be worth checking that for error detection.

      writeFile(new File(target, "Lua/m2d_core.lua"), LuaCode.core_library, " core library")
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
        writeFile(new File(target, patchTarget), LuaCode.core_entrypoint_hook+readFile(new File(assetDir, sourceFile)),
          " patch for entry points")
      }
    }
}
