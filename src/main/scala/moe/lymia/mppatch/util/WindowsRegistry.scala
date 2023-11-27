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

package moe.lymia.mppatch.util

import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.prefs.Preferences
import scala.sys.process.Process

object WindowsRegistry {
  private def callRegQuery(key: String, value: String): Option[String] = try
    Some(Process(Seq("reg", "query", key, "/v", value)).!!)
  catch {
    case e: Exception => None
  }

  private def parseRegQuery(data: String, value: String): Option[String] =
    data
      .strip()
      .split("\n")
      .tail
      .flatMap { line =>
        val arr = line.strip().split(" +", 3)
        if (arr.length == 3 && arr(0) == value) {
          Some(arr(2))
        } else {
          None
        }
      }
      .headOption

  def regQuery(key: String, value: String): Option[String] =
    callRegQuery(key, value).flatMap(x => parseRegQuery(x, value))
}
