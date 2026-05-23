package org.duangsuse.bin.io

import kotlinx.io.*
import org.duangsuse.bin.Nat8Writer
import org.duangsuse.bin.type.Cnt
import org.duangsuse.bin.type.IdxRange

class BufferWriter(n: Cnt = 0) : Writer(
  object : Nat8Writer {
    override val sink = Buffer()
    override fun write(x: Int) { sink.writeByte(x.toByte()) }
    override fun writeFrom(buffer: ByteArray, indices: IdxRange) {
      sink.write(buffer, indices.first, indices.last + 1)
    }
    override fun close() { sink.close() }
  }
) {
  private val bufferSink: Buffer get() = (asNat8Writer().sink as Buffer)

  fun byteArray(): ByteArray {
    return bufferSink.peek().readByteArray()
  }

  fun clear() {
    bufferSink.clear()
  }
}