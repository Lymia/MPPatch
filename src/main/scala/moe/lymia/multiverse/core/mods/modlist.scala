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

package moe.lymia.multiverse.core.mods

import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import moe.lymia.multiverse.util.IOUtils

case class ModEntry(path: Path, manifest: ModManifest)
case class ModList(modList: Seq[ModEntry], conflictingModList: Seq[ModEntry]) {
  lazy val allMods = modList ++ conflictingModList

  private def byUUIDCache = new collection.mutable.HashMap[UUID, Option[ModEntry]]
  def getByUUID(uuid: UUID) = byUUIDCache.getOrElseUpdate(uuid, {
    val found = allMods.filter(_.manifest.uuid == uuid)
    if(found.length == 1) Some(found.head) else None
  })
}

object ModList {
  def apply(path: Path) = {
    val enumeratedMods = ModEnumerator.enumerateModFiles(path)
    new ModList(ModEnumerator.loadEnumeratedMods(enumeratedMods.foundFiles      ),
                ModEnumerator.loadEnumeratedMods(enumeratedMods.conflictingFiles))
  }
}

object ModEnumerator {
  case class ModFileList(foundFiles: Seq[Path] = Seq(), conflictingFiles: Seq[Path] = Seq()) {
    val hasConflicts = conflictingFiles.nonEmpty
    def ++(fl: ModFileList) = ModFileList(foundFiles ++ fl.foundFiles, conflictingFiles ++ fl.conflictingFiles)
  }

  def enumerateModFiles(base: Path, pFoundConflict: Boolean = false): ModFileList = {
    val files = IOUtils.listFiles(base)
    val foundModFiles = files.filter(_.getFileName.toString.toLowerCase(Locale.ENGLISH).endsWith(".modinfo"))
    val foundConflict = pFoundConflict || foundModFiles.nonEmpty

    val current =
      if     (foundModFiles.isEmpty)     ModFileList()
      else if(foundModFiles.length == 1) ModFileList(foundFiles       = foundModFiles)
      else                               ModFileList(conflictingFiles = foundModFiles)

    files.filter(x => Files.isDirectory(x))
         .map(x => enumerateModFiles(x, foundConflict)).fold(current)(_ ++ _)
  }

  def loadEnumeratedMods(paths: Seq[Path]) =
    paths.map(x => ModEntry(x, ModDataReader.readModManifest(IOUtils.readXML(x))))
}
