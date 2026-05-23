package org.duangsuse.bin.io

import kotlinx.io.*
import org.duangsuse.bin.*
import org.duangsuse.bin.type.*

open class Reader(private val r: Nat8Reader): org.duangsuse.bin.Reader {
  override var byteOrder: ByteOrder = nativeOrder
  override val position get() = mPosition
  private var mPosition: Long = 0
  private val mPositionStack: MutableList<Long> = mutableListOf()

  private val s: Source get() = r.source

  override fun readNat8(): Nat8 = r.read().also { if (it != -1) ++mPosition }
  override fun readInt8(): Int8 = s.readByte().also { ++mPosition }

  override fun readInt16(): Int16 {
    val res = if (shouldSwap) s.readShortLe() else s.readShort()
    mPosition += 2
    return res
  }

  override fun readInt32(): Int32 {
    val res = if (shouldSwap) s.readIntLe() else s.readInt()
    mPosition += 4
    return res
  }

  override fun readInt64(): Int64 {
    val res = if (shouldSwap) s.readLongLe() else s.readLong()
    mPosition += 8
    return res
  }

  override fun readRat32(): Rat32 = Float.fromBits(readInt32())
  override fun readRat64(): Rat64 = Double.fromBits(readInt64())

  private val shouldSwap: Boolean get() = byteOrder == ByteOrder.LittleEndian

  override val estimate: Long get() = r.estimate
  override fun skip(n: Long) {
    s.skip(n)
    mPosition += n
  }

  override fun mark() {
    // 注意：kotlinx.io 0.8.0 的 Source 并不直接支持 mark/reset
    // 这里假设底层 Nat8Reader 实现了某种形式的缓存或状态保存
    if (r is MarkReset) r.mark()
    mPositionStack.add(mPosition)
  }

  override fun reset() {
    if (r is MarkReset) r.reset()
    mPosition = mPositionStack.removeAt(mPositionStack.size - 1)
  }

  override fun close() = r.close()

  private inner class AsNat8Reader : Nat8Reader {
    override val source: Source get() = r.source
    override val estimate: Long get() = r.estimate
    override fun read(): Nat8 = this@Reader.readNat8()
    override fun readTo(buffer: ByteArray, indices: IdxRange) {
      s.readTo(buffer, indices.first, indices.last + 1)
      mPosition += (indices.last - indices.first + 1)
    }
    override fun skip(n: Long) = this@Reader.skip(n)
    override fun close() = this@Reader.close()
  }

  private val nat8Reader by lazy(::AsNat8Reader)
  override fun asNat8Reader(): Nat8Reader = nat8Reader
}