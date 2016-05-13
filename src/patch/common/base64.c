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

// TODO: Eventually make this code more robust against bad input.

#include "base64.h"

static int decodeChar(char c) {
    if(c >= 'A' && c <= 'Z') return c - 'A';
    if(c >= 'a' && c <= 'z') return c - 'a' + 26;
    if(c >= '0' && c <= '9') return c - '0' + 26 + 26;

    if(c == '+') return 62;
    if(c == '/') return 63;

    return 0;
}

size_t base64OutputSize(size_t len) {
    return ((len - 1) / 4 + 1) * 3 + 1;
}
void decodeBase64(const char* in, char* out, size_t len, size_t outlen) {
    for(int i=0, j=0; i < len && j < outlen; i += 4, j += 3) {
        char a = in[i  ];
        char b = in[i+1];
        char c = in[i+2];
        char d = in[i+3];

        if(a == 0 || b == 0 || c == 0 || d == 0) break;

        a = decodeChar(a);
        b = decodeChar(b);
        c = decodeChar(c);
        d = decodeChar(d);

        out[j  ] = ((a << 2) | (b >> 4)) & 0xFF;
        out[j+1] = ((b << 4) | (c >> 2)) & 0xFF;
        out[j+2] = ((c << 6) | (d     )) & 0xFF;
    }
}
