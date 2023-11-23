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
import java.util.{Locale, Properties}

import moe.lymia.mppatch.util.io.IOUtils

import scala.collection.JavaConverters._

case class I18N(locale: Locale, map: Map[String, String]) {
  private val messageFormatCache = new collection.mutable.HashMap[String, Option[MessageFormat]]
  def getFormat(key: String) =
    messageFormatCache.getOrElseUpdate(key, map.get(key).map(s => new MessageFormat(s, locale)))
  def hasKey(key: String) = map.contains(key)
  def apply(key: String, args: Any*) =
    getFormat(key).map(format => format.format(args.toArray)).getOrElse("<" + key + ">")
}

object I18N {
  def loadI18NData(sourceFile: String): Map[String, String] = {
    val prop   = new Properties()
    val reader = new InputStreamReader(IOUtils.getResource(sourceFile), StandardCharsets.UTF_8)
    prop.load(reader)
    reader.close()

    val includes = prop.getProperty("includes")
    val includeData = if (includes != null && includes.trim.nonEmpty) {
      includes.trim.split(",").map(x => loadI18NData(x.trim)).reduce(_ ++ _)
    } else Map()

    (
      includeData.toSeq ++ prop.asScala.filter(_._1 != "includes").map(x => x.copy(_1 = x._1.trim, _2 = x._2)).toSeq
    ).toMap
  }

  private def defaultLocale = Locale.US
  private def sourceFile(locale: Locale, generic: Boolean) =
    s"text/i18n_${locale.getLanguage}_${if (generic) "generic" else locale.getCountry}.properties"
  private def getSingleLocaleStrings(locale: Locale, generic: Boolean): Map[String, String] = {
    val file = sourceFile(locale, generic)
    if (IOUtils.resourceExists(file)) loadI18NData(file) else Map()
  }
  private def getLocaleStrings(locale: Locale): Map[String, String] =
    getSingleLocaleStrings(defaultLocale, true) ++ getSingleLocaleStrings(defaultLocale, false) ++
      getSingleLocaleStrings(locale, true) ++ getSingleLocaleStrings(locale, false)

  def apply(locale: Locale) = new I18N(locale, getLocaleStrings(locale))
}
