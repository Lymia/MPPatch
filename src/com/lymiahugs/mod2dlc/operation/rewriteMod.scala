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

package com.lymiahugs.mod2dlc.operation

import com.lymiahugs.mod2dlc.util._
import java.io.File
import scala.xml.{Node, XML}
import java.util.UUID
import scala.collection.mutable
import com.lymiahugs.util.StreamUtils
import com.lymiahugs.mod2dlc.util.{DLCKey, ParseDatabaseUpdate}
import com.lymiahugs.mod2dlc.data.LuaFrag

object rewriteMod {
  def apply(modSource: File, target: File, languageFile: File)(implicit logger: FileOperationLogger) =
    logger.logException {
      import logger._

      // Create target directory if it does not alredy exist
      if(!target.exists) target.mkdirs()

      ////////////////
      // Load mod data
      val sourceFiles = listFiles(modSource)
      val modinfoFile = sourceFiles.find(_.getName.endsWith(".modinfo")).
        getOrElse(sys.error("No .modinfo found!"))
      val modinfo = XML.loadFile(modinfoFile)
      val version = (modinfo \ "@version").text.toInt
      val modName = (modinfo \ "Properties" \ "Name").text
      val uuid = UUID.fromString((modinfo \ "@id").text)
      val uuid_string = uuid.toString.filter(_!='-').toLowerCase

      // Read file list
      val files = modinfo \ "Files" \ "File"
      val importedFiles = files.filter(x => (x \ "@import").text == "1").map(x => x.text).toSet

      //////////////////////
      // Copy imported files
      for(file <- importedFiles)
        copy(new File(modSource, file), new File(target, file))

      /////////////////////
      // Track unused files
      var unusedFiles = new mutable.TreeSet[String]
      unusedFiles ++= (sourceFiles.map(_.getCanonicalPath).toSet --
        importedFiles.map(x => new File(modSource, x).getCanonicalPath).toSet -
        modinfoFile.getCanonicalPath)
      def markFileUsed(file: File) {
        unusedFiles -= file.getCanonicalPath
      }

      //////////////////////////
      // Parse and write Actions
      val actions = (modinfo \ "Actions").flatMap(_.child).filter(!_.label.startsWith("#"))
      var usesCvGameDatabasePatch = false
      val actionTags = (actions \ "OnModActivated" \ "UpdateDatabase").map { tag =>
        val fileName = tag.text.trim
        val file = new File(modSource, fileName)
        markFileUsed(file)

        val outputFileName = "_mod2dlc_noimport_"+uuid_string+file.getName()+
          (if(file.getName.endsWith(".xml")) "" else ".xml")
        val outputFile = new File(target, "XML/"+outputFileName)

        if(fileName.endsWith(".sql")) {
          generateXML(<GameData>
            <__MOD2DLC_PATCH_IGNORE>
              <!-- This triggers an error for players who have an unmodded CvGameDatabase file -->
              <Please_install_the_CvGameDatabase_patch_for_Mod2DLC>
              </Please_install_the_CvGameDatabase_patch_for_Mod2DLC>
            </__MOD2DLC_PATCH_IGNORE>
            <__MOD2DLC_PATCH_RAWSQL>{readFile(file)}</__MOD2DLC_PATCH_RAWSQL>
          </GameData>, outputFile, " SQL data from "+file.getCanonicalFile)
          usesCvGameDatabasePatch = true
        } else copy(file, outputFile)

        <GameData>{outputFileName}</GameData>
      }
      // TODO: Add support for UpdateUserData and SetDLLPath, and wtf ExecuteScript is

      /////////////////////
      // Parse entry points
      val entryPoints =
        (modinfo \ "EntryPoints" \ "EntryPoint") map { ep =>
          ((ep \ "Name").text, (ep \ "Description").text, (ep \ "@type").text, (ep \ "@file").text)
        } groupBy (_._3)
      val entryPointData = StreamUtils.writeToString { out =>
        for ((eventType, epList) <- entryPoints) {
          out.println("-- Entry Points for event " + eventType)
          out.println("entryPoints." + eventType + " = {")
          for (ep <- epList) {
            val file = new File(modSource, ep._4)
            val outPath = "Lua/_mod2dlc_" + uuid_string + "_entrypoint_" + file.getName
            val targetPath = new File(target, outPath)
            markFileUsed(file)
            copy(file, targetPath)
            out.printf("  {name=%s, description=%s, path=currentExecutingPath..%s},\n",
              quoteLuaString(ep._1), quoteLuaString(ep._2), quoteLuaString(outPath))
          }
          out.println("}")
        }
      }

      /////////////////////
      // Write mod manifest
      writeFileFromStream(new File(target, "UI/Mod2DLC/_mod2dlc_"+uuid_string+"_manifest.lua"),
        " mod manifest") { out =>
        out.println(LuaFrag.mod_datafile_header)
        out.println("--- BEGIN GENERATED CODE ---")

        out.println("local name = "+quoteLuaString(modName))
        out.println("local id = "+quoteLuaString(uuid.toString))
        out.println("local usesCvGameDatabasePatch = "+(if(usesCvGameDatabasePatch) "1" else "false"))
        out.println()

        out.println(entryPointData)

        out.println("--- END GENERATED CODE ---")
        out.println(LuaFrag.mod_datafile_footer)
      }

      //////////////////////////////
      // Extract and write text data
      def mergeMaps(data: Seq[Map[String, Seq[Node]]]) =
        data.reduce { (x, y) =>
          (x.keySet ++ y.keySet).map(key =>
            key -> (x.getOrElse(key, Seq()) ++ y.getOrElse(key, Seq()))
          ).toMap
        }
      generateXML(<GameData>
        {
          mergeMaps((modinfo \ "Actions" \\ "UpdateDatabase").map(_.text.trim).filter(_.endsWith(".xml")).
            map(x => {
              val file = new File(modSource, x)
              val xml = XML.loadFile(file)
              log("Searched for text data in "+file.getCanonicalPath+".")
              xml.child.filter(x => languageTableList.contains(x.label)).map(
                x => x.label -> x.child
              ).toMap
            })
          ).map( x =>
            <tag>{x._2}</tag>.copy(label = x._1)
          )
        }
      </GameData>, new File(target, "_mod2dlc_"+uuid_string+"_textdata.xml"), " string data")

      /////////////////////////
      // Generate DLC name file
      generateLanguageFile(modName, modName, uuid_string.toUpperCase, languageFile)

      /////////////////////////
      // Generate main manifest
      generateXML(<Civ5Package>
        <!-- TODO: Generate comments for authorship/etc here, so we don't lose credit and such for authors! -->
        <GUID>{"{"+uuid+"}"}</GUID>
        <SteamApp>99999</SteamApp>
        <Version>{version.toString}</Version>
        <Ownership>FREE</Ownership>
        <Priority>251</Priority>
        <PTags>
          <Tag>Version</Tag>
          <Tag>Ownership</Tag>
        </PTags>
        <Key>{DLCKey.key(uuid, Seq(99999), version, "FREE")}</Key>

        <Name>
          <Value language="en_US">{modName}</Value>
        </Name>
        <Description>
          <Value language="en_US">{modName}</Value>
        </Description>

        <Gameplay>
          <TextData>{"_mod2dlc_"+uuid_string+"_textdata.xml"}</TextData>
          {actionTags}
        </Gameplay>
      </Civ5Package>, new File(target, modinfoFile.getName.replace(".modinfo", "")+".Civ5Pkg"), "Civ5Pkg File")
    }
}
