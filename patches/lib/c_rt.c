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
#include <time.h>

// Debug logging
#ifdef DEBUG
    static FILE* debug_log_file;
    #define debug_print(format, arg...) \
        fprintf(debug_log_file, "[%lld] %s at %s:%u - " format "\n", \
            (long long) time(NULL), __PRETTY_FUNCTION__, strrchr(__FILE__, '/') + 1, __LINE__, ##arg); \
        fflush(debug_log_file);
    __attribute__((constructor(150))) static void initDebugLogging() {
        debug_log_file = fopen("Mod2DLC_" target_library_name "Patch_debug.log", "w");
    }
#else
    #define debug_print(format, ...)
#endif

// Runtime for DLL proxying
static HMODULE baseDll;
static bool checkFileExists(LPCTSTR szPath) {
  DWORD attrib = GetFileAttributes(szPath);
  return (attrib != INVALID_FILE_ATTRIBUTES &&
         !(attrib & FILE_ATTRIBUTE_DIRECTORY));
}
static void fatalProxyFailure(const char* error) {
    char buffer[1024];
    snprintf(buffer, 1024, "Cannot proxy " target_library_name "!\n%s", error);
    FatalAppExit(0, buffer);
}
static void* resolveSymbol(const char* symbol) {
    void* procAddress = GetProcAddress(baseDll, symbol);
    if(!procAddress) {
        char buffer[1024];
        snprintf(buffer, 1024, "Failed to load symbol %s.", symbol);
        fatalProxyFailure(buffer);
    }

    debug_print("Resolving symbol - %s = 0x%08x", symbol, procAddress);

    return procAddress;
}
__stdcall void* asm_resolveSymbol(const char* symbol) {
    return resolveSymbol(symbol);
}

extern __stdcall void InitializeProxy();
__attribute__((constructor(200))) static void initializeProxy() {
    debug_print("Loading original " target_library_name);
    char buffer[1024];
    if(!checkFileExists(target_library))
        fatalProxyFailure("Original .dll file not found.");
    baseDll = LoadLibrary(target_library);
    if(baseDll == NULL) {
        snprintf(buffer, 1024, "Could not load original .dll file. (code: 0x%08x)", GetLastError());
        fatalProxyFailure(buffer);
    }
    debug_print("Initializing symbol cache for proxying");
    InitializeProxy();
}
__attribute__((destructor(200))) static void deinitializeProxy() {
    debug_print("Unloading original " target_library_name);
    FreeLibrary(baseDll);
}

// Look up relative addresses in the target .dll
static void* constant_symbol_addr;
static void* resolveAddress(int address) {
    return constant_symbol_addr + (address - constant_symbol_offset);
}
__stdcall void* asm_resolveAddress(int address) {
    return resolveAddress(address);
}
__attribute__((constructor(201))) static void initializeConstantSymbol() {
    debug_print("Loading constant symbol (to deal with ASLR/general .dll rebasing)");
    constant_symbol_addr = resolveSymbol(constant_symbol_name);
}

// Actual patch code!
typedef struct UnpatchData {
    void* offset;
    char oldData[5];
} UnpatchData_Struct;
typedef UnpatchData_Struct* UnpatchData;
static UnpatchData writeRelativeJmp(void* targetAddress, void* hookAddress, const char* reason) {
    // Register the patch for unpatching
    UnpatchData unpatch = malloc(sizeof(UnpatchData_Struct));
    unpatch->offset = targetAddress;
    memcpy(unpatch->oldData, targetAddress, 5);

    // Actually generate the patch opcode.
    int offsetDiff = (int) hookAddress - (int) targetAddress - 5;
    debug_print("Writing JMP (%s) - 0x%08x => 0x%08x (diff: 0x%08x)",
        reason, targetAddress, hookAddress, offsetDiff);
    *((char*)(targetAddress    )) = 0xe9; // jmp opcode
    *((int *)(targetAddress + 1)) = offsetDiff;

    return unpatch;
}
static UnpatchData doPatch(int address, void* hookAddress, const char* reason) {
    void* targetAddress = resolveAddress(address);
    char reason_buf[256];
    snprintf(reason_buf, 256, "patch: %s", reason);

    DWORD protectFlags;
    VirtualProtect(targetAddress, 5, PAGE_EXECUTE_READWRITE, &protectFlags);
    UnpatchData unpatch = writeRelativeJmp(targetAddress, hookAddress, reason_buf);
    VirtualProtect(targetAddress, 5, protectFlags, &protectFlags);
    return unpatch;
}
static void unpatch(UnpatchData data) {
    DWORD protectFlags;
    debug_print("Unpatching at 0x%08x", data->offset);
    VirtualProtect(data->offset, 5, PAGE_EXECUTE_READWRITE, &protectFlags);
    memcpy(data->offset, data->oldData, 5);
    VirtualProtect(data->offset, 5, protectFlags, &protectFlags);
    free(data);
}