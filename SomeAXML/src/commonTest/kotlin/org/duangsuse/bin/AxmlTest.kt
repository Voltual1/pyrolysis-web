package org.duangsuse.bin.test

import kotlinx.io.*
import kotlinx.io.files.*
import org.duangsuse.bin.io.Reader
import org.duangsuse.bin.Nat8Reader
import org.duangsuse.bin.axml.AxmlPatterns
import org.duangsuse.bin.axml.AxmlExtractor
import kotlin.test.Test
import kotlin.test.assertEquals

class AxmlTest {
  @Test
  fun testParseAndroidManifestFile() {
    val path = Path("src/commonTest/resources/AndroidManifest.xml")
    if (!SystemFileSystem.exists(path)) return

    val fileSource = SystemFileSystem.source(path).buffered()
    val fileSize = SystemFileSystem.metadataOrNull(path)?.size ?: 0L

    val nat8Reader = object : Nat8Reader {
      override val source: Source = fileSource
      override val estimate: Long = fileSize
      override fun skip(n: Long) { source.skip(n) }
      override fun close() { source.close() }
    }

    val reader = Reader(nat8Reader)
    reader.byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian
    
    try {
      // 修正解构赋值：确保 axmlValidator 返回的是 Pair
      val result = AxmlPatterns.axmlValidator.read(reader)
      val fileHeader = result.first
      val poolInfo = result.second

      assertEquals(0x0003, fileHeader.type, "AXML Magic 应该是 0x0003")
      assertEquals(15712L, fileHeader.totalSize.toLong(), "文件总大小应匹配")
      assertEquals(125, poolInfo.stringCount, "字符串数量应匹配") 

      // 测试提取器
      reader.reset() // 需要 Reader 支持 Mark/Reset 或者重新打开
      // 这里为了简单，我们重新创建一个 Reader 进行提取测试
    } finally {
      reader.close()
    }
  }

  @Test
  fun testExtractAppInfo() {
    val path = Path("src/commonTest/resources/AndroidManifest.xml")
    if (!SystemFileSystem.exists(path)) return
    
    val fileSource = SystemFileSystem.source(path).buffered()
    val fileSize = SystemFileSystem.metadataOrNull(path)?.size ?: 0L
    val reader = Reader(object : Nat8Reader {
      override val source = fileSource
      override val estimate = fileSize
      override fun skip(n: Long) { source.skip(n) }
      override fun close() { source.close() }
    })

    val extractor = AxmlExtractor(reader)
    val info = extractor.extract()
    
    println("Extracted Label Res: ${info.labelRes?.toString(16)}")
    println("Extracted Icon Res: ${info.iconRes?.toString(16)}")
    
    // 根据 AndroidManifest.xml 的实际内容进行断言
    // assertEquals(0x7f100000, info.labelRes) 
    reader.close()
  }
}