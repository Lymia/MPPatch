-- Create the _mpPatch table
local function createTable()
    _mpPatch = {}
    _mpPatch._mt = {}
    setmetatable(_mpPatch, _mpPatch._mt)
end

-- Called to signal that the MPPatch loading has failed
local function loadFailed(errorStr, statusMarker)
    -- Try to print in the native logs, if possible
    if _mpPatch and _mpPatch.patch then
        _mpPatch.patch.debugPrint("Cannot load due to critical error: " .. errorStr)
    end

    -- Print to the Lua logs
    print("[MPPatch] Cannot load due to critical error: " .. errorStr)

    -- Replace _mpPatch with a sentinel table
    createTable()
    _mpPatch.loaded = false
    _mpPatch.status = {}
    if statusMarker then
        _mpPatch.status[statusMarker] = true
    end
    function _mpPatch._mt.__index(_, k)
        error("Access to field " .. k .. " in MpPatch runtime without patch installed.")
    end
    function _mpPatch._mt.__newindex(_, k, v)
        error("Write to field " .. k .. " in MpPatch runtime without patch installed.")
    end
end

-- Retrieve the binary patch API, if it is installed
local patch = _MPPatch_Core
if not patch or not patch.__mppatch_marker then
    loadFailed("Could not load binary patch.", "binaryLoadFailed")
    return
end
createTable()
_mpPatch.patch = patch

-- Check the version information to make sure nothing has gone wrong
do
    include "mppatch_version.lua"
    if not _mpPatch.version or not _mpPatch.version.loaded then
        loadFailed("Could not load version information.")
        return
    end
    local expectedVersion = "0.2.0-alpha.2"
    if expectedVersion ~= patch.version.versionString then
        local format = "Version mismatch. (platform: %s, got: %s, expected: %s)"
        loadFailed(format:format(patch.version.platform, patch.version.buildId, expectedBuildId))
        return
    end
end

-- Load the actual _mpPatch runtime contents
_mpPatch.versionString = _mpPatch.version.info["mppatch.version.string"] or "<unknown version>"
_mpPatch.context = "<init>"
_mpPatch.loaded = true
_mpPatch.debug = patch.config.enableDebug
function _mpPatch.debugPrint(...)
    local args = { ... }
    local accum = ""
    local count = 0
    for k, _ in pairs(args) do
        if k > count then
            count = k
        end
    end
    for i = 1, count do
        accum = accum .. tostring(args[i])
        if i ~= count then
            accum = accum .. "\t"
        end
    end

    print("[MPPatch] " .. accum)
    patch.debugPrint(_mpPatch.fullPath .. ": " .. accum)
end

_mpPatch.hooks = {}

include "mppatch_mtutils.lua"
include "mppatch_utils.lua"
include "mppatch_modutils.lua"
include "mppatch_serialize.lua"
include "mppatch_uiutils.lua"
include "mppatch_chatprotocol.lua"

_mpPatch.context = _mpPatch.fullPath
_mpPatch.debugPrint("MPPatch runtime loaded in " .. _mpPatch.context)
