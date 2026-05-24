package dev.rushii.arsc

// 1. 导入 kotlinx.io 的内部 API 标记
import kotlinx.io.InternalIoApi
import kotlinx.io.*
import dev.rushii.arsc.internal.*

// 2. 告诉编译器：我知道这是内部 API，我接受风险，请让我通过
@OptIn(InternalIoApi::class)
public class ArscFile(public val packages: List<ArscPackage>) {

	public constructor(bytes: ByteArray) : this(Buffer().apply { write(bytes) })

	public constructor(source: Source) : this(parseContent(source))

	public fun finalize(): ByteArray {
		val globalBuffer = Buffer()
		val globalStrings = packages.flatMap { pkg ->
			pkg.types.values.flatMap { type ->
				type.configs.flatMap { cfg ->
					cfg.resources.mapNotNull { (it.value as? ArscValue.Plain.String)?.data }
				}
			}
		}.distinct()
		
		val globalStringPool = ArscStringPool(globalStrings, emptyList(), ArscStringPool.UTF_8_FLAG)
		globalBuffer.writeU32(packages.size.toUInt())
		val writtenGlobalPool = ArscStringPool.write(globalBuffer, globalStringPool)

		for (pkg in packages) {
			ArscPackage.write(globalBuffer, pkg, writtenGlobalPool)
		}

		val finalBuffer = Buffer()
		val header = ArscHeader(
			ArscHeaderType.Table, 
			ArscHeader.size().toUShort(), 
			(ArscHeader.size() + globalBuffer.size).toUInt()
		)
		ArscHeader.write(finalBuffer, header)
		finalBuffer.write(globalBuffer, globalBuffer.size)
		
		return finalBuffer.readByteArray()
	}

	override fun toString(): String = "Arsc[packages=$packages]"

	private companion object {
		/**
		 * 辅助函数：在构造函数完成前解析内容
		 */
		fun parseContent(source: Source): List<ArscPackage> {
			val header = ArscHeader.parse(source, 0L)
			val packageCount = source.readU32()
			val globalStringPool = ArscStringPool.parse(source)
			return List(packageCount.toInt()) {
				ArscPackage.parse(source, globalStringPool)
			}
		}
	}
}