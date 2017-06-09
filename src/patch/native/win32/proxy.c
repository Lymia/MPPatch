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

#include "c_rt.h"
#include "platform.h"

typedef struct symbolTable_type {
    int32_t exists;
    int32_t addr;
    const char* target;
} __attribute__((packed)) symbolTable_type;
extern symbolTable_type proxy_symbolTable[] __asm__("cif_symbolTable");

__attribute__((constructor(CONSTRUCTOR_PROXY_INIT))) static void initProxy() {
    debug_print("Initializing CvGameDatabase proxy.");
    for(symbolTable_type* t = proxy_symbolTable; t->exists; t++) {
        void* target = filterProxySymbol(t->target, resolveSymbol(CV_GAME_DATABASE, t->target));
        char buffer[1024];
        snprintf(buffer, 1024, "proxy initialization: %s", t->target);
        patchJmpInstruction((void*) t->addr, target, buffer);
    }
}