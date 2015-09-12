-- Injected by Multiverse Mod Manager
include "mvmm_runtime"
if not mod2dlc.disabled then
    local moduleName = mod2dlc.getSourcePath(function() end):gsub(".*\\(.*)%.lua", "%1")
    mod2dlc.tryHook(moduleName)
end
-- End Injection

