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

local function createTable()
    _mpPatch = {}
    _mpPatch._mt = {}
    setmetatable(_mpPatch, _mpPatch._mt)
end
local function patchCriticalError(error, statusMarker)
    createTable()
    print("[MPPatch] Cannot load due to critical error: "..error)
    _mpPatch.enabled = false
    _mpPatch.canEnable = false
    if statusMarker then _mpPatch[statusMarker] = true end
    function _mpPatch._mt.__index(_, k)
        error("Access to field "..k.." in MpPatch runtime without patch installed.")
    end
    function _mpPatch._mt.__newindex(_, k, v)
        error("Write to field "..k.." in MpPatch runtime without patch installed.")
    end
end

local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70")
if not patch.__mppatch_marker then
    patchCriticalError("Could not load binary patch.")
    return
end

createTable()
_mpPatch.patch = patch

do
    include "mppatch_version.lua"
    if not _mpPatch.version then
        patchCriticalError("Could not load version information.")
        return
    end
    local platformString = patch.version.platform.."_"..patch.version.sha256
    local expectedBuildId = _mpPatch.version.buildId[platformString]
    if not expectedBuildId or expectedBuildId ~= patch.version.buildId then
        patchCriticalError("BuildID mismatch.")
        return
    end
end

_mpPatch.versionString = patch.version.versionString
_mpPatch.context = "<init>"
_mpPatch.uuid = "df74f698-2343-11e6-89c4-8fef6d8f889e"
_mpPatch.enabled = ContentManager.IsActive(_mpPatch.uuid, ContentType.GAMEPLAY)
_mpPatch.canEnable = true
_mpPatch.debug = patch.config.enableDebug
function _mpPatch.debugPrint(...)
    local args = {...}
    local accum = ""
    local count = 0
    for k, _ in pairs(args) do
        if k > count then count = k end
    end
    for i=1,count do
        accum = accum .. args[i]
        if i ~= count then accum = accum .. "\t" end
    end

    print("[MPPatch] "..accum)
    patch.debugPrint(_mpPatch.fullPath..": "..accum)
end

include "mppatch_mtutils.lua"
include "mppatch_utils.lua"
include "mppatch_modutils.lua"
include "mppatch_uiutils.lua"

_mpPatch.debugPrint("MPPatch runtime loaded")
_mpPatch.context = _mpPatch.fullPath
_mpPatch.debugPrint("Current UI path: ".._mpPatch.context)
