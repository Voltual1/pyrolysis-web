package org.duangsuse.bin.pat.atom

import org.duangsuse.bin.Reader
import org.duangsuse.bin.Writer
import org.duangsuse.bin.pat.Pattern.StaticallySized
import org.duangsuse.bin.type.*
import org.duangsuse.bin.uExt

val nat8 = object: StaticallySized<Nat8> {
  override fun read(s: Reader): Nat8 = s.readNat8()
  override fun write(s: Writer, x: Nat8): Unit = s.writeNat8(x)
  override val size: Cnt = 1L
}
val int8 = object: StaticallySized<Int8> {
  override fun read(s: Reader): Int8 = s.readInt8()
  override fun write(s: Writer, x: Int8): Unit = s.writeInt8(x)
  override val size: Cnt = 1L
}
val int16 = object: StaticallySized<Int16> {
  override fun read(s: Reader): Int16 = s.readInt16()
  override fun write(s: Writer, x: Int16): Unit = s.writeInt16(x)
  override val size: Cnt = 2L
}
val int32 = object: StaticallySized<Int32> {
  override fun read(s: Reader): Int32 = s.readInt32()
  override fun write(s: Writer, x: Int32): Unit = s.writeInt32(x)
  override val size: Cnt = 4L
}
val int64 = object: StaticallySized<Int64> {
  override fun read(s: Reader): Int64 = s.readInt64()
  override fun write(s: Writer, x: Int64): Unit = s.writeInt64(x)
  override val size: Cnt = 8L
}
val rat32 = object: StaticallySized<Rat32> {
  override fun read(s: Reader): Rat32 = s.readRat32()
  override fun write(s: Writer, x: Rat32): Unit = s.writeRat32(x)
  override val size: Cnt = 4L
}
val rat64 = object: StaticallySized<Rat64> {
  override fun read(s: Reader): Rat64 = s.readRat64()
  override fun write(s: Writer, x: Rat64): Unit = s.writeRat64(x)
  override val size: Cnt = 8L
}

val bool8 = object: StaticallySized<Boolean> {
  override fun read(s: Reader): Boolean = s.readNat8() != 0
  override fun write(s: Writer, x: Boolean): Unit = s.writeNat8(if (x) 1 else 0)
  override val size: Cnt = 1L
}
val char16 = object: StaticallySized<Char> {
  // 读：转成 Int 后，抹去高位的符号位扩展，再安全转成 Char
  override fun read(s: Reader): Char = (s.readInt16().toInt() and 0xFFFF).toChar()
  
  // 写：用 .code 代替旧的 .toShort() 对应 Char 的直接转换
  override fun write(s: Writer, x: Char): Unit = s.writeInt16(x.code.toShort())
  
  override val size: Cnt = 2L
}
val nat16 = object: StaticallySized<Nat16> {
  override fun read(s: Reader): Nat16 = s.readInt16().uExt()
  override fun write(s: Writer, x: Nat16): Unit = s.writeInt16(x.toShort())
  override val size: Cnt = 2L
}
val nat32 = object: StaticallySized<Nat32> {
  override fun read(s: Reader): Nat32 = s.readInt32().uExt()
  override fun write(s: Writer, x: Nat32): Unit = s.writeInt32(x.toInt())
  override val size: Cnt = 4L
}