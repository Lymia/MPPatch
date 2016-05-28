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

__attribute__((noreturn)) void fatalError_fn(const char* message) {
    debug_print("%s", message);
    FatalAppExit(0, message);
    exit(1);
}

void unprotectMemoryRegion(void* start, size_t length, memory_oldProtect* old) {
    VirtualProtect(start, length, PAGE_EXECUTE_READWRITE, old);
}
void protectMemoryRegion  (void* start, size_t length, memory_oldProtect* old) {
    VirtualProtect(start, length, *old, old);
}

ExecutableMemory* executable_malloc(int length) {
    ExecutableMemory* memory = (ExecutableMemory*) VirtualAlloc(NULL, sizeof(ExecutableMemory) + length,
                                                                MEM_COMMIT, PAGE_EXECUTE_READWRITE);
    memory->length = length;
    return memory;
}
void executable_prepare(ExecutableMemory* memory) {}
void executable_free(ExecutableMemory* memory) {
    VirtualFree(memory, memory->length, MEM_RELEASE);
}

// Get executable type
static BinaryType detectedBinaryType;
BinaryType getBinaryType() {
    return detectedBinaryType;
}
__attribute__((constructor(201))) static void initializeBinaryType() {
    debug_print("Finding binary type");

    char moduleName[1024];
    if(!GetModuleFileName(NULL, moduleName, sizeof(moduleName)))
        fatalError("Could not get main executable binary name. (code: 0x%08x)", GetLastError());
    debug_print("Binary name: %s", moduleName);

    if     (endsWith(moduleName, "CivilizationV.exe"       )) detectedBinaryType = BIN_DX9   ;
    else if(endsWith(moduleName, "CivilizationV_DX11.exe"  )) detectedBinaryType = BIN_DX11  ;
    else if(endsWith(moduleName, "CivilizationV_Tablet.exe")) detectedBinaryType = BIN_TABLET;
    else fatalError("Unknown main executable type! (executable path: %s)", moduleName);

    debug_print("Detected binary type: %d", detectedBinaryType)
}

// Runtime for DLL proxying
static HMODULE baseDll;
static bool checkFileExists(LPCTSTR szPath) {
  DWORD attrib = GetFileAttributes(szPath);
  return (attrib != INVALID_FILE_ATTRIBUTES &&
         !(attrib & FILE_ATTRIBUTE_DIRECTORY));
}
#define TARGET_LIBRARY_NAME "CvGameDatabase_orig_" CV_CHECKSUM ".dll"
__attribute__((constructor(200))) static void initializeProxy() {
    debug_print("Loading original CvGameDatabase");
    if(!checkFileExists(TARGET_LIBRARY_NAME))
        fatalError("Cannot proxy CvGameDatabase!\nOriginal .dll file not found.");
    baseDll = LoadLibrary(TARGET_LIBRARY_NAME);
    if(baseDll == NULL)
        fatalError("Cannot proxy CvGameDatabase!\nCould not load original .dll file. (code: 0x%08x)", GetLastError());
}
__attribute__((destructor(200))) static void deinitializeProxy() {
    debug_print("Unloading original CvGameDatabase");
    FreeLibrary(baseDll);
}

// Symbol resolution
void* resolveSymbol(AddressDomain domain, const char* symbol) {
    if(domain != CV_GAME_DATABASE) fatalError("resolveSymbol only supported in CV_GAME_DATABASE on win32");

    void* procAddress = GetProcAddress(baseDll, symbol);
    if(!procAddress) fatalError("Failed to load symbol %s.", symbol);

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
CppList* CppList_alloc() {
    CppList* list = (CppList*) malloc(sizeof(CppList));
    list->unk0    = 0;
    list->head    = CppListLink_alloc(0);
    list->length  = 0;
    return list;
}
void* CppList_newLink(CppList* list, int length) {
    list->length++;
    return CppListLink_newLink(list->head, length);
}
CppListLink* CppList_begin(CppList* list) {
    return list->head->next;
}
CppListLink* CppList_end(CppList* list) {
    return list->head;
}
int CppList_size(CppList* list) {
    return list->length;
}
void CppList_clear(CppList* list) {
    CppListLink_clear(list->head);
}
void CppList_free(CppList* list) {
    CppListLink_free(list->head);
    free(list);
}