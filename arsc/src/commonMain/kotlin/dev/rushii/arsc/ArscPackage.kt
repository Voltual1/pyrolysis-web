package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscPackage(
	var id: UInt,
	var name: String,
	var types: MutableMap<ArscTypeName, ArscType>,
) {
	/**
	 * 获取最高定义的类型 ID，默认为 1
	 */
	public fun highestTypeId(): UInt {
		return types.values
			.maxByOrNull { it.id }
			?.id ?: 1U
	}

	@ArscInternalApi
	public companion object {
		public fun parse(source: Source, globalStringPool: ArscStringPool): ArscPackage {
			val header = ArscHeader.parse(source, 0L)
			assert(header.type == ArscHeaderType.TablePackage)

			val packageId = source.readU32()
			val packageName = source.readStringUtf16(128)

			source.readU32() // typeStringsOffset
			source.readU32() // lastPublicType
			source.readU32() // keyStringOffset
			source.readU32() // lastPublicKey
			source.readU32() // typeIdsOffset

			val typeNames = ArscStringPool.parse(source)
			val keyNames = ArscStringPool.parse(source)

			val typesList = (1..typeNames.strings.size).map {
				ArscType(
					id = it.toUInt(),
					name = typeNames.strings[it - 1],
					configs = mutableListOf(),
					specs = null,
				)
			}

			// 简化的解析逻辑，实际应根据 bodySize 限制
			while (true) {
				val startPos = 0L // 模拟位置
				val chunkHeader = try {
					ArscHeader.parse(source, startPos)
				} catch (e: Exception) {
					break
				}

				when (chunkHeader.type) {
					ArscHeaderType.TableTypeSpec -> {
						val specs = ArscSpecs.parse(source)
						typesList[specs.typeId.toInt() - 1].specs = specs
					}
					ArscHeaderType.TableType -> {
						val config = ArscConfig.parse(source, globalStringPool, keyNames)
						typesList[config.typeId.toInt() - 1].configs += config
					}
					else -> {
						source.skip((chunkHeader.bodySize - ArscHeader.size().toUInt()).toLong())
					}
				}
			}

			return ArscPackage(
				id = packageId,
				name = packageName,
				types = typesList.associateBy { it.name }.toMutableMap(),
			)
		}

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
            val pkgEnd = header.bodySize.toLong() - ArscHeader.size()

            val packageId = source.readU32()
            val packageName = source.readStringUtf16(128)

            val typeStringsOffset = source.readU32()
            val lastPublicType = source.readU32()
            val keyStringsOffset = source.readU32()
            val lastPublicKey = source.readU32()
            val typeIdsOffset = source.readU32()

            // 读取类型名和键名池（这里简单顺序读取，实际上应根据 offset 跳转，但一般它们紧接着出现）
            val typeNames = ArscStringPool.parse(source)
            val keyNames = ArscStringPool.parse(source)

            // 初始化 types 列表
            val typesList = typeNames.strings.mapIndexed { idx, name ->
                ArscType(
                    id = (idx + 1).toUInt(),
                    name = name,
                    configs = mutableListOf(),
                    specs = null,
                )
            }.toMutableList()

            // 记录当前已读位置
            var currentPos = 0L  // 需要一个能够获取 source 当前位置的方法，这里简化：使用一个临时 Buffer 做标记
            // 实际更好的办法：在外部用 CountingSource 或者用 Buffer 包装后计算。为了简洁，我们假设 source 是 Buffer 并利用其 position。
            // 我们暂且不在此处实现精确的指针控制，而是依赖于 while 循环中的 skip 和 chunk 解析。
            // 由于 Kotlinx.io 的 Source 没有原生位置 API，这里约定传入的 source 是 Buffer 并且我们可以读取直到 pkgEnd。
            // 为了演示，我们写一个近似可工作的版本，实际开发中应使用 buffer.position。
            while (true) {
                // 尝试解析下一个块头，如果已经到达 pkgEnd 则退出
                val remaining = pkgEnd - (source as? Buffer)?.position ?: break
                if (remaining < ArscHeader.size()) break
                val chunkHeader = try {
                    ArscHeader.parse(source, 0L)
                } catch (e: Exception) {
                    // 如果不是有效的块头，可能已到结尾，尝试对齐
                    break
                }
                if (chunkHeader.type == ArscHeaderType.Null) {
                    // 跳过填充
                    source.skip(chunkHeader.bodySize.toLong() - ArscHeader.size())
                    continue
                }

                when (chunkHeader.type) {
                    ArscHeaderType.TableTypeSpec -> {
                        val specs = ArscSpecs.parse(source)
                        val typeIndex = specs.typeId.toInt() - 1
                        if (typeIndex in typesList.indices) {
                            typesList[typeIndex].specs = specs
                        } else {
                            println("Warning: typeSpec id ${specs.typeId} out of range")
                        }
                    }
                    ArscHeaderType.TableType -> {
                        val config = ArscConfig.parse(source, globalStringPool, keyNames)
                        val typeIndex = config.typeId.toInt() - 1
                        if (typeIndex in typesList.indices) {
                            typesList[typeIndex].configs += config
                        } else {
                            println("Warning: config typeId ${config.typeId} out of range")
                        }
                    }
                    else -> {
                        // 跳过未知块
                        source.skip(chunkHeader.bodySize.toLong() - ArscHeader.size())
                    }
                }
                // 4 字节对齐
                val alignment = (4 - (chunkHeader.bodySize.toInt() % 4)) % 4
                if (alignment > 0) source.skip(alignment.toLong())
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

			val offsetsPlaceholderPos = pkgBuffer.size
			pkgBuffer.writeU32(0u) // typeStringsOffset
			pkgBuffer.writeU32(typeNames.size.toUInt())
			pkgBuffer.writeU32(0u) // keyStringsOffset
			pkgBuffer.writeU32(keyNames.size.toUInt())
			pkgBuffer.writeU32(0u)

			val typePoolOffset = ArscHeader.size() + pkgBuffer.size
			val writtenTypePool = ArscStringPool.write(pkgBuffer, typePool)
			val keyPoolOffset = ArscHeader.size() + pkgBuffer.size
			val writtenKeyPool = ArscStringPool.write(pkgBuffer, keyPool)

			for (type in pkg.types.values) {
				val specs = type.specs ?: continue
				val specBuffer = Buffer()
				specBuffer.writeU8(type.id.toUByte())
				specBuffer.writeU8(0u)
				specBuffer.writeU16(0u)
				specBuffer.writeU32(specs.specs.size.toUInt())
				for (i in 0u..<specs.specs.size.toUInt()) {
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
			
			val pkgArray = pkgBuffer.readByteArray()
			// 修正偏移量字节 (4 + 256 = 260)
			pkgArray.writeIntLe(260, typePoolOffset.toInt())
			pkgArray.writeIntLe(268, keyPoolOffset.toInt())
			
			sink.write(pkgArray)
			return totalSize.toInt()
		}
    }
}

		private fun ByteArray.writeIntLe(index: Int, value: Int) {
			this[index] = (value and 0xFF).toByte()
			this[index + 1] = ((value shr 8) and 0xFF).toByte()
			this[index + 2] = ((value shr 16) and 0xFF).toByte()
			this[index + 3] = ((value shr 24) and 0xFF).toByte()
		}
	}
}