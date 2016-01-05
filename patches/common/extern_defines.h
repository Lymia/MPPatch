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

#ifndef EXTERN_DEFINES_H
#define EXTERN_DEFINES_H

#include <stddef.h>
#include <stdbool.h>

// Database library components
typedef struct class_Database class_Database;
bool Database_ExecuteMultiple(class_Database* this, const char* string, int length);
bool Database_LogMessage     (class_Database* this, const char* string);

// XML library components
typedef struct class_XmlNode  class_XmlNode;
bool XmlNode_NameMatches(class_XmlNode* this, const char* string, size_t* tagSize);
void XmlNode_GetValUtf8 (class_XmlNode* this, char** string_out, size_t* length_out);

// Lua library components
typedef struct lua_State lua_State;
typedef ptrdiff_t lua_Integer;

int lua_gettop (lua_State *L);
void  lua_settop (lua_State *L, int index);
void  lua_pushvalue (lua_State *L, int index);
#define lua_pop(L,n)  lua_settop(L, -(n)-1)

#define LUA_REGISTRYINDEX       (-10000)
#define LUA_GLOBALSINDEX        (-10002)
void lua_gettable (lua_State *L, int index);
void lua_settable (lua_State *L, int index);

void lua_createtable (lua_State *L, int narr, int nrec);
void lua_rawset (lua_State *L, int index);

const char *luaL_checklstring (lua_State *L, int narg, size_t *len);
#define luaL_checkstring(L,n)   (luaL_checklstring(L, (n), NULL))
lua_Integer luaL_checkinteger (lua_State *L, int narg);

int lua_type (lua_State *L, int idx);
#define LUA_TNIL                0
#define LUA_TSTRING             4
#define lua_isnil(L,n)          (lua_type(L, (n)) == LUA_TNIL)

void lua_pushstring (lua_State *L, const char *s);
void lua_pushinteger (lua_State *L, lua_Integer n);

typedef int (*lua_CFunction) (lua_State *L);
void lua_pushcclosure (lua_State *L, lua_CFunction fn, int n);
#define lua_pushcfunction(L,f)  lua_pushcclosure(L,f,0)

#endif /* EXTERN_DEFINES_H */
