/*
 * Copyright (c) 2015 Lymia Alusyia <lymia@lymiahugs.com>
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

import java.nio.file.Path
import java.util.UUID

import moe.lymia.multiverse.platform.Platform

import scala.xml.Node

case class DLCUISkin(name: String, set: String, platform: String, skinSpecificDirectory: Map[String, Seq[Byte]])
case class DLCInclude(event: String, fileData: Node)
case class DLCData(id: UUID, version: Int, priority: Int, name: String, description: String,
                   gameplayIncludes: Seq[DLCInclude], globalIncludes: Seq[DLCInclude], mapEntries: Seq[Seq[Byte]],
                   importFileList: Map[String, Seq[Byte]], uiSkins: Seq[DLCUISkin],
                   sourceModInfo: Option[ModAuthorshipInformation] = None)

object DLCDataWriter {
  def writeDLC(dlcBasePath: Path, dlcData: DLCData, platform: Platform) = {

  }
}