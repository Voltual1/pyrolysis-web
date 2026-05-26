package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscPackage(
    public var id: UInt,
    public var name: String,
    public var types: MutableMap<ArscTypeName, ArscType>,
) {
    public fun highestTypeId(): UInt {
        return types.values.maxByOrNull { it.id }?.id ?: 1U
    }

    @ArscInternalApi
    public companion object {
        public fun parse(source: Source, globalStringPool: ArscStringPool): ArscPackage {
            val header = ArscHeader.parse(source, 0L)
            val bodySize = header.bodySize.toLong() - 8
            val body = Buffer()
            source.readTo(body, bodySize)

            val packageId = body.readU32()
            val packageName = body.readStringUtf16(128)

            repeat(5) { body.readU32() }

            val typeNames = ArscStringPool.parse(body)
            val keyNames = ArscStringPool.parse(body)

            val typesList = typeNames.strings.mapIndexed { idx, name ->
                ArscType(id = (idx + 1).toUInt(), name = name, configs = mutableListOf(), specs = null)
            }

            while (!body.exhausted()) {
                val chunkHeader = try { ArscHeader.parse(body, 0L) } catch (e: Exception) { break }
                val chunkBodySize = chunkHeader.bodySize.toLong() - 8
                if (chunkBodySize < 0) break

                when (chunkHeader.type) {
                    ArscHeaderType.TableTypeSpec -> {
                        val specs = ArscSpecs.parse(body)
                        val idx = specs.typeId.toInt() - 1
                        if (idx in typesList.indices) typesList[idx].specs = specs
                    }
                    ArscHeaderType.TableType -> {
                        val config = ArscConfig.parse(body, globalStringPool, keyNames)
                        val idx = config.typeId.toInt() - 1
                        if (idx in typesList.indices) typesList[idx].configs += config
                    }
                    else -> body.skip(chunkBodySize)
                }
                val padding = (4 - (chunkHeader.bodySize.toLong() % 4)) % 4
                if (padding > 0L && body.size >= padding) body.skip(padding)
            }

            return ArscPackage(id = packageId, name = packageName, types = typesList.associateBy { it.name }.toMutableMap())
        }

        public fun write(sink: Sink, pkg: ArscPackage, writtenGlobalPool: ArscStringPool.WrittenPool): Int {
            val pkgBuffer = Buffer()
            pkgBuffer.writeU32(pkg.id)
            pkgBuffer.putStringUtf16(pkg.name, 128)

            val typeNames = pkg.types.keys.toList()
            val keyNames = pkg.types.values.flatMap { it.configs.flatMap { c -> c.resources.map { r -> r.name } } }.distinct()
            
            val typePool = ArscStringPool(typeNames, emptyList(), 0u)
            val keyPool = ArscStringPool(keyNames, emptyList(), ArscStringPool.UTF_8_FLAG)

            repeat(5) { pkgBuffer.writeU32(0u) }

            ArscStringPool.write(pkgBuffer, typePool)
            val writtenKeyPool = ArscStringPool.write(pkgBuffer, keyPool)

            for (type in pkg.types.values) {
                val specs = type.specs ?: continue
                val specBuffer = Buffer()
                specBuffer.writeU8(type.id.toUByte())
                specBuffer.writeU8(0u); specBuffer.writeU16(0u)
                specBuffer.writeU32(specs.specs.size.toUInt())
                for (i in 0 until specs.specs.size) {
                    specBuffer.writeU32(specs.specs[i.toUInt()]?.flags ?: 0u)
                }
                ArscHeader.write(pkgBuffer, ArscHeader(ArscHeaderType.TableTypeSpec, 8u.toUShort(), (8 + specBuffer.size).toUInt()))
                pkgBuffer.write(specBuffer, specBuffer.size)

                for (config in type.configs) {
                    ArscConfig.write(pkgBuffer, config, writtenGlobalPool, writtenKeyPool)
                }
            }

            val totalSize = 8 + pkgBuffer.size.toInt()
            ArscHeader.write(sink, ArscHeader(ArscHeaderType.TablePackage, 8u.toUShort(), totalSize.toUInt()))
            sink.write(pkgBuffer, pkgBuffer.size)
            return totalSize
        }
    }
}