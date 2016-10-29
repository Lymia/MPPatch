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

import java.io.{DataInputStream, InputStream}
import java.nio.file.Path
import java.util.regex.Pattern
import java.util.{Locale, UUID}

import moe.lymia.mppatch.util.{IOUtils, VersionInfo}
import moe.lymia.mppatch.util.XMLUtils._
import moe.lymia.mppatch.util.common.{Crypto, IOWrappers, PatchPackage}

import scala.xml.{Node, XML}

trait PatchFileSource {
  def loadResource(name: String): String
  def loadBinary  (name: String): Array[Byte]
}

case class AdditionalFile(filename: String, source: String, isExecutable: Boolean)
case class InstallScript(replacementTarget: String, renameTo: String, patchTarget: String,
                         additionalFiles: Seq[AdditionalFile], leftoverFilter: Seq[String]) {
  lazy val leftoverRegex = leftoverFilter.map(x => Pattern.compile(x))
  def isLeftoverFile(x: String) = leftoverRegex.exists(_.matcher(x).matches())
}
object InstallScript {
  def loadFilename(node: Node) = getAttribute(node, "Filename")
  def loadAdditionalFile(xml: Node) =
    AdditionalFile(loadFilename(xml), getAttribute(xml, "Source"), getBoolAttribute(xml, "SetExecutable"))
  def loadFromXML(xml: Node) =
    InstallScript(loadFilename((xml \ "ReplacementTarget").head),
                  loadFilename((xml \ "RenameTo"         ).head),
                  loadFilename((xml \ "InstallBinary"    ).head),
                  (xml \ "AdditionalFile").map(loadAdditionalFile),
                  (xml \ "LeftoverFilter").map(x => getAttribute(x, "Regex")))
}

case class LuaOverride(fileName: String, includes: Seq[String],
                       injectBefore: Seq[String] = Seq(), injectAfter: Seq[String] = Seq())
case class NativePatch(platform: String, version: String, path: String)
case class PatchManifest(dlcManifest: DLCManifest, patchVersion: String, timestamp: Long,
                         luaPatches: Seq[LuaOverride], libraryFileNames: Seq[String],
                         newScreenFileNames: Seq[String], textFileNames: Seq[String],
                         nativePatches: Seq[NativePatch], installScripts: Map[String, String])
object PatchManifest {
  def readDLCManifest(node: Node) =
    DLCManifest(UUID.fromString(getAttribute(node, "UUID")),
                getAttribute(node, "Version").toInt, getAttribute(node, "Priority").toInt,
                getAttribute(node, "ShortName"), getAttribute(node, "Name"))
  def loadFilename(node: Node) = getAttribute(node, "Filename")
  def loadLuaOverride(node: Node) =
    LuaOverride(loadFilename(node), (node \ "Include").map(loadFilename),
                (node \ "InjectBefore").map(loadFilename), (node \ "InjectAfter").map(loadFilename))
  def loadNativePatch(node: Node) =
    NativePatch(getAttribute(node, "Platform"), getAttribute(node, "Version"), getAttribute(node, "Filename"))
  def loadInstallScript(node: Node) =
    getAttribute(node, "Platform") -> loadFilename(node)
  def loadFromXML(xml: Node) = {
    val manifestVersion = getAttribute(xml, "ManifestVersion")
    if(manifestVersion != "0") sys.error("Unknown ManifestVersion: "+manifestVersion)
    PatchManifest(readDLCManifest((xml \ "Info").head),
                                  getAttribute(xml, "PatchVersion"), getAttribute(xml, "Timestamp").toLong,
                                  (xml \ "Hook"         ).map(loadLuaOverride),
                                  (xml \ "Include"      ).map(loadFilename),
                                  (xml \ "Screen"       ).map(loadFilename),
                                  (xml \ "TextData"     ).map(loadFilename),
                                  (xml \ "Version"      ).map(loadNativePatch),
                                  (xml \ "InstallScript").map(loadInstallScript).toMap)
  }
}

class PatchLoader(val source: PatchFileSource) {
  val data = PatchManifest.loadFromXML(XML.loadString(source.loadResource("manifest.xml")))

  lazy val luaPatchList = data.luaPatches.map(x => x.fileName.toLowerCase -> x).toMap
  lazy val libraryFiles = data.libraryFileNames.map(x =>
    x -> source.loadResource(s"$x")
  ).toMap
  val newScreenFiles = data.newScreenFileNames.flatMap(x => Seq(
    s"$x.lua" -> source.loadResource(s"$x.lua"),
    s"$x.xml" -> source.loadResource(s"$x.xml")
  )).toMap
  val textFiles = data.textFileNames.map(x =>
    x -> XML.loadString(source.loadResource(x))
  ).toMap

  private def loadWrapper(str: Seq[String]) =
    if(str.isEmpty) "" else {
      "--- BEGIN INJECTED MPPATCH CODE ---\n\n"+
      str.mkString("\n")+
      "\n--- END INJECTED MPPATCH CODE ---"
    }
  private def getLuaFragment(path: String) = {
    val code = source.loadResource(path)
    "-- source file: "+path+"\n\n"+
    code+(if(!code.endsWith("\n")) "\n" else "")
  }
  def patchFile(path: Path) = {
    val fileName = path.getFileName.toString
    luaPatchList.get(fileName.toLowerCase(Locale.ENGLISH)) match {
      case Some(patch) =>
        val runtime      = s"${patch.includes.map(x => s"include [[$x]]").mkString("\n")}\n"
        val injectBefore = runtime +: patch.injectBefore.map(getLuaFragment)
        val contents     = IOUtils.readFileAsString(path)
        val injectAfter  = patch.injectAfter.map(getLuaFragment)
        val finalFile    = loadWrapper(injectBefore)+"\n\n"+contents+"\n\n"+loadWrapper(injectAfter)
        Some(finalFile)
      case None =>
        None
    }
  }

  def loadInstallScript(name: String) =
    data.installScripts.get(name).map(x => InstallScript.loadFromXML(XML.loadString(source.loadResource(x))))

  val versionMap = data.nativePatches.map(x => (x.platform, x.version) -> x).toMap
  def getNativePatch(targetPlatform: String, versionName: String) =
    versionMap.get((targetPlatform, versionName))
  def nativePatchExists(targetPlatform: String, versionName: String) =
    versionMap.contains((targetPlatform, versionName))
  def loadVersion(patch: NativePatch) = source.loadBinary(patch.path)
}

case class PatchPackageLoader(data: PatchPackage) extends PatchFileSource {
  override def loadResource(name: String): String = data.loadResource(name)
  override def loadBinary(name: String): Array[Byte] = data.loadBinaryResource(name)
}
object PatchPackageLoader {
  def apply(in: InputStream): PatchPackageLoader =
    PatchPackageLoader(IOWrappers.readPatchPackage(in match {
      case in: DataInputStream => in
      case _ => new DataInputStream(in)
    }))
  def apply(resource: String): PatchPackageLoader = PatchPackageLoader(IOUtils.getResource(resource))
}