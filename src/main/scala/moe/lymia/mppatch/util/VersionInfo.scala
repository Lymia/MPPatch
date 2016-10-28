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

import java.util.Properties

import moe.lymia.mppatch.util.common.IOUtils

object VersionInfo {
  private lazy val properties = {
    val prop = new Properties()
    val resource = IOUtils.getResource("version.properties")
    if(resource==null) (key: String, default: String) => default
    else {
      prop.load(resource)
      (key: String, default: String) => {
        val p = prop.getProperty("mppatch."+key)
        if(p==null || p.isEmpty) default else p
      }
    }
  }

  lazy val majorVersion  = Integer.parseInt(properties("version.major","-1"))
  lazy val minorVersion  = Integer.parseInt(properties("version.minor","-1"))
  lazy val patchVersion  = Integer.parseInt(properties("version.patch","0"))
  lazy val versionSuffix = properties("version.suffix","0")
  lazy val commit        = properties("version.commit","unknown")
  lazy val treeStatus    = properties("version.treestatus","unknown")
  lazy val versionString = properties("version.string","unknown")
  lazy val isDirty       = properties("version.clean", "false") == "true"

  lazy val patchCompat   = Integer.parseInt(properties("patch.compat", "-1"))
}