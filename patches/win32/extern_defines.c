/**
    Copyright (C) 2015 Lymia Aluysia <lymiahugs@gmail.com>

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
#include "c_defines.h"
#include "extern_defines.h"

// Database library components
extern __thiscall bool cif_Database_ExecuteMultiple(class_Database* this, const char* string, size_t length) __asm__("cif_Database_ExecuteMultiple");
bool Database_ExecuteMultiple(class_Database* this, const char* string, size_t length) {
  return cif_Database_ExecuteMultiple(this, string, length);
}

extern __thiscall bool cif_Database_LogMessage     (class_Database* this, const char* string) __asm__("cif_Database_LogMessage");
bool Database_LogMessage(class_Database* this, const char* string) {
  return cif_Database_LogMessage(this, string);
}
