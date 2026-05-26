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
        // 此处仅为估算，实际以写入时的 Buffer 大小为准
        return 0 
    }

    public companion object {
        public const val UTF_8_FLAG: UInt = 0x00000100u

        public fun parse(source: Source): ArscStringPool {
            val header = ArscHeader.parse(source, 0L)
            val bodySize = header.bodySize.toLong() - 8
            
            // 关键修复：将整个块的内容读入独立 Buffer，确保 source 指针同步
            val body = Buffer()
            source.readTo(body, bodySize)
            
            val stringsCount = body.readU32()
            val stylesCount = body.readU32()
            val flags = body.readU32()
            val stringsOffset = body.readU32()
            val stylesOffset = body.readU32()

            // 读取偏移量数组
            val stringOffsets = Array(stringsCount.toInt()) { body.readU32() }
            val styleOffsets = Array(stylesCount.toInt()) { body.readU32() }

            val useUtf8 = (flags and UTF_8_FLAG) != 0u
            val strings = mutableListOf<String>()

            // 字符串数据相对于块起始位置 (Header 开始) 的偏移
            // 我们已经在 body 内部，body 的起点是 Header 之后 (偏移 8)
            val currentPosInBody = 20L + (stringsCount.toLong() * 4) + (stylesCount.toLong() * 4)
            val targetStringsStartInBody = stringsOffset.toLong() - 8
            
            if (targetStringsStartInBody > currentPosInBody) {
                body.skip(targetStringsStartInBody - currentPosInBody)
            }

            // 为了应对可能的格式异常，我们使用 try-catch 或 check
            for (i in 0 until stringsCount.toInt()) {
                if (body.exhausted()) break
                strings += if (useUtf8) readUtf8String(body) else readUtf16String(body)
            }

            return ArscStringPool(strings, emptyList(), flags)
        }

        private fun readUtf8String(source: Source): String {
            // UTF-8 字符串前有两个长度：字符数和字节数
            readLen8(source) 
            val byteLen = readLen8(source)
            if (byteLen == 0) {
                source.readByte() // 消耗 null
                return ""
            }
            val bytes = ByteArray(byteLen)
            source.readTo(bytes)
            source.readByte() // 消耗 null 终止符
            return bytes.decodeToString()
        }

        private fun readLen8(source: Source): Int {
            val val1 = source.readU8().toInt()
            return if ((val1 and 0x80) != 0) {
                ((val1 and 0x7F) shl 8) or source.readU8().toInt()
            } else val1
        }

        private fun readUtf16String(source: Source): String {
            val charLen = readLen16(source)
            if (charLen == 0) {
                source.readU16() // 消耗 null
                return ""
            }
            val chars = CharArray(charLen)
            for (i in 0 until charLen) {
                chars[i] = source.readU16().toInt().toChar()
            }
            source.readU16() // 消耗 null
            return String(chars)
        }

        private fun readLen16(source: Source): Int {
            val val1 = source.readU16().toInt()
            return if ((val1 and 0x8000) != 0) {
                ((val1 and 0x7FFF) shl 16) or source.readU16().toInt()
            } else val1
        }

        // write 实现保持之前的逻辑，但确保对齐
        public fun write(sink: Sink, pool: ArscStringPool): ArscStringPool.WrittenPool {
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
            if (padding > 0) repeat(padding) { sink.writeByte(0) }

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