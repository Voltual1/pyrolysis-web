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

			@JvmStatic
			public fun parse(source: Source, globalStringPool: ArscStringPool): Plain {
				val size = source.readU16() // const u16 = 8
				val zero = source.readU8() // const u8 = 0
				val type = source.readU8()
				val data = source.readU32()

				return if (type == TYPE_STRING) {
					String(
						data = globalStringPool.strings[data.toInt()]
					)
				} else {
					Raw(
						type = type,
						data = data,
					)
				}
			}

			@JvmStatic
			public fun write(sink: Sink, value: Plain, writtenGlobalPool: ArscStringPool.WrittenPool) {
				sink.writeU16(8u) // size
				sink.writeU8(0u) // zero
				sink.writeU8(value.type) // type

				when (value) {
					is Raw -> {
						sink.writeU32(value.data)
					}

					is String -> {
						sink.writeU32(writtenGlobalPool.strings[value.data]!!.toUInt())
					}
				}
			}
		}

		public data class Raw(
			override val type: UByte,
			val data: UInt,
		) : Plain()

		public data class String(
			val data: kotlin.String,
		) : Plain() {
			override val type: UByte = 0x3U
		}
	}

	public data class Bag(
		val parent: UInt,
		val values: Map<UInt, Plain>,
	) : ArscValue
}