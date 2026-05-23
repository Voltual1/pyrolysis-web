package org.duangsuse.bin.io

import kotlinx.io.*
import org.duangsuse.bin.*
import org.duangsuse.bin.type.*

open class Writer(private val w: Nat8Writer): org.duangsuse.bin.Writer {
  override var byteOrder: ByteOrder = LANGUAGE_ORDER
  override val count get() = mCount
  private var mCount: Long = 0

  private val s: Sink get() = w.sink

  override fun writeNat8(x: Nat8) {
    s.writeByte(x.toByte())
    mCount++
  }

  override fun writeInt8(x: Int8) {
    s.writeByte(x)
    mCount++
  }

  override fun writeInt16(x: Int16) {
    if (shouldSwap) s.writeShortLe(x) else s.writeShort(x)
    mCount += 2
  }

  override fun writeInt32(x: Int32) {
    if (shouldSwap) s.writeIntLe(x) else s.writeInt(x)
    mCount += 4
  }

  override fun writeInt64(x: Int64) {
    if (shouldSwap) s.writeLongLe(x) else s.writeLong(x)
    mCount += 8
  }

  override fun writeRat32(x: Rat32) = writeInt32(x.toBits())
  override fun writeRat64(x: Rat64) = writeInt64(x.toBits())

  private val shouldSwap: Boolean get() = byteOrder == ByteOrder.LittleEndian

  override fun close() = w.close()
  override fun flush() = w.flush()

  private inner class AsNat8Writer : Nat8Writer {
    override val sink: Sink get() = w.sink
    override fun write(x: Nat8) = this@Writer.writeNat8(x)
    override fun writeFrom(buffer: ByteArray, indices: IdxRange) {
      s.write(buffer, indices.first, indices.last + 1)
      mCount += (indices.last - indices.first + 1)
    }
    override fun close() = this@Writer.close()
  }

  private val nat8Writer by lazy(::AsNat8Writer)
  override fun asNat8Writer(): Nat8Writer = nat8Writer
}