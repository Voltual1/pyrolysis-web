package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.*
import org.duangsuse.bin.pat.basic.*
import org.duangsuse.bin.pat.extra.*
import org.duangsuse.bin.type.Cnt

object AxmlPatterns {
  /** 通用块头部模式 (Little Endian) */
  val chunkHeader = Seq(::ChunkHeader,
    int16.widen16(), // type
    int16.widen16(), // headerSize
    int32            // totalSize
  ).littleEndian()

  /** 字符串池信息模式 (Little Endian) */
  val stringPoolHeader = Seq(::StringPoolHeader,
    int32, // stringCount
    int32, // styleCount
    int32, // flags
    int32, // stringsOffset
    int32  // stylesOffset
  ).littleEndian()

  /** 简单的 AXML 结构验证器 */
  val axmlValidator = object : Pattern<Pair<ChunkHeader, StringPoolHeader>> {
    override fun read(s: org.duangsuse.bin.Reader): Pair<ChunkHeader, StringPoolHeader> {
      // 1. 读取文件总头部 (期待 type=0x0003)
      val fileHeader = chunkHeader.read(s)
      if (fileHeader.type != 0x0003) error("Not a valid AXML file: ${fileHeader.type}")

      // 2. 读取字符串池头部 (期待 type=0x0001)
      val poolHeader = chunkHeader.read(s)
      if (poolHeader.type != 0x0001) error("String pool not found")

      // 3. 读取字符串池详细信息
      val poolInfo = stringPoolHeader.read(s)
      return fileHeader to poolInfo
    }

    override fun write(s: org.duangsuse.bin.Writer, x: Pair<ChunkHeader, StringPoolHeader>) {
      chunkHeader.write(s, x.first)
      // 简化处理，仅用于测试 read
    }

    override fun writeSize(x: Pair<ChunkHeader, StringPoolHeader>): Cnt = 8L + 8L + 20L
  }
}