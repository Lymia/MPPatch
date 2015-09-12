/*
 * Copyright (c) 2015 Lymia Alusyia <lymia@lymiahugs.com>
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

package moe.lymia.multiverse

import java.nio._
import java.security.MessageDigest
import java.util.UUID

object Crypto {
  def digest(algorithm: String, data: Seq[Byte]) = {
    val md = MessageDigest.getInstance(algorithm)
    val hash = md.digest(data.toArray)
    hash    
  }
  def hexdigest(algorithm: String, data: Seq[Byte]) =
    digest(algorithm, data).map(x => "%02x".format(x)).reduce(_ + _)

  def md5_hex(data: Seq[Byte]) = hexdigest("MD5", data)
  def sha1_hex(data: Seq[Byte]) = hexdigest("SHA1", data)

  def md5(data: Seq[Byte]) = digest("MD5", data)
  def sha1(data: Seq[Byte]) = digest("SHA1", data)

  private def makeUUID(data: Seq[Byte], version: Int) = {
    val newData    = data.updated(6, ((data(6) & 0x0F) | (version << 4)).toByte)
                         .updated(8, ((data(8) & 0x3F) | 0x80).toByte)
    val buffer = ByteBuffer.wrap(newData.toArray).asLongBuffer()
    new UUID(buffer.get(0), buffer.get(1))
  }
  def uuidToBytes(namespace: UUID) =
    ByteBuffer.allocate(16).putLong(namespace.getMostSignificantBits).putLong(namespace.getLeastSignificantBits).array
  def md5_uuid (namespace: UUID, data: Seq[Byte]) = makeUUID(md5 (uuidToBytes(namespace) ++ data), 3)
  def sha1_uuid(namespace: UUID, data: Seq[Byte]) = makeUUID(sha1(uuidToBytes(namespace) ++ data), 5)
}

object DLCKey {
  val staticInterlace =
    Seq(0x1f, 0x33, 0x93, 0xfb, 0x35, 0x0f, 0x42, 0xc7,
      0xbd, 0x50, 0xbe, 0x7a, 0xa5, 0xc2, 0x61, 0x81) map (_.toByte)
  val staticInterlaceStream = Stream.from(0) map (x => staticInterlace(x%staticInterlace.length))
  def interlaceData(data: Seq[Byte]) =
    data.zip(staticInterlaceStream).flatMap(x => Seq(x._1, x._2))

  def encodeLe32(i: Int) =
    Seq(i&0xFF, (i>>8)&0xFF, (i>>16)&0xFF, (i>>24)&0xFF)
  def encodeBe32(i: Int) = encodeLe32(i).reverse
  def encodeLe16(i: Int) =
    Seq(i&0xFF, (i>>8)&0xFF)
  def encodeUUID(u: UUID) =
    (encodeLe32(((u.getMostSignificantBits >>32) & 0xFFFFFFFF).toInt) ++
     encodeLe16(((u.getMostSignificantBits >>16) &     0xFFFF).toInt) ++
     encodeLe16(((u.getMostSignificantBits >> 0) &     0xFFFF).toInt) ++
     encodeBe32(((u.getLeastSignificantBits>>32) & 0xFFFFFFFF).toInt) ++
     encodeBe32(((u.getLeastSignificantBits>> 0) & 0xFFFFFFFF).toInt)).map(_.toByte)

  def encodeNumber(i: Int) = i.toString.getBytes("UTF8").toSeq
  def key(u: UUID, sid: Seq[Int], ptags: String*) = {
    val data = sid.map(encodeNumber) ++ ptags.map(_.getBytes("UTF8").toSeq)
    Crypto.md5_hex(interlaceData(encodeUUID(u) ++ data.fold(Seq())(_ ++ _)))
  }
}
