/*
 * Copyright (c) 2015-2017 Lymia Alusyia <lymia@lymiahugs.com>
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

import org.tukaani.xz._

case class PatchPackage(data: Map[String, Array[Byte]]) {
  def loadResource(name: String) = new String(data(name), StandardCharsets.UTF_8)
  def loadBinaryResource(name: String) = data(name)
}

object IOWrappers {
  private def writeArray(out: DataOutputStream, data: Array[Byte]) = {
    out.writeInt(data.length)
    out.write(data)
  }
  private def readArray(in: DataInputStream) = {
    val data = new Array[Byte](in.readInt())
    in.readFully(data)
    data
  }

  // XZ compression
  private val lzmaOptions  = new LZMA2Options(7)
  private val xzBCJOptions = Array[FilterOptions](new X86Options, lzmaOptions)
  private val xzOptions    = Array[FilterOptions](lzmaOptions)
  def writeXZ(out: DataOutputStream, useBCJ: Boolean = false)(f: DataOutputStream => Unit) = {
    val xzOut   = new XZOutputStream(out, if(useBCJ) xzBCJOptions else xzOptions, XZ.CHECK_SHA256)
    val dataOut = new DataOutputStream(xzOut)
    f(dataOut)
    dataOut.flush()
    xzOut.finish()
  }
  def readXZ[T](in: DataInputStream)(f: DataInputStream => T) = {
    val inStream = new XZInputStream(in)
    val out = f(new DataInputStream(inStream))
    if(inStream.read() != -1) sys.error("Expected EOF")
    out
  }

  // Patch package writer
  private val patchPackageHeader  = "MpPatchPackage"
  private val patchPackageVersion = 0
  def writePatchPackage(out: DataOutputStream, data: PatchPackage) = {
    out.writeUTF(patchPackageHeader)
    out.writeInt(patchPackageVersion)
    writeXZ(out, true) { out =>
      out.writeInt(data.data.size)
      for((k, v) <- data.data.toSeq.sortBy(_._1)) {
        out.writeUTF(k)
        writeArray(out, v)
      }
    }
  }
  def readPatchPackage(in: DataInputStream) = {
    if(in.readUTF() != patchPackageHeader ) throw new IOException("Patch package has wrong header.")
    if(in.readInt() != patchPackageVersion) throw new IOException("Patch package has unknown version.")
    readXZ(in) { in =>
      PatchPackage((for(_ <- 0 until in.readInt()) yield (in.readUTF(), readArray(in))).toMap)
    }
  }
}