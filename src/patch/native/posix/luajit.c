/**
    Copyright (C) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>

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
    "lua_atpanic", "lua_call", "lua_checkstack", "lua_close", "lua_concat", "lua_copy", "lua_cpcall",
    "lua_createtable", "lua_dump", "lua_equal", "lua_error", "lua_gc", "lua_getallocf", "lua_getexdata",
    "lua_getexdata2", "lua_getfenv", "lua_getfield", "lua_gethook", "lua_gethookcount", "lua_gethookmask",
    "lua_getinfo", "lua_getlocal", "lua_getmetatable", "lua_getstack", "lua_gettable", "lua_gettop",
    "lua_getupvalue", "lua_insert", "lua_iscfunction", "lua_isnumber", "lua_isstring", "lua_isuserdata",
    "lua_isyieldable", "luaJIT_profile_dumpstack", "luaJIT_profile_start", "luaJIT_profile_stop",
    "luaJIT_setmode", "luaJIT_version_2_1_0_beta3", "luaL_addlstring", "luaL_addstring", "luaL_addvalue",
    "luaL_argerror", "luaL_buffinit", "luaL_callmeta", "luaL_checkany", "luaL_checkinteger", "luaL_checklstring",
    "luaL_checknumber", "luaL_checkoption", "luaL_checkstack", "luaL_checktype", "luaL_checkudata", "luaL_error",
    "lua_lessthan", "luaL_execresult", "luaL_fileresult", "luaL_findtable", "luaL_getmetafield", "luaL_gsub",
    "luaL_loadbuffer", "luaL_loadbufferx", "luaL_loadfile", "luaL_loadfilex", "luaL_loadstring",
    "luaL_newmetatable", "luaL_newstate", "lua_load", "lua_loadx", "luaL_openlib", "luaL_openlibs",
    "luaL_optinteger", "luaL_optlstring", "luaL_optnumber", "luaL_prepbuffer", "luaL_pushmodule",
    "luaL_pushresult", "luaL_ref", "luaL_register", "luaL_setfuncs", "luaL_setmetatable", "luaL_testudata",
    "luaL_traceback", "luaL_typerror", "luaL_unref", "luaL_where", "lua_newstate", "lua_newthread",
    "lua_newuserdata", "lua_next", "lua_objlen", "luaopen_base", "luaopen_bit", "luaopen_debug", "luaopen_ffi",
    "luaopen_io", "luaopen_jit", "luaopen_math", "luaopen_os", "luaopen_package", "luaopen_string",
    "luaopen_table", "lua_pcall", "lua_pushboolean", "lua_pushcclosure", "lua_pushfstring", "lua_pushinteger",
    "lua_pushlightuserdata", "lua_pushlstring", "lua_pushnil", "lua_pushnumber", "lua_pushstring",
    "lua_pushthread", "lua_pushvalue", "lua_pushvfstring", "lua_rawequal", "lua_rawget", "lua_rawgeti",
    "lua_rawset", "lua_rawseti", "lua_remove", "lua_replace", "lua_resetthread", "lua_resume", "lua_setallocf",
    "lua_setexdata", "lua_setexdata2", "lua_setfenv", "lua_setfield", "lua_sethook", "lua_setlocal",
    "lua_setmetatable", "lua_settable", "lua_settop", "lua_setupvalue", "lua_status", "lua_toboolean",
    "lua_tocfunction", "lua_tointeger", "lua_tointegerx", "lua_tolstring", "lua_tonumber", "lua_tonumberx",
    "lua_topointer", "lua_tothread", "lua_touserdata", "lua_type", "lua_typename", "lua_upvalueid",
    "lua_upvaluejoin", "lua_version", "lua_xmove", "lua_yield", "recff_thread_exdata", "recff_thread_exdata2"
};

__attribute__((constructor(CONSTRUCTOR_HOOK_INIT))) static void installLuaJIT() {
    if(enableLuaJIT) {
        debug_print("Loading LuaJIT...");

        char buffer[PATH_MAX];
        getSupportFilePath(buffer, LUAJIT_LIBRARY);

        void* luaJIT = dlopen(buffer, RTLD_LOCAL | RTLD_NOW);
        if(luaJIT == NULL) fatalError("Could not open LuaJIT library: %s", dlerror());

        for(int i=0; i < sizeof(luaJITSymbols) / sizeof(const char*); i++) {
            const char* symbol = luaJITSymbols[i];

            void* targetSym = resolveSymbol(symbol);
            void* patchSym  = dlsym(luaJIT, symbol);

            if(targetSym == NULL) debug_print("WARNING: Symbol %s does not exist in Civ V binary.", symbol);
            if(patchSym  == NULL) debug_print("WARNING: Symbol %s does not exist in LuaJIT binary.", symbol);
            if(targetSym == NULL || patchSym == NULL) continue;

            patchJmpInstruction(targetSym, patchSym, symbol);
        }
    }
}