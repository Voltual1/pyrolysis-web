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
        public const val UTF_8_FLAG: UInt = 0x00000100u

        public fun parse(source: Source): ArscStringPool {
            val header = ArscHeader.parse(source, 0L)
            val chunkEnd = header.bodySize.toLong() - ArscHeader.size()

            val stringsCount = source.readU32()
            val stylesCount = source.readU32()
            val flags = source.readU32()
            val stringsOffset = source.readU32()
            val stylesOffset = source.readU32()

            // 读取偏移数组
            val stringOffsets = Array(stringsCount.toInt()) { source.readU32() }
            val styleOffsets = Array(stylesCount.toInt()) { source.readU32() }

            // 计算已经读取的字节数：header(8) + info(20) + offsets(4*count)
            var readSoFar = 28L + (stringsCount.toLong() * 4) + (stylesCount.toLong() * 4)

            // 跳转到字符串数据区
            if (stringsOffset.toLong() > readSoFar) {
                source.skip(stringsOffset.toLong() - readSoFar)
                readSoFar = stringsOffset.toLong()
            }

            val useUtf8 = flags and UTF_8_FLAG != 0u
            val strings = mutableListOf<String>()
            for (i in 0 until stringsCount.toInt()) {
                strings += if (useUtf8) readUtf8String(source) else readUtf16String(source)
            }

            // 计算当前读取位置（用于对齐检查）
            val totalRead = ArscHeader.size() + header.bodySize.toInt()
            // 注意：有些 ARSC 文件在字符串池末尾可能没有填充，不需要额外处理，由外部解析器负责对齐
            return ArscStringPool(strings, emptyList(), flags)
        }

        private fun readUtf8String(source: Source): String {
            val charLen = readLen8(source)
            val byteLen = readLen8(source)
            val bytes = ByteArray(byteLen.toInt())
            source.readTo(bytes)
            source.readByte() // 0x00 终止符
            return bytes.decodeToString()
        }

        private fun readLen8(source: Source): Int {
            val first = source.readU8().toInt()
            return if (first and 0x80 != 0) {
                val second = source.readU8().toInt()
                ((first and 0x7F) shl 8) or second
            } else first
        }

        private fun readUtf16String(source: Source): String {
            val charLen = readLen16(source)
            val bytes = ByteArray(charLen.toInt() * 2)
            source.readTo(bytes)
            // 读取 null 终止符 (2 字节)
            source.readU16()
            // 将小端 UTF-16 转换为 Kotlin String
            val chars = CharArray(charLen.toInt()) { i ->
                val lo = bytes[i * 2].toInt() and 0xFF
                val hi = bytes[i * 2 + 1].toInt() and 0xFF
                ((hi shl 8) or lo).toChar()
            }
            return String(chars)
        }

        private fun readLen16(source: Source): Int {
            val first = source.readU16().toInt()
            return if (first and 0x8000 != 0) {
                val second = source.readU16().toInt()
                ((first and 0x7FFF) shl 16) or second
            } else first
        }

        public fun write(sink: Sink, pool: ArscStringPool): WrittenPool {
            val stringsBuffer = Buffer()
            val useUtf8 = pool.flags and UTF_8_FLAG != 0u
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
                    for (char in s) {
                        stringsBuffer.writeShortLe(char.code.toShort())
                    }
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
            sink.writeU32(0u) // stylesOffset (我们忽略样式)
            for (off in offsets) sink.writeU32(off.toUInt())
            sink.write(stringsBuffer, stringsBuffer.size)
            // 对齐填充
            val padding = (4 - (totalSize % 4)) % 4
            if (padding > 0) sink.putNullBytes(padding)

            return WrittenPool(
                pool.strings.mapIndexed { i, s -> s to i }.toMap(),
                emptyMap()
            )
        }

        private fun writeLen8(sink: Sink, len: Int) {
            if (len > 0x7F) {
                sink.writeByte(((len shr 8) or 0x80).toByte())
                sink.writeByte((len and 0xFF).toByte())
            } else {
                sink.writeByte(len.toByte())
            }
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