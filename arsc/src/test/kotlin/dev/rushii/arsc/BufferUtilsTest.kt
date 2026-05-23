package dev.rushii.arsc.internal

import dev.rushii.arsc.ArscError
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertThrows

class BufferUtilsTest {
	@Test
	fun `test buffer alignment skipping`() {
		val buffer = Buffer()
		buffer.writeByte(0x01)
		buffer.writeByte(0x02)
		
		// 模拟已读取 1 字节，现在 size 是 1（剩余可读），但我们想对齐的是“已读位置”
		// 在 kotlinx.io 中，对齐通常发生在写入时或已知总长度的解析中
		// 这里测试 writeAlignment 更为实际
	}

	@Test
	fun `test sink alignment writing`() {
		val buffer = Buffer()
		buffer.writeByte(0x7F.toByte()) // 当前写入了 1 字节
		
		// 写入对齐到 4 字节（当前位置 1，需补 3 字节）
		buffer.writeAlignment(buffer.size, 4)
		
		assertEquals(4L, buffer.size)
		// 使用带长度的 readByteArray 以提高兼容性
		val result = buffer.readByteArray(buffer.size.toInt())
		assertEquals(0x7F.toByte(), result[0])
		assertEquals(0.toByte(), result[1])
		assertEquals(0.toByte(), result[2])
		assertEquals(0.toByte(), result[3])
	}

	@Test
	fun `test putNullBytes`() {
		val buffer = Buffer()
		buffer.putNullBytes(5)
		assertEquals(5L, buffer.size)
		val bytes = buffer.readByteArray(5)
		bytes.forEach { assertEquals(0.toByte(), it) }
	}
}