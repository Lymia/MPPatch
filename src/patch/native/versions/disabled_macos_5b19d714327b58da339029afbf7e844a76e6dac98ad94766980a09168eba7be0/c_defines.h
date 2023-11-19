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

#define lGetMemoryUsage_symbol          "_ZN8Database9Scripting3Lua15lGetMemoryUsageEP9lua_State"
#define lGetMemoryUsage_hook_length     5

#define SetActiveDLCAndMods_symbol      "_ZN25CvModdingFrameworkAppSide19SetActiveDLCandModsERK22cvContentPackageIDListRKSt4listIN15ModAssociations7ModInfoESaIS5_EEbb"
#define SetActiveDLCAndMods_hook_length 5

#define KNOWN_PUBLIC_BINARY_SYMBOL      "_ZrsR11FDataStreamRSs"