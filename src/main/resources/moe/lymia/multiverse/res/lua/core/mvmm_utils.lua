-- Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.

_mvmm.loadedModules.utils = true

local function decode_u32(string, i)
    return string:byte(i+0) * 0x00000001 +
           string:byte(i+1) * 0x00000100 +
           string:byte(i+2) * 0x00010000 +
           string:byte(i+3) * 0x01000000
end
function _mvmm.getSourcePath(fn)
    local luaFile = ("").dump(fn)
    local luaFile_expectedHeader = "\27\76\117\97\81\0\1\4\4\4\8\0"
    local luaFile_header = luaFile:sub(1, #luaFile_expectedHeader)
    assert(luaFile_header == luaFile_expectedHeader, "Unexpected Lua bytecode format!")

    local str_len = decode_u32(luaFile, 13)
    return luaFile:sub(17, 17+str_len-2)
end
function _mvmm.cleanSourcePath(str)
    return str:gsub("[^/\\]*[/\\][^/\\]*%.lua$", ""):gsub("\\", "/")
end
function _mvmm.getAssetPrefix(fn)
    return _mvmm.cleanSourcePath(_mvmm.getSourcePath(fn))
end

function _mvmm.panic(s)
    print(s)
    patch.panic(s)
end
function _mvmm.versionString(versioninfo)
    return "v" .. versioninfo.major .. "." .. versioninfo.minor
end

function _mvmm.print(s)
    print("[Multiverse Mod Manager] "..s)
end
function _mvmm.debugPrint(s)
    if _mvmm.patch.debug then
        _mvmm.print(s)
    end
end