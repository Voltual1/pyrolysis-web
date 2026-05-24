package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.*
import org.duangsuse.bin.pat.basic.*
import org.duangsuse.bin.pat.extra.*
import org.duangsuse.bin.type.Cnt

object AxmlPatterns {
  /** 通用块头部模式 (T=Int) */
  val chunkHeader: Pattern.Sized<ChunkHeader> = Seq<ChunkHeader, Int>(
    ::ChunkHeader,
    int16.widen16(), // Pattern<Int>
    int16.widen16(), // Pattern<Int>
    int32            // Pattern<Int>
  ).littleEndian()

  /** 字符串池信息模式 (T=Int) */
  val stringPoolHeader: Pattern.Sized<StringPoolHeader> = Seq<StringPoolHeader, Int>(
    ::StringPoolHeader,
    int32, // stringCount
    int32, // styleCount
    int32, // flags
    int32, // stringsOffset
    int32  // stylesOffset
  ).littleEndian()

  /** AXML 验证模式 */
  val axmlValidator = object : Pattern<Pair<ChunkHeader, StringPoolHeader>> {
    override fun read(s: org.duangsuse.bin.Reader): Pair<ChunkHeader, StringPoolHeader> {
      val fileHeader = chunkHeader.read(s)
      if (fileHeader.type != 0x0003) error("Not a valid AXML file")
      
      val poolHeader = chunkHeader.read(s)
      if (poolHeader.type != 0x0001) error("String pool not found")
      
      val poolInfo = stringPoolHeader.read(s)
      return fileHeader to poolInfo
    }

    override fun write(s: org.duangsuse.bin.Writer, x: Pair<ChunkHeader, StringPoolHeader>) {
      chunkHeader.write(s, x.first)
      // 写入逻辑略
    }

    override fun writeSize(x: Pair<ChunkHeader, StringPoolHeader>): Cnt = 8L + 8L + 20L
  }
}