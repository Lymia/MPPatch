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

#ifndef C_DEFINES_H
#define C_DEFINES_H

#define XmlParserHook_offset            0x08B9C012
#define LuaTableHook_offset             0x08B9AE76

#define SetActiveDLCAndMods_resolve(type)     (resolveSymbol(CV_MERGED_BINARY, "_ZN25CvModdingFrameworkAppSide19SetActiveDLCandModsERK22cvContentPackageIDListRKNSt3__14listIN15ModAssociations7ModInfoENS3_9allocatorIS6_EEEEbb"))
#define NetGameStartHook_offset_resolve(type) 0x08711C6B

#endif /* C_DEFINES_H */

