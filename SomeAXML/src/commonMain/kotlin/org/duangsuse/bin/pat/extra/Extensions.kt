package org.duangsuse.bin.pat.extra

import org.duangsuse.bin.*
import org.duangsuse.bin.bitflag.BitFlags
import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.ConvertedPattern
import org.duangsuse.bin.pat.atom.int32
import org.duangsuse.bin.pat.basic.Cond
import org.duangsuse.bin.pat.basic.Repeat
import org.duangsuse.bin.type.*

fun <T, T1> Pattern<T>.converted(from: (T) -> T1, to: (T1) -> T) = object: ConvertedPattern<T, T1>(this) {
  override fun from(src: T): T1 = from(src)
  override fun to(x: T1): T = to(x)
}

infix fun <T: Any> Pattern<Cnt>.sizedRepeat(item: Pattern<T>) = Repeat(this, item)
fun <TAG, E> Pattern<TAG>.cond(vararg conditions: Cond.Case<TAG, E>) = Cond(this, *conditions)

fun <T> Pattern<T>.littleEndian() = EndianSwitch.LittleEndian(this)
fun <T> Pattern<T>.bigEndian() = EndianSwitch.BigEndian(this)

fun <T> Pattern<T>.aligned(n: Cnt) = Aligned(n, this)
infix fun <A, B> Pattern<A>.contextual(body: (A) -> Pattern<B>) = Contextual(this, body)

fun <T> T.statically() = object: Pattern.StaticallySized<T> {
  override fun read(s: Reader): T = this@statically
  override fun write(s: Writer, x: T) {}
  override val size: Cnt = 0L
}

infix fun <T> Pattern<T>.magic(value: T) = magic(value) { error("Unknown magic <$it>") }

infix fun Pattern<Int>.padding(k: Int) = converted({ it + k }, { it - k })

fun <BIT_FL: BitFlags> ((Int32) -> BIT_FL).bitFlags() = object: ConvertedPattern<Int32, BIT_FL>(int32) {
  override fun from(src: Int32): BIT_FL = this@bitFlags(src)
  override fun to(x: BIT_FL): Int32 = x.toInt()
}