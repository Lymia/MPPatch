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

package moe.lymia.mppatch.ui

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.util.{Locale, Properties}

import moe.lymia.mppatch.util.IOUtils

import scala.annotation.tailrec
import scala.collection.JavaConversions._

case class I18N(locale: Locale, map: Map[String, String]) {
  private val messageFormatCache = new collection.mutable.HashMap[String, Option[MessageFormat]]
  def getFormat(key: String) =
    messageFormatCache.getOrElseUpdate(key, map.get(key).map(s => new MessageFormat(s, locale)))
  def apply(key: String, args: Any*) = getFormat(key).map(format =>
    format.format(args.toArray)
  ).getOrElse("<"+key+">")
}

object I18N {
  private def defaultLocale = Locale.US
  private def sourceFile(locale: Locale, generic: Boolean) =
    s"i18n/${locale.getLanguage}_${if(generic) "generic" else locale.getCountry}.properties"

  @tailrec def findSourceFile(locale: Locale): String =
         if(IOUtils.resourceExists(sourceFile(locale, false))) sourceFile(locale, false)
    else if(IOUtils.resourceExists(sourceFile(locale, true ))) sourceFile(locale, true )
    else if(locale != defaultLocale)                           findSourceFile(defaultLocale)
    else                                                       sys.error("default locale file not found!")

  def loadI18NData(sourceFile: String): Map[String, String] = {
    val prop = new Properties()
    prop.load(new InputStreamReader(IOUtils.getResource(sourceFile), StandardCharsets.UTF_8))

    val includes = prop.getProperty("includes")
    val includeData = if(includes != null && includes.trim.nonEmpty) {
      includes.trim.split(",").map(x => loadI18NData(x.trim)).reduce(_ ++ _)
    } else Map()

    includeData ++ prop.filter(_._1 != "includes").map(x => x.copy(_1 = x._1.trim, _2 = x._2.trim))
  }

  def apply(locale: Locale) = new I18N(locale, loadI18NData(findSourceFile(locale)))
}