-- Injected by Mod2DLC
include "m2d_core"
if not mod2dlc.disabled then
    local moduleName = mod2dlc.getSourcePath(function() end):gsub(".*\\(.*)%.lua", "%1")
    mod2dlc.tryHook(moduleName)
end
-- End Injection

