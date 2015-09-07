/**
    Copyright (C) 2015 Lymia Aluysia <lymiahugs@gmail.com>

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

#ifndef C_DEFINES_H
#define C_DEFINES_H

#define Database_ExecuteMultiple_offset 0x08B95426
#define Database_LogMessage_offset      0x08B9564E

#define XmlNode_NameMatches_offset      0x08C5786C
#define XmlNode_GetValUtf8_offset       0x08C57834

#define lua_gettop_offset               0x08ddbecc
#define lua_pushinteger_offset          0x08ddc6da
#define lua_pushstring_offset           0x08ddc759
#define lua_pushcclosure_offset         0x08ddc822
#define lua_createtable_offset          0x08ddca72
#define lua_rawset_offset               0x08ddcc57
#define luaL_checklstring_offset        0x08ddd870
#define luaL_checkinteger_offset        0x08dddbaa
#define lua_settop_offset               0x08DDBEE0
#define lua_pushvalue_offset            0x08DDC140
#define lua_gettable_offset             0x08DDC920
#define lua_settable_offset             0x08DDCB9A
#define lua_type_offset                 0x08DDC179

#define XmlParserHook_offset            0x08B9C012
#define LuaTableHook_offset             0x08B9AE76

#endif /* C_DEFINES_H */

