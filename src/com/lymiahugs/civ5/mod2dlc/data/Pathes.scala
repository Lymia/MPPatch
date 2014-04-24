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

package com.lymiahugs.civ5.mod2dlc.data

import java.io._

object Pathes {
  val isWindows = System.getProperty("os.name").toLowerCase.contains("windows")

  var gamePath: Option[File] = if(isWindows) {
    import com.lymiahugs.util.WindowsRegistry._
    HKEY_CURRENT_USER("Software\\Firaxis\\Civilization5", "LastKnownPath").map(x => new File(x))
  } else None
  def ensureGamePath() =
    if(gamePath.isEmpty || !gamePath.get.exists) {
      import javax.swing._

      JOptionPane.showMessageDialog(null,
        """
          |Could not determine Civilization V game directory. Please manually select path.
          |(Example: C:\Program Files (x86)\Steam\steamapps\common\Sid Meier's Civilization V)
        """.stripMargin, "Mod2DLC", JOptionPane.ERROR_MESSAGE)
      val fileChooser = new JFileChooser
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
      if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        gamePath = Some(fileChooser.getSelectedFile)
        true
      } else {
        JOptionPane.showMessageDialog(null, "Startup aborted.", "Mod2DLC", JOptionPane.ERROR_MESSAGE)
        false
      }
    } else true

  var userPath: Option[File] = if(isWindows) {
    import com.lymiahugs.util.WindowsRegistry._
    HKEY_CURRENT_USER("Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders",
      "Personal").map(x => new File(x))
  } else None
  def ensureUserPath() =
    if(userPath.isEmpty || !userPath.get.exists) {
      import javax.swing._

      JOptionPane.showMessageDialog(null,
        """
          |Could not determine Civilization V user directory. Please manually select path.
          |(Example: C:\Users\__USER__\My Documents\My Games\Sid Meier's Civilization 5)
        """.stripMargin.replace("__USER__", System.getProperty("user.name")), "Mod2DLC", JOptionPane.ERROR_MESSAGE)
      val fileChooser = new JFileChooser
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
      if(fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        userPath = Some(fileChooser.getSelectedFile)
        true
      } else {
        JOptionPane.showMessageDialog(null, "Startup aborted.", "Mod2DLC", JOptionPane.ERROR_MESSAGE)
        false
      }
    } else true

  def ensurePathsExist() = ensureGamePath() && ensureUserPath()

  //////////////////////////////
  // Important subdirectories //
  //////////////////////////////

  def ensureSubdirectories() = true
}
