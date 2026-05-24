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

class AxmlTest {

  @Test
  fun testParseManifestHeader() {
    // 假设文件已经放在 commonTest/resources/AndroidManifest.xml
    // 这里我们使用 kotlinx-io 的方式打开它（具体取决于平台实现，这里展示核心逻辑）
    val path = Path("src/commonTest/resources/AndroidManifest.xml")
    
    // 包装 kotlinx.io 的 Source 为我们的 Nat8Reader
    val fileSource = SystemFileSystem.source(path).buffered()
    
    val nat8Reader = object : Nat8Reader {
      override val source: Source = fileSource
      override val estimate: Cnt get() = 3744L // 根据 hex dump 已知大小
      override fun skip(n: Cnt) { source.skip(n) }
      override fun close() { source.close() }
    }

    val reader = Reader(nat8Reader)
    
    // 执行解析
    val (fileHeader, poolInfo) = AxmlPatterns.axmlFileHeader.read(reader)

    // 验证文件头 (03 00 08 00 a0 0e 00 00)
    assertEquals(0x0008, fileHeader.type)
    assertEquals(0x0EA0, fileHeader.totalSize) // 3744 字节

    // 验证字符串池头 (01 00 1c 00 ...)
    assertEquals(49, poolInfo.stringCount) // 31 00 00 00 -> 49
    assertEquals(0, poolInfo.styleCount)
    assertEquals(224, poolInfo.stringsOffset) // e0 00 00 00 -> 224

    println("AXML 解析测试通过！总大小: ${fileHeader.totalSize}, 字符串数: ${poolInfo.stringCount}")
    
    fileSource.close()
  }
}