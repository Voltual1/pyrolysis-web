package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscPackage(
    var id: UInt,
    var name: String,
    var types: MutableMap<ArscTypeName, ArscType>,
) {
    public fun highestTypeId(): UInt {
        return types.values.maxByOrNull { it.id }?.id ?: 1U
    }

    @ArscInternalApi
    public companion object {
        public fun parse(source: Source, globalStringPool: ArscStringPool): ArscPackage {
            val header = ArscHeader.parse(source, 0L)
            if (header.type != ArscHeaderType.TablePackage) {
                throw ArscError(0, header.type, "Expected TablePackage, got ${header.type}")
            }
            
            // 追踪在 Package 块内读取的字节数
            var bytesRead = 8L 

            val packageId = source.readU32(); bytesRead += 4
            val packageName = source.readStringUtf16(128); bytesRead += 256

            source.readU32() // typeStringsOffset
            source.readU32() // lastPublicType
            source.readU32() // keyStringOffset
            source.readU32() // lastPublicKey
            source.readU32() // typeIdsOffset
            bytesRead += 20

            val typeNames = ArscStringPool.parse(source)
            bytesRead += typeNames.size().toLong()
            
            val keyNames = ArscStringPool.parse(source)
            bytesRead += keyNames.size().toLong()

            val typesList = typeNames.strings.mapIndexed { idx, name ->
                ArscType(id = (idx + 1).toUInt(), name = name, configs = mutableListOf(), specs = null)
            }.toMutableList()

            // 循环解析子块直到 Package 块结束
            while (bytesRead < header.bodySize.toLong()) {
                val chunkHeader = ArscHeader.parse(source, bytesRead)
                val chunkStart = bytesRead
                
                if (chunkHeader.type == ArscHeaderType.Null) {
                    val skip = chunkHeader.bodySize.toLong() - 8
                    source.skip(skip)
                    bytesRead += chunkHeader.bodySize.toLong()
                    continue
                }

                when (chunkHeader.type) {
                    ArscHeaderType.TableTypeSpec -> {
                        val specs = ArscSpecs.parse(source)
                        val typeIndex = specs.typeId.toInt() - 1
                        if (typeIndex in typesList.indices) typesList[typeIndex].specs = specs
                    }
                    ArscHeaderType.TableType -> {
                        val config = ArscConfig.parse(source, globalStringPool, keyNames)
                        val typeIndex = config.typeId.toInt() - 1
                        if (typeIndex in typesList.indices) typesList[typeIndex].configs += config
                    }
                    else -> {
                        source.skip(chunkHeader.bodySize.toLong() - 8)
                    }
                }
                
                bytesRead += chunkHeader.bodySize.toLong()
                // 处理 4 字节对齐
                val padding = (4 - (chunkHeader.bodySize.toLong() % 4)) % 4
                if (padding > 0) {
                    source.skip(padding)
                    bytesRead += padding
                }
            }

            return ArscPackage(
                id = packageId,
                name = packageName,
                types = typesList.associateBy { it.name }.toMutableMap(),
            )
        }

        public fun write(
            sink: Sink,
            pkg: ArscPackage,
            writtenGlobalPool: ArscStringPool.WrittenPool
        ): Int {
            val pkgBuffer = Buffer()
            pkgBuffer.writeU32(pkg.id)
            pkgBuffer.putStringUtf16(pkg.name, 256)

            val typeNames = pkg.types.keys.toList()
            val keyNames = pkg.types.values.flatMap { it.configs.flatMap { c -> c.resources.map { r -> r.name } } }.distinct()
            
            val typePool = ArscStringPool(typeNames, emptyList(), 0u)
            val keyPool = ArscStringPool(keyNames, emptyList(), ArscStringPool.UTF_8_FLAG)

            pkgBuffer.writeU32(284u) // typeStringsOffset (8+256+20)
            pkgBuffer.writeU32(typeNames.size.toUInt())
            
            val tempTypePoolBuffer = Buffer()
            val writtenTypePool = ArscStringPool.write(tempTypePoolBuffer, typePool)
            val keyPoolOffset = 284u + tempTypePoolBuffer.size.toUInt()
            
            pkgBuffer.writeU32(keyPoolOffset)
            pkgBuffer.writeU32(keyNames.size.toUInt())
            pkgBuffer.writeU32(0u) // typeIdsOffset

            pkgBuffer.write(tempTypePoolBuffer, tempTypePoolBuffer.size)
            val writtenKeyPool = ArscStringPool.write(pkgBuffer, keyPool)

            for (type in pkg.types.values) {
                val specs = type.specs ?: continue
                val specBuffer = Buffer()
                specBuffer.writeU8(type.id.toUByte())
                specBuffer.writeU8(0u)
                specBuffer.writeU16(0u)
                specBuffer.writeU32(specs.specs.size.toUInt())
                for (i in 0u until specs.specs.size.toUInt()) {
                    specBuffer.writeU32(specs.specs[i]?.flags ?: 0u)
                }
                ArscHeader.write(pkgBuffer, ArscHeader(ArscHeaderType.TableTypeSpec, ArscHeader.size().toUShort(), (ArscHeader.size() + specBuffer.size).toUInt()))
                pkgBuffer.write(specBuffer, specBuffer.size)

                for (config in type.configs) {
                    ArscConfig.write(pkgBuffer, config, writtenGlobalPool, writtenKeyPool)
                }
            }

            val totalSize = ArscHeader.size() + pkgBuffer.size
            ArscHeader.write(sink, ArscHeader(ArscHeaderType.TablePackage, ArscHeader.size().toUShort(), totalSize.toUInt()))
            sink.write(pkgBuffer, pkgBuffer.size)
            return totalSize.toInt()
        }
    }
}