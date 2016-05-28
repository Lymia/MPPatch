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

#ifndef PLATFORM_DEFINES_H
#define PLATFORM_DEFINES_H

#define DEBUG_TIME_STR "%I64d"

#define SetActiveDLCAndMods_return char
#define SetActiveDLCAndMods_signature SetActiveDLCAndMods_return __thiscall

#define switchOnType(type, name) ((type) == BIN_DX9    ? name##_BIN_DX9    : \
                                  (type) == BIN_DX11   ? name##_BIN_DX11   : \
                                  (type) == BIN_TABLET ? name##_BIN_TABLET : \
                                  0)

#define SetActiveDLCAndMods_resolve(type)     (resolveAddress(CV_BINARY, switchOnType(type, SetActiveDLCAndMods_offset)))
#define NetGameStartHook_offset_resolve(type) (switchOnType(type, NetGameStartHook_offset))

// std::list data structure
typedef struct CppListLink {
    struct CppListLink* next;
    struct CppListLink* prev;
    char data[];
} CppListLink;

typedef struct CppList {
    uint32_t unk0; // refcount?
    CppListLink* head;
    int length;
} CppList;

#endif /* PLATFORM_DEFINES_H */
