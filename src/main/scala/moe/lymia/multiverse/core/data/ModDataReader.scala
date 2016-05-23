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

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Locale, UUID}

import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.{Crypto, IOUtils}
import moe.lymia.multiverse.util.XMLUtils._

import scala.xml.{Node, NodeSeq, XML}

object ModDataReader {
  private def normalizeName(name: String) = name.toLowerCase(Locale.ENGLISH)

  // Mod manifest reader
  private def readModReferenceList(n: NodeSeq) = n.flatMap(_.child).collect {
    case game if game.label == "Game" => ModGameVersion(getAttribute(game, "minversion"), getAttribute(game, "maxversion"))
    case mod if mod.label == "Mod" =>
      ModToModReference(UUID.fromString(getAttribute(mod, "id")),
        getAttribute(mod, "minversion").toInt, getAttribute(mod, "maxversion").toInt,
        getAttribute(mod, "title"))
    case dlc if dlc.label == "DLC" =>
      ModDlcDependency(UUID.fromString(getAttribute(dlc, "id")),
        getAttribute(dlc, "minversion").toInt, getAttribute(dlc, "maxversion").toInt)
  }
  private def readModRawProperties(modData: Node) = (modData \ "Properties").flatMap(_.child).collect {
    case n: Node => (n.label, n.text)
  }.toMap
  private def readModManifestData(modBasePath: Option[Path], modData: Node, manifestChecksum: String) = {
    val properties = (modData \ "Properties").head
    val authorship = ModAuthorshipInformation(getNodeText(properties, "Authors"),
                                              getNodeText(properties, "SpecialThanks"),
                                              getNodeText(properties, "Homepage"))

    ModManifest(UUID.fromString(getAttribute(modData, "id")), getAttribute(modData, "version").toInt,
                getNodeText(properties, "Name"),
                getNodeText(properties, "Teaser"),
                getNodeText(properties, "Description"),
                authorship,
                readModRawProperties(modData),
                readModReferenceList(modData \ "Dependencies"),
                readModReferenceList(modData \ "References"),
                readModReferenceList(modData \ "Blocks"),
                manifestChecksum, modData, modBasePath)
  }
  private def readModManifestFromFile(modBasePath: Option[Path], modData: Path) =
    readModManifestData(modBasePath, IOUtils.readXML(modData), Crypto.sha1_hex(IOUtils.readFileAsBytes(modData)))
  def readModManifest(modBasePath: Path, modData: Path) = readModManifestFromFile(Some(modBasePath), modData)
  def readModManifest(                   modData: Path) = readModManifestFromFile(None             , modData)

  // Mod gameplay reader
  private def loadScriptFile(fileName: String, string: String) =
    if(normalizeName(fileName).endsWith(".xml")) Some(ModXmlSource(XML.loadString(string)))
    else if(normalizeName(fileName).endsWith(".sql")) Some(ModSqlSource(string))
    else None
  private def readFile(modBasePath: Path, path: String, platform: Platform) =
    Files.readAllBytes(platform.resolve(modBasePath, path))
  private def readModActionList(modBasePath: Path, n: NodeSeq, platform: Platform) = n.flatMap(_.child).collect {
    case x: Node => x.label match {
      case "UpdateDatabase" | "UpdateUserData" =>
        val fileName = x.text.trim
        val contents = new String(readFile(modBasePath, fileName, platform), StandardCharsets.UTF_8)
        loadScriptFile(fileName, contents).toSeq.map(ModUpdateDatabaseAction)
      case "ExecuteScript" =>
        Seq(ModExecuteScriptAction (new String(readFile(modBasePath, x.text.trim, platform))))
      case _ => Seq()
    }
  }.flatten

  private case class FileListEntry(name: String, isImported: Boolean, md5: String, path0: Path) {
    lazy val path = {
      if(!Files.exists(path0)) sys.error(s"file $name could not be found!")
      path0
    }
  }
  private def readFileList(modBasePath: Path, n: NodeSeq, p: Platform): Map[String, FileListEntry] =
    n.flatMap(_.child).collect {
      case file if file.label == "File" =>
        normalizeName(file.text) ->
          FileListEntry(file.text, getAttribute(file, "import") == "1", getAttribute(file, "md5"),
                        p.resolve(modBasePath, file.text))
    }.toMap
  private def loadFileList(modBasePath: Path, files: Map[String, (String, Boolean, String)], platform: Platform) =
    for((_, (name, doImport, md5)) <- files if doImport) yield
      (name, platform.resolve(modBasePath, name))
  private def lookupFile(files: Map[String, FileListEntry], path: String) = files.get(normalizeName(path))
  private def extractImportedFiles(files: Map[String, FileListEntry]) =
    files.filter(_._2.isImported).map(x => (x._1, ImportFromPath(x._2.path)))

  private def readModEntryPoints(uuid: UUID, modBasePath: Path, n: NodeSeq,
                                 fileList: Map[String, FileListEntry], platform: Platform) = {
    val uuidStr    = normalizeName(uuid.toString).replace("-", "")
    val out        = n.flatMap(_.child).collect {
      case ep if ep.label == "EntryPoint" =>
        val fileName  = getAttribute(ep, "file")
        val fileEntry = lookupFile(fileList, fileName).getOrElse(sys.error("Entry point references unknown file!"))

        val filteredFileName = fileName.split(raw"[/\\]").last
        val outputName = if(fileEntry.isImported) fileName
        else s"mvmm_entrypoint_noexport_${uuidStr}_${fileEntry.md5}_$filteredFileName"

        val entryPoint = ModEntryPoint(getAttribute(ep, "type"), getNodeText(ep, "Name"),
                                       getNodeText(ep, "Description"), outputName)
        val fileListEntry = if(!fileEntry.isImported) Some((outputName, ImportFromPath(fileEntry.path))) else None
        (entryPoint, fileListEntry)
    }
    (out.map(_._1), out.flatMap(_._2.toSeq))
  }

  def readModGameplay(modBasePath: Path, manifest: ModManifest, platform: Platform): ModGameplay = {
    val modData = manifest.manifestData
    val uuid = UUID.fromString(getAttribute(modData, "id"))
    val fileList = readFileList(modBasePath, modData \ "Files", platform)
    val (entryPoints, entryFiles) = readModEntryPoints(uuid, modBasePath, modData \ "EntryPoints", fileList, platform)
    ModGameplay(extractImportedFiles(fileList) ++ entryFiles,
                entryPoints,
                readModActionList(modBasePath, modData \ "Actions" \ "OnModActivated"     , platform),
                readModActionList(modBasePath, modData \ "Actions" \ "OnCreateModUserData", platform),
                (modData \ "Actions" \ "OnGetDLLPath" \ "SetDLLPath").headOption flatMap { pathElem =>
                  lookupFile(fileList, pathElem.text.trim).map(x => ImportFromPath(x.path))})
  }
  def readModGameplay(manifest: ModManifest, platform: Platform): ModGameplay =
    readModGameplay(manifest.modDataPath.getOrElse(sys.error("gameplay data path not set!")), manifest, platform)

  // Mod loader
  def loadMod(modBasePath: Path, manifestPath: Path, platform: Platform) = {
    val manifest = readModManifest(modBasePath, manifestPath)
    ModData(manifest, readModGameplay(modBasePath, manifest, platform))
  }
}
