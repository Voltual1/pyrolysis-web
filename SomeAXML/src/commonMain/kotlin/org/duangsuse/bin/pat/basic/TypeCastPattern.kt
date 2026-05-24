package org.duangsuse.bin.pat.basic

import org.duangsuse.bin.*
import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.Tuple
import org.duangsuse.bin.type.*
import kotlin.reflect.KProperty

open class AnyTuple(size: Cnt)
  : Tuple<Any>(size) {
  @Suppress("UNCHECKED_CAST")
  final override val items: Array<Any> = arrayOfNulls<Any>(size.toInt()) as Array<Any>
}

open class AnySeq<TUP: Tuple<Any>>(creator: Producer<TUP>, vararg items: Pattern<Any>): Seq<TUP, Any>(creator, *items)
open class AnyRepeat(sizer: Pattern<Cnt>, item: Pattern<Any>): Repeat<Any>(sizer, item)
open class AnyCond(flag: Pattern<Idx>, vararg conditions: Case<Idx, Any>): Cond<Idx, Any>(flag, *conditions)
open class CondByIdx<T>(flag: Pattern<Idx>, vararg conditions: Case<Idx, T>): Cond<Idx, T>(flag, *conditions)

class CastIndex<T>(private val idx: Idx) {
  @Suppress("UNCHECKED_CAST")
  operator fun getValue(self: Tuple<*>, _p: KProperty<*>): T = self[idx] as T
  operator fun setValue(self: Tuple<Any>, _p: KProperty<*>, value: T) { self[idx] = value as Any }
}

inline operator fun <reified T: Any> Pattern<T>.unaryPlus() = object: Pattern.BySized<Any>(this) {
  override fun read(s: Reader): Any = this@unaryPlus.read(s)
  override fun write(s: Writer, x: Any) = this@unaryPlus.write(s, x as T)
  override fun writeSize(x: Any): Cnt = this@unaryPlus.writeSize(x as T)
}