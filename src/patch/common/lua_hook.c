/**
    Copyright (C) 2015-2016 Lymia Aluysia <lymiahugs@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
    of the Software, and to permit persons to whom the Software is furnished to do
    so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/

// This hooks Database::Scripting::Lua::lGetMemoryUsage, injecting tables into the table loaded
// by DB.GetMemoryUsage(). This lets us add new functionality for Lua scripts while the core DLC
// is installed, allowing us to do some things that would be otherwise impossible in Lua code.
//
// Like throwing a fatal error. :|

#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>

#include "c_rt.h"
#include "c_defines.h"
#include "extern_defines.h"
#include "version.h"
#include "lua_hook.h"
#include "net_hook.h"

// Setup new Lua tables
#define LuaTableHook_REGINDEX "2c11892f-7ad1-4ea1-bc4e-770a86c387e6"
#define LuaTableHook_SENTINEL "216f0090-85dd-4061-8371-3d8ba2099a70"

static void table_setTable(lua_State *L, int table, const char* name, void (*fn)(lua_State *L, int table)) {
    lua_pushstring(L, name);
    lua_createtable(L, 0, 0);
    int top = lua_gettop(L);
    fn(L, top);
    lua_rawset(L, table);
}
static void table_setInteger(lua_State *L, int table, const char* name, lua_Integer val) {
    lua_pushstring(L, name);
    lua_pushinteger(L, val);
    lua_rawset(L, table);
}
static void table_setString(lua_State *L, int table, const char* name, const char* val) {
    lua_pushstring(L, name);
    lua_pushstring(L, val);
    lua_rawset(L, table);
}
static void table_setCFunction(lua_State *L, int table, const char* name, lua_CFunction val) {
    lua_pushstring(L, name);
    lua_pushcfunction(L, val);
    lua_rawset(L, table);
}

static int luaHook_NetPatch_pushMod(lua_State *L) {
    NetPatch_pushMod(luaL_checkstring(L, 1), luaL_checkinteger(L, 2));
    return 0;
}
static int luaHook_NetPatch_overrideReloadMods(lua_State *L) {
    NetPatch_overrideReloadMods(luaL_checkinteger(L, 1) != 0);
    return 0;
}
static int luaHook_NetPatch_overrideModList(lua_State *L) {
    NetPatch_overrideModList();
    return 0;
}

static int luaHook_NetPatch_pushDLC(lua_State *L) {
    NetPatch_pushDLC(luaL_checkinteger(L, 1), luaL_checkinteger(L, 2), luaL_checkinteger(L, 3),
                     (((uint64_t) luaL_checkinteger(L, 4) << 32) & 0xFFFFFFFF) | (luaL_checkinteger(L, 5) & 0xFFFFFFFF));
    return 0;
}
static int luaHook_NetPatch_overrideReloadDLC(lua_State *L) {
    NetPatch_overrideReloadDLC(luaL_checkinteger(L, 1) != 0);
    return 0;
}
static int luaHook_NetPatch_overrideDLCList(lua_State *L) {
    NetPatch_overrideDLCList();
    return 0;
}

static int luaHook_NetPatch_reset(lua_State *L) {
    NetPatch_reset();
    return 0;
}
static void luaTable_NetPatch(lua_State *L, int table) {
    table_setCFunction(L, table, "pushMod"           , luaHook_NetPatch_pushMod           );
    table_setCFunction(L, table, "overrideReloadMods", luaHook_NetPatch_overrideReloadMods);
    table_setCFunction(L, table, "overrideModList"   , luaHook_NetPatch_overrideModList   );

    table_setCFunction(L, table, "pushDLC"           , luaHook_NetPatch_pushDLC           );
    table_setCFunction(L, table, "overrideReloadDLC" , luaHook_NetPatch_overrideReloadDLC );
    table_setCFunction(L, table, "overrideDLCList"   , luaHook_NetPatch_overrideDLCList   );

    table_setCFunction(L, table, "reset"             , luaHook_NetPatch_reset             );
}

static int luaHook_panic(lua_State *L) {
    fatalError("[MPPatch] Critical error in Lua code:\n%s", luaL_checkstring(L, 1));
}
static void luaTable_versioninfo(lua_State *L, int table) {
    table_setInteger(L, table, "major", patchVersionMajor);
    table_setInteger(L, table, "minor", patchVersionMinor);
    table_setInteger(L, table, "compatVersion", patchCompatVersion);
    table_setString (L, table, "versionString", patchFullVersion);
}

static void lua_pushGlobals(lua_State *L) {
    lua_pushstring(L, ""); // S
    lua_pushstring(L, "gsub"); // S S
    lua_gettable(L, -2);
    lua_getfenv(L, -1);
    lua_insert(L, -3);
    lua_pop(L, 2);
}

#ifdef DEBUG
    static void luaTable_debug(lua_State *L, int table) {
        lua_pushstring(L, "globals");
        lua_pushGlobals(L);
        lua_rawset(L, table);

        lua_pushstring(L, "_G");
        lua_pushvalue(L, LUA_GLOBALSINDEX);
        lua_rawset(L, table);

        lua_pushstring(L, "registry");
        lua_pushvalue(L, LUA_REGISTRYINDEX);
        lua_rawset(L, table);
    }
#endif
static void luaTable_pushSharedState(lua_State *L) {
    lua_pushstring(L, LuaTableHook_REGINDEX);
    lua_gettable(L, LUA_REGISTRYINDEX);
    if(lua_isnil(L, lua_gettop(L))) {
        lua_pop(L, 1);
        lua_createtable(L, 0, 0);
        int table = lua_gettop(L);

        lua_pushstring(L, LuaTableHook_REGINDEX);
        lua_pushvalue(L, table);
        lua_settable(L, LUA_REGISTRYINDEX);
    }
}

static const char* copyList[] = {"rawset", "rawget", NULL};
static void luaTable_globals(lua_State *L, int table) {
    lua_pushGlobals(L);
    int globals = lua_gettop(L);
    for(int i=0; copyList[i] != NULL; i++) {
        const char* name = copyList[i];
        lua_pushstring(L, name);
        lua_pushstring(L, name);
        lua_gettable(L, globals);
        lua_rawset(L, table);
    }
    lua_pop(L, 1);
}

lGetMemoryUsage_t lGetMemoryUsage;
ENTRY lGetMemoryUsage_attributes int lGetMemoryUsageProxy(lua_State *L) {
    if(lua_type(L, 1) == LUA_TSTRING && !strcmp(luaL_checkstring(L, 1), LuaTableHook_SENTINEL)) {
        debug_print("Found sentinel value, returning MPPatch table.")

        lua_createtable(L, 0, 0);
        int table = lua_gettop(L);

        table_setInteger(L, table, "__mppatch_marker", 1);
        table_setTable(L, table, "version", luaTable_versioninfo);
        table_setTable(L, table, "NetPatch", luaTable_NetPatch);
        table_setTable(L, table, "globals", luaTable_globals);
        table_setString(L, table, "credits", patchMarkerString);
        table_setCFunction(L, table, "panic", luaHook_panic);

        lua_pushstring(L, "shared");
        luaTable_pushSharedState(L);
        lua_rawset(L, table);

        #ifdef DEBUG
            table_setTable(L, table, "debug", luaTable_debug);
        #endif

        return 1;
    } else {
        debug_print("lGetMemoryUsage called, but sentinel value not found. Calling original function.")
        return lGetMemoryUsage(L);
    }
}
