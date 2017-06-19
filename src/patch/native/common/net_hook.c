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

#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>

#include "c_rt.h"
#include "platform.h"
#include "net_hook.h"

static bool installLock = false;
#define spinUnlock() while(!__sync_bool_compare_and_swap(&installLock, true, false));
#define spinLock()   while(!__sync_bool_compare_and_swap(&installLock, false, true));

typedef struct GUID {
    uint32_t data1;
    uint16_t data2;
    uint16_t data3;
    uint64_t data4;
} GUID;
typedef struct ModInfo {
    char modId[64];
    int version;
} ModInfo;

static CppList* overrideDLCList = NULL;
static CppList* overrideModList = NULL;
static bool overrideDLCActive  = false, overrideReloadDLC  = false, reloadDLC ;
static bool overrideModsActive = false, overrideReloadMods = false, reloadMods;
__attribute__((constructor(CONSTRUCTOR_EARLY_INIT))) static void initNetHook() {
    overrideDLCList = CppList_alloc();
    overrideModList = CppList_alloc();
}

void NetPatch_pushMod(const char* modId, int version) {
    ModInfo* info = (ModInfo*) CppList_newLink(overrideModList, sizeof(ModInfo));
    strncpy(info->modId, modId, 64);
    info->modId[63] = '\0';
    info->version = version;
}
void NetPatch_overrideReloadMods(bool val) {
    overrideReloadMods = true;
    reloadMods = val;
}
void NetPatch_overrideModList() {
    overrideModsActive = true;
}

void NetPatch_pushDLC(uint32_t data1, uint16_t data2, uint16_t data3, uint64_t data4) {
    GUID* guid = (GUID*) CppList_newLink(overrideDLCList, sizeof(GUID));
    guid->data1 = data1;
    guid->data2 = data2;
    guid->data3 = data3;
    guid->data4 = data4;
}
void NetPatch_overrideReloadDLC(bool val) {
    overrideReloadDLC = true;
    reloadDLC = val;
}
void NetPatch_overrideDLCList() {
    overrideDLCActive = true;
}

void NetPatch_install() {
    spinLock();
    if(SetActiveDLCAndMods_patchInfo == 0) installNetHook();
    spinUnlock();
}

void NetPatch_reset() {
    overrideDLCActive  = false;
    overrideModsActive = false;
    overrideReloadDLC  = false;
    overrideReloadMods = false;
    CppList_clear(overrideDLCList);
    CppList_clear(overrideModList);

    spinLock();
    if(SetActiveDLCAndMods_patchInfo != 0) {
        unpatch(SetActiveDLCAndMods_patchInfo);
        SetActiveDLCAndMods_patchInfo = 0;
    }
    spinUnlock();
}

static void printGUID(void* ptr) {
    GUID* m = (GUID*) ptr;
    uint64_t data4_le = __builtin_bswap64(m->data4);
    debug_print_raw(" - {%08x-%04x-%04x-%04x-%04x%08x}",
                    m->data1, m->data2, m->data3,
                    (uint32_t) (data4_le >> 48) & 0xFFFF,
                    (uint32_t) (data4_le >> 32) & 0xFFFF,
                    (uint32_t) data4_le);
}
static void printMod(void* ptr) {
    ModInfo* m = (ModInfo*) ptr;
    debug_print_raw(" - {id = \"%s\", version = %d}", m->modId, m->version);
}
static void debugPrintList(CppList* list, const char* header, void (*printFn)(void*)) {
    if(list == NULL) { debug_print_raw("%s (is null)", header); }
    else {
        debug_print_raw("%s (stored length: %d):", header, CppList_size(list));
        if(CppList_size(list) == 0) { debug_print_raw(" - <no entries>"); }
        else for(CppListLink* i = CppList_begin(list); i != CppList_end(list); i = i->next) printFn(i->data);
    }
}

SetActiveDLCAndMods_t SetActiveDLCAndMods;
ENTRY int SetActiveDLCAndMods_attributes SetActiveDLCAndModsProxy(void* this, CppList* dlcList, CppList* modList,
                                                                  char pReloadDlc, char pReloadMods) {
    spinLock();
    PatchInformation* patchInfo = SetActiveDLCAndMods_patchInfo;
    unpatchCode(SetActiveDLCAndMods_patchInfo);
    SetActiveDLCAndMods_patchInfo = 0;
    spinUnlock();

    debug_print("In SetActiveDLCAndModsProxy. (this = %p, reloadDlc = %d, reloadMods = %d)",
                this, pReloadDlc, pReloadMods)
    debugPrintList(dlcList, "Original DLC GUID List", printGUID);
    debugPrintList(modList, "Original Mod List", printMod);

    if(overrideDLCActive ) {
        debug_print("Overriding DLC list.")
        debugPrintList(overrideDLCList, "Override DLC List", printGUID);
        dlcList = overrideDLCList;
    }
    if(overrideModsActive) {
        debug_print("Overriding mods list.")
        debugPrintList(overrideModList, "Override Mod List", printMod);
        modList = overrideModList;
    }
    if(overrideReloadDLC ) {
        debug_print("Overriding reload DLCs flag (new value: %s)", reloadDLC ? "true" : "false")
        pReloadDlc  = reloadDLC;
    }
    if(overrideReloadMods) {
        debug_print("Overriding reload mods flag (new value: %s)", reloadMods ? "true" : "false")
        pReloadMods = reloadMods;
    }

    int ret = SetActiveDLCAndMods(this, dlcList, modList, pReloadDlc, pReloadMods);
    free(patchInfo);
    NetPatch_reset();
    return ret;
}