package dev.rushii.arsc.internal

import dev.rushii.arsc.ArscError
import kotlinx.io.Source
import kotlinx.io.Sink

@ArscInternalApi
public enum class ArscHeaderType(public val value: UShort) {
	StringPool(0x0001u),
	Table(0x0002u),
	TablePackage(0x0200u),
	TableType(0x0201u),
	TableTypeSpec(0x0202u),
	TableLibrary(0x0203u);

	public companion object {
		@JvmStatic
		public fun size(): Int = 2 // UShort.SIZE_BYTES

		@JvmStatic
		public fun parse(source: Source, position: Long): ArscHeaderType {
			val value = source.readU16()
			return when (value) {
				StringPool.value -> StringPool
				Table.value -> Table
				TablePackage.value -> TablePackage
				TableType.value -> TableType
				TableTypeSpec.value -> TableTypeSpec
				TableLibrary.value -> TableLibrary
				else -> throw ArscError(position.toInt(), value, "Invalid header type 0x${value.toString(16)}")
			}
		}

		@JvmStatic
		public fun write(sink: Sink, value: ArscHeaderType) {
			sink.writeU16(value.value)
		}
	}
}