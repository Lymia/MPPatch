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

import scala.xml._
import java.io.PrintWriter

object ParseDatabaseUpdate {
  // TODO: Figure out if XMLSerializer uses type information to decide whether to do this or not
  // TODO: Use prepared statements instead of this junk
  def parseValues(table: String, row: String, data: String) =
    if(data.matches("-?[0-9]+(\\.[0-9]+)?")) data
    else if(data.equalsIgnoreCase("true")) "1"
    else if(data.equalsIgnoreCase("false")) "0"
    else "'"+data.replace("'", "''")+"'"

  def parseDatabaseNode(n: Node) =
    n.child.filter(!_.label.startsWith("#")).map(x => x.label -> x.text.trim) ++
    n.attributes.map(x => x.key -> x.value.text.trim)
  def parseIntoWhere(table: String, n: Node) = {
    val data = parseDatabaseNode(n)
    "where "+data.map(x => x._1+" = "+parseValues(table, x._1, x._2)).reduce(_ + " and " + _)
  }
  def parseIntoSet(table: String, n: Node) = {
    val data = parseDatabaseNode(n)
    "set "+data.map(x => x._1+" = "+parseValues(table, x._1, x._2)).reduce(_ + ", " + _)
  }
  def parseDatabaseUpdate(data: Elem)(out: PrintWriter) = {
    for(instruction <- data.child) {
      instruction.label match {
        case "Table" =>
          // TODO: Implement
          throw new NotImplementedError("Not yet implemented")
        case "Index" =>
          // TODO: Reverse engineer format
          throw new NotImplementedError("Not yet implemented")
        case "DeleteMissingReferences" =>
          // TODO: Figure out WTF this is
          throw new NotImplementedError("wtf is this")
        case table if !table.startsWith("#") =>
          for(action <- instruction.child) action.label match {
            case "Row" | "Replace" | "InsertOrIgnore" | "InsertOrAbort" =>
              out.print(action.label match {
                case "Row" => "insert into "
                case "Replace" => "insert or replace into "
                case "InsertOrIgnore" => "insert or ignore into "
                case "InsertOrAbort" => "insert or abort into "
              })
              out.print(table+" ")

              val data = parseDatabaseNode(action)
              out.print("("+data.map(_._1).reduce(_ + ", " + _)+") ")
              out.print("values ("+data.map(x => parseValues(table, x._1, x._2)).reduce(_ + ", " + _)+");")
              out.println()
            case "Update" =>
              out.print("update "+table+" ")
              out.print(parseIntoWhere(table, action \ "Where" head)+" ")
              out.print(parseIntoSet  (table, action \ "Set" head))
              out.println(";")
            case "Delete" =>
              out.print("delete from "+table+" ")
              out.print(parseIntoWhere(table, action \ "Where" head))
              out.println(";")
            case x => // ignore
          }
        case _ => // ignore
      }
    }
    out.flush()
  }
}
