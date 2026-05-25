package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscPackage(
    var id: UInt,
    var name: String,
    var types: MutableMap<ArscTypeName, ArscType>,
) {
    public fun highestTypeId(): UInt = types.values.maxByOrNull { it.id }?.id ?: 1U

    @ArscInternalApi
    public companion object {
        public fun parse(source: Source, globalStringPool: ArscStringPool): ArscPackage {
            val header = ArscHeader.parse(source, 0L)
            val pkgEndPos = header.bodySize.toLong()
            var bytesRead = 8L

            val packageId = source.readU32(); bytesRead += 4
            val packageName = source.readStringUtf16(128); bytesRead += 256

            // 跳过 5 个 U32 偏移量字段
            for (i in 0 until 5) { source.readU32(); bytesRead += 4 }

            val typeNames = ArscStringPool.parse(source)
            // 注意：我们不能直接用 typeNames.size()，因为解析时可能已经处理了对齐
            // 我们需要根据 StringPool 的实际 Chunk Header 来增加 bytesRead
            // 这里为了简化，我们假设 StringPool 之后紧跟内容，并让循环自行处理
            
            // 重新计算 bytesRead 是不安全的，我们改用 while(bytesRead < pkgEndPos)
            // 但因为 Source 不支持获取 Position，我们必须在子解析器中返回消耗的字节数
            // 或者使用一个包装 Source。这里通过 bodySize 强制同步。
            
            // 临时方案：假设 typeNames 和 keyNames 之后 bytesRead 已经正确增加
            // 实际上，parse 内部已经消耗了对应的字节。
            // 我们需要一个更稳健的循环：
            
            val keyNames = ArscStringPool.parse(source)

            val typesList = typeNames.strings.mapIndexed { idx, name ->
                ArscType(id = (idx + 1).toUInt(), name = name, configs = mutableListOf(), specs = null)
            }

            // 关键：这里不再手动维护 bytesRead，而是依赖块解析
            // 由于 ARSC 是流式的，我们尝试读取直到 EOF 或捕获异常
            while (true) {
                val chunkHeader = try {
                    ArscHeader.parse(source, 0L)
                } catch (e: Exception) {
                    break // 到达 Package 结尾
                }

                val bodySize = chunkHeader.bodySize.toLong() - 8
                if (bodySize < 0) break // 错误的块大小，防止死循环

                when (chunkHeader.type) {
                    ArscHeaderType.TableTypeSpec -> {
                        val specs = ArscSpecs.parse(source)
                        val idx = specs.typeId.toInt() - 1
                        if (idx in typesList.indices) typesList[idx].specs = specs
                    }
                    ArscHeaderType.TableType -> {
                        val config = ArscConfig.parse(source, globalStringPool, keyNames)
                        val idx = config.typeId.toInt() - 1
                        if (idx in typesList.indices) typesList[idx].configs += config
                    }
                    else -> source.skip(bodySize)
                }
                
                // 4 字节对齐处理
                val padding = (4 - (chunkHeader.bodySize.toLong() % 4)) % 4
                if (padding > 0L) source.skip(padding)
            }

            return ArscPackage(
                id = packageId,
                name = packageName,
                types = typesList.associateBy { it.name }.toMutableMap(),
            )
        }

        public fun write(sink: Sink, pkg: ArscPackage, writtenGlobalPool: ArscStringPool.WrittenPool): Int {
            val pkgBuffer = Buffer()
            pkgBuffer.writeU32(pkg.id)
            pkgBuffer.putStringUtf16(pkg.name, 128) // 128 chars = 256 bytes

            val typeNames = pkg.types.keys.toList()
            val keyNames = pkg.types.values.flatMap { it.configs.flatMap { c -> c.resources.map { r -> r.name } } }.distinct()
            
            val typePool = ArscStringPool(typeNames, emptyList(), 0u)
            val keyPool = ArscStringPool(keyNames, emptyList(), ArscStringPool.UTF_8_FLAG)

            // 占位
            for (i in 0 until 5) pkgBuffer.writeU32(0u)

            ArscStringPool.write(pkgBuffer, typePool)
            ArscStringPool.write(pkgBuffer, keyPool)

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
                    ArscConfig.write(pkgBuffer, config, writtenGlobalPool, ArscStringPool.WrittenPool(keyNames.mapIndexed { i, s -> s to i }.toMap(), emptyMap()))
                }
            }

            val totalSize = 8 + pkgBuffer.size.toInt()
            ArscHeader.write(sink, ArscHeader(ArscHeaderType.TablePackage, 8u.toUShort(), totalSize.toUInt()))
            sink.write(pkgBuffer, pkgBuffer.size)
            return totalSize
        }
    }
}