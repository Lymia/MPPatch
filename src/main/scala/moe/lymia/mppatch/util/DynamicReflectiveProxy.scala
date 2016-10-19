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

package moe.lymia.mppatch.util

import java.lang.reflect.{Array => _, _}

import scala.annotation.tailrec
import scala.language.dynamics
import scala.reflect._

object DynamicReflectiveProxy {
  def apply(x: Any): DynamicReflectiveProxy = x match {
    case x: Array[AnyRef] => new DynamicReflectiveArrayProxy(x.asInstanceOf[Array[Any]])
    case null             => null
    case x: Any           => new DynamicReflectiveProxy(x.asInstanceOf[AnyRef])
  }

  def static[T : ClassTag]: DynamicReflectiveProxy =
    new DynamicStaticReflectiveProxy(implicitly[ClassTag[T]].runtimeClass)
  def static(name: String): DynamicReflectiveProxy =
    new DynamicStaticReflectiveProxy(getClass.getClassLoader.loadClass(name))
  def static(clazz: Class[_]): DynamicReflectiveProxy =
    new DynamicStaticReflectiveProxy(clazz)
}

class DynamicReflectiveProxy(obj: AnyRef) extends Dynamic {
  private lazy val _clazz: Class[_] = obj.getClass
  protected def clazz = _clazz

  @tailrec
  private def resolveDeclaredField(name: String, current: Class[_] = clazz): Option[Field] =
    current.getDeclaredField(name) match {
      case null =>
        if(current != classOf[AnyRef]) resolveDeclaredField(name, current.getSuperclass)
        else None
      case x    => Some(x)
    }

  private def wraps(clazz: Class[_]) = clazz match {
    case m if m == classOf[java.lang.Boolean  ] => java.lang.Boolean  .TYPE
    case m if m == classOf[java.lang.Byte     ] => java.lang.Byte     .TYPE
    case m if m == classOf[java.lang.Character] => java.lang.Character.TYPE
    case m if m == classOf[java.lang.Short    ] => java.lang.Short    .TYPE
    case m if m == classOf[java.lang.Integer  ] => java.lang.Integer  .TYPE
    case m if m == classOf[java.lang.Long     ] => java.lang.Long     .TYPE
    case m if m == classOf[java.lang.Float    ] => java.lang.Float    .TYPE
    case m if m == classOf[java.lang.Double   ] => java.lang.Double   .TYPE
    case _ => null
  }
  @tailrec
  private def resolveDeclaredMethod(name: String, types: Seq[Class[_]], current: Class[_] = clazz): Option[Method] =
    current.getDeclaredMethods.find(method =>
      method.getName == name &&
      (method.getParameterTypes.length == types.length) && (
        types.isEmpty ||
        (method.getParameterTypes zip types).map(x =>
          x._1.isAssignableFrom(x._2) || x._1 == wraps(x._2)).reduce(_ && _)
      )
    ) match {
      case None =>
        if(current != classOf[AnyRef]) resolveDeclaredMethod(name, types, current.getSuperclass)
        else None
      case Some(x) => Some(x)
    }

  def applyDynamic(name: String)(params: Any*) =
    resolveDeclaredMethod(name, params.map(_.getClass)) match {
      case Some(x) =>
        x.setAccessible(true)
        DynamicReflectiveProxy(x.invoke(obj, params.map(_.asInstanceOf[AnyRef]) : _*))
      case None =>
        if(params.length == 1) {
          resolveDeclaredField(name) match {
            case Some(x) =>
              x.setAccessible(true)
              DynamicReflectiveProxy(x.get(obj))(params.head.asInstanceOf[Int])
            case None =>
              throw new NoSuchMethodError(s"Non-existent method $name in ${clazz.getName} was accessed.")
          }
        } else throw new NoSuchMethodError(s"Non-existent method $name in ${clazz.getName} was accessed.")
    }
  def selectDynamic(name: String) =
    resolveDeclaredField(name) match {
      case Some(x) =>
        x.setAccessible(true)
        DynamicReflectiveProxy(x.get(obj))
      case None => resolveDeclaredMethod(name, Seq()) match {
        case Some(x) =>
          x.setAccessible(true)
          DynamicReflectiveProxy(x.invoke(obj))
        case None =>
          throw new NoSuchFieldError(s"Non-existent field $name in ${clazz.getName} was accessed.")
      }
    }
  def updateDynamic(name: String)(v: Any) =
    resolveDeclaredField(name) match {
      case Some(x) =>
        x.setAccessible(true)
        x.set(obj, v)
      case None =>
        throw new NoSuchFieldError(s"Non-existent field $name in ${clazz.getName} was accessed.")
    }
  def applyDynamicNamed(name: String)(params: (String, Any)*) =
    sys.error("DynamicReflectiveProxy does not support named parameter calls")

  def length = selectDynamic("length")
  def update(i: Int, b: Any) { throw new ClassCastException("Array access on non-array class") }
  def apply(i: Int): DynamicReflectiveProxy = throw new ClassCastException("Array access on non-array class")

  def get[T] = obj.asInstanceOf[T]
}
class DynamicStaticReflectiveProxy(obj: Class[_]) extends DynamicReflectiveProxy(null) {
  override protected def clazz = obj
}
class DynamicReflectiveArrayProxy(arr: Array[Any]) extends DynamicReflectiveProxy(arr) {
  override def length = DynamicReflectiveProxy(arr.length)
  override def update(i: Int, b: Any) {
    arr(i) = b
  }
  override def apply(i: Int) = DynamicReflectiveProxy(arr(i))
}