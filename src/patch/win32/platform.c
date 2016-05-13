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
extern __stdcall void* asm_resolveSymbol(const char* symbol) __asm__("cif_resolveSymbol");
__stdcall void* asm_resolveSymbol(const char* symbol) /*   */ {
    return resolveSymbol(symbol);
}

// Symbol resolution
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

void* resolveSymbol(const char* symbol) {
    void* procAddress = GetProcAddress(baseDll, symbol);
    if(!procAddress) {
        char buffer[1024];
        snprintf(buffer, 1024, "Failed to load symbol %s.", symbol);
        fatalProxyFailure(buffer);
    }

    debug_print("Resolving symbol - %s = 0x%08x", symbol, procAddress);

    return procAddress;
}

// Address resolution
static void* constant_symbol_addr;
void* resolveAddress(int address) {
    return constant_symbol_addr + (address - WIN32_REF_SYMBOL_ADDR);
}
__attribute__((constructor(201))) static void initializeConstantSymbol() {
    debug_print("Loading constant symbol (to deal with ASLR/general .dll rebasing)");
    constant_symbol_addr = resolveSymbol(WIN32_REF_SYMBOL_NAME);
}

