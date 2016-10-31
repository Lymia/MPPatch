/*
 * Copyright (c) 2015-2016 Lymia Alusyia <lymia@lymiahugs.com>
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

package moe.lymia.mppatch.util.common

import java.io._
import java.nio.charset.StandardCharsets
import java.util

import org.tukaani.xz.{LZMA2Options, XZInputStream, XZOutputStream}

case class PatchPackage(data: Map[String, Array[Byte]]) {
  def loadResource(name: String) = new String(data(name), StandardCharsets.UTF_8)
  def loadBinaryResource(name: String) = data(name)
}

object IOWrappers {
  // XZ compression
  val xzOptions = new LZMA2Options(9)
  def writeXZ(out: DataOutputStream)(f: DataOutputStream => Unit) = {
    val xzOut   = new XZOutputStream(out, xzOptions)
    val dataOut = new DataOutputStream(xzOut)
    f(dataOut)
    dataOut.flush()
    xzOut.finish()
  }
  def readXZ[T](in: DataInputStream)(f: DataInputStream => T) = {
    val inStream = new XZInputStream(in)
    f(new DataInputStream(inStream))
  }

  // Integrity
  def writeIntegrity(out: DataOutputStream)(f: DataOutputStream => Unit) = {
    val byteOut = new ByteArrayOutputStream()
    val dataOut = new DataOutputStream(byteOut)
    f(dataOut)
    dataOut.flush()

    val data = byteOut.toByteArray
    dataOut.close()
    val hash = Crypto.sha256(data)

    writeArray(out, hash)
    writeArray(out, data)
  }
  def readIntegrity[T](in: DataInputStream)(f: DataInputStream => T) = {
    val hash = readArray(in)
    val data = readArray(in)
    if(!util.Arrays.equals(Crypto.sha256(data), hash))
      throw new IOException("Checksum mismatch.")
    f(new DataInputStream(new ByteArrayInputStream(data)))
  }

  // Patch package writer
  val patchPackageHeader  = "MpPatchPackage"
  val patchPackageVersion = 0
  def writePatchPackage(out: DataOutputStream, data: PatchPackage) = {
    out.writeUTF(patchPackageHeader)
    out.writeInt(patchPackageVersion)
    writeIntegrity(out) { out =>
      writeXZ(out) { out =>
        out.writeInt(data.data.size)
        for((k, v) <- data.data) {
          out.writeUTF(k)
          writeArray(out, v)
        }
      }
    }
  }
  def readPatchPackage(in: DataInputStream) = {
    if(in.readUTF() != patchPackageHeader ) throw new IOException("Patch package has wrong header.")
    if(in.readInt() != patchPackageVersion) throw new IOException("Patch package has unknown version.")
    readIntegrity(in) { in =>
      readXZ(in) { in =>
        PatchPackage((for(_ <- 0 until in.readInt()) yield (in.readUTF(), readArray(in))).toMap)
      }
    }
  }
}