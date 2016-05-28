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

#include "c_rt.h"
#include "platform.h"
#include "c_defines.h"
#include "extern_defines.h"

#include "net_hook.h"
#include "lua_hook.h"

static PatchInformation* lGetMemoryUsage_patchInfo;
static PatchInformation* SetActiveDLCAndMods_patchInfo;

__attribute__((constructor(500))) static void installHooks() {
    // Lua hook
    lGetMemoryUsage_patchInfo = proxyFunction(resolveSymbol(CV_MERGED_BINARY, lGetMemoryUsage_symbol),
                                             lGetMemoryUsageProxy, lGetMemoryUsage_hook_length, "lGetMemoryUsage");
    lGetMemoryUsage = (lGetMemoryUsage_t) lGetMemoryUsage_patchInfo->functionFragment->data;

    // DLC/Mod hook
    SetActiveDLCAndMods_patchInfo = proxyFunction(resolveSymbol(CV_MERGED_BINARY, SetActiveDLCAndMods_symbol),
                                                  SetActiveDLCAndModsProxy, SetActiveDLCAndMods_hook_length,
                                                  "SetActiveDLCAndMods");
    SetActiveDLCAndMods = (SetActiveDLCAndMods_t) SetActiveDLCAndMods_patchInfo->functionFragment->data;
}
__attribute__((destructor(500))) static void destroyHooks() {
    unpatch(lGetMemoryUsage_patchInfo);
    unpatch(SetActiveDLCAndMods_patchInfo);
}