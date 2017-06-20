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

#include <stdlib.h>
#include <stdbool.h>
#include <unistd.h>

#include "config.h"
#include "c_rt.h"
#include "ini.h"

bool enableLogging = false;
bool enableDebug = false;
bool enableMultiplayerPatch = false;
bool enableLuaJIT = false;

static int readConfig_handler(void* user, const char* section, const char* name, const char* value) {
    bool isFlagSet = strcmp(value, "true") == 0;

    #define CFG_MATCH(s, n) strcmp(section, s) == 0 && strcmp(name, n) == 0
         if (CFG_MATCH("MPPatch", "enableLogging"         )) enableLogging          = isFlagSet;
    else if (CFG_MATCH("MPPatch", "enableDebug"           )) enableDebug            = isFlagSet;
    else if (CFG_MATCH("MPPatch", "enableMultiplayerPatch")) enableMultiplayerPatch = isFlagSet;
    else if (CFG_MATCH("MPPatch", "enableLuaJIT"          )) enableLuaJIT           = isFlagSet;
    #undef CFG_MATCH

    return 1;
}

static bool exists(const char *fname) {
    FILE *file = fopen(fname, "r");
    if (file) fclose(file);
    return file ? true : false;
}
__attribute__((constructor(CONSTRUCTOR_READ_CONFIG))) static void readConfig() {
    char buffer[PATH_MAX];
    getSupportFilePath(buffer, CONFIG_FILENAME);
    if(exists(buffer)) ini_parse(buffer, readConfig_handler, NULL);
}