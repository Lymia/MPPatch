Implementation Details
======================

For some reason, Firaxis decided to not release Civilization V with multiplayer modding support enabled, even though
much of the code for it already existed. Reactivating it is not possible solely from Lua code, as
`Matchmaking.LaunchMultiplayerGame` eventually ends up calling `CvModdingFrameworkAppSide::SetActiveDLCandMods` with an
empty mods list, hence deactivating all mods that may have been active. There seems to be no way around this, thus, a
patch to the binary is needed.
   
Patch technical details
-----------------------

The functions our patch hooks is exported to Lua code as `DB.GetMemoryUsage`. The hook checks if a marker value is
passed into the function, and if it is, returns a different table that contains native patch's Lua API. Thus, to load
the injected API:

```lua
local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70")
if not patch.__mppatch_marker then error("Native patch not installed!") end
```

This exports a few functions and tables:

 * `patch.version`, containing version information for the patch.
 * `patch.panic(string)`, a function allowing a Lua script to report a critical error and bail.
 * `patch.debugPrint(string)`, a function allowing a Lua script to output to `mppatch_debug.log`.
 * `patch.shared`, a table that's shared between different script contexts.
 * `patch.NetPatch`, a table contains functions that controls our modification to the `SetActiveDLCAndMods` function.
 * `patch.globals`, a table containing global functions normally unavailable to Lua code that our UI patch uses.
   Currently, the `rawget` and `rawset` functions are exposed through this table.

Our patch to `SetActiveDLCandMods` fundamentally works by replacing the arguments to the functions in a way controlled
by Lua code. These functions are exported in the `patch.NetPatch` table, containing the following functions:

 * `patch.NetPatch.pushMod(string modId, int version)` which pushes a mod to the list that will be loaded instead of
   whatever the original function passes. Notably, this list starts empty, and will *not* include the original list.
 * `patch.NetPatch.overrideModList()` which must be called to actually replace the original mod list with the one built
   up using `pushMod`.
 * `patch.NetPatch.overrideReloadMods(int value)`, which overrides the flag passed to `SetActiveDLCAndMods` which
   controls if mods are reloaded. Although I'm not sure of this, this flag seems to force mods to be reloaded,
   regardless of whether the mod list has changed at all. All non-zero values passed in are treated as `true`, and zero
   is treated as `false`.
 * `patch.NetPatch.pushDLC(int guid_1, int guid_2, int guid_3, int guid_4_high, int guid_4_low)`,
   `patch.NetPatch.overrideDLCList()`, and `patch.NetPatch.overrideReloadDLC(int value)` function identically to the
   above functions, except they operate on the DLC list instead of the mod list. The parameters to `pushDLC` are the 4
   fields of the in-memory storage format Microsoft uses for GUIDs. The `Data4` field is split into two values,
   `guid_4_high`, containing the most significant 32 bits, and `guid_4_low`, containing the least significant 32 bits.
   See [here](https://msdn.microsoft.com/en-us/library/windows/desktop/aa373931\(v=vs.85\).aspx) for documentation.
 * `patch.NetPatch.install()`, which causes the patch to SetActiveDLCandMods to actually be written to memory. Steam
    CEG or some other DRM system seems to interfere with the functioning of the patch, and this is an attempt to dodge
    it.
 * `patch.NetPatch.reset()`, which clears any overrides set. This function is automatically called after every time the
   `SetActiveDLCAndMods` function is called for any reason to avoid any unintentional overrides.

Platform-specific Details
-------------------------

On Windows, the patch creates a proxy around the CvGameDatabase.dll file, which gives us a foothold to patch the main
binary, as well as providing our entry point to export functions to Lua. There are three versions of the main binary,
so, we have to provide 3 sets of offsets for the function we override in it. Unfortunately, due to Steam's CEG DRM,
we cannot use hashes of the binary to identify them, so we use executable names intead.

On Linux, however, the CvGameDatabase binary is statically compiled into the main binary, forcing us to patch the main
binary instead of a shared library. Luckily, LD_PRELOAD lets us inject a patch into the main binary directly. Notably,
on Linux, public symbols and many private symbols are *not* stripped, and are, in fact, exported. This means the patch
doesn't need offsets for every function it uses hardcoded into it.
