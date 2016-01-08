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

package moe.lymia.multiverse

import java.io.InputStream

package object data {
  val languageList = Seq("en_US","fr_FR","de_DE","es_ES","it_IT","ru_RU","ja_JP","pl_PL","ko_KR","zh_Hant_HK")

  private[data] def getResource(s: String) =
    getClass.getResourceAsStream("/moe/lymia/multiverse/data/" +s)

  private[data] def loadFromStream(s: InputStream) =
    io.Source.fromInputStream(s, "UTF-8").mkString
  private[data] def loadBinaryResourceFromStream(s: InputStream) =
    Stream.continually(s.read).takeWhile(_ != -1).map(_.toByte).toArray

  private[data] def loadResource(s: String) = loadFromStream(getResource(s))
  private[data] def loadBinaryResource(s: String) = loadBinaryResourceFromStream(getResource(s))
}
