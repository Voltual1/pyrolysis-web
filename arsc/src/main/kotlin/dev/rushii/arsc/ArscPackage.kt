package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscPackage(
	var id: UInt,
	var name: String,
	var types: MutableMap<ArscTypeName, ArscType>,
) {
	@ArscInternalApi
	public companion object {
		@JvmStatic
		public fun write(
			sink: Sink,
			pkg: ArscPackage,
			writtenGlobalPool: ArscStringPool.WrittenPool
		): Int {
			val pkgBuffer = Buffer()
			
			// 1. 基础信息
			pkgBuffer.writeU32(pkg.id)
			pkgBuffer.putStringUtf16(pkg.name, 256) // 128 chars * 2 bytes

			// 2. 准备字符串池
			val typeNames = pkg.types.keys.toList()
			val keyNames = pkg.types.values.flatMap { it.configs.flatMap { c -> c.resources.map { r -> r.name } } }.distinct()
			
			val typePool = ArscStringPool(typeNames, emptyList(), 0u)
			val keyPool = ArscStringPool(keyNames, emptyList(), ArscStringPool.UTF_8_FLAG)

			// 记录偏移量位置
			val typeStringsOffsetPos = pkgBuffer.size
			pkgBuffer.writeU32(0u) // typeStringsOffset
			pkgBuffer.writeU32(typeNames.size.toUInt()) // lastPublicType
			pkgBuffer.writeU32(0u) // keyStringsOffset
			pkgBuffer.writeU32(keyNames.size.toUInt()) // lastPublicKey
			pkgBuffer.writeU32(0u) // typeIdOffset (通常为 0)

			// 3. 写入 Pool 并记录真实偏移
			val typePoolOffset = ArscHeader.size() + pkgBuffer.size
			val writtenTypePool = ArscStringPool.write(pkgBuffer, typePool)
			
			val keyPoolOffset = ArscHeader.size() + pkgBuffer.size
			val writtenKeyPool = ArscStringPool.write(pkgBuffer, keyPool)

			// 4. 写入 TypeSpec 和 Type Chunks
			for (type in pkg.types.values) {
				// 写入 TypeSpec
				val specs = type.specs ?: continue
				val specBuffer = Buffer()
				specBuffer.writeU8(type.id.toUByte())
				specBuffer.writeU8(0u) // zero
				specBuffer.writeU16(0u) // zero
				specBuffer.writeU32(specs.specs.size.toUInt())
				for (i in 0u..<specs.specs.size.toUInt()) {
					specBuffer.writeU32(specs.specs[i]?.flags ?: 0u)
				}
				val specHeader = ArscHeader(ArscHeaderType.TableTypeSpec, ArscHeader.size().toUShort(), (ArscHeader.size() + specBuffer.size).toUInt())
				ArscHeader.write(pkgBuffer, specHeader)
				pkgBuffer.write(specBuffer, specBuffer.size)

				// 写入 Configs
				for (config in type.configs) {
					ArscConfig.write(pkgBuffer, config, writtenGlobalPool, writtenKeyPool)
				}
			}

			// 5. 组装最终 Package Chunk
			val totalSize = ArscHeader.size() + pkgBuffer.size
			val header = ArscHeader(ArscHeaderType.TablePackage, ArscHeader.size().toUShort(), totalSize.toUInt())
			
			ArscHeader.write(sink, header)
			
			// 注意：由于 Sink 不支持随机写，我们需要在写入 pkgBuffer 之前修正偏移量
			// 修正方法：将 pkgBuffer 读入内存 ByteArray，手动修改偏移量字节，再写入 sink
			val pkgArray = pkgBuffer.readByteArray()
			
			// 修改 typeStringsOffset (位于 pkgArray 的 4+256 = 260 字节处)
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