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

package moe.lymia.mppatch.util

import java.util.{Date, Properties}

import moe.lymia.mppatch.util.io.IOUtils

trait VersionInfoSource {
  def apply(key: String, default: String): String
}
object NullSource extends VersionInfoSource {
  def apply(key: String, default: String) = default
}
case class PropertiesSource(prop: Properties) extends VersionInfoSource {
  def apply(key: String, default: String) = {
    val p = prop.getProperty(key)
    if(p == null || p.isEmpty) default else p
  }
}

class VersionInfo(properties: VersionInfoSource) {
  lazy val majorVersion  = properties("mppatch.version.major","-1").toInt
  lazy val minorVersion  = properties("mppatch.version.minor","-1").toInt
  lazy val patchVersion  = properties("mppatch.version.patch","0").toInt

  lazy val versionSuffix = properties("mppatch.version.suffix","0")
  lazy val commit        = properties("mppatch.version.commit","unknown")
  lazy val treeStatus    = properties("mppatch.version.treestatus","unknown")
  lazy val versionString = properties("mppatch.version.string","unknown")
  lazy val isDirty       = properties("mppatch.version.clean", "false") == "false"

  lazy val buildDate     = new Date(properties("build.time", "0").toLong)
  lazy val buildUser     = properties("build.user", "<unknown>")
}
object VersionInfo {
  def loadFromResource(resource: String) = {
    val stream = IOUtils.getResource(resource)
    new VersionInfo(if(stream == null) NullSource else {
      val prop = new Properties()
      prop.load(stream)
      PropertiesSource(prop)
    })
  }

  val fromJar = loadFromResource("version.properties")
}