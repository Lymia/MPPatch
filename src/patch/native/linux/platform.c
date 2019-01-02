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

#define _GNU_SOURCE

#include <dlfcn.h>
#include <unistd.h>
#include <string.h>
#include <SDL.h>

#include "c_rt.h"
#include "platform.h"

const char* getExecutablePath() {
    char* buffer = malloc(PATH_MAX + 1);
    ssize_t name_bytes = readlink("/proc/self/exe", buffer, PATH_MAX);
    if (name_bytes == -1)
        fatalError("Could not find executable location!");
    buffer[name_bytes] = '\0';

    char* location = strrchr(buffer, '/');
    if (location) *location = '\0';

    return (const char*) buffer;
}

__attribute__((noreturn)) void fatalError_fn(const char* message) {
  fputs(message, stderr);
  debug_print("%s", message);
  SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, "MPPatch", message, 0);
  exit(1);
}

void* resolveSymbol(const char* symbol) {
    return dlsym(RTLD_DEFAULT, symbol);
}