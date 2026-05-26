package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscConfig(
    public val typeId: UByte = 0u,
    public val configId: ConfigId,
    public var resources: MutableList<ArscResource>,
) {
    @ArscInternalApi
    public companion object {
        public fun parse(source: Source, globalStringPool: ArscStringPool, keyStringPool: ArscStringPool): ArscConfig {
            val typeId = source.readU8()
            source.readU8(); source.readU16()
            val resourceCount = source.readU32()
            source.readU32()
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
            val resourceCount = if (config.resources.isEmpty()) 0u else (config.resources.maxOf { it.specId } + 1u)
            val dataBuffer = Buffer()
            val offsets = UIntArray(resourceCount.toInt()) { UInt.MAX_VALUE }
            
            for (res in config.resources) {
                offsets[res.specId.toInt()] = dataBuffer.size.toUInt()
                ArscResource.write(dataBuffer, res, writtenGlobalPool, writtenKeyPool)
            }

            val configIdSize = config.configId.data.size
            val resStart = 8 + 12 + configIdSize + (resourceCount.toInt() * 4)
            val totalSize = resStart + dataBuffer.size.toInt()

            ArscHeader.write(sink, ArscHeader(ArscHeaderType.TableType, 8u.toUShort(), totalSize.toUInt()))
            sink.writeU8(config.typeId); sink.writeU8(0u); sink.writeU16(0u)
            sink.writeU32(resourceCount); sink.writeU32(resStart.toUInt())
            sink.write(config.configId.data.toByteArray())
            for (offset in offsets) sink.writeU32(offset)
            sink.write(dataBuffer, dataBuffer.size)
        }
    }

    public data class ConfigId(public var data: UByteArray) {
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
    }
}