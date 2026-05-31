@file:Suppress("NOTHING_TO_INLINE")

package dev.rushii.arsc.internal

import dev.rushii.arsc.ArscError
import kotlinx.io.*

internal inline fun Source.readU8(): UByte = readByte().toUByte()
internal inline fun Source.readU16(): UShort = readShortLe().toUShort()
internal inline fun Source.readU32(): UInt = readIntLe().toUInt()

internal inline fun Sink.writeU8(value: UByte) = writeByte(value.toByte())
internal inline fun Sink.writeU16(value: UShort) = writeShortLe(value.toShort())
internal inline fun Sink.writeU32(value: UInt) = writeIntLe(value.toInt())

internal fun Buffer.align(alignment: Int) {
	val remaining = size % alignment
	if (remaining > 0L) {
		skip(alignment - remaining)
	}
}

internal fun Sink.writeAlignment(currentSize: Long, alignment: Int) {
	val remaining = currentSize % alignment
	if (remaining > 0L) {
		putNullBytes((alignment - remaining.toInt()))
	}
}

internal fun Sink.putNullBytes(amount: Int) {
	for (i in 0..<amount) writeByte(0)
}

internal fun Source.readStringUtf16(charSize: Int): String {
	val chars = CharArray(charSize)
	var actualLen = charSize
	for (i in 0 until charSize) {
		val low = readByte().toInt() and 0xFF
		val high = readByte().toInt() and 0xFF
		val code = (high shl 8) or low
		if (code == 0 && actualLen == charSize) actualLen = i
		chars[i] = code.toChar()
	}
	return chars.concatToString(0, actualLen) // <-- 换成这个（注意参数是 startIndex 和 endIndex）
}

internal fun Sink.putStringUtf16(string: String, outSize: Int) {
	var bytesWritten = 0
	for (char in string) {
		if (bytesWritten + 2 > outSize) break
		val code = char.code
		writeByte((code and 0xFF).toByte())
		writeByte(((code shr 8) and 0xFF).toByte())
		bytesWritten += 2
	}
	putNullBytes(outSize - bytesWritten)
}