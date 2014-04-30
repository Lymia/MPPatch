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

package com.lymiahugs.mod2dlc

import javax.swing._

object Mod2DLC {
  def main(args: Array[String]) {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
    data.Pathes.ensurePathsExist()
    if(!data.Pathes.ensureSubdirectories()) {
      JOptionPane.showMessageDialog(null,
        """
          |An expected subdirectory in either the game data directory or the user directory was not found.
          |If you selected paths manually, please check your paths.
          |Otherwise, try running the game first, if you haven't already.
        """.stripMargin, "Mod2DLC", JOptionPane.ERROR_MESSAGE)
      sys.error("subdirectory check failed")
    }
  }
}
