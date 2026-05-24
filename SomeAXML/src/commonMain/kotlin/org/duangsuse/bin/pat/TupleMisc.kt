package org.duangsuse.bin.pat

import org.duangsuse.bin.type.Cnt
// 移除对 org.duangsuse.bin.indices 的模糊引用，改用显式逻辑

/** Creates an object like [Tuple] with given size */
typealias Allocator<T> = (Cnt) -> T

/** Mutable version of [Pair] */
data class Tuple2<A, B>(var first: A, var second: B)

operator fun <E> Tuple<E>.component1() = this[0]
operator fun <E> Tuple<E>.component2() = this[1]
operator fun <E> Tuple<E>.component3() = this[2]
operator fun <E> Tuple<E>.component4() = this[3]

/** 将 Tuple 转换为 List，适配 Long 类型的 size */
fun <E> Tuple<E>.toList(): List<E> {
  val res = mutableListOf<E>()
  // 直接使用 size.toInt()，因为 JVM 数组限制了 Tuple 的实际大小不会超过 Int.MAX_VALUE
  for (i in 0 until size.toInt()) {
    res.add(this[i])
  }
  return res
}