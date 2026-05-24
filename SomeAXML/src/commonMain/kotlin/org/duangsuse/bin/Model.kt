package org.duangsuse.bin

import org.duangsuse.bin.type.*
import kotlinx.io.*

/** Something like stream with [estimate] and [skip] subtracts it */
interface Estimable {
  val estimate: Long; fun skip(n: Long)
}

interface MarkReset { fun mark(); fun reset() }

// 定义基础 Closeable 接口，避免 kotlinx.io 版本差异导致的引用问题
interface Closeable { fun close() }
interface Flushable { fun flush() }

class StreamEnd: Exception("unexpected EOF")

//// Low-level IO stream defined using kotlinx.io
interface Nat8Reader: Estimable, Closeable {
  val source: Source
  fun read(): Nat8 = try { source.readByte().toInt() and 0xFF } catch (e: EOFException) { -1 }
  fun readTo(buffer: ByteArray, indices: IdxRange) {
    source.readTo(buffer, indices.first, indices.last + 1)
  }
}

interface Nat8Writer: Closeable, Flushable {
  val sink: Sink
  fun write(x: Nat8) { sink.writeByte(x.toByte()) }
  fun writeFrom(buffer: ByteArray, indices: IdxRange) {
    sink.write(buffer, indices.first, indices.last + 1)
  }
  override fun flush() { sink.flush() }
}

//// High-level data IO stream
interface Reader: ReadControl, DataReader {
  fun asNat8Reader(): Nat8Reader
}
interface Writer: WriteControl, DataWriter {
  fun asNat8Writer(): Nat8Writer
}

interface ReadControl: Estimable, MarkReset, Closeable {
  val position: Long
}
interface WriteControl: Flushable, Closeable {
  val count: Long
}

//// ByteOrder and data IO interface
enum class ByteOrder { BigEndian, LittleEndian }
interface ByteOrdered { var byteOrder: ByteOrder }

val nativeOrder: ByteOrder = ByteOrder.BigEndian
val LANGUAGE_ORDER = ByteOrder.BigEndian

interface DataReader: ByteOrdered {
  fun readNat8(): Nat8
  fun readInt8(): Int8
  fun readInt16(): Int16
  fun readInt32(): Int32
  fun readInt64(): Int64
  fun readRat32(): Rat32
  fun readRat64(): Rat64
}

interface DataWriter: ByteOrdered {
  fun writeNat8(x: Nat8)
  fun writeInt8(x: Int8)
  fun writeInt16(x: Int16)
  fun writeInt32(x: Int32)
  fun writeInt64(x: Int64)
  fun writeRat32(x: Rat32)
  fun writeRat64(x: Rat64)
}

//// Sizing Types
interface OptionalSized {
  val size: Long?
}
interface Sized: OptionalSized {
  override val size: Long
}