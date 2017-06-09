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

#pragma once

#include "c_rt.h"
#include "platform.h"

void NetPatch_pushMod(const char* modId, int version);
void NetPatch_overrideReloadMods(bool val);
void NetPatch_overrideModList();

void NetPatch_pushDLC(uint32_t data1, uint16_t data2, uint16_t data3, uint64_t data4);
void NetPatch_overrideReloadDLC(bool val);
void NetPatch_overrideDLCList();

void NetPatch_install();
void NetPatch_reset();


ENTRY int SetActiveDLCAndMods_attributes SetActiveDLCAndModsProxy(void* this, CppList* dlcList, CppList* modList,
                                                                  char reloadDlc, char reloadMods);
typedef int SetActiveDLCAndMods_attributes (*SetActiveDLCAndMods_t)(void*, CppList*, CppList*, char, char);
extern PatchInformation* SetActiveDLCAndMods_patchInfo;
extern SetActiveDLCAndMods_t SetActiveDLCAndMods;

void installNetHook();
