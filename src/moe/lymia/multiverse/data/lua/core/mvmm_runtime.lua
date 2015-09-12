-- Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy of
-- this software and associated documentation files (the "Software"), to deal in
-- the Software without restriction, including without limitation the rights to
-- use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
-- of the Software, and to permit persons to whom the Software is furnished to do
-- so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.

_mvmm = {}
_mvmm.version = {major=1, minor=0 }
local requestedPatchVersion = 1

local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70").__mvmm_load_patch
if patch then patch = patch() end
_mvmm.patch = patch

if not patch then
    _mvmm = nil
    print("Multiverse Mod Manager requires the CvGameDatabase patch to installed to function correctly!")
    print("The core library will be disabled.  Most installed mods will not function correctly.")
    -- TODO: Automatically disable all converted mods in this case. ... that or...
    -- TODO: Deliberately exploit some Lua flaw to crash the game? Something that causes an instant segfault.
    return
end

function _mvmm.panic(s)
    print(s)
    patch.panic(s)
end
function _mvmm.versionString(versioninfo)
    return "v" .. versioninfo.major .. "." .. versioninfo.minor
end

print("Loading Multiverse Mod Manager runtime "..versionString(_mvmm.version)..".")
if patch.version.major ~= requestedPatchVersion then
    panic("Wrong version of the Multiverse Mod Manager CvGameDatabase patch installed! "..
          "(Expected v"..requestedPatchVersion..".x, found "..versionString(patch.version)..")")
    return
end
if patch.debug then
    print("Debug version of CvGameDatabase patch installed. This will create a logging file in addition to "..
          "allowing access to Lua functions that are otherwise not accessible from Lua code.")
end

_mvmm.loadedModules = {}
local function loadModule(name)
    include("mvmm_"..name..".lua")
    if not _mvmm.loadedModules[name] then
        print("WARNING: Could not load module "..name..".")
    end
end

loadModule("mods")
loadModule("moddinghook")

function _mvmm.installHooks()
    _mvmm.installModdingHook()
end