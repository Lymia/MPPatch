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

#include <stdlib.h>
#include <stdbool.h>

#include <sys/mman.h>
#include <dlfcn.h>
#include <link.h>
#include <unistd.h>

#include <SDL.h>

#include "c_rt.h"
#include "c_defines.h"
#include "platform.h"

__attribute__((noreturn)) void fatalError(const char* message) {
  fputs(message, stderr);
  SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, "Multiverse Mod Manager", message, 0);
  exit(1);
}

static int protectRange(size_t start, size_t length, int flags) {
  int page_size = getpagesize();
  size_t end = start + length;
  start = (start / page_size) * page_size;
  end   = ((end + page_size) / page_size) * page_size;
  return mprotect((void*) start, end - start, flags);
}
void unprotectMemoryRegion(void* start, size_t length, memory_oldProtect* old) {
  protectRange((size_t) start, length, PROT_READ | PROT_WRITE);
}
void protectMemoryRegion  (void* start, size_t length, memory_oldProtect* old) {
  protectRange((size_t) start, length, PROT_READ | PROT_EXEC);
}

// Symbol & address resolution
static void checkDomain(AddressDomain domain, const char* fn) {
    char buffer[256];
    snprintf(buffer, 256, "%s in unknown domain %d", fn, domain);
    if(domain != CV_BINARY && domain != CV_GAME_DATABASE && domain != CV_MERGED_BINARY)
        fatalError(buffer);
}

static void* dlsymHandle;
void* resolveSymbol(AddressDomain domain, const char* symbol) {
    checkDomain(domain, "resolveSymbol");
    return dlsym(dlsymHandle, symbol);
}

static unsigned base_offset;
void* resolveAddress(AddressDomain domain, int address) {
    checkDomain(domain, "resolveAddress");
    return (void*) (base_offset + address);
}

__attribute__((constructor(201))) static void loadDysymHandle() {
    debug_print("Opening handle to main binary");
    dlsymHandle = dlopen(0, RTLD_NOW);
    debug_print("Finding l_addr (to deal with ASLR)");
    struct link_map *lm = (struct link_map*) dlsymHandle;
    base_offset = lm->l_addr;
}
__attribute__((destructor(201))) static void closeDysymHandle() {
    debug_print("Closing handle to main binary");
    dlclose(dlsymHandle);
}

// std::list implementation
struct CppList {
    CppList* prev;
    CppList* next;
    union {
        void* data;
        int length;
    };
};

CppList* CppList_alloc() {
    CppList* list = (CppList*) malloc(sizeof(CppList));
    list->prev   = list;
    list->next   = list;
    list->length = 0;
    return list;
}
void CppList_insert(CppList* list, void* obj) {
    CppList* link = CppList_alloc();
    link->data = obj;

    link->prev = list->prev;
    link->next = list;

    list->prev->next = link;
    list->prev       = link;

    list->length++;
}
void CppList_clear(CppList* list) {
    CppList* link = list->next;
    while(link != list) {
        CppList* nextLink = link->next;
        if(link->data != NULL) free(link->data);
        free(link);
        link = nextLink;
    }
    list->length = 0;
}
void CppList_free(CppList* list) {
    CppList_clear(list);
    free(list);
}