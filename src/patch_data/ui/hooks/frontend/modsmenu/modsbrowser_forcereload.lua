-- A strange and sporatic bug sometimes happens when SetActiveDLCAndMods is called multiple times in the modding
-- menu, then the modding menu is exited and reentered.
--
-- This seems to cause already enabled mods to not be properly cleared from the database and Civ V attempts to
-- load a mod that is already loaded. This causes unique key errors, and civilizations to fail to load. And probably
-- other nastiness too.

if _mpPatch and _mpPatch.loaded then
    Modding = _mpPatch.hookTable(Modding, {
        ActivateEnabledMods = function(...)
            _mpPatch.forceReloadMods()
            Modding._super.ActivateEnabledMods(...)
        end
    })
end