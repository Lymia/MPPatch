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

package com.lymiahugs.mod2dlc.data

case class Patch(patch: String, debugPatch: String, copyPath: String) {
  def fileData(debug: Boolean) =
    if(debug) loadBinaryResource("patches/"+debugPatch)
    else loadBinaryResource("patches/"+patch)
}
case class PatchSet(name: String, origName: String, patches: Map[String, Patch])
object Patches {
  private def loadPatchSet(name: String) =
    loadResource("patches/"+name+"_versions.mf").split("\n").filter(!_.isEmpty).map(_.split(" ") match {
      case Array(version, sourcePath, debugPath, copyPath) => version -> Patch(sourcePath, debugPath, copyPath)
    }).toMap

  lazy val CvGameDatabase_versions =
    PatchSet("CvGameDatabase", "CvGameDatabaseWin32Final Release.dll", loadPatchSet("CvGameDatabase"))
}
