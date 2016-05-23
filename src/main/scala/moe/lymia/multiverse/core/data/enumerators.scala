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

import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import scala.Predef.{Manifest => _, _} // shut up compiler warning

import moe.lymia.multiverse.util.IOUtils

case class ManifestEntry[Manifest <: ManifestCommon](path: Path, manifest: Manifest)
case class ManifestList [Manifest <: ManifestCommon](manifestList: Seq[ManifestEntry[Manifest]]) {
  lazy val (byUUID, uuidConflicts) = {
    val byUUID        = new collection.mutable.HashMap[UUID, ManifestEntry[Manifest]]
    val uuidConflicts = new collection.mutable.TreeSet[UUID]
    for(entry <- manifestList) {
      if(byUUID.contains(entry.manifest.uuid)) {
        byUUID.remove(entry.manifest.uuid)
        uuidConflicts.add(entry.manifest.uuid)
      } else if(!uuidConflicts.contains(entry.manifest.uuid)) {
        byUUID.put(entry.manifest.uuid, entry)
      }
    }
    (byUUID.toMap, uuidConflicts.toSet)
  }
}

class GenericListWrapper[Manifest <: ManifestCommon](defaultExtension: String,
                                                     loader: Seq[Path] => Seq[ManifestEntry[Manifest]]) {
  def apply(path: Path, extension: String = defaultExtension, filter: Path => Boolean = _ => true) = {
    val enumeratedFiles = DirectoryEnumerator.enumerateFiles(path, extension, filter)
    new ManifestList[Manifest](loader(enumeratedFiles.foundFiles ++ enumeratedFiles.conflictingFiles))
  }
}
object ModList extends GenericListWrapper(".modinfo", DirectoryEnumerator.loadEnumeratedMods)
object DLCList extends GenericListWrapper(".civ5pkg", DirectoryEnumerator.loadEnumeratedDLC )

object DirectoryEnumerator {
  case class EnumeratedFilesList(foundFiles: Seq[Path] = Seq(), conflictingFiles: Seq[Path] = Seq()) {
    lazy val files = foundFiles ++ conflictingFiles

    val hasConflicts = conflictingFiles.nonEmpty
    def ++(fl: EnumeratedFilesList) =
      EnumeratedFilesList(foundFiles ++ fl.foundFiles, conflictingFiles ++ fl.conflictingFiles)
  }

  def enumerateFiles(base: Path, extension: String, filter: Path => Boolean = _ => true,
                     pFoundConflict: Boolean = false): EnumeratedFilesList =
    if(filter(base)) {
      val files = IOUtils.listFiles(base)
      val foundModFiles = files.filter(_.getFileName.toString.toLowerCase(Locale.ENGLISH).endsWith(extension))
      val foundConflict = pFoundConflict || foundModFiles.nonEmpty

      val current =
        if     (foundModFiles.isEmpty)     EnumeratedFilesList()
        else if(foundModFiles.length == 1) EnumeratedFilesList(foundFiles       = foundModFiles)
        else                               EnumeratedFilesList(conflictingFiles = foundModFiles)

      files.filter(x => Files.isDirectory(x))
           .map(x => enumerateFiles(x, extension, filter, foundConflict)).fold(current)(_ ++ _)
    } else EnumeratedFilesList()

  def loadEnumeratedMods(paths: Seq[Path]) =
    paths.map(x => ManifestEntry(x, ModDataReader.readModManifest(x)))
  def loadEnumeratedDLC(paths: Seq[Path]) =
    paths.map(x => ManifestEntry(x, DLCDataReader.readDlcManifest(x)))
}
