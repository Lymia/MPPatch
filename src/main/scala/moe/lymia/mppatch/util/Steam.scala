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

import java.awt.Desktop
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import scala.jdk.CollectionConverters._

object Steam {
  // TODO: Parse this properly instead of this weirdness.
  val lineRegex = "\"[0-9]+\"\\s+\"(.*)\"".r
  def loadLibraryFolders(p: Path): Seq[Path] =
    if (Files.exists(p))
      (for (
        l <- Files
          .readAllLines(p.resolve("steamapps").resolve("libraryfolders.vdf"), StandardCharsets.UTF_8)
          .asScala
          .map(_.trim);
        m <- lineRegex.unapplySeq(l)
      ) yield Paths.get(m.head)).toSeq :+ p
    else Seq(p)

  private val desktop                    = Desktop.getDesktop
  private def loadURI(uri: String): Unit = desktop.browse(new URI(uri))

  def launchGame(gameId: Int): Unit        = loadURI("steam://run/" + gameId)
  def validateGameFiles(gameId: Int): Unit = loadURI("steam://validate/" + gameId)
}
