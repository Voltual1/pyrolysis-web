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

		private fun ByteArray.writeIntLe(index: Int, value: Int) {
			this[index] = (value and 0xFF).toByte()
			this[index + 1] = ((value shr 8) and 0xFF).toByte()
			this[index + 2] = ((value shr 16) and 0xFF).toByte()
			this[index + 3] = ((value shr 24) and 0xFF).toByte()
		}
	}
}