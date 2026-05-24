package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscResource(
	val specId: UInt = 0u,
	val flags: UShort,
	val name: String,
	val value: ArscValue,
) {
	@ArscInternalApi
	public companion object {
		private const val FLAG_COMPLEX: UShort = 0x0001u

		public fun write(
			sink: Sink,
			resource: ArscResource,
			writtenGlobalPool: ArscStringPool.WrittenPool,
			writtenKeyPool: ArscStringPool.WrittenPool
		) {
			val nameIndex = writtenKeyPool.strings[resource.name]
				?: throw IllegalStateException("Resource name '${resource.name}' not in key string pool")

			// 写入 Entry Header
			val isComplex = resource.value is ArscValue.Bag
			val entrySize = if (isComplex) 16u else 8u
			
			sink.writeU16(entrySize.toUShort())
			sink.writeU16(resource.flags)
			sink.writeU32(nameIndex.toUInt())

			when (val v = resource.value) {
				is ArscValue.Plain -> {
					ArscValue.Plain.write(sink, v, writtenGlobalPool)
				}
				is ArscValue.Bag -> {
					sink.writeU32(v.parent)
					sink.writeU32(v.values.size.toUInt())
					v.values.forEach { (key, plainValue) ->
						sink.writeU32(key)
						ArscValue.Plain.write(sink, plainValue, writtenGlobalPool)
					}
				}
			}
		}
        public fun parse(
			source: Source,
			resourceCount: Int,
			globalStringPool: ArscStringPool,
			keyStringPool: ArscStringPool,
		): MutableList<ArscResource> {
			val entries = (0..<resourceCount).map { source.readU32() }
			val resources = mutableListOf<ArscResource>()

			entries.forEachIndexed { specIndex, entry ->
				if (entry == UInt.MAX_VALUE) return@forEachIndexed

				val size = source.readU16()
				val flags = source.readU16()
				val nameIndex = source.readU32()
				
				val value = if (flags and FLAG_COMPLEX != 0.toUShort()) {
					val parent = source.readU32()
					val count = source.readU32()
					val values = (0..<count.toInt()).associate {
						val index = source.readU32()
						val v = ArscValue.Plain.parse(source, globalStringPool)
						index to v
					}
					ArscValue.Bag(parent, values)
				} else {
					ArscValue.Plain.parse(source, globalStringPool)
				}

				resources += ArscResource(
					specId = specIndex.toUInt(),
					flags = flags,
					name = keyStringPool.strings[nameIndex.toInt()],
					value = value,
				)
			}
			return resources
		}	
    }
}