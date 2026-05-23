package org.duangsuse.bin.pat

import org.duangsuse.bin.Sized
import org.duangsuse.bin.type.Cnt
import org.duangsuse.bin.type.Idx
import kotlin.reflect.KProperty

abstract class Tuple<E>(override val size: Cnt): Sized {
  protected abstract val items: Array<E>
  operator fun get(index: Idx) = items[index]
  operator fun set(index: Idx, value: E) { items[index] = value }
  fun toArray(): Array<E> = items

  protected fun <T> index(idx: Idx) = Index<T>(idx)
  protected class Index<T>(private val idx: Idx) {
    operator fun getValue(self: Tuple<out T>, _p: KProperty<*>): T = self[idx]
    operator fun setValue(self: Tuple<in T>, _p: KProperty<*>, value: T) { self[idx] = value }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    return if (other == null || other !is Tuple<*>) false
    else (size == other.size) && items.contentEquals(other.items)
  }
  override fun hashCode(): Int  = 31 * size.toInt() + items.contentHashCode()
  override fun toString(): String = "(${items.joinToString(", ")})"
}