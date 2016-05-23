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

import moe.lymia.multiverse.platform.Platform
import moe.lymia.multiverse.util.{Crypto, IOUtils}

object DLCKey {
  private val staticInterlace =
    Seq(0x1f, 0x33, 0x93, 0xfb, 0x35, 0x0f, 0x42, 0xc7,
      0xbd, 0x50, 0xbe, 0x7a, 0xa5, 0xc2, 0x61, 0x81) map (_.toByte)
  private val staticInterlaceStream = Stream.from(0) map (x => staticInterlace(x%staticInterlace.length))
  private def interlaceData(data: Seq[Byte]) =
    data.zip(staticInterlaceStream).flatMap(x => Seq(x._1, x._2))

  private def encodeLe32(i: Int) =
    Seq(i&0xFF, (i>>8)&0xFF, (i>>16)&0xFF, (i>>24)&0xFF)
  private def encodeBe32(i: Int) = encodeLe32(i).reverse
  private def encodeLe16(i: Int) =
    Seq(i&0xFF, (i>>8)&0xFF)
  private def encodeUUID(u: UUID) =
    (encodeLe32(((u.getMostSignificantBits >>32) & 0xFFFFFFFF).toInt) ++
     encodeLe16(((u.getMostSignificantBits >>16) &     0xFFFF).toInt) ++
     encodeLe16(((u.getMostSignificantBits >> 0) &     0xFFFF).toInt) ++
     encodeBe32(((u.getLeastSignificantBits>>32) & 0xFFFFFFFF).toInt) ++
     encodeBe32(((u.getLeastSignificantBits>> 0) & 0xFFFFFFFF).toInt)).map(_.toByte)
  private def encodeNumber(i: Int) = i.toString.getBytes("UTF8").toSeq

  def key(u: UUID, sid: Seq[Int], ptags: String*) = {
    val data = sid.map(encodeNumber) ++ ptags.map(_.getBytes("UTF8").toSeq)
    Crypto.md5_hex(interlaceData(encodeUUID(u) ++ data.fold(Seq())(_ ++ _)))
  }
}

object DLCDataWriter {
  private val languageList = Seq("en_US","fr_FR","de_DE","es_ES","it_IT","ru_RU","ja_JP","pl_PL","ko_KR","zh_Hant_HK")
  private def languageValues(string: String) = languageList.map { x =>
    <Value language={x}>{string}</Value>
  }

  private def writeImportedFile(target: Path, source: ImportedFile) = source match {
    case ImportFromMemory(data) => IOUtils.writeFile(target, data)
    case ImportFromPath  (path) => Files.copy(path, target)
  }

  private def commonHeader(name: String, id: UUID, version: Int) =
    <GUID>{s"{$id}"}</GUID>
    <Version>{version.toString}</Version>
    <Name> {languageValues(s"${id.toString.replace("-", "")}_v$version")} </Name>
    <Description> {languageValues(name)} </Description>

    <SteamApp>99999</SteamApp>
    <Ownership>FREE</Ownership>
    <PTags>
      <Tag>Version</Tag>
      <Tag>Ownership</Tag>
    </PTags>
    <Key>{DLCKey.key(id, Seq(99999), version.toString, "FREE")}</Key>
  def writeDLC(dlcBasePath: Path, languageFilePath: Option[Path], dlcData: DLCData, platform: Platform) = {
    var id = 0
    def newId() = {
      id = id + 1
      id
    }

    val nameString = s"${dlcData.manifest.uuid.toString.replace("-", "")}_v${dlcData.manifest.version}"

    def writeIncludes(pathName: String, includes: Seq[DLCInclude]) = {
      val path = platform.resolve(dlcBasePath, pathName)
      if(includes.nonEmpty) Files.createDirectories(path)
      for(DLCInclude(event, fileData) <- includes) yield {
        val fileName = s"mvmm_include_${nameString}_${event}_${newId()}.xml"
        IOUtils.writeXML(platform.resolve(path, fileName), fileData)
        <NODE>{fileName}</NODE>.copy(label = event)
      }
    }
    def writeUISkins(skins: Seq[DLCUISkin]) =
      for(DLCUISkin(name, set, skinPlatform, includeImports, files) <- skins) yield {
        val dirName = s"UISkin_${name}_${set}_$skinPlatform"
        val dirPath = platform.resolve(dlcBasePath, dirName)
        if(files.nonEmpty) Files.createDirectories(dirPath)
        for((name, file) <- files) writeImportedFile(platform.resolve(dirPath, name), file)
        <UISkin name={name} set={set} platform={skinPlatform}>
          <Skin>
            { if(files.nonEmpty) Seq(<Directory>{dirName}</Directory>) else Seq() }
            { if(includeImports) Seq(<Directory>Files</Directory>) else Seq() }
          </Skin>
        </UISkin>
      }

    if(dlcData.data.mapEntries.nonEmpty) {
      val mapDirectory = platform.resolve(dlcBasePath, "Maps")
      Files.createDirectories(mapDirectory)
      for (DLCMap(extension, data) <- dlcData.data.mapEntries)
        writeImportedFile(mapDirectory.resolve(s"mvmm_map_${nameString}_${newId()}.$extension"), data)
    }
    val mapsTag = if(dlcData.data.mapEntries.nonEmpty) Seq(<MapDirectory>Maps</MapDirectory>) else Seq()

    val filesDirectory = platform.resolve(dlcBasePath, "Files")
    Files.createDirectories(filesDirectory)
    for((name, file) <- dlcData.data.importFileList) writeImportedFile(platform.resolve(filesDirectory, name), file)

    IOUtils.writeXML(platform.resolve(dlcBasePath, s"$nameString.Civ5Pkg"), <Civ5Package>
      {commonHeader(dlcData.manifest.name, dlcData.manifest.uuid, dlcData.manifest.version)}

      <Priority>{dlcData.manifest.priority.toString}</Priority>

      {writeIncludes("GlobalImports", dlcData.data.globalIncludes)}
      <Gameplay>
        {writeIncludes("GameplayImports", dlcData.data.gameplayIncludes)}
        <Directory>Files</Directory>
        {if(dlcData.data.gameplayIncludes.nonEmpty) Seq(<Directory>GameplayImports</Directory>) else Seq()}
        {mapsTag}
      </Gameplay>

      { writeUISkins(dlcData.data.uiSkins) }
    </Civ5Package>)

    val uuid_string = dlcData.manifest.uuid.toString.replace("-", "").toUpperCase(Locale.ENGLISH)
    languageFilePath.foreach(languagePath =>
      IOUtils.writeXML(languagePath, <GameData>
        {
          languageList.flatMap(x =>
            <NODE>
              <Row Tag={s"TXT_KEY_${uuid_string}_NAME"}>
                <Text>{dlcData.manifest.uuid.toString.replace("-", "")+"_v"+dlcData.manifest.version}</Text>
              </Row>
              <Row Tag={s"TXT_KEY_${uuid_string}_DESCRIPTION"}>
                <Text>{dlcData.manifest.name}</Text>
              </Row>
            </NODE>.copy(label = s"Language_$x")
          )
        }
      </GameData>)
    )
  }
}
