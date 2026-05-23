package dev.rushii.arsc

import kotlinx.io.*

fun bufferOf(vararg bytes: Byte): Buffer {
	val buffer = Buffer()
	buffer.write(bytes)
	return buffer
}

fun Buffer.toHexString(): String {
	// 使用 peek() 避免消费掉 buffer 内容，同时明确指定读取长度
	val snapshot = this.peek()
	val bytes = snapshot.readByteArray(this.size.toInt())
	return bytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
}

fun assertBuffersEqual(expected: Buffer, actual: Buffer) {
	val expHex = expected.toHexString()
	val actHex = actual.toHexString()
	if (expHex != actHex) {
		throw AssertionError("Buffers not equal.\nExpected: $expHex\nActual:   $actHex")
	}
}