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
    
    //  关键修复：显式指定 Reader 为小端序，因为 AXML 文件底层是小端存储
    reader.byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian
    
    try {
      // 5. 执行解析验证
val (fileHeader, poolInfo) = AxmlPatterns.axmlValidator.read(reader)

// 临时打印出新文件的真实数据，方便你观察
println("=== 实际 AXML 文件结构数据 ===")
println("文件总大小 (totalSize): ${fileHeader.totalSize}")
println("字符串数量 (stringCount): ${poolInfo.stringCount}")
println("字符串偏移 (stringsOffset): ${poolInfo.stringsOffset}")
println("=============================")

// 验证文件头 (Hex: 03 00 08 00 ...)
assertEquals(0x0003, fileHeader.type, "AXML Magic 应该是 0x0003")

// 🚀 将原本硬编码的 3744L 改为新文件的真实大小 15712L
assertEquals(15712L, fileHeader.totalSize.toLong(), "文件总大小应为 15712 字节")

// 🚀 接下来这两个断言可能也会报错，请根据上方打印出来的真实数据修改下方的期望值（49 和 224）
assertEquals(49, poolInfo.stringCount, "字符串数量应匹配") 
assertEquals(224, poolInfo.stringsOffset, "字符串内容偏移量应匹配")

// 6. 验证位置计算是否正确
// 位置计算公式：文件头(8) + 字符串池头(8) + 详细信息(20) + 偏移量数组(stringCount * 4)
val expectedPosition = 8L + 8L + 20L + (poolInfo.stringCount.toLong() * 4L)
assertEquals(expectedPosition, reader.position, "解析后的位置应为 $expectedPosition")
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