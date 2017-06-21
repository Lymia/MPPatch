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

#include <stdio.h>

#include <CoreFoundation/CoreFoundation.h>

#include "c_rt.h"
#include "platform.h"

// Based on code used by Civ V for locating libCvGameCoreDLL_DLL.dylib, etc
// If this breaks, then Civ V breaks
const char* getExecutablePath() {
    CFBundleRef bundle = CFBundleGetMainBundle();
    CFURLRef executableUrl = CFBundleCopyExecutableURL(bundle);
    CFURLRef pathUrl = CFURLCreateCopyDeletingLastPathComponent(NULL, executableUrl);
    CFRelease(executableUrl);

    UInt8* buffer = malloc(PATH_MAX);
    if(!CFURLGetFileSystemRepresentation(pathUrl, false, buffer, PATH_MAX))
        fatalError("Could not find executable location!");
    CFRelease(pathUrl);

    return (const char*) buffer;
}

__attribute__((noreturn)) void fatalError_fn(const char* message) {
    fputs(message, stderr);
    debug_print("%s", message);

    CFStringRef message_ref = CFStringCreateWithCString(NULL, message, strlen(message));
    CFOptionFlags result;
    CFUserNotificationDisplayAlert(0, kCFUserNotificationStopAlertLevel,
                                   NULL, NULL, NULL, CFSTR("MPPatch"), message_ref,
                                   NULL, NULL, NULL, &result);

    exit(1);
}

void* resolveSymbol(const char* symbol) {
    return NULL;
}