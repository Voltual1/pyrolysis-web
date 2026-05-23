package dev.rushii.arsc

import dev.rushii.arsc.internal.align
import dev.rushii.arsc.internal.writeAlignment
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferUtilsTest {
	@Test
	fun `test buffer alignment skipping`() {
		val buffer = Buffer()
		buffer.write(ByteArray(1)) // pos = 1
		
		// 对齐到 4 字节，应该跳过 3 字节
		buffer.align(4)
		// 注意：kotlinx.io.Buffer 的 align 是通过 skip 实现的，
		// 在读取模式下，它会改变 buffer.size
		assertEquals(0L, buffer.size) // 假设 buffer 原本只有 1 字节，skip(3) 会耗尽或报错
	}

	@Test
	fun `test sink alignment writing`() {
		val buffer = Buffer()
		buffer.writeByte(0x7F.toByte()) // size = 1
		
		// 写入对齐到 4 字节
		buffer.writeAlignment(buffer.size, 4)
		
		assertEquals(4L, buffer.size)
		val result = buffer.readByteArray()
		assertEquals(0x7F.toByte(), result[0])
		assertEquals(0.toByte(), result[1])
		assertEquals(0.toByte(), result[2])
		assertEquals(0.toByte(), result[3])
	}
}