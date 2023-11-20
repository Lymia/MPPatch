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

#pragma once

#define lGetMemoryUsage_symbol                     "?lGetMemoryUsage@Lua@Scripting@Database@@SAHPAUlua_State@@@Z"

#define SetActiveDLCAndMods_offset_BIN_DX9         0x006CD160
#define SetActiveDLCAndMods_hook_length_BIN_DX9    6

#define SetActiveDLCAndMods_offset_BIN_DX11        0x006B8E50
#define SetActiveDLCAndMods_hook_length_BIN_DX11   6

#define SetActiveDLCAndMods_offset_BIN_TABLET      0x0065DC10
#define SetActiveDLCAndMods_hook_length_BIN_TABLET 6

#define WIN32_REF_SYMBOL_NAME                      "??0BinaryIO@Database@@QAE@PBD@Z"
#define WIN32_REF_SYMBOL_ADDR                      0x100062D0

#define WIN32_BINARY_BASE                          0x00400000
