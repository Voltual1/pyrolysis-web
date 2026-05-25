package dev.rushii.arsc.internal

import dev.rushii.arsc.ArscError
import kotlinx.io.Source
import kotlinx.io.Sink

@ArscInternalApi
public enum class ArscHeaderType(public val value: UShort) {
    Null(0x0000u),
    StringPool(0x0001u),
    Table(0x0002u),
    TablePackage(0x0200u),
    TableType(0x0201u),
    TableTypeSpec(0x0202u),
    TableLibrary(0x0203u);

    public companion object {
        public fun parse(source: Source, position: Long): ArscHeaderType {
            val value = source.readU16()
            return entries.find { it.value == value }
                ?: throw ArscError(position.toInt(), value, "Invalid header type 0x${value.toString(16)}")
        }

        public fun write(sink: Sink, value: ArscHeaderType) {
            sink.writeU16(value.value)
        }
    }
}