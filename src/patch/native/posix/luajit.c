/**
    Copyright (C) 2015-2017 Lymia Aluysia <lymiahugs@gmail.com>

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

#include <stdlib.h>
#include <dlfcn.h>

#include "c_rt.h"
#include "platform.h"
#include "config.h"

static const char* luaJITSymbols[] = {
    "lua_pushfstring", "luaL_typerror", "luaL_register", "lua_getfield", "lua_pushvfstring", "luaL_pushresult",
    "luaopen_io", "lua_xmove", "lua_pushthread", "luaL_openlib", "luaL_addlstring", "lua_resume", "luaL_optnumber",
    "lua_setfield", "luaopen_math", "lua_newuserdata", "luaJIT_profile_stop", "lua_gettable", "lua_gethookcount",
    "lua_settop", "lua_rawequal", "lua_pushstring", "luaL_execresult", "lua_dump", "luaL_where", "lua_error",
    "luaopen_ffi", "lua_settable", "luaL_loadfilex", "lua_getinfo", "lua_isnumber", "luaopen_base", "lua_equal",
    "lua_upvalueid", "lua_sethook", "lua_next", "luaL_checklstring", "lua_topointer", "lua_tothread", "lua_gc",
    "lua_createtable", "luaopen_jit", "lua_setfenv", "lua_pcall", "luaL_unref", "lua_call", "lua_type",
    "luaL_newmetatable", "lua_concat", "lua_gethookmask", "lua_lessthan", "lua_tocfunction", "luaL_loadbuffer",
    "lua_getmetatable", "luaL_loadstring", "luaopen_package", "lua_pushcclosure", "luaL_checkudata", "lua_gethook",
    "lua_newstate", "lua_getupvalue", "luaL_optlstring", "luaJIT_setmode", "luaL_newstate", "lua_pushnil", "luaL_gsub",
    "lua_getfenv", "luaL_findtable", "lua_objlen", "lua_remove", "lua_rawseti", "lua_close", "lua_pushlightuserdata",
    "luaL_addstring", "luaL_checkinteger", "lua_rawset", "luaL_loadbufferx", "luaL_checknumber", "luaopen_table",
    "lua_rawgeti", "luaopen_string", "luaL_loadfile", "luaL_addvalue", "lua_setupvalue", "luaopen_os", "luaL_callmeta",
    "lua_gettop", "lua_replace", "luaL_prepbuffer", "lua_cpcall", "lua_getlocal", "lua_pushvalue", "luaL_traceback",
    "lua_pushnumber", "luaL_ref", "lua_newthread", "lua_isstring", "lua_iscfunction", "lua_touserdata",
    "lua_tolstring", "lua_setlocal", "luaL_checkany", "lua_checkstack", "lua_getallocf", "luaL_optinteger",
    "luaL_checktype", "luaopen_bit", "lua_rawget", "lua_load", "luaL_checkstack", "lua_pushlstring", "lua_toboolean",
    "lua_insert", "luaL_checkoption", "luaL_fileresult", "luaopen_debug", "luaJIT_profile_start", "lua_tonumber",
    "lua_typename", "luaL_buffinit", "luaJIT_profile_dumpstack", "lua_tointeger", "luaL_error", "lua_pushboolean",
    "lua_status", "lua_yield", "luaL_openlibs", "luaL_getmetafield", "lua_upvaluejoin", "lua_setallocf",
    "lua_setmetatable", "lua_loadx", "lua_isuserdata", "lua_pushinteger", "lua_getstack", "luaL_argerror",
    "lua_atpanic"
};

__attribute__((constructor(CONSTRUCTOR_HOOK_INIT))) static void installLuaJIT() {
    if(enableLuaJIT) {
        debug_print("Loading LuaJIT...");

        char buffer[PATH_MAX];
        getSupportFilePath(buffer, LUAJIT_LIBRARY);

        void* luaJIT = dlopen(buffer, RTLD_NOW);
        if(luaJIT == NULL) fatalError("Could not open LuaJIT library: %s", dlerror());

        char symbol[512];
        for(int i=0; i < sizeof(luaJITSymbols) / sizeof(const char*); i++) {
            snprintf(symbol, sizeof(symbol), LUAJIT_SYMBOL_FORMAT, luaJITSymbols[i]);

            void* targetSym = resolveSymbol(CV_GAME_DATABASE, symbol);
            void* patchSym  = dlsym(luaJIT, symbol);

            if(targetSym == NULL) debug_print("WARNING: Symbol %s does not exist in Civ V binary.", symbol);
            if(patchSym  == NULL) debug_print("WARNING: Symbol %s does not exist in LuaJIT binary.", symbol);
            if(targetSym == NULL || patchSym == NULL) continue;

            patchJmpInstruction(targetSym, patchSym, symbol);
        }
    }
}