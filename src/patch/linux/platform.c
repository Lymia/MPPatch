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

// Symbol resolution
static void* dlsymHandle;
void* resolveSymbol (const char* symbol) {
    return dlsym(dlsymHandle, symbol);
}
__attribute__((constructor(201))) static void loadDysymHandle() {
    dlsymHandle = dlopen(0, RTLD_NOW);
}

// Address resolution
static unsigned base_offset;
void* resolveAddress(int address) {
    return (void*) (base_offset + address);
}
__attribute__((constructor(201))) static void initializeConstantSymbol() {
    debug_print("Finding l_addr (to deal with ASLR)");
    struct link_map *lm = (struct link_map*) dlopen(0, RTLD_NOW);
    base_offset = lm->l_addr;
}

