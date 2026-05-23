package dev.rushii.arsc.internal

import dev.rushii.arsc.ArscError
import dev.rushii.arsc.ArscStyle
import kotlinx.io.*

@ArscInternalApi
public data class ArscStringPool(
	val strings: List<String>,
	val styles: List<ArscStyle>,
	val flags: UInt,
) {
	public fun size(): Int {
		var s = ArscHeader.size() + 20 + (strings.size * 4) + (styles.size * 4)
		// 估算字符串内容大小
		val useUtf8 = flags and UTF_8_FLAG != 0u
		for (str in strings) {
			if (useUtf8) {
				val b = str.encodeToByteArray()
				s += b.size + (if (str.length > 0x7F) 2 else 1) + (if (b.size > 0x7F) 2 else 1) + 1
			} else {
				s += (str.length * 2) + (if (str.length > 0x7FFF) 4 else 2) + 2
			}
		}
		return s
	}

	public data class WrittenPool(
		val strings: Map<String, Int>,
		val styles: Map<ArscStyle, Int>,
	)

	public companion object {
		public val UTF_8_FLAG: UInt = 0x00000100u

		public fun parse(source: Source): ArscStringPool {
			val header = ArscHeader.parse(source, 0L)
			val stringsCount = source.readU32()
			val stylesCount = source.readU32()
			val flags = source.readU32()
			val stringsOffset = source.readU32()
			val stylesOffset = source.readU32()

			val offsets = Array(stringsCount.toInt()) { source.readU32() }
			val styleOffsets = Array(stylesCount.toInt()) { source.readU32() }

			val useUTF8 = flags and UTF_8_FLAG != 0U
			val strings = List(stringsCount.toInt()) {
				if (useUTF8) readUtf8String(source) else readUtf16String(source)
			}

			val styles = List(stylesCount.toInt()) { ArscStyle.parse(source) }

			return ArscStringPool(strings, styles, flags)
		}

		private fun readUtf8String(source: Source): String {
			val charLen = readLen8(source)
			val byteLen = readLen8(source)
			val bytes = ByteArray(byteLen.toInt())
			source.readTo(bytes)
			val s = bytes.decodeToString()
			source.readByte() // null
			return s
		}

		private fun readLen8(source: Source): UShort {
			val l = source.readU8().toUShort()
			return if (l and 0x80u > 0u) (((l and 0x7Fu) shl 8) or source.readU8().toUShort()) else l
		}

		private fun readUtf16String(source: Source): String {
			val charLen = readLen16(source)
			val chars = CharArray(charLen.toInt())
			for (i in 0 until charLen.toInt()) {
				chars[i] = (source.readU16().toInt()).toChar()
			}
			source.readU16() // null
			return String(chars)
		}

		private fun readLen16(source: Source): UInt {
			val l = source.readU16()
			return if (l > 0x7FFFu) (((l and 0x7FFFu).toUInt() shl 16) or source.readU16().toUInt()) else l.toUInt()
		}

		public fun write(sink: Sink, pool: ArscStringPool): WrittenPool {
			val stringsBuffer = Buffer()
			val useUtf8 = pool.flags and UTF_8_FLAG != 0U
			val offsets = IntArray(pool.strings.size)

			for ((i, s) in pool.strings.withIndex()) {
				offsets[i] = stringsBuffer.size.toInt()
				if (useUtf8) {
					val b = s.encodeToByteArray()
					writeLen8(stringsBuffer, s.length)
					writeLen8(stringsBuffer, b.size)
					stringsBuffer.write(b)
					stringsBuffer.writeByte(0)
				} else {
					writeLen16(stringsBuffer, s.length)
					for (char in s) stringsBuffer.writeShortLe(char.code.toShort())
					stringsBuffer.writeShortLe(0)
				}
			}

			val baseOffset = ArscHeader.size() + 20 + (pool.strings.size * 4) + (pool.styles.size * 4)
			val totalSize = baseOffset + stringsBuffer.size
			
			val header = ArscHeader(ArscHeaderType.StringPool, ArscHeader.size().toUShort(), totalSize.toUInt())
			ArscHeader.write(sink, header)
			sink.writeU32(pool.strings.size.toUInt())
			sink.writeU32(pool.styles.size.toUInt())
			sink.writeU32(pool.flags)
			sink.writeU32(baseOffset.toUInt())
			sink.writeU32(0u) // stylesOffset

			for (off in offsets) sink.writeU32(off.toUInt())
			sink.write(stringsBuffer, stringsBuffer.size)

			return WrittenPool(
				pool.strings.mapIndexed { i, s -> s to i }.toMap(),
				pool.styles.mapIndexed { i, s -> s to i }.toMap()
			)
		}

		private fun writeLen8(sink: Sink, len: Int) {
			if (len > 0x7F) sink.writeByte(((len shr 8) or 0x80).toByte())
			sink.writeByte((len and 0xFF).toByte())
		}

		private fun writeLen16(sink: Sink, len: Int) {
			if (len > 0x7FFF) {
				sink.writeShortLe(((len shr 16) or 0x8000).toShort())
				sink.writeShortLe(len.toShort())
			} else {
				sink.writeShortLe(len.toShort())
			}
		}
	}
}