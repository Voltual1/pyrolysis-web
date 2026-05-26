package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscStyle(public val spans: List<Span>) {
    public data class Span(public val name: String, public val start: UInt, public val end: UInt)

    @ArscInternalApi
    public companion object {
        private const val SPAN_END = UInt.MAX_VALUE

        public fun parse(source: Source): ArscStyle {
            val spans = mutableListOf<Span>()
            while (true) {
                val name = source.readU32()
                if (name == SPAN_END) break
                spans += Span("", source.readU32(), source.readU32())
            }
            return ArscStyle(spans)
        }

        public fun write(sink: Sink, style: ArscStyle, writtenPool: ArscStringPool.WrittenPool) {
            for (span in style.spans) {
                val idx = writtenPool.strings[span.name] ?: 0
                sink.writeU32(idx.toUInt())
                sink.writeU32(span.start); sink.writeU32(span.end)
            }
            sink.writeU32(SPAN_END)
        }
    }
}