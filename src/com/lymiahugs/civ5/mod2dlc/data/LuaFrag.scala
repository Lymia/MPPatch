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

package com.lymiahugs.civ5.mod2dlc.data

object LuaFrag {
  def includeOverrideHeader(luaFilePrefix: String) =
    """
      |-- Injected by Mod2Dlc
      |-- Hook include so it loads files with our unique prefix!
      |local include = include
      |do
      |  local include_ = include
      |  local prefix = "__PREFIX__"
      |  -- TODO: Make this return the right values somehow??
      |  local function include(str, ...)
      |    include_(str, ...)
      |    return include_(prefix..str, ...)
      |  end
      |end
      |-- End injection
      |
    """.stripMargin.replace("__PREFIX__", luaFilePrefix)

  def mod_datafile =
    """
      |-- Get the path of the currently executing Lua file using some very evil magic.
      |local currentExecutingPath
      |do
      |  local luaFile = ("").dump(function() end)
      |  local luaFile_expectedHeader = "\27\76\117\97\81\0\1\4\4\4\8\0"
      |  local luaFile_header = luaFile:sub(1, #luaFile_expectedHeader)
      |  assert(luaFile_header == luaFile_expectedHeader, "Unexpected Lua bytecode format!")
      |  local function decode_u32(string, i)
      |    return string:byte(i+0) * 0x00000001 +
      |           string:byte(i+1) * 0x00000100 +
      |           string:byte(i+2) * 0x00010000 +
      |           string:byte(i+3) * 0x01000000
      |  end
      |  local str_len = decode_u32(luaFile, 13)
      |  currentExecutingPath = luaFile:sub(17, 17+str_len)
      |end
    """.stripMargin

  val legalScreenHook =
    """
      |-- Injected by Mod2Dlc
      |__mod2dlc_api = {}
      |
      |local mod_info = {}
      |function __mod2dlc_api.registerMod(name, entryPoints, sqlUpdates)
      |  mod_info[name] = {name=name, entryPoints=entryPoints, sqlUpdates=sqlUpdates}
      |end
      |
      |function __mod2dlc_api.runSqlUpdates()
      |  for _, mod in pairs(mod_info) do
      |    for _, sqlUpdate in ipairs(mod.sqlUpdates) do
      |      DB.StatementSQL(sqlUpdate)
      |    end
      |  end
      |end
      |
      |function __mod2dlc_api.
      |-- End injection
    """.stripMargin
}
