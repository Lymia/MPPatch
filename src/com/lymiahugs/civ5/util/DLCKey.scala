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

package com.lymiahugs.civ5.util

import java.util.UUID
import java.security.MessageDigest

// Please do not use this to do anything nasty like crack DLC's DRMs or anything.
// I will not like you if you do that. :(
// You want to be liked, don't you?
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

  def md5(data: Seq[Byte]) = {
    val md = MessageDigest.getInstance("MD5")
    val hash = md.digest(data.toArray)
    hash.map(x => "%02x".format(x)).reduce(_ + _)
  }
  def encodeNumber(i: Int) =
    i.toString.getBytes("UTF-8").toSeq
  def key(u: UUID, sid: Seq[Int], version: Int, ownership: String) =
    md5(interlaceData(encodeUUID(u) ++ sid.map(encodeNumber _).fold(Seq())(_ ++ _) ++
                      encodeNumber(version) ++ ownership.getBytes("UTF-8")))
}
