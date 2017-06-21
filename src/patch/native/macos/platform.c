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

#include <stdio.h>

#include <dlfcn.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <mach-o/dyld.h>
#include <mach-o/nlist.h>

#include <CoreFoundation/CoreFoundation.h>

#include "c_rt.h"
#include "c_defines.h"
#include "hashmap.h"
#include "platform.h"

// Based on code used by Civ V for locating libCvGameCoreDLL_DLL.dylib, etc
// If this breaks, then Civ V breaks
const char* getExecutablePath() {
    CFBundleRef bundle = CFBundleGetMainBundle();
    CFURLRef executableUrl = CFBundleCopyExecutableURL(bundle);
    CFURLRef pathUrl = CFURLCreateCopyDeletingLastPathComponent(NULL, executableUrl);
    CFRelease(executableUrl);

    UInt8* buffer = malloc(PATH_MAX);
    if(!CFURLGetFileSystemRepresentation(pathUrl, false, buffer, PATH_MAX))
        fatalError("Could not find executable location!");
    CFRelease(pathUrl);

    return (const char*) buffer;
}

__attribute__((noreturn)) void fatalError_fn(const char* message) {
    fputs(message, stderr);
    debug_print("%s", message);

    CFStringRef message_ref = CFStringCreateWithCString(NULL, message, strlen(message));
    CFOptionFlags result;
    CFUserNotificationDisplayAlert(0, kCFUserNotificationStopAlertLevel,
                                   NULL, NULL, NULL, CFSTR("MPPatch"), message_ref,
                                   NULL, NULL, NULL, &result);

    exit(1);
}

static struct mach_header* getBinaryHeader() {
    void* knownSymbol = dlsym(RTLD_DEFAULT, KNOWN_PUBLIC_BINARY_SYMBOL);
    if(!knownSymbol) fatalError("Could not find symbol %s.", KNOWN_PUBLIC_BINARY_SYMBOL);

    Dl_info info;
    if(!dladdr(knownSymbol, &info)) fatalError("Could not retrieve executable header.");

    debug_print("Target executable: %s", info.dli_fname);
    return (struct mach_header*) info.dli_fbase;
}

static void loadSymbols(map_t symbolMap, struct mach_header* image) {
    struct segment_command* seg_text = NULL;
    struct segment_command* seg_linkedit = NULL;
	struct symtab_command* symtab = NULL;

	void* current_cmd = (void*) image + sizeof(struct mach_header);
	for(int i=0; i < image->ncmds; i++) {
	    struct load_command* cmd = (struct load_command*) current_cmd;
	    if(cmd->cmd == LC_SEGMENT) {
            struct segment_command* segment = (struct segment_command*) current_cmd;
                 if(!strcmp(segment->segname, SEG_TEXT    )) seg_text = segment;
            else if(!strcmp(segment->segname, SEG_LINKEDIT)) seg_linkedit = segment;
	    } else if(cmd->cmd == LC_SYMTAB) symtab = (struct symtab_command*) current_cmd;
	    current_cmd += cmd->cmdsize;
    }

	if(seg_text == NULL || seg_linkedit == NULL || symtab == NULL) fatalError("Could not parse executable header.");

    void* imageBase = (void*) image - seg_text->vmaddr;
    void* linkeditFileBase = imageBase + seg_linkedit->vmaddr - seg_linkedit->fileoff;

	struct nlist *symbase = (struct nlist*) (linkeditFileBase + symtab->symoff);
	char *strings = (char*) (linkeditFileBase + symtab->stroff);

    struct nlist* sym = symbase;
	for(int i=0; i < symtab->nsyms; i++, sym++) if(sym->n_un.n_strx != 0) {
        char* name = strings + sym->n_un.n_strx;
        void* addr = imageBase + sym->n_value;

        if(*name == '_') hashmap_put(symbolMap, name + 1, addr);
        else             hashmap_put(symbolMap, name, addr);
    }
}

static map_t symbolMap;
__attribute__((constructor(CONSTRUCTOR_BINARY_INIT_EARLY))) static void loadSymbolsFromBinary() {
    debug_print("Loading Civilization V binary symbols...");
    symbolMap = hashmap_new();
    loadSymbols(symbolMap, getBinaryHeader());
    debug_print("Loaded %d symbols.", hashmap_length(symbolMap));
}

void* resolveSymbol(const char* symbol) {
    any_t out;
    if(hashmap_get(symbolMap, (char*) symbol, &out) == MAP_MISSING) return NULL;
    return (void*) out;
}