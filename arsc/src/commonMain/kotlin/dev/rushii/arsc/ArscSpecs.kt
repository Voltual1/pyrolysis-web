package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.Source

public data class ArscSpecs(
	val typeId: UByte = 0u,
	val specs: MutableMap<UInt, Spec>,
) {
	public fun highestSpecId(): UInt {
		return specs.values.maxByOrNull { it.id }?.id ?: 1U
	}

	public data class Spec(
		val id: UInt,
		val flags: UInt,
	)

	@ArscInternalApi
	public companion object {
		public fun parse(source: Source): ArscSpecs {
			val typeId = source.readU8()
			val res0 = source.readU8()
			val res1 = source.readU16()
			val specCount = source.readU32()

			val specs = (0u..<specCount).map { id ->
				Spec(id, source.readU32())
			}

			return ArscSpecs(
				typeId = typeId,
				specs = specs.associateBy { it.id }.toMutableMap()
			)
		}
	}
}