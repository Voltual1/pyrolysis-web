@file:Suppress("NOTHING_TO_INLINE")

package dev.rushii.arsc.internal

import dev.rushii.arsc.ArscError
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString

internal inline fun Source.readU8(): UByte = readByte().toUByte()
internal inline fun Source.readU16(): UShort = readShortLe().toUShort()
internal inline fun Source.readU32(): UInt = readIntLe().toUInt()

internal inline fun Sink.writeU8(value: UByte) = writeByte(value.toByte())
internal inline fun Sink.writeU16(value: UShort) = writeShortLe(value.toShort())
internal inline fun Sink.writeU32(value: UInt) = writeIntLe(value.toInt())

/**
 * 由于 kotlinx.io.Source 没有 position()，我们需要在解析 chunk 时手动跟踪偏移量，
 * 或者在 Buffer 中通过 size 差异来计算。
 * 这里提供一个针对 Buffer 的对齐工具。
 */
internal fun Buffer.align(alignment: Int) {
    val remaining = size % alignment
    if (remaining > 0L) {
        val padding = alignment - remaining.toInt()
        // 在读取模式下，跳过字节
        skip(padding.toLong())
    }
}

internal fun Sink.writeAlignment(currentSize: Long, alignment: Int) {
    val remaining = currentSize % alignment
    if (remaining > 0L) {
        val target = alignment - remaining.toInt()
        putNullBytes(target)
    }
}

internal fun Sink.putNullBytes(amount: Int) {
    for (i in 0..<amount)
        writeByte(0)
}

/**
 * 读取 UTF16-LE 字符串。
 * @param charSize 字符数量。
 */
internal fun Source.readStringUtf16(charSize: Int): String {
    val byteCount = charSize * 2
    val bytes = readByteArray(byteCount)
    
    // 找到第一个 null 终止符 (0x00 0x00)
    var stringBytesSize = byteCount
    for (i in 0 until byteCount - 1 step 2) {
        if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte()) {
            stringBytesSize = i
            break
        }
    }
    
    // 使用 ByteArray 构造，注意在 KMP 中可能需要特定处理，
    // 这里假设目标是 JVM 或有相应的解码支持。
    // kotlinx-io 暂无直接的 Charset 支持，通常使用 ByteString 或平台特定转换。
    return bytes.decodeToString(0, stringBytesSize, charset = "UTF-16LE")
}

// 辅助函数用于解码特定编码
private fun ByteArray.decodeToString(startIndex: Int, endIndex: Int, charset: String): String {
    // 在纯 Kotlin (KMP) 环境下，通常依赖平台实现。
    // 这里为了保持逻辑，我们假设环境支持 charset 参数或手动处理。
    // 如果是纯 Kotlin 且不依赖 JVM，UTF-16LE 需要手动按位拼接。
    return String(this, startIndex, endIndex - startIndex, Charsets.forName(charset))
}

internal fun Sink.putStringUtf16(string: String, outSize: Int) {
    val bytes = string.toByteArray(Charsets.forName("UTF-16LE"))
    if (bytes.size > outSize)
        throw IllegalArgumentException("outSize is smaller than utf-16le bytes of string")

    write(bytes)
    putNullBytes(outSize - bytes.size)
}