package org.duangsuse.bin.pat.basic

import org.duangsuse.bin.*
import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.*
import org.duangsuse.bin.type.Cnt

inline fun <reified T> Pattern<T>.primitiveArray(sizer: Pattern<Cnt>, init: T): Pattern<Array<T>>
  = object: Pattern<Array<T>> {
  override fun read(s: Reader): Array<T> {
    val size = sizer.read(s).toInt()
    val ary: Array<T> = Array(size) {init}
    for (i in ary.indices) ary[i] = this@primitiveArray.read(s)
    return ary
  }
  override fun write(s: Writer, x: Array<T>) {
    sizer.write(s, x.size.toLong())
    for (item in x) this@primitiveArray.write(s, item)
  }
  override fun writeSize(x: Array<T>): Cnt = sizer.writeSize(x.size.toLong()) + x.map(this@primitiveArray::writeSize).sum()
}

fun Pattern<Cnt>.sizedByteArray() = object: Pattern<ByteArray> {
  override fun read(s: Reader): ByteArray {
    val n = this@sizedByteArray.read(s)
    return s.asNat8Reader().takeByte(n)
  }
  override fun write(s: Writer, x: ByteArray) {
    this@sizedByteArray.write(s, x.size.toLong())
    s.asNat8Writer().writeFrom(x)
  }
  override fun writeSize(x: ByteArray): Cnt = this@sizedByteArray.writeSize(x.size.toLong()) + x.size.toLong()
}