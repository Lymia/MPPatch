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
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <time.h>

#include "c_rt.h"
#include "platform.h"

// Debug logging
#ifdef DEBUG
    FILE* debug_log_file;
    __attribute__((constructor(150))) static void initDebugLogging() {
        debug_log_file = fopen("mppatch_debug.log", "w");
    }
#else
    #define debug_print(format, ...)
#endif

// String manipulation
bool endsWith(const char* str, const char* ending) {
    size_t str_len = strlen(str), ending_len = strlen(ending);
    return str_len >= ending_len && !strcmp(str + str_len - ending_len, ending);
}

// std::list implementation
CppListLink* CppListLink_alloc(int length) {
    CppListLink* link = (CppListLink*) malloc(sizeof(CppListLink) + length);
    link->next = link;
    link->prev = link;
    return link;
}
void* CppListLink_newLink(CppListLink* list, int length) {
    CppListLink* link = CppListLink_alloc(length);

    link->prev = list->prev;
    link->next = list;

    list->prev->next = link;
    list->prev       = link;

    return link->data;
}
void CppListLink_clear(CppListLink* list) {
    CppListLink* link = list->next;
    while(link != list) {
        CppListLink* nextLink = link->next;
        free(link);
        link = nextLink;
    }

    list->prev = list;
    list->next = list;
}
void CppListLink_free(CppListLink* list) {
    CppListLink_clear(list);
    free(list);
}

// Patch writing code
static void writeJmpInstruction(void* fromAddress, void* toAddress, const char* logReason) {
    int offsetDiff = (int) toAddress - (int) fromAddress - 5;
    debug_print("Writing JMP (%s) - %p => %p (diff: 0x%08x)", logReason, fromAddress, toAddress, offsetDiff);

    *((char*)(fromAddress    )) = 0xe9;
    *((int *)(fromAddress + 1)) = offsetDiff;
}
void patchJmpInstruction(void* fromAddress, void* toAddress, const char* logReason) {
    memory_oldProtect protectFlags;
    unprotectMemoryRegion(fromAddress, 5, &protectFlags);
    writeJmpInstruction(fromAddress, toAddress, logReason);
    protectMemoryRegion(fromAddress, 5, &protectFlags);
}

PatchInformation* proxyFunction(void* fromAddress, void* toAddress, int patchBytes, const char* logReason) {
    PatchInformation* info = malloc(sizeof(PatchInformation));
    info->offset = fromAddress;
    memcpy(info->oldData, fromAddress, 5);

    info->functionFragment = executable_malloc(patchBytes + 5);
    memcpy(info->functionFragment->data, fromAddress, patchBytes);

    char buffer[1024];
    snprintf(buffer, 1024, "function fragment epilogue for %s", logReason);
    writeJmpInstruction(info->functionFragment->data + patchBytes, fromAddress + patchBytes, buffer);

    executable_prepare(info->functionFragment);

    snprintf(buffer, 1024, "hook for %s", logReason);
    patchJmpInstruction(fromAddress, toAddress, buffer);

    return info;
}
void unpatchCode(PatchInformation* info) {
    memory_oldProtect protectFlags;
    debug_print("Unpatching at %p", info->offset);
    unprotectMemoryRegion(info->offset, 5, &protectFlags);
    memcpy(info->offset, info->oldData, 5);
    protectMemoryRegion(info->offset, 5, &protectFlags);
}
void unpatch(PatchInformation* info) {
    unpatchCode(info);
    executable_free(info->functionFragment);
    free(info);
}
