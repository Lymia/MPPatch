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

package moe.lymia.multiverse.core.data

import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import moe.lymia.multiverse.util.IOUtils

case class ManifestEntry[Manifest <: ManifestCommon](path: Path, manifest: Manifest)
case class ManifestList [Manifest <: ManifestCommon](manifestList: Seq[ManifestEntry[Manifest]]) {
  private def byUUIDCache = new collection.mutable.HashMap[UUID, Option[ManifestEntry[Manifest]]]
  def getByUUID(uuid: UUID) = byUUIDCache.getOrElseUpdate(uuid, {
    val found = manifestList.filter(_.manifest.uuid == uuid)
    if(found.length == 1) Some(found.head) else None
  })
}
class GenericListWrapper[Manifest <: ManifestCommon](defaultExtension: String,
                                                     loader: Seq[Path] => Seq[ManifestEntry[Manifest]]) {
  def apply(path: Path, extension: String = defaultExtension) = {
    val enumeratedFiles = ModEnumerator.enumerateFiles(path, extension)
    new ManifestList[Manifest](loader(enumeratedFiles.foundFiles ++ enumeratedFiles.conflictingFiles))
  }
}

object ModList extends GenericListWrapper(".modinfo", ModEnumerator.loadEnumeratedMods)
object DLCList extends GenericListWrapper(".civ5pkg", ModEnumerator.loadEnumeratedDLC )

object ModEnumerator {
  case class EnumeratedFilesList(foundFiles: Seq[Path] = Seq(), conflictingFiles: Seq[Path] = Seq()) {
    lazy val files = foundFiles ++ conflictingFiles

    val hasConflicts = conflictingFiles.nonEmpty
    def ++(fl: EnumeratedFilesList) =
      EnumeratedFilesList(foundFiles ++ fl.foundFiles, conflictingFiles ++ fl.conflictingFiles)
  }

  def enumerateFiles(base: Path, extension: String, pFoundConflict: Boolean = false): EnumeratedFilesList = {
    val files = IOUtils.listFiles(base)
    val foundModFiles = files.filter(_.getFileName.toString.toLowerCase(Locale.ENGLISH).endsWith(extension))
    val foundConflict = pFoundConflict || foundModFiles.nonEmpty

    val current =
      if     (foundModFiles.isEmpty)     EnumeratedFilesList()
      else if(foundModFiles.length == 1) EnumeratedFilesList(foundFiles       = foundModFiles)
      else                               EnumeratedFilesList(conflictingFiles = foundModFiles)

    files.filter(x => Files.isDirectory(x))
         .map(x => enumerateFiles(x, extension, foundConflict)).fold(current)(_ ++ _)
  }

  def loadEnumeratedMods(paths: Seq[Path]) =
    paths.map(x => ManifestEntry(x, ModDataReader.readModManifest(IOUtils.readXML(x))))
  def loadEnumeratedDLC(paths: Seq[Path]) =
    paths.map(x => ManifestEntry(x, DLCDataReader.readDlcManifest(IOUtils.readXML(x))))
}
