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

package com.lymiahugs.civ5.mod2dlc

import java.io._
import scala.xml.XML
import java.util.UUID
import java.nio.file.Files
import com.lymiahugs.civ5.mod2dlc.data.LuaFrag
import com.lymiahugs.civ5.util.{ParseDatabaseUpdate, DLCKey}
import scala.collection.mutable

class RewriteCore(log_callback: String => Unit) {
  def listFiles(file: File): Seq[File] =
    if(file.isFile) Seq(file)
    else file.listFiles().flatMap(listFiles)

  def log(format: String, args: Any*) =
    log_callback(format.format(args: _*))

  def assurePresence(f: File) =
    if(!f.exists) {
      f.mkdirs()
      f.delete()
    }

  def readFile(file: File) =
    scala.io.Source.fromFile(file).mkString
  def writeFile(file: File, data: String, source: String = "") {
    log("Writing%s to %s", source, file.getCanonicalPath)
    assurePresence(file)
    val s = new PrintWriter(new FileWriter(file))
    s.print(data)
    s.close()
  }
  def writeToString(p: PrintWriter => Any) = {
    val out = new StringWriter()
    val print = new PrintWriter(out)
    p(print)
    out.getBuffer.toString
  }
  def writeFileFromStream(file: File, source: String = "")(p: PrintWriter => Any) {
    writeFile(file, writeToString(p), source)
  }
  def copy(source: File, target: File) = {
    log("Copying %s to %s", source.getCanonicalPath, target.getCanonicalPath)
    assurePresence(target)
    Files.copy(source.toPath, target.toPath)
  }

  def quoteLuaString(string: String) =
    "[["+string.replace("]", "]]..\"]\"..[[")+"]]"
  def translateMod(modSource: File, target: File, assetDir: File) =
    try {
      if(!target.exists) target.mkdirs()

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

      // Copy imported files
      for(file <- importedFiles)
        copy(new File(modSource, file), new File(target, file))

      // Write mod manifest
      var unusedFiles = new mutable.TreeSet[String]
      unusedFiles ++= (sourceFiles.map(_.getCanonicalPath).toSet --
        importedFiles.map(x => new File(modSource, x).getCanonicalPath).toSet -
        modinfoFile.getCanonicalPath)
      writeFileFromStream(new File(target, "UI/Mod2DLC/_mod2dlc_"+uuid_string+"_manifest.lua")) { out =>
        out.println(LuaFrag.mod_datafile_header)
        out.println("--- BEGIN GENERATED CODE ---")
        out.println("local name = "+quoteLuaString(modName))
        out.println("local id = "+quoteLuaString(uuid.toString))
        out.println()

        // Parse and write Actions
        for(action <- (modinfo \ "Actions").flatMap(_.child)) if(!action.label.startsWith("#")) {
          val actionName = action.label

          out.println("-- Actions for event "+actionName)
          out.println("actions."+actionName+" = {")
          for(act <- action.child) act.label match {
            case "UpdateDatabase" =>
              val fileName = act.text.trim
              val file = new File(modSource, fileName)
              val fileData =
                if(fileName.endsWith(".sql")) readFile(file)
                else writeToString(ParseDatabaseUpdate.parseDatabaseUpdate(XML.loadFile(file)))
              log(if(!fileName.endsWith(".sql")) "Translated database update %s to SQL in action %s."
                  else "Copied database update %s into action %s.",
                file.getCanonicalPath, actionName)
              out.printf("  {type=\"UpdateDatabase\", source=%s, data=\n-- Contents of file %s\n%s\n  },\n",
                quoteLuaString(fileName), fileName, quoteLuaString(fileData))
            case x if !x.startsWith("#") =>
              // unknown action
              log("Warning: Unknown action %s in trigger %s", act.label, actionName)
              out.printf("  {type=%s, data=%s},\n", quoteLuaString(act.label), quoteLuaString(act.text))
            case _ => // ignore
          }
          out.println("}")
          out.println()
        }

        out.println("--- END GENERATED CODE ---")
        out.println(LuaFrag.mod_datafile_footer)
      }

      // Parse entry points
      val entryPoints =
        (modinfo \ "EntryPoints" \ "EntryPoint") map { ep =>
          ((ep \ "Name").text, (ep \ "Description").text, (ep \ "@type").text, (ep \ "@file").text)
        }

      // Parse database updates
      // TODO Support UpdateUserData, ExecuteScript, SetDllPath

      // Write XML file
      def generated_civ5pkg =
        <Civ5Pkg>
          <UUID>{"{"+uuid+"}"}</UUID>
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
            <Directory>Mod2DLC</Directory>
          </Gameplay>
        </Civ5Pkg>
      writeFile(new File(target, modinfoFile.getName.replace(".modinfo", "")+".civ5pkg"),
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
        "<!-- Generated by Mod2Dlc by Lymia -->\n"+
        generated_civ5pkg.toString(), " mod description")
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        log("Error encountered: "+t.getClass.getCanonicalName+": "+t.getMessage)
        throw t
    }

  val mod2dlc_uuid = UUID.fromString("3d2df716-2c91-454f-8a5d-c21cfded78f8")
  def mod2dlcCore(target: File, assetDir: File) = try {
    if(!target.exists) target.mkdirs()

    def generated_civ5pkg =
      <Civ5Pkg>
        <UUID>{"{"+mod2dlc_uuid+"}"}</UUID>
        <SteamApp>99999</SteamApp>
        <Version>1</Version>
        <Ownership>FREE</Ownership>
        <Priority>250</Priority>
        <PTags>
          <Tag>Version</Tag>
          <Tag>Ownership</Tag>
        </PTags>
        <Key>{DLCKey.key(mod2dlc_uuid, Seq(99999), 1, "FREE")}</Key>

        <Name>
          <Value language="en_US">Mod2Dlc Core</Value>
        </Name>
        <Description>
          <Value language="en_US">Mod2Dlc Core</Value>
        </Description>
      </Civ5Pkg>

    writeFile(new File(target, "UI/FrontEnd/FrontEnd.lua"), LuaFrag.core_ui_frontend, " FrontEnd hook")
    writeFile(new File(target, "UI/FrontEnd/ContentSwitch.lua"), LuaFrag.core_ui_contentswitch, " ContentSwitch hook")
    writeFile(new File(target, "Gameplay/Lua/mod2dlc.lua"), LuaFrag.core_library, " core library")
    writeFile(new File(target, "Mod2Dlc.civ5pkg"),
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
      generated_civ5pkg.toString(), " DLC metadata")


  } catch {
    case t: Throwable =>
      t.printStackTrace()
      log("Error encountered: "+t.getClass.getCanonicalName+": "+t.getMessage)
      throw t
  }
}
