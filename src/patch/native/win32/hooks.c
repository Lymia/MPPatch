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
#include <windows.h>

#include "c_rt.h"
#include "platform.h"
#include "c_defines.h"

#include "net_hook.h"
#include "lua_hook.h"
#include "config.h"

// Detect binary type
#define switchOnType(type, name) ((type) == BIN_DX9    ? name##_BIN_DX9    : \
                                  (type) == BIN_DX11   ? name##_BIN_DX11   : \
                                  (type) == BIN_TABLET ? name##_BIN_TABLET : \
                                  0)
typedef enum BinaryType { BIN_DX9, BIN_DX11, BIN_TABLET } BinaryType;

static BinaryType detectedBinaryType;
__attribute__((constructor(CONSTRUCTOR_BINARY_INIT))) static void initializeBinaryType() {
    debug_print("Finding binary type");

    char moduleName[1024];
    if(!GetModuleFileName(NULL, moduleName, sizeof(moduleName)))
        fatalError("Could not get main executable binary name. (code: 0x%08lx)", GetLastError());
    debug_print("Binary name: %s", moduleName);

    if     (endsWith(moduleName, "CivilizationV.exe"       )) detectedBinaryType = BIN_DX9   ;
    else if(endsWith(moduleName, "CivilizationV_DX11.exe"  )) detectedBinaryType = BIN_DX11  ;
    else if(endsWith(moduleName, "CivilizationV_Tablet.exe")) detectedBinaryType = BIN_TABLET;
    else fatalError("Unknown main executable type! (executable path: %s)", moduleName);

    debug_print("Detected binary type: %d", detectedBinaryType)
}

// Address resolution
static void* binary_base_addr;
static void* resolveAddress(int address) {
    return binary_base_addr + (address - WIN32_BINARY_BASE);
}
__attribute__((constructor(CONSTRUCTOR_BINARY_INIT))) static void initializeBinaryBase() {
    debug_print("Finding Civ V binary base address (to deal with ASLR)");
    binary_base_addr = GetModuleHandle(NULL);
}

// Hooks
void* filterProxySymbol(const char* name, void* target) {
    if(enableMultiplayerPatch && !strcmp(name, lGetMemoryUsage_symbol)) {
        debug_print("Intercepting lGetMemoryUsage proxy target.");
        return lGetMemoryUsageProxy;
    } else return target;
}
PatchInformation* SetActiveDLCAndMods_patchInfo = NULL;
__attribute__((constructor(CONSTRUCTOR_HOOK_INIT))) static void installHooks() {
    // Lua hook
    lGetMemoryUsage = resolveSymbol(lGetMemoryUsage_symbol);
}

void installNetHook() {
    void* offset    = resolveAddress(switchOnType(detectedBinaryType, SetActiveDLCAndMods_offset));
    int   patchSize = switchOnType(detectedBinaryType, SetActiveDLCAndMods_hook_length);

    SetActiveDLCAndMods_patchInfo = proxyFunction(offset, SetActiveDLCAndModsProxy, patchSize, "SetActiveDLCAndMods");
    SetActiveDLCAndMods = (SetActiveDLCAndMods_t) SetActiveDLCAndMods_patchInfo->functionFragment->data;
}