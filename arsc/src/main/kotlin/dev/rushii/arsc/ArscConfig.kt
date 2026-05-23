package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscConfig(
	val typeId: UByte = 0u,
	val configId: ConfigId,
	var resources: MutableList<ArscResource>,
) {
	@ArscInternalApi
	public companion object {
		public fun parse(
			source: Source,
			globalStringPool: ArscStringPool,
			keyStringPool: ArscStringPool,
		): ArscConfig {
			val typeId = source.readU8()
			val res0 = source.readU8()
			val res1 = source.readU16()
			val resourceCount = source.readU32()
			val resourcesStart = source.readU32()
			val configId = ConfigId.parse(source)
			val resources = ArscResource.parse(source, resourceCount.toInt(), globalStringPool, keyStringPool)

			return ArscConfig(typeId, configId, resources)
		}

		public fun write(
			sink: Sink,
			config: ArscConfig,
			writtenGlobalPool: ArscStringPool.WrittenPool,
			writtenKeyPool: ArscStringPool.WrittenPool
		) {
			val resourceCount = if (config.resources.isEmpty()) 0u 
                               else (config.resources.maxOf { it.specId } + 1u)
			
			val resourceDataBuffer = Buffer()
			val entryOffsets = UIntArray(resourceCount.toInt()) { UInt.MAX_VALUE }
			
			for (res in config.resources) {
				entryOffsets[res.specId.toInt()] = resourceDataBuffer.size.toUInt()
				ArscResource.write(resourceDataBuffer, res, writtenGlobalPool, writtenKeyPool)
			}

			val headerSize = ArscHeader.size()
			val configIdSize = config.configId.data.size
			val resourcesStart = headerSize + 8 + 4 + configIdSize + (resourceCount.toInt() * 4)

			val header = ArscHeader(
				ArscHeaderType.TableType,
				headerSize.toUShort(),
				(resourcesStart + resourceDataBuffer.size).toUInt()
			)

			ArscHeader.write(sink, header)
			sink.writeU8(config.typeId)
			sink.writeU8(0u)
			sink.writeU16(0u)
			sink.writeU32(resourceCount)
			sink.writeU32(resourcesStart.toUInt())
			sink.write(config.configId.data.toByteArray())
			
			for (offset in entryOffsets) {
				sink.writeU32(offset)
			}
			sink.write(resourceDataBuffer, resourceDataBuffer.size)
		}
	}

	public data class ConfigId(var data: UByteArray) {
		@ArscInternalApi
		public companion object {
			public fun parse(source: Source): ConfigId {
				val size = source.readU32()
				val idBytes = ByteArray(size.toInt() - 4)
				source.readTo(idBytes)
				
				val fullData = UByteArray(size.toInt())
				fullData[0] = (size and 0xFFu).toUByte()
				fullData[1] = ((size shr 8) and 0xFFu).toUByte()
				fullData[2] = ((size shr 16) and 0xFFu).toUByte()
				fullData[3] = ((size shr 24) and 0xFFu).toUByte()
				idBytes.toUByteArray().copyInto(fullData, 4)

				return ConfigId(fullData)
			}
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is ConfigId) return false
			return data.contentEquals(other.data)
		}

		override fun hashCode(): Int = data.contentHashCode()
	}
}