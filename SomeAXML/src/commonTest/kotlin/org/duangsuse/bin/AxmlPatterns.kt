package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.*
import org.duangsuse.bin.pat.basic.*
import org.duangsuse.bin.pat.extra.*
import org.duangsuse.bin.type.Cnt

object AxmlPatterns {
  /** 块头部模式 (Little Endian) */
  val chunkHeader: Pattern.Sized<ChunkHeader> = Seq<ChunkHeader, Int>(
    ::ChunkHeader,
    int16.widen16(),
    int16.widen16(),
    int32
  ).littleEndian()

  /** 字符串池详细信息模式 (Little Endian) */
  val stringPoolHeader: Pattern.Sized<StringPoolHeader> = Seq<StringPoolHeader, Int>(
    ::StringPoolHeader,
    int32, // stringCount
    int32, // styleCount
    int32, // flags
    int32, // stringsOffset
    int32  // stylesOffset
  ).littleEndian()

  /** 验证 AndroidManifest.xml 的核心模式 */
  val axmlValidator = object : Pattern<Pair<ChunkHeader, StringPoolHeader>> {
    override fun read(s: org.duangsuse.bin.Reader): Pair<ChunkHeader, StringPoolHeader> {
      // 1. 文件头 (Expected Type: 0x0003)
      val fileHeader = chunkHeader.read(s)
      if (fileHeader.type != 0x0003) error("Not a valid AXML file: ${fileHeader.type}")

      // 2. 字符串池头 (Expected Type: 0x0001)
      val poolHeader = chunkHeader.read(s)
      if (poolHeader.type != 0x0001) error("String pool chunk not found")

      // 3. 字符串池详细属性
      val poolInfo = stringPoolHeader.read(s)
      
      // 4. 跳过字符串偏移量数组 (测试 skip 和 estimate)
      val offsetsSize = poolInfo.stringCount.toLong() * 4L
      s.skip(offsetsSize)
      
      return fileHeader to poolInfo
    }

    override fun write(s: org.duangsuse.bin.Writer, x: Pair<ChunkHeader, StringPoolHeader>) {
      chunkHeader.write(s, x.first)
      stringPoolHeader.write(s, x.second)
    }

    override fun writeSize(x: Pair<ChunkHeader, StringPoolHeader>): Cnt = 8L + 20L
  }
}