package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscStyle(
	val spans: List<Span>,
) {
	public fun size(): Int {
		return (spans.size * Span.size()) + 4 // SPAN_END
	}

	public data class Span(
		val name: String,
		val start: UInt,
		val end: UInt,
	) {
		public companion object {
			public fun size(): Int = 12
		}
	}

	@ArscInternalApi
	public companion object {
		private const val SPAN_END = UInt.MAX_VALUE

		@JvmStatic
		public fun parse(source: Source): ArscStyle {
			val spans = mutableListOf<Span>()
			while (true) {
				val name = source.readU32()
				if (name == SPAN_END) break
				val start = source.readU32()
				val end = source.readU32()
				spans += Span(name = "", start = start, end = end)
			}
			return ArscStyle(spans)
		}

		@JvmStatic
		public fun write(sink: Sink, style: ArscStyle, writtenPool: ArscStringPool.WrittenPool) {
			for (span in style.spans) {
				val nameIndex = writtenPool.strings[span.name] 
					?: throw IllegalStateException("Style span name '${span.name}' not in string pool")
				sink.writeU32(nameIndex.toUInt())
				sink.writeU32(span.start)
				sink.writeU32(span.end)
			}
			sink.writeU32(SPAN_END)
		}
	}
}