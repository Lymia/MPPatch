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

#include <stdbool.h>
#include <stdint.h>
#include <windows.h>

#include "c_rt.h"
#include "c_defines.h"
#include "platform.h"

__attribute__((noreturn)) void fatalError(const char* message) {
  FatalAppExit(0, message);
  exit(1);
}

void unprotectMemoryRegion(void* start, size_t length, memory_oldProtect* old) {
  VirtualProtect(start, length, PAGE_EXECUTE_READWRITE, old);
}
void protectMemoryRegion  (void* start, size_t length, memory_oldProtect* old) {
  VirtualProtect(start, length, *old, old);
}

// Runtime for DLL proxying
static HMODULE baseDll;
static bool checkFileExists(LPCTSTR szPath) {
  DWORD attrib = GetFileAttributes(szPath);
  return (attrib != INVALID_FILE_ATTRIBUTES &&
         !(attrib & FILE_ATTRIBUTE_DIRECTORY));
}
static void fatalProxyFailure(const char* error) {
    char buffer[1024];
    snprintf(buffer, 1024, "Cannot proxy CvGameDatabase!\n%s", error);
    FatalAppExit(0, buffer);
}
#define TARGET_LIBRARY_NAME "CvGameDatabase_orig_" CV_CHECKSUM ".dll"
__attribute__((constructor(200))) static void initializeProxy() {
    debug_print("Loading original CvGameDatabase");
    char buffer[1024];
    if(!checkFileExists(TARGET_LIBRARY_NAME))
        fatalProxyFailure("Original .dll file not found.");
    baseDll = LoadLibrary(TARGET_LIBRARY_NAME);
    if(baseDll == NULL) {
        snprintf(buffer, 1024, "Could not load original .dll file. (code: 0x%08x)", GetLastError());
        fatalProxyFailure(buffer);
    }
}
__attribute__((destructor(200))) static void deinitializeProxy() {
    debug_print("Unloading original CvGameDatabase");
    FreeLibrary(baseDll);
}

// Symbol resolution
void* resolveSymbol(AddressDomain domain, const char* symbol) {
    if(domain != CV_GAME_DATABASE) fatalError("resolveSymbol only supported in CV_GAME_DATABASE on win32");

    void* procAddress = GetProcAddress(baseDll, symbol);
    if(!procAddress) {
        char buffer[1024];
        snprintf(buffer, 1024, "Failed to load symbol %s.", symbol);
        fatalError(buffer);
    }

    debug_print("Resolving symbol - %s = %p", symbol, procAddress);

    return procAddress;
}

// Address resolution
static void* binary_base_addr;
static void* database_constant_symbol_addr;
void* resolveAddress(AddressDomain domain, int address) {
    if(domain == CV_GAME_DATABASE) return database_constant_symbol_addr + (address - WIN32_REF_SYMBOL_ADDR);
    if(domain == CV_BINARY       ) return binary_base_addr              + (address - WIN32_BINARY_BASE    );
    fatalError("resolveAddress in unknown domain");
}

__attribute__((constructor(201))) static void initializeBinaryBase() {
    debug_print("Finding Civ V binary base address (to deal with ASLR)");
    binary_base_addr = GetModuleHandle(NULL);
}
__attribute__((constructor(201))) static void initializeConstantSymbol() {
    debug_print("Loading constant symbol (to deal with ASLR/general .dll rebasing)");
    database_constant_symbol_addr = resolveSymbol(CV_GAME_DATABASE, WIN32_REF_SYMBOL_NAME);
}

// std::list implementation
typedef struct CppListLink {
    struct CppListLink* next;
    struct CppListLink* prev;
    void* data;
} CppListLink;

struct CppList {
    uint32_t unk0; // refcount?
    void* head;
    int length;
};

static CppListLink* CppListLink_alloc() {
    CppListLink* link = (CppListLink*) malloc(sizeof(CppListLink));
    link->next = link;
    link->prev = link;
    link->data = NULL;
    return link;
}
CppList* CppList_alloc() {
    CppList* list = (CppList*) malloc(sizeof(CppList));
    list->unk0   = 0;
    list->head   = list;
    list->length = 0;
    return list;
}
static CppListLink* CppList_getHead(CppList* list) {
    if(list->head == list) return NULL;
    else return (CppListLink*) list->head;
}
void CppList_insert(CppList* list, void* obj) {
    CppListLink* head = CppList_getHead(list);
    if(head == NULL) {
        head = CppListLink_alloc();
        head->data = obj;
        list->head = obj;
    } else {
        CppListLink* link = CppListLink_alloc();

        link->prev = head->prev;
        link->next = head;

        head->prev->next = link;
        head->prev       = link;
    }

    list->length++;
}
void CppList_clear(CppList* list) {
    CppListLink* head = CppList_getHead(list);
    if(head != NULL) {
        CppListLink* link = head;
        do {
            CppListLink* nextLink = link->next;
            if(link->data != NULL) free(link->data);
            free(link);
            link = nextLink;
        } while(link != head);
    }

    list->head   = list;
    list->length = 0;
}
void CppList_free(CppList* list) {
    CppList_clear(list);
    free(list);
}