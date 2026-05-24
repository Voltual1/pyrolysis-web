package org.duangsuse.bin.test

import kotlinx.io.*
import org.duangsuse.bin.io.Reader
import org.duangsuse.bin.Nat8Reader
import org.duangsuse.bin.axml.AxmlPatterns
import org.duangsuse.bin.type.Cnt
import org.duangsuse.bin.type.IdxRange
import kotlin.test.Test
import kotlin.test.assertEquals

class AxmlTest {

  @Test
  fun testParseAndroidManifest() {
    // 这里模拟从资源加载 Source。
    // 在 commonTest 中，我们通常通过平台特定的方式获取 Source。
    // 这里假设我们已经拿到了 Source（例如通过 kotlinx-io 的 FileSystem）
    
    // 为了演示，我们直接用您提供的 Hex Dump 前几个关键字节构建一个 Buffer
    val buffer = Buffer()
    // 03 00 08 00 a0 0e 00 00 (File Header)
    // 01 00 1c 00 1c 08 00 00 (String Pool Chunk Header)
    // 31 00 00 00 00 00 00 00 00 00 00 00 e0 00 00 00 (String Pool Info)
    val hexData = byteArrayOf(
      0x03, 0x00, 0x08, 0x00, 0xA0.toByte(), 0x0E, 0x00, 0x00,
      0x01, 0x00, 0x1C, 0x00, 0x1C, 0x08, 0x00, 0x00,
      0x31, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0xE0.toByte(), 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00 // stylesOffset
    )
    buffer.write(hexData)

    val nat8Reader = object : Nat8Reader {
      override val source: Source = buffer
      override val estimate: Cnt = buffer.size
      override fun skip(n: Cnt) { source.skip(n) }
      override fun close() { source.close() }
    }

    val reader = Reader(nat8Reader)
    
    // 执行解析
    val (fileHeader, poolInfo) = AxmlPatterns.axmlValidator.read(reader)

    // 验证文件头
    // Hex: 03 00 08 00 -> Type 3
    assertEquals(3, fileHeader.type)
    // Hex: a0 0e 00 00 -> 0x0EA0 = 3744 bytes
    assertEquals(3744L, fileHeader.totalSize.toLong())

    // 验证字符串池信息
    // Hex: 31 00 00 00 -> 49 strings
    assertEquals(49, poolInfo.stringCount)
    // Hex: e0 00 00 00 -> Offset 224
    assertEquals(224, poolInfo.stringsOffset)

    println("AXML 头部验证成功！")
    println("文件大小: ${fileHeader.totalSize}")
    println("字符串数量: ${poolInfo.stringCount}")
  }
}