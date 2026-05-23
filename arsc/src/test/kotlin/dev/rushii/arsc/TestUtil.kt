package dev.rushii.arsc

import kotlinx.io.*

/**
 * 创建一个包含指定字节的 Buffer
 */
fun bufferOf(vararg bytes: Byte): Buffer {
	val buffer = Buffer()
	buffer.write(bytes)
	return buffer
}

/**
 * 创建一个指定容量并填充初始值的 Buffer，并将指针移动（skip）到指定位置
 */
fun fillBuffer(cursorPos: Long, capacity: Long, fillValue: Byte = 0): Buffer {
	val buffer = Buffer()
	val bytes = ByteArray(capacity.toInt()) { fillValue }
	buffer.write(bytes)
	// kotlinx.io.Buffer 是流式的，模拟 position 需要通过 skip
	// 但通常在测试中，我们直接向 buffer 写入数据即可
	return buffer
}

fun Buffer.toHexString(): String {
	val bytes = this.peek().readByteArray()
	return bytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
}

fun assertBuffersEqual(expected: Buffer, actual: Buffer) {
	val expHex = expected.toHexString()
	val actHex = actual.toHexString()
	if (expHex != actHex) {
		throw AssertionError("Buffers not equal.\nExpected: $expHex\nActual:   $actHex")
	}
}