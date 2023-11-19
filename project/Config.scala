/*
 * Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

object Config {
  val config_make         = "make"
  val config_mingw_prefix = "i686-w64-mingw32-"
  val config_macos_prefix = "i386-apple-darwin17-"

  val config_win32_cc = "clang"
  val config_macos_cc = "o32-clang"
  val config_linux_cc = "clang"
  val config_nasm     = "nasm"

  val config_target_win32 = "i686-pc-windows-gnu"
  val config_target_linux = "i686-unknown-linux-gnu"
  val config_target_macos = "i386-apple-darwin15"

  val config_win32_secureFlags  = Seq("-Wl,-Bstatic", "-lssp", "-Wl,--dynamicbase,--nxcompat")
  val config_common_secureFlags = Seq("-fstack-protector", "-fstack-protector-strong", "-D_FORTIFY_SOURCE=2")

  val config_steam_sdlbin_path = "libsdl2_2.0.3+steamrt1+srt4_i386.deb"
  val config_steam_sdldev_path = "libsdl2-dev_2.0.3+steamrt1+srt4_i386.deb"
  val config_steam_sdlbin_name = "libSDL2-2.0.so.0"

  val config_macos_ld = "" // findExecutableOnPath(config_macos_prefix + "ld")

  private def findExecutableOnPath(name: String): String = {
    for (dirname <- System.getenv("PATH").split(java.io.File.pathSeparator)) {
      val file = new java.io.File(dirname, name)
      if (file.isFile && file.canExecute) return file.getAbsolutePath
    }
    throw new AssertionError("should have found the executable")
  }
}
