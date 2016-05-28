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

#ifndef C_RT_H
#define C_RT_H

#include <stdio.h>
#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include "platform.h"

#ifdef DEBUG
    extern FILE* debug_log_file;
    #define debug_print_raw(format, arg...) { \
        time_t time_val = time(NULL); \
        char* time_str_tmp = asctime(localtime(&time_val)); \
        time_str_tmp[strlen(time_str_tmp) - 1] = 0; \
        fprintf(debug_log_file, "[%s] " format "\n", time_str_tmp, ##arg); \
        fflush(debug_log_file); \
    }
    #define debug_print(format, arg...) \
        debug_print_raw("%s at %s:%u - " format, __PRETTY_FUNCTION__, strrchr(__FILE__, '/') + 1, __LINE__, ##arg)
#else
    #define debug_print_raw(format, ...)
    #define debug_print(format, ...)
#endif

#define ENTRY __attribute__((force_align_arg_pointer))

bool endsWith(const char* str, const char* ending);

CppListLink* CppListLink_alloc(int length);
void* CppListLink_newLink(CppListLink* list, int length);
void CppListLink_clear(CppListLink* list);
void CppListLink_free(CppListLink* list);

typedef struct PatchInformation {
    void* offset;
    char oldData[5];
    ExecutableMemory* functionFragment;
} PatchInformation;

void patchJmpInstruction(void* fromAddress, void* toAddress, const char* logReason);
PatchInformation* proxyFunction(void* fromAddress, void* toAddress, int patchBytes, const char* logReason);
void unpatch(PatchInformation* data);

#endif /* C_RT_H */

