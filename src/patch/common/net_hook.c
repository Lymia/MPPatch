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

#include <stdio.h>
#include <string.h>

#include "c_rt.h"
#include "c_defines.h"
#include "extern_defines.h"
#include "platform.h"

typedef SetActiveDLCAndMods_signature (*SetActiveDLCAndMods_type)(void*, CppList*, CppList*, char, char);
static SetActiveDLCAndMods_type SetActiveDLCAndMods_ptr;
static ENTRY SetActiveDLCAndMods_signature SetActiveDLCAndModsProxy(void* this, CppList* dlcList, CppList* modList,
                                                                    char reloadDlc, char reloadMods) {
    return SetActiveDLCAndMods_ptr(this, dlcList, modList, reloadDlc, reloadMods);
}

UnpatchData* NetPatch;
__attribute__((constructor(500))) static void installNetHook() {
    SetActiveDLCAndMods_ptr = (SetActiveDLCAndMods_type) SetActiveDLCAndMods_resolve(getBinaryType());
    int hookOffset = NetGameStartHook_offset_resolve(getBinaryType());
    if(SetActiveDLCAndMods_ptr == 0 || hookOffset == 0) fatalError("Failed to get NetStartLaunchHook offsets.");
    NetPatch = doPatch(CV_BINARY, hookOffset, SetActiveDLCAndModsProxy, true, "NetStartLaunchHook");
}
__attribute__((destructor(500))) static void uninstallNetHook() {
    unpatch(NetPatch);
}