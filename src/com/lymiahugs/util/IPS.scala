/*
 * Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.lymiahugs.util

import java.io.{File, DataInputStream, RandomAccessFile, InputStream}

object IPS {
  private def readUByte(in: DataInputStream) =
    in.readByte() & 0xFF
  private def readOffset(in: DataInputStream) =
    readUByte(in) | (readUByte(in) << 8) | (readUByte(in) << 16)
  private def readShort(in: DataInputStream) =
    readUByte(in) | (readUByte(in) << 16)

  def applyPatch(patch: InputStream, source: File, output: File) {

  }
  def applyPatch(patch: InputStream, output: RandomAccessFile) {
    val in = new DataInputStream(patch)

    try {
      // check for the "PATCH" header
      assert(in.readInt () == 0x50415443 &&
             in.readByte() == 0x48)

      // read record offset, and check for "EOF" marker
      var offset = 0
      while({offset = readOffset(in); offset} != 0x464f45) {
        output.seek(offset)

        val length = readShort(in)
        if(length==0) {
          val rle_length = readShort(in)
          val rle_byte   = in.read().toByte
          output.write((0 until rle_length) map (_ => rle_byte) toArray)
        } else {
          val buffer = new Array[Byte](length)
          in.readFully(buffer)
          output.write(buffer)
        }
      }
    } finally {
      in.close()
    }
  }
}
