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

_mpPatch = {}
_mpPatch._mt = {}
setmetatable(_mpPatch, _mpPatch._mt)

local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70")

if not patch.__mppatch_marker then
    _mpPatch.enabled = false
    _mpPatch.canEnable = false
    function _mpPatch._mt.__index(_, k)
        error("Access to field "..k.." in MpPatch runtime without patch installed.")
    end
    return
end

_mpPatch.patch = patch
_mpPatch.versionString = patch.version.versionString

_mpPatch.context = "<init>"
_mpPatch.uuid = "df74f698-2343-11e6-89c4-8fef6d8f889e"
_mpPatch.enabled = ContentManager.IsActive(_mpPatch.uuid, ContentType.GAMEPLAY)
_mpPatch.canEnable = true

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

    print(accum)
    patch.debugPrint(_mpPatch.fullPath..": "..accum)
end

include "mppatch_mtutils.lua"
include "mppatch_utils.lua"
include "mppatch_modutils.lua"
include "mppatch_uiutils.lua"

_mpPatch.debugPrint("MPPatch runtime loaded")
_mpPatch.context = _mpPatch.fullPath
_mpPatch.debugPrint("Current UI path: ".._mpPatch.context)
