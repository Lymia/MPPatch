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

typedef void lua_State;
typedef ptrdiff_t lua_Integer;

extern __stdcall void lua_createtable (lua_State *L, int narr, int nrec);
extern __stdcall int  lua_gettop      (lua_State *L);
extern __stdcall void lua_pushstring  (lua_State *L, const char *s);
extern __stdcall void lua_pushinteger (lua_State *L, lua_Integer n);
extern __stdcall void lua_rawset      (lua_State *L, int index);

static void createTable(lua_State *L, int table, const char* name, void (*fn)(lua_State *L, int table)) {
    lua_pushstring(L, name);
    lua_createtable(L, 0, 0);
    int top = lua_gettop(L);
    fn(L, top);
    lua_rawset(L, table);
}
static void pushInteger(lua_State *L, int table, const char* name, lua_Integer val) {
    lua_pushstring(L, name);
    lua_pushinteger(L, val);
    lua_rawset(L, table);
}
static void pushString(lua_State *L, int table, const char* name, const char* val) {
    lua_pushstring(L, name);
    lua_pushstring(L, val);
    lua_rawset(L, table);
}

static void luaTable_versioninfo(lua_State *L, int table) {
    pushInteger(L, table, "major", patchVersionMajor);
    pushInteger(L, table, "minor", patchVersionMinor);

    pushInteger(L, table, "mincompat", patchMinCompat);
    pushInteger(L, table, "core_mincompat", patchCoreMinCompat);
}
static void luaTable_main(lua_State *L, int table) {
    createTable(L, table, "versioninfo", luaTable_versioninfo);
    pushString(L, table, "creditstring", patchMarkerString);
}

__stdcall void LuaTableHookCore(lua_State *L, int table) {
    createTable(L, table, "__mod2dlc_patch", luaTable_main);
}

extern void LuaTableHook();
UnpatchData LuaTablePatch;
__attribute__((constructor(500))) static void installLuaHook() {
    LuaTablePatch = doPatch(lua_table_hook_offset, LuaTableHook);
}
__attribute__((destructor(500))) static void destroyLuaHook() {
    unpatch(LuaTablePatch);
}