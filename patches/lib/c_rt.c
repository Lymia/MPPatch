/**
    Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>

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

#include <windows.h>
#include <stdio.h>

// Runtime for DLL proxying
static HMODULE baseDll;
static bool checkFileExists(LPCTSTR szPath) {
  DWORD attrib = GetFileAttributes(szPath);
  return (attrib != INVALID_FILE_ATTRIBUTES &&
         !(attrib & FILE_ATTRIBUTE_DIRECTORY));
}
extern bool InitializeProxy();
static void fatalProxyFailure(const char* error) {
    char buffer[1024];
    snprintf(buffer, 1024, "Cannot proxy " target_library_name "!\n%s", error);
    FatalAppExit(0, buffer);
}
__attribute__((constructor(200))) static void initializeProxy() {
    if(!checkFileExists(target_library))
        fatalProxyFailure("Original .dll file not found.");
    baseDll = LoadLibrary(target_library);
    if(baseDll == NULL)
        fatalProxyFailure("Could not load original .dll file.");
    if(!InitializeProxy())
        fatalProxyFailure("Failed to load symbol.");
}
static void* resolveSymbol(const char* symbol) {
    return GetProcAddress(baseDll, symbol);
}
__stdcall void* asm_resolveSymbol(const char* symbol) {
    return resolveSymbol(symbol);
}

// Look up relative addresses in the target .dll
static void* constant_symbol_addr;
static void* loadRelativeAddress(int address) {
    return constant_symbol_addr + (address - constant_symbol_offset);
}
__stdcall void* asm_resolveAddress(int address) {
    return loadRelativeAddress(address);
}
__attribute__((constructor(201))) static void initializeConstantSymbol() {
    constant_symbol_addr = resolveSymbol(constant_symbol_name);
}