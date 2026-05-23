package org.duangsuse.bin.pat.extra

import org.duangsuse.bin.*
import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.type.Cnt

object Keep: Pattern<ByteArray> {
  override fun read(s: Reader): ByteArray = s.asNat8Reader().takeByte(s.estimate)
  override fun write(s: Writer, x: ByteArray): Unit = s.asNat8Writer().writeFrom(x)
  override fun writeSize(x: ByteArray): Cnt = x.size.toLong()
}