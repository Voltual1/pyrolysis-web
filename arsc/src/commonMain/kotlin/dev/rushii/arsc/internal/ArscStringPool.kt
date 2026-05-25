package dev.rushii.arsc.internal

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
        val useUtf8 = (flags and UTF_8_FLAG) != 0u
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
        public const val UTF_8_FLAG: UInt = 0x00000100u

        public fun parse(source: Source): ArscStringPool {
            val header = ArscHeader.parse(source, 0L)
            val bodySize = header.bodySize.toLong() - 8
            
            // 使用 tempBuffer 限制读取范围，防止越界
            val body = Buffer()
            source.readTo(body, bodySize)
            
            val stringsCount = body.readU32()
            val stylesCount = body.readU32()
            val flags = body.readU32()
            val stringsOffset = body.readU32()
            val stylesOffset = body.readU32()

            val stringOffsets = Array(stringsCount.toInt()) { body.readU32() }
            val styleOffsets = Array(stylesCount.toInt()) { body.readU32() }

            val useUtf8 = (flags and UTF_8_FLAG) != 0u
            
            // 计算字符串内容相对于 body 开始的偏移
            // 当前 body 位置已经在 offsets 之后了
            val currentPosInBody = 20L + (stringsCount.toLong() * 4) + (stylesCount.toLong() * 4)
            val toSkip = stringsOffset.toLong() - currentPosInBody
            if (toSkip > 0) body.skip(toSkip)

            val strings = mutableListOf<String>()
            for (i in 0 until stringsCount.toInt()) {
                strings += if (useUtf8) readUtf8String(body) else readUtf16String(body)
            }

            // 忽略 Styles，直接返回。body 已经被 source.readTo 消耗完了。
            return ArscStringPool(strings, emptyList(), flags)
        }

        private fun readUtf8String(source: Source): String {
            readLen8(source) // char len
            val byteLen = readLen8(source)
            val bytes = ByteArray(byteLen)
            source.readTo(bytes)
            source.readByte() // null
            return bytes.decodeToString()
        }

        private fun readLen8(source: Source): Int {
            val l = source.readU8().toInt()
            return if (l and 0x80 != 0) ((l and 0x7F) shl 8) or source.readU8().toInt() else l
        }

        private fun readUtf16String(source: Source): String {
            val charLen = readLen16(source)
            val chars = CharArray(charLen)
            for (i in 0 until charLen) {
                chars[i] = source.readU16().toInt().toChar()
            }
            source.readU16() // null
            return String(chars)
        }

        private fun readLen16(source: Source): Int {
            val l = source.readU16().toInt()
            return if (l and 0x8000 != 0) ((l and 0x7FFF) shl 16) or source.readU16().toInt() else l
        }

        public fun write(sink: Sink, pool: ArscStringPool): WrittenPool {
            val stringsBuffer = Buffer()
            val useUtf8 = (pool.flags and UTF_8_FLAG) != 0u
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

            val baseOffset = 8 + 20 + (pool.strings.size * 4) + (pool.styles.size * 4)
            val totalSize = baseOffset + stringsBuffer.size.toInt()

            val header = ArscHeader(ArscHeaderType.StringPool, 8u.toUShort(), totalSize.toUInt())
            ArscHeader.write(sink, header)
            sink.writeU32(pool.strings.size.toUInt())
            sink.writeU32(pool.styles.size.toUInt())
            sink.writeU32(pool.flags)
            sink.writeU32(baseOffset.toUInt())
            sink.writeU32(0u)

            for (off in offsets) sink.writeU32(off.toUInt())
            sink.write(stringsBuffer, stringsBuffer.size)
            
            val padding = (4 - (totalSize % 4)) % 4
            if (padding > 0) for (i in 0 until padding) sink.writeByte(0)

            return WrittenPool(pool.strings.mapIndexed { i, s -> s to i }.toMap(), emptyMap())
        }

        private fun writeLen8(sink: Sink, len: Int) {
            if (len > 0x7F) sink.writeByte(((len shr 8) or 0x80).toByte())
            sink.writeByte((len and 0xFF).toByte())
        }

        private fun writeLen16(sink: Sink, len: Int) {
            if (len > 0x7FFF) {
                sink.writeShortLe(((len shr 16) or 0x8000).toShort())
                sink.writeShortLe(len.toShort())
            } else sink.writeShortLe(len.toShort())
        }
    }
}