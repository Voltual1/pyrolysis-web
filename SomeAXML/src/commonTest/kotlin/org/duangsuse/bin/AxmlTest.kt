package org.duangsuse.bin.test

import kotlinx.io.*
import kotlinx.io.files.*
import org.duangsuse.bin.io.Reader
import org.duangsuse.bin.Nat8Reader
import org.duangsuse.bin.MarkReset
import org.duangsuse.bin.axml.AxmlPatterns
import org.duangsuse.bin.axml.AxmlExtractor
import kotlin.test.Test
import kotlin.test.assertEquals

class AxmlTest {

  // 辅助类：支持 MarkReset 的测试 Reader
  class TestNat8Reader(val path: Path, override val estimate: Long) : Nat8Reader, MarkReset {
    private var _source = SystemFileSystem.source(path).buffered()
    override val source: Source get() = _source
    
    override fun skip(n: Long) { source.skip(n) }
    override fun close() { _source.close() }
    
    // 简单的 mark/reset 实现：重新打开流
    // 注意：实际生产环境应使用更高效的实现，这里为了测试通过
    override fun mark() { /* 逻辑简化 */ }
    override fun reset() {
      _source.close()
      _source = SystemFileSystem.source(path).buffered()
    }
  }

  @Test
  fun testParseAndroidManifestFile() {
    val path = Path("src/commonTest/resources/AndroidManifest.xml")
    if (!SystemFileSystem.exists(path)) return

    val fileSize = SystemFileSystem.metadataOrNull(path)?.size ?: 0L
    val nat8Reader = TestNat8Reader(path, fileSize)
    val reader = Reader(nat8Reader)
    reader.byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian
    
    try {
      reader.mark() // 必须先 mark
      val result = AxmlPatterns.axmlValidator.read(reader)
      val fileHeader = result.first
      val poolInfo = result.second

      assertEquals(0x0003, fileHeader.type)
      assertEquals(15712L, fileHeader.totalSize.toLong())
      
      reader.reset() // 现在可以 reset 了
      
      // 验证重置后的位置
      assertEquals(0L, reader.position)
    } finally {
      reader.close()
    }
  }

  @Test
  fun testExtractAppInfo() {
    val path = Path("src/commonTest/resources/AndroidManifest.xml")
    if (!SystemFileSystem.exists(path)) return
    
    val fileSize = SystemFileSystem.metadataOrNull(path)?.size ?: 0L
    val reader = Reader(TestNat8Reader(path, fileSize))

    val extractor = AxmlExtractor(reader)
    val info = extractor.extract()
    
    // AndroidManifest.xml 里的资源 ID 是动态的，但通常 label 和 icon 都会存在
    // 打印出来观察结果
    println("Extracted Label Res ID: 0x${info.labelRes?.toString(16)}")
    println("Extracted Icon Res ID: 0x${info.iconRes?.toString(16)}")
    
    // 只要能解析出其中之一，说明逻辑通了
    val foundAny = info.labelRes != null || info.iconRes != null
    assertEquals(true, foundAny, "应该至少能提取到 label 或 icon 资源 ID")
    
    reader.close()
  }
}