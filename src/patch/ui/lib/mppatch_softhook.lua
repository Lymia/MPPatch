do
    local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70")
    local debugPrint
    if patch.__mppatch_marker then
        function debugPrint(line)
            patch.debugPrint(line)
            print("[MPPatch] "..line)
        end
    else
        function debugPrint(line)
            print("[MPPatch] "..line)
        end
    end

    include "mppatch_softhook_info.lua"
    if _mpPatch_SoftHookInfo and ContextPtr and ContextPtr:GetID() then
        local contextId = ContextPtr:GetID()
        local data = _mpPatch_SoftHookInfo[contextId]
        if data and not data.loaded then
            debugPrint("Loading soft hook for context "..contextId.."...")

            data.loaded = true
            for _, v in ipairs(data.include) do
                debugPrint("- Loading include "..v)
                include(v)
            end
            for _, v in ipairs(data.inject) do
                debugPrint("- Injecting "..v)
                include(v)
            end
        end
    end
end