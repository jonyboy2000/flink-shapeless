/*
 * Copyright 2017 Georgi Krastev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink
package api.scala.derived.typeutils

import api.common.ExecutionConfig
import api.common.typeinfo.TypeInformation
import api.common.typeutils.TypeSerializer

import scala.reflect.ClassTag

/** `TypeInformation` for recursive product types (case classes). */
class ProductTypeInfo[P](fs: => Seq[TypeInformation[_]])
    (from: Seq[Any] => P, to: P => Seq[Any])(implicit tag: ClassTag[P])
    extends TypeInformation[P] with InductiveObject {

  private lazy val fields = fs
  @transient private var serializer: ProductSerializer[P] = _

  def isBasicType: Boolean = false
  def isTupleType: Boolean = false
  def isKeyType: Boolean = false
  def getArity: Int = fields.size
  def getTotalFields: Int = getArity

  def getTypeClass: Class[P] =
    tag.runtimeClass.asInstanceOf[Class[P]]

  // Handle cycles in the object graph.
  def createSerializer(config: ExecutionConfig): TypeSerializer[P] =
    inductive(serializer) {
      serializer = ProductSerializer()(from, to)
      serializer.fields = for (f <- fields)
        yield f.createSerializer(config).asInstanceOf[TypeSerializer[Any]]
      serializer
    }

  def canEqual(that: Any): Boolean =
    that.isInstanceOf[ProductTypeInfo[_]]

  override def equals(other: Any): Boolean = other match {
    case that: ProductTypeInfo[_] =>
      (this eq that) || (that canEqual this) && this.fields == that.fields
    case _ => false
  }

  override def hashCode: Int =
    inductive(0)(31 * fields.##)

  override def toString: String = inductive("this") {
    s"${getTypeClass.getTypeName}(${fields.mkString(", ")})"
  }
}
