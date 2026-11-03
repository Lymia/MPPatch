local LookUpControl = ContextPtr.LookUpControl -- cached because we override this in some of our hooks

_mpPatch._mt.registerLazyVal("fullPath", function()
    local accum = ContextPtr:GetID()
    local path = ".."
    local seen = {}
    while true do
        local currentContext = LookUpControl(ContextPtr, path)
        if not currentContext then break end
        if seen[currentContext:GetID()] then
            return "{...}/"..accum
        end
        seen[currentContext:GetID()] = true
        accum = currentContext:GetID().."/"..accum
        path = path.."/.."
    end
    return accum
end)

function _mpPatch.loadElementFromProxy(proxyName, controlName)
    local proxy = ContextPtr:LoadNewContext(proxyName)
    Controls[controlName] = LookUpControl(proxy, controlName)
end

function _mpPatch.setBIsModding()
    function ContextPtr.LookUpControl()
        return {
            GetID = function() return "ModMultiplayerSelectScreen" end
        }
    end
end

-- Update function hooking
function _mpPatch.hookUpdate()
    local onUpdate = _mpPatch.event.update
    ContextPtr:SetUpdate(function(...) return onUpdate(...) end)
end
function _mpPatch.unhookUpdate()
    ContextPtr:ClearUpdate()
end