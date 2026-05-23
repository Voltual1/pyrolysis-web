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
		@JvmStatic
		public fun write(
			sink: Sink,
			config: ArscConfig,
			writtenGlobalPool: ArscStringPool.WrittenPool,
			writtenKeyPool: ArscStringPool.WrittenPool
		) {
			val configBuffer = Buffer()
			
			// 1. 写入 Config 结构体
			configBuffer.writeU8(config.typeId)
			configBuffer.writeU8(0u) // zero
			configBuffer.writeU16(0u) // zero
			
			// 资源数量
			val maxSpecId = config.resources.maxOfOrNull { it.specId } ?: 0u
			val resourceCount = if (config.resources.isEmpty()) 0u else maxSpecId + 1u
			configBuffer.writeU32(resourceCount)

			// 记录 resourcesStart 偏移量的位置
			val resourcesStartOffset = configBuffer.size
			configBuffer.writeU32(0u) // 占位符

			// 2. 写入 Config ID (通常是 64 字节)
			configBuffer.write(config.configId.data.toByteArray())

			// 3. 写入资源项偏移数组
			val entriesBuffer = Buffer()
			val entryOffsets = UIntArray(resourceCount.toInt()) { UInt.MAX_VALUE }
			
			val resourceDataBuffer = Buffer()
			for (res in config.resources) {
				entryOffsets[res.specId.toInt()] = resourceDataBuffer.size.toUInt()
				ArscResource.write(resourceDataBuffer, res, writtenGlobalPool, writtenKeyPool)
			}

			// 修正 resourcesStart 偏移
			val finalResourcesStart = ArscHeader.size() + configBuffer.size + (resourceCount.toLong() * 4)
			// 由于 Buffer 无法随机访问，我们先构建偏移数组
			for (offset in entryOffsets) {
				configBuffer.writeU32(offset)
			}
			
			// 组装 Header
			val totalSize = ArscHeader.size() + configBuffer.size + resourceDataBuffer.size
			val header = ArscHeader(ArscHeaderType.TableType, ArscHeader.size().toUShort(), totalSize.toUInt())
			
			ArscHeader.write(sink, header)
			// 更新 resourcesStart (在 header 之后偏移 12 字节处，但我们直接流式写入)
			// 注意：ARSC 的 TableType 结构中 resourcesStart 是相对于 Chunk 起始位置的
			// 这里需要重新组织写入顺序以确保 resourcesStart 正确
			
			// 重新流式写入修正后的数据
			sink.writeU8(config.typeId)
			sink.writeByte(0)
			sink.writeShortLe(0)
			sink.writeU32(resourceCount)
			sink.writeU32(finalResourcesStart.toUInt())
			sink.write(config.configId.data.toByteArray())
			for (offset in entryOffsets) sink.writeU32(offset)
			sink.write(resourceDataBuffer, resourceDataBuffer.size)
		}
	}
}