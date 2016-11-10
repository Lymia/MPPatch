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

package moe.lymia.mppatch.core

import java.util.{Locale, UUID}

import moe.lymia.mppatch.util.io._

import scala.xml.Node

case class DLCUISkin(name: String, set: String, platform: String)
case class DLCManifest(uuid: UUID, version: Int, priority: Int, shortName: String, name: String)
case class DLCGameplay(textData: Map[String, Node] = Map(),
                       uiFiles: Map[String, Map[String, Array[Byte]]] = Map(), uiSkins: Seq[DLCUISkin] = Nil)
case class DLCData(manifest: DLCManifest, data: DLCGameplay)

object DLCDataWriter {
  private val languageList = Seq("en_US","fr_FR","de_DE","es_ES","it_IT","ru_RU","ja_JP","pl_PL","ko_KR","zh_Hant_HK")
  private def languageValues(string: String) = languageList.map { x =>
    <Value language={x}>{string}</Value>
  }

  def writeDLC(dlcBasePath: String, languageDirPath: Option[String], dlcData: DLCData, platform: Platform) = {
    val nameString = s"${dlcData.manifest.uuid.toString.replace("-", "")}_v${dlcData.manifest.version}"

    val uiFiles = dlcData.data.uiFiles.map(y => y._2.map(x =>
      s"$dlcBasePath/${platform.mapPath(y._1)}/${platform.mapPath(x._1)}" -> x._2)).reduce(_ ++ _)
    val newFiles = Map(s"$dlcBasePath/${platform.mapPath(s"$nameString.Civ5Pkg")}" -> IOUtils.writeXMLBytes(
      <Civ5Package>
        <GUID>{s"{${dlcData.manifest.uuid}}"}</GUID>
        <Version>{dlcData.manifest.version.toString}</Version>
        <Name>{languageValues(nameString)}</Name>
        <Description>{languageValues(dlcData.manifest.name)}</Description>
        <Priority>{dlcData.manifest.priority.toString}</Priority>

        <SteamApp>99999</SteamApp>
        <Ownership>FREE</Ownership>
        <PTags>
          <Tag>Version</Tag>
          <Tag>Ownership</Tag>
        </PTags>
        {
          for(DLCUISkin(name, set, skinPlatform) <- dlcData.data.uiSkins) yield
            <UISkin name={name} set={set} platform={skinPlatform}>
              <Skin>
                { dlcData.data.uiFiles.map(x => <Directory>{x._1}</Directory>) }
              </Skin>
            </UISkin>
        }
      </Civ5Package>
    ))
    val uuid_string = dlcData.manifest.uuid.toString.replace("-", "").toUpperCase(Locale.ENGLISH)
    val languageFiles = languageDirPath.fold(Map[String, Array[Byte]]()) { languagePath =>
      dlcData.data.textData.map(x =>
        s"$languagePath/${platform.mapPath(s"${nameString}_TextData_${x._1}")}" -> IOUtils.writeXMLBytes(x._2))
    }

    languageFiles ++ newFiles ++ uiFiles : Map[String, Array[Byte]]
  }
}
