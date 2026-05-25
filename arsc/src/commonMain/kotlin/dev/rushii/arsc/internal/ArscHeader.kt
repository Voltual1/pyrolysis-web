package dev.rushii.arsc.internal

import kotlinx.io.Source
import kotlinx.io.Sink

@ArscInternalApi
public data class ArscHeader(
    val type: ArscHeaderType,
    val headerSize: UShort,
    val bodySize: UInt,
) {
    public companion object {
        public fun size(): Int = 8 // 2 + 2 + 4

        public fun parse(source: Source, currentPos: Long): ArscHeader {
            val type = ArscHeaderType.parse(source, currentPos)
            val headerSize = source.readU16()
            val bodySize = source.readU32()
            return ArscHeader(type, headerSize, bodySize)
        }

        public fun write(sink: Sink, value: ArscHeader) {
            ArscHeaderType.write(sink, value.type)
            sink.writeU16(value.headerSize)
            sink.writeU32(value.bodySize)
        }

        /** 跳过当前块剩余部分以及填充字节，返回下一块开始的位置（通常不需要） */
        public fun skipChunk(source: Source, header: ArscHeader) {
            val chunkTotalSize = header.bodySize.toLong()
            val alreadyRead = size().toLong()
            if (chunkTotalSize > alreadyRead) {
                source.skip(chunkTotalSize - alreadyRead)
            }
            // 4字节对齐
            val padding = (4 - (chunkTotalSize % 4)) % 4
            if (padding > 0) source.skip(padding)
        }
    }
}