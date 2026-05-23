package org.duangsuse.bin.pat

import org.duangsuse.bin.type.Cnt
import org.duangsuse.bin.type.Idx

typealias IndexedProducer<R> = (Idx) -> R

open class BooleanTuple(size: Cnt, init: IndexedProducer<Boolean> = {false})
  : Tuple<Boolean>(size) {
  final override val items: Array<Boolean> = Array(size.toInt(), init)
}

open class ByteTuple(size: Cnt, init: IndexedProducer<Byte> = {0.toByte()})
  : Tuple<Byte>(size) {
  final override val items: Array<Byte> = Array(size.toInt(), init)
}
open class ShortTuple(size: Cnt, init: IndexedProducer<Short> = {0.toShort()})
  : Tuple<Short>(size) {
  final override val items: Array<Short> = Array(size.toInt(), init)
}
open class CharTuple(size: Cnt, init: IndexedProducer<Char> = {'\u0000'})
  : Tuple<Char>(size) {
  final override val items: Array<Char> = Array(size.toInt(), init)
}
open class IntTuple(size: Cnt, init: IndexedProducer<Int> = {0})
  : Tuple<Int>(size) {
  final override val items: Array<Int> = Array(size.toInt(), init)
}
open class LongTuple(size: Cnt, init: IndexedProducer<Long> = {0L})
  : Tuple<Long>(size) {
  final override val items: Array<Long> = Array(size.toInt(), init)
}
open class FloatTuple(size: Cnt, init: IndexedProducer<Float> = {0.0F})
  : Tuple<Float>(size) {
  final override val items: Array<Float> = Array(size.toInt(), init)
}
open class DoubleTuple(size: Cnt, init: IndexedProducer<Double> = {0.0})
  : Tuple<Double>(size) {
  final override val items: Array<Double> = Array(size.toInt(), init)
}