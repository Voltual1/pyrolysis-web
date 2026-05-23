package dev.rushii.arsc.internal

import dev.rushii.arsc.ArscError
import dev.rushii.arsc.ArscStyle
import kotlinx.io.*
import kotlin.experimental.and

@ArscInternalApi
public data class ArscStringPool(
	val strings: List<String>,
	val styles: List<ArscStyle>,
	val flags: UInt,
) {
	public fun size(): Int {
		var size = 0
		size += ArscHeader.size()
		size += 5 * 4 // stringsCount, stylesCount, flags, stringsOffset, stylesOffset
		size += strings.size * 4
		size += styles.size * 4
		
		// 这里简化计算，实际写入时会通过 Buffer 确定
		val stringsBuffer = Buffer()
		val useUtf8 = flags and UTF_8_FLAG != 0u
		for (s in strings) {
			if (useUtf8) putUtf8String(stringsBuffer, s) else putUtf16String(stringsBuffer, s)
		}
		size += stringsBuffer.size.toInt()
		
		// 对齐
		val padding = if (size % 4 != 0L) (4 - (size % 4)).toInt() else 0
		size += padding
		
		size += styles.sumOf { it.size() }
		if (styles.isNotEmpty()) size += 8 // SPAN_END terminators
		
		return size
	}

	public data class WrittenPool(
		val strings: Map<String, Int>,
		val styles: Map<ArscStyle, Int>,
	)

	public companion object {
		public val UTF_8_FLAG: UInt = 0x00000100u

		@JvmStatic
		public fun parse(source: Source): ArscStringPool {
			// 由于 Source 无法获取绝对 position，我们假设调用者已知起始位置
			// 或者将 Source 读入 Buffer 处理
			val header = ArscHeader.parse(source, 0L) 
			assert(header.type == ArscHeaderType.StringPool)

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

			// 对齐处理：在 kotlinx.io 中通常需要根据已读字节数手动跳过
			return ArscStringPool(strings, styles, flags)
		}

		private fun readUtf8Length(source: Source): UShort {
			val length = source.readU8().toUShort()
			return if (length and 0x80u > 0u) {
				val length2 = source.readU8().toUShort()
				((length and 0x7Fu) shl 8) or length2
			} else {
				length
			}
		}

		private fun readUtf8String(source: Source): String {
			val charCount = readUtf8Length(source)
			val byteCount = readUtf8Length(source)
			val bytes = source.readByteArray(byteCount.toInt())
			val string = bytes.decodeToString() // 默认 UTF-8
			val terminator = source.readByte()
			assert(terminator.toInt() == 0)
			return string
		}

		private fun readUtf16Length(source: Source): UInt {
			val length = source.readU16()
			return if (length > 0x7FFFu) {
				((length and 0x7FFFu).toUInt() shl 16) or source.readU16().toUInt()
			} else {
				length.toUInt()
			}
		}

		private fun readUtf16String(source: Source): String {
			val charCount = readUtf16Length(source)
			val bytes = source.readByteArray(charCount.toInt() * 2)
			val string = bytes.decodeToString() // 需确保环境支持 UTF-16LE
			val terminator = source.readU16()
			if (terminator.toInt() != 0) throw ArscError(0, terminator, "Invalid NULL terminator")
			return string
		}

		@JvmStatic
		public fun write(sink: Sink, pool: ArscStringPool): WrittenPool {
			val poolBuffer = Buffer()
			val stringsBuffer = Buffer()
			val useUtf8 = pool.flags and UTF_8_FLAG != 0U
			
			val stringOffsets = IntArray(pool.strings.size)
			for ((i, s) in pool.strings.withIndex()) {
				stringOffsets[i] = stringsBuffer.size.toInt()
				if (useUtf8) putUtf8String(stringsBuffer, s) else putUtf16String(stringsBuffer, s)
			}

			val stylesBuffer = Buffer()
			val styleOffsets = IntArray(pool.styles.size)
			for ((i, style) in pool.styles.withIndex()) {
				styleOffsets[i] = stylesBuffer.size.toInt()
				// TODO: 写入 Style 逻辑
			}

			// 组装 Header 之后的内容
			poolBuffer.writeU32(pool.strings.size.toUInt())
			poolBuffer.writeU32(pool.styles.size.toUInt())
			poolBuffer.writeU32(pool.flags)
			
			val baseOffset = ArscHeader.size() + 5 * 4 + pool.strings.size * 4 + pool.styles.size * 4
			poolBuffer.writeU32(baseOffset.toUInt())
			
			val stylesOffsetVal = if (pool.styles.isEmpty()) 0u else (baseOffset + stringsBuffer.size).toUInt()
			poolBuffer.writeU32(stylesOffsetVal)

			for (offset in stringOffsets) poolBuffer.writeU32(offset.toUInt())
			for (offset in styleOffsets) poolBuffer.writeU32(offset.toUInt())
			
			poolBuffer.write(stringsBuffer, stringsBuffer.size)
			poolBuffer.writeAlignment(poolBuffer.size, 4)
			poolBuffer.write(stylesBuffer, stylesBuffer.size)

			val header = ArscHeader(
				ArscHeaderType.StringPool,
				ArscHeader.size().toUShort(),
				(ArscHeader.size() + poolBuffer.size).toUInt()
			)
			ArscHeader.write(sink, header)
			sink.write(poolBuffer, poolBuffer.size)

			return WrittenPool(
				strings = pool.strings.mapIndexed { i, s -> s to i }.toMap(),
				styles = pool.styles.mapIndexed { i, s -> s to i }.toMap()
			)
		}

		internal fun putUtf8Length(sink: Sink, length: Int) {
			if (length > 0x7F) {
				sink.writeByte(((length shr 8) or 0x80).toByte())
			}
			sink.writeByte((length and 0xFF).toByte())
		}

		internal fun putUtf8String(sink: Sink, string: String) {
			val bytes = string.encodeToByteArray()
			putUtf8Length(sink, string.length)
			putUtf8Length(sink, bytes.size)
			sink.write(bytes)
			sink.writeByte(0)
		}

		internal fun putUtf16Length(sink: Sink, length: Int) {
			if (length > 0x7FFF) {
				val leading = (length shr 16) or 0x8000
				sink.writeShortLe(leading.toShort())
				sink.writeShortLe(length.toShort())
			} else {
				sink.writeShortLe(length.toShort())
			}
		}

		internal fun putUtf16String(sink: Sink, string: String) {
			val bytes = string.encodeToByteArray() // 需为 UTF-16LE
			putUtf16Length(sink, string.length)
			sink.write(bytes)
			sink.writeShortLe(0)
		}
	}
}