package org.duangsuse.bin

import org.duangsuse.bin.type.*
import kotlinx.io.*

//// Sized & Idx

inline val Sized.lastIndex: Long get() = size.dec()
// 注意：IdxRange 仍然是 IntRange，对于超大 Buffer 的切片可能需要特殊处理，
// 但在本项目上下文中，数组索引通常在 Int 范围内。

//// MarkReset & Closeable

inline fun <R> MarkReset.positional(op: Producer<R>): R
  = try { mark(); op() } finally { reset() }

inline fun <R> Closeable.use(op: Producer<R>): R
  = try { op() } finally { close() }

//// Nat8Reader / Nat8Writer 扩展

fun Nat8Reader.readTo(buffer: ByteArray) { readTo(buffer, buffer.indices) }
fun Nat8Writer.writeFrom(buffer: ByteArray) { writeFrom(buffer, buffer.indices) }

fun Nat8Reader.takeByte(n: Cnt): ByteArray {
  return source.readByteArray(n.toInt())
}

fun Nat8Reader.takeNat8(n: Cnt): IntArray {
  val buffer = IntArray(n.toInt())
  for (i in 0 until n.toInt()) {
    val b = read()
    if (b == -1) throw StreamEnd()
    buffer[i] = b
  }
  return buffer
}

//// 对齐逻辑

fun ReadControl.makeAligned(n: Cnt) {
  val chunkPosition = (position % n)
  if (chunkPosition != 0L) skip(n - chunkPosition)
}

fun Writer.makeAligned(n: Cnt) {
  val chunkPosition = (count % n)
  if (chunkPosition != 0L) asNat8Writer().writePadding(n - chunkPosition)
}

fun Nat8Writer.writePadding(n: Cnt, x: Byte = 0x00) {
  val padding = ByteArray(n.toInt()) { x }
  sink.write(padding)
}

fun ByteOrdered.makeBigEndian() { byteOrder = ByteOrder.BigEndian }
fun ByteOrdered.makeLittleEndian() { byteOrder = ByteOrder.LittleEndian }

//// 位运算与集合转换

fun Int8.uExt(): Int32 = this.toInt() and 0xFF
fun Int16.uExt(): Int32 = this.toInt() and 0xFFFF
fun Int32.uExt(): Int64 = this.toLong() and 0xFFFF_FFFFL

internal fun Int.bitUnion(other: Int): Int = this or other
internal fun Int.bitSubtract(mask: Int): Int = this and mask.inv()

fun Iterable<Nat8>.toArray(n: Cnt): IntArray {
  val buffer = IntArray(n.toInt())
  for ((i, b) in this.withIndex()) {
    if (i >= n.toInt()) break
    buffer[i] = b
  }
  return buffer
}

internal fun <E> MutableList<E>.removeLast(): E = removeAt(lastIndex)
internal fun <K, V> Map<K, V>.reverseMap(): Map<V, K> = entries.associate { it.value to it.key }
internal fun <K, V> Iterable<V>.mapBy(key: (V) -> K): Map<K, V> = associateBy(key)

internal inline fun <reified T> Collection<*>.takeIfAllIsInstance(): List<T>?
  = if (all { it is T }) filterIsInstance<T>() else null

internal fun <T, R: Any> Collection<T>.mapTakeIfAllNotNull(op: (T) -> R?): List<R>? {
  val result = mapNotNull(op)
  return if (result.size == this.size) result else null
}

internal fun impossible(): Nothing = error("impossible")