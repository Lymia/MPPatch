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

local mod_info
local mod_count = 0
function _mvmm.registerMod(coreVersion, id, modVersion, name, modData)
    _mvmm.debugPrint(" - Discovered mod "..name.." v"..modVersion.." (uuid: "..id..")")
    if coreVersion ~= _mvmm.version.major then
        _mvmm.print(" - WARNING: Mod expects v"..coreVersion..".x of the Multiverse Mod Manager runtime, but "..
                    _mvmm.versionString(_mvmm.version).." is currently installed. "..
                    "The mod may function incorrectly.")
    end
    mod_info[id] = { id=id, name=name, modVersion = modVersion }
    for k, v in pairs(modData) do
        mod_info[id][k] = v
    end
    mod_count = mod_count + 1
end

local function discoverMods()
    _mvmm.print("Discovering mods...")
    local packageIDs = ContentManager.GetAllPackageIDs()
    for _, v in ipairs(packageIDs) do
        if not ContentManager.IsUpgrade(v) and ContentManager.IsActive(v, ContentType.GAMEPLAY) then
            local canonical = v:lower():gsub("-", "")
            include("mvmm_modmanifest_"..canonical..".lua")
        end
    end
    _mvmm.print("Discovered "..mod_count.." mods.")
end

function _mvmm.getMods()
    if mod_info == nil then
        mod_info = {}
        discoverMods()
    end
    return mod_info
end

_mvmm.loadedModules.mods = true
