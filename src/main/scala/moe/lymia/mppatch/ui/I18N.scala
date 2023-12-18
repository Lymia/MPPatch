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

package moe.lymia.mppatch.ui

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.util.{Locale, Properties, ResourceBundle}
import moe.lymia.mppatch.util.io.IOUtils

import scala.jdk.CollectionConverters.*

case class I18N(bundle: ResourceBundle) {
  private val messageFormatCache = new collection.mutable.HashMap[String, Option[MessageFormat]]
  def getFormat(key: String) =
    messageFormatCache.getOrElseUpdate(
      key,
      Option(bundle.getString(key)).map(s => new MessageFormat(s, bundle.getLocale))
    )
  def hasKey(key: String) = bundle.containsKey(key)
  def apply(key: String, args: Any*) =
    getFormat(key).map(format => format.format(args.toArray)).getOrElse("<" + key + ">")
}

object I18N {
  def apply(locale: Locale) = new I18N(ResourceBundle.getBundle("moe.lymia.mppatch.ui.TextData", locale))
}
