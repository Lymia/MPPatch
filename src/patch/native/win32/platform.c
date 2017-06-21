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

#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include <windows.h>

#include "c_rt.h"
#include "c_defines.h"
#include "platform.h"



// Memory management functions
const char* getExecutablePath() {
    char* buffer = malloc(PATH_MAX);
    GetModuleFileName(NULL, buffer, PATH_MAX);

    char* location = strrchr(buffer, '\\');
    if(location) *location = '\0';

    return (const char*) buffer;
}

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

// Symbol resolution
static HMODULE baseDll;
#define TARGET_LIBRARY_NAME "CvGameDatabase_Original.dll"
__attribute__((constructor(CONSTRUCTOR_BINARY_INIT_EARLY))) static void initializeProxy() {
    char buffer[PATH_MAX];
    getSupportFilePath(buffer, TARGET_LIBRARY_NAME);

    debug_print("Loading original CvGameDatabase");
    if(fileExists(buffer))
        fatalError("Cannot proxy CvGameDatabase!\nOriginal .dll file not found.");
    baseDll = LoadLibrary(buffer);
    if(baseDll == NULL)
        fatalError("Cannot proxy CvGameDatabase!\nCould not load original .dll file. (code: 0x%08lx)", GetLastError());
}
void* resolveSymbol(const char* symbol) {
    void* procAddress = GetProcAddress(baseDll, symbol);
    if(!procAddress) fatalError("Failed to load symbol %s.", symbol);

    debug_print("Resolving symbol - %s = %p", symbol, procAddress);

    return procAddress;
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
    list->length = 0;
    CppListLink_clear(list->head);
}
void CppList_free(CppList* list) {
    CppListLink_free(list->head);
    free(list);
}