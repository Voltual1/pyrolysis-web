package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public class ArscFile(public val packages: List<ArscPackage>) {

	public fun finalize(): ByteArray {
		val globalBuffer = Buffer()
		
		// 1. 收集全局字符串
		val globalStrings = packages.flatMap { pkg ->
			pkg.types.values.flatMap { type ->
				type.configs.flatMap { cfg ->
					cfg.resources.mapNotNull { (it.value as? ArscValue.Plain.String)?.data }
				}
			}
		}.distinct()
		
		val globalStringPool = ArscStringPool(
			strings = globalStrings,
			styles = emptyList(),
			flags = ArscStringPool.UTF_8_FLAG
		)

		// 2. 写入 Package 数量
		globalBuffer.writeU32(packages.size.toUInt())

		// 3. 写入全局字符串池
		val writtenGlobalPool = ArscStringPool.write(globalBuffer, globalStringPool)

		// 4. 写入所有 Package
		for (pkg in packages) {
			ArscPackage.write(globalBuffer, pkg, writtenGlobalPool)
		}

		// 5. 写入主 Header
		val finalBuffer = Buffer()
		val totalSize = ArscHeader.size() + globalBuffer.size
		val mainHeader = ArscHeader(
			type = ArscHeaderType.Table,
			headerSize = ArscHeader.size().toUShort(),
			bodySize = totalSize.toUInt()
		)
		
		ArscHeader.write(finalBuffer, mainHeader)
		finalBuffer.write(globalBuffer, globalBuffer.size)
		
		return finalBuffer.readByteArray()
	}
}