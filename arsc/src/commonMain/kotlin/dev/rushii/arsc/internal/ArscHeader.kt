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

			return ArscHeader(
				type = type,
				headerSize = headerSize,
				bodySize = bodySize,
			)
		}

		public fun write(sink: Sink, value: ArscHeader) {
			ArscHeaderType.write(sink, value.type)
			sink.writeU16(value.headerSize)
			sink.writeU32(value.bodySize)
		}
	}
}