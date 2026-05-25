package org.duangsuse.bin.test

import kotlinx.io.*
import kotlinx.io.files.*
import org.duangsuse.bin.io.Reader
import org.duangsuse.bin.Nat8Reader
import org.duangsuse.bin.axml.AxmlPatterns
import org.duangsuse.bin.type.Cnt
import org.duangsuse.bin.type.IdxRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AxmlTest {

  @Test
  fun testParseAndroidManifestFile() {
    // 1. 定位资源文件
    val path = Path("src/commonTest/resources/AndroidManifest.xml")
    if (!SystemFileSystem.exists(path)) {
      println("警告: 测试文件不存在于 $path，跳过测试")
      return
    }

    // 2. 使用 SystemFileSystem 打开 Source
    val fileSource = SystemFileSystem.source(path).buffered()
    val fileSize = SystemFileSystem.metadataOrNull(path)?.size ?: 0L

    // 3. 包装为 Nat8Reader
    val nat8Reader = object : Nat8Reader {
      override val source: Source = fileSource
      override val estimate: Cnt = fileSize
      override fun skip(n: Cnt) { source.skip(n) }
      override fun close() { source.close() }
    }
    
    val peekSource = SystemFileSystem.source(path).buffered()
    val bytes = ByteArray(8)
    peekSource.readTo(bytes)
    println("DEBUG HEX: ${bytes.joinToString(" ") { String.format("%02X", it) }}")
    peekSource.close()

    // 4. 初始化 Reader
    val reader = Reader(nat8Reader)
    
    // ⭐ 关键修复：显式指定 Reader 为小端序，因为 AXML 文件底层是小端存储
    reader.byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian
    
    try {
      // 5. 执行解析验证
      val (fileHeader, poolInfo) = AxmlPatterns.axmlValidator.read(reader)

      // 验证文件头 (Hex: 03 00 08 00 a0 0e 00 00)
      assertEquals(0x0003, fileHeader.type, "AXML Magic 应该是 0x0003")
      assertEquals(3744L, fileHeader.totalSize.toLong(), "文件总大小应为 3744 字节")

      // 验证字符串池 (Hex: 31 00 00 00 ... e0 00 00 00)
      assertEquals(49, poolInfo.stringCount, "字符串数量应为 49")
      assertEquals(224, poolInfo.stringsOffset, "字符串内容偏移量应为 224")

      // 6. 验证位置计算是否正确
      // 文件头(8) + 字符串池头(8) + 详细信息(20) + 偏移量数组(49*4=196) = 232
      assertEquals(232L, reader.position, "解析后的位置应为 232")
      
      println("AXML 集成测试成功！")
      println("Parsed Package Name Offset: ${poolInfo.stringsOffset}")

    } finally {
      reader.close()
    }
  }

  @Test
  fun testBufferWriterConsistency() {
    val writer = org.duangsuse.bin.io.BufferWriter()
    writer.byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian
    
    // 写入一个测试 Int
    writer.writeInt32(0x12345678)
    val bytes = writer.byteArray()
    
    // 验证小端序写入 (78 56 34 12)
    assertEquals(0x78.toByte(), bytes[0])
    assertEquals(0x56.toByte(), bytes[1])
    assertEquals(0x34.toByte(), bytes[2])
    assertEquals(0x12.toByte(), bytes[3])
    
    println("BufferWriter 字节序测试成功！")
  }
}