Binary Patch Implementation Details
===================================

Multiverse Mod Manager's patch hooks two functions in CvGameDatabase:
 
 * `Database::XMLSerializer::LoadFromMemory`, in order to add a new XML tag that allows us to encapsulate SQL to be run
   directly with `Database::Connection::ExecuteMultiple`, allowing us to translate .sql files to .xml files sanely.
 * `Database::Scripting::Lua::lGetMemoryUsage`, in order to add new functions to the Lua API for Multiverse Mod
   Manager's Lua runtime to use.
   
Lua Patch API Details (lua_hook.c)
----------------------------------

In Lua, the function our patch hooks is called `DB.GetMemoryUsage`. The hook checks if a marker value is passed into
the function, and if it is, adds a function to the table it returns named `__mvmm_load_patch` which loads the patch's
Lua API. Thus, to load the patch:

```lua
local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70").__mvmm_load_patch
if patch then patch = patch() end
```

From here, it exports a few functions and tables:

 * `patch.version`, containing version information for the patch.
 * `patch.panic(string)`, a function allowing a Lua script to report a critical error and bail.
 * `patch.shared`, a table that's shared between different script contexts.

In addition, if a debug version of the patch is installed, additional tables are exported in `patch.debug`:

 * `patch.debug.globals`, the contents of the actual `_G` table of the `Lua_State` used by Civilization V. This allows
   lua code to access functions it normally can't access, such as getfenv. Obviously this breaks security wide open.
 * `patch.debug.registry`, the contents of the Lua registery. (pesudo-index LUA_REGISTRYINDEX)

XML Patch Details (xml_hook.c)
------------------------------

In addition to hooking Lua, the patch adds two additional tags to the .xml serialization format that Civilization V
uses. Mods can use .sql files to modify the database, while DLC file can only use .xml files. A good portion of mods
wouldn't run without this capability, which has been a stumbling block for JdH's CiV MP Mod Manager, and the reason
MPMPM was written.

The patch adds the following tags to the .xml serialization format:

 * `<__MVMM_PATCH_IGNORE>`, which causes all its contents to be ignored. This is used to is used to create an canary
   value to cause errors when someone tries to load a converted .sql file without the patch installed.
 * `<__MVMM_PATCH_RAWSQL>`, which runs SQL code directly through `Database::Connection::ExecuteMultiple`, allowing us
   to easily translate .sql files into .xml files. The contents of this tag are base64 encoded before being stored in
   this tag, as preserving newlines is important for .sql files. Unfortunately, as Scala has crappy support for it, and
   Civiliation V's XML library behaves weirdly when it is used, CDATA could not be used.

Platform-specific Details
-------------------------

On Windows, the patch creates a proxy around the CvGameDatabase.dll file, which contains both of the patched functions.

On Linux, however, the CvGameDatabase binary is statically compiled into the main binary, forcing us to patch the main
binary instead of a shared library. Luckily, LD_PRELOAD lets us inject a patch into the main binary directly. Notably,
on Linux, public symbols and many private symbols are *not* stripped, and are, in fact, exported. This means the patch
doesn't need offsets for every function it uses hardcoded into it.