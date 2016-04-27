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

package moe.lymia.multiverse.core.data

import java.util.UUID

import scala.xml.Node

case class DLCUISkin(name: String, set: String, platform: String, includeImports: Boolean,
                     skinSpecificDirectory: Map[String, Array[Byte]])
case class DLCInclude(event: String, fileData: Node)
case class DLCMap(extension: String, data: Array[Byte])

case class DLCGameplay(gameplayIncludes: Seq[DLCInclude], globalIncludes: Seq[DLCInclude], mapEntries: Seq[DLCMap],
                       importFileList: Map[String, Array[Byte]], uiSkins: Seq[DLCUISkin])
case class DLCManifest(uuid: UUID, version: Int, priority: Int, shortName: String, name: String)
  extends ManifestCommon

case class DLCData(manifest: DLCManifest, data: DLCGameplay)
