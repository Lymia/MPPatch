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

#define switchOnType(type, name) ((type) == BIN_DX9    ? name##_BIN_DX9    : \
                                  (type) == BIN_DX11   ? name##_BIN_DX11   : \
                                  (type) == BIN_TABLET ? name##_BIN_TABLET : \
                                  0)

void* filterProxySymbol(const char* name, void* target) {
    if(!strcmp(name, lGetMemoryUsage_symbol)) {
        debug_print("Intercepting lGetMemoryUsage proxy target.");
        return lGetMemoryUsageProxy;
    } else return target;
}
PatchInformation* SetActiveDLCAndMods_patchInfo = 0;
__attribute__((constructor(CONSTRUCTOR_HOOK_INIT))) static void installHooks() {
    // Lua hook
    lGetMemoryUsage = resolveSymbol(CV_GAME_DATABASE, lGetMemoryUsage_symbol);
}

void installNetHook() {
    BinaryType type = getBinaryType();

    void* offset    = resolveAddress(CV_BINARY, switchOnType(type, SetActiveDLCAndMods_offset));
    int   patchSize = switchOnType(type, SetActiveDLCAndMods_hook_length);

    SetActiveDLCAndMods_patchInfo = proxyFunction(offset, SetActiveDLCAndModsProxy, patchSize, "SetActiveDLCAndMods");
    SetActiveDLCAndMods = (SetActiveDLCAndMods_t) SetActiveDLCAndMods_patchInfo->functionFragment->data;
}