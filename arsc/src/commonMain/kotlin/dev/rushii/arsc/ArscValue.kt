package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.Source
import kotlinx.io.Sink

public sealed interface ArscValue {
    public sealed class Plain : ArscValue {
        public abstract val type: UByte

        @ArscInternalApi
        public companion object {
            private const val TYPE_STRING: UByte = 0x03u

            public fun parse(source: Source, globalStringPool: ArscStringPool): Plain {
                source.readU16(); source.readU8()
                val type = source.readU8()
                val data = source.readU32()
                return if (type == TYPE_STRING) String(globalStringPool.strings[data.toInt()]) else Raw(type, data)
            }

            public fun write(sink: Sink, value: Plain, writtenGlobalPool: ArscStringPool.WrittenPool) {
                sink.writeU16(8u); sink.writeU8(0u); sink.writeU8(value.type)
                when (value) {
                    is Raw -> sink.writeU32(value.data)
                    is String -> sink.writeU32(writtenGlobalPool.strings[value.data]?.toUInt() ?: 0u)
                }
            }
        }

        public data class Raw(override val type: UByte, val data: UInt) : Plain()
        public data class String(val data: kotlin.String) : Plain() { override val type: UByte = 0x3U }
    }

    public data class Bag(val parent: UInt, val values: Map<UInt, Plain>) : ArscValue
}