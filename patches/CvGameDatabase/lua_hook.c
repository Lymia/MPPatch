/**
    Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>

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

#include <lua.h>
#include <lauxlib.h>
#include <string.h>
#include <windows.h>

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

static int luaHook_panic(lua_State *L) {
    char buffer[1024];
    snprintf(buffer, 1024, "Mod2DLC's Lua component encountered a critical error:\n%s", luaL_checkstring(L, 1));
    FatalAppExit(0, buffer);
}
static int luaHook_getCallerAtLevel(lua_State *L) {
    int level = luaL_checkinteger(L, 1);
    lua_Debug ar;
    if(lua_getstack(L, level+1, &ar) && lua_getinfo(L, "nS", &ar)) {
        lua_pushstring(L, ar.source);
        lua_pushstring(L, ar.short_src);
        if(ar.name) {
            lua_pushstring(L, ar.name);
            return 3;
        } else return 2;
    } else return 0;
}
static void luaTable_versioninfo(lua_State *L, int table) {
    table_setString (L, table, "component", "Mod2DLC CvGameDatabase patch");
    table_setInteger(L, table, "major", patchVersionMajor);
    table_setInteger(L, table, "minor", patchVersionMinor);
    table_setInteger(L, table, "mincompat", patchMinCompat);
}
static void luaTable_main(lua_State *L, int table) {
    table_setTable(L, table, "version", luaTable_versioninfo);
    table_setString(L, table, "credits", patchMarkerString);
    table_setCFunction(L, table, "panic", luaHook_panic);
    table_setCFunction(L, table, "getCallerAtLevel", luaHook_getCallerAtLevel);
}

__stdcall void LuaTableHookCore(lua_State *L, int table) {
    table_setTable(L, table, "__mod2dlc_patch", luaTable_main);
}

extern void LuaTableHook();
UnpatchData LuaTablePatch;
__attribute__((constructor(500))) static void installLuaHook() {
    LuaTablePatch = doPatch(lua_table_hook_offset, LuaTableHook, "LuaTableHook");
}
__attribute__((destructor(500))) static void destroyLuaHook() {
    unpatch(LuaTablePatch);
}