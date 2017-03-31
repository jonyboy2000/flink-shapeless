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
package api.scala.typeutils

import api.common.typeutils.TypeSerializer
import core.memory.DataInputView
import core.memory.DataOutputView

/** A [[TypeSerializer]] for recursive coproduct types (sealed traits). */
case class CoproductSerializer[T](var variants: Seq[TypeSerializer[T]] = Seq.empty)
    (which: T => Int) extends TypeSerializer[T] with InductiveObject {

  def getLength = -1

  def isImmutableType = inductive(true) {
    variants.forall(_.isImmutableType)
  }

  def duplicate = inductive(this) {
    val serializer = CoproductSerializer()(which)
    serializer.variants = for (v <- variants) yield v.duplicate
    serializer
  }

  def createInstance =
    variants.head.createInstance

  def copy(record: T, reuse: T) =
    copy(record)

  def copy(record: T) =
    variants(which(record)).copy(record)

  def copy(source: DataInputView, target: DataOutputView) = {
    val i = source.readInt()
    target.writeInt(i)
    variants(i).copy(source, target)
  }

  def serialize(record: T, target: DataOutputView) = {
    val i = which(record)
    target.writeInt(i)
    variants(i).serialize(record, target)
  }

  def deserialize(reuse: T, source: DataInputView) =
    deserialize(source)

  def deserialize(source: DataInputView) =
    variants(source.readInt()).deserialize(source)

  override def hashCode =
    inductive(0)(31 * variants.##)

  override def toString = inductive("this") {
    s"CoproductSerializer(${variants.mkString(", ")})"
  }
}
