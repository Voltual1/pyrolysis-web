package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.*
import org.duangsuse.bin.pat.basic.*
import org.duangsuse.bin.pat.extra.*
import org.duangsuse.bin.type.Cnt

object AxmlPatterns {
  /** AXML 块通用头部模式 */
  val chunkHeader = Seq(::ChunkHeader,
    int16.widen16(), // type
    int16.widen16(), // headerSize
    int32            // totalSize
  ).littleEndian()

  /** 字符串池头部模式 */
  val stringPoolHeader = Seq(::StringPoolHeader,
    int32, // stringCount
    int32, // styleCount
    int32, // flags
    int32, // stringsOffset
    int32  // stylesOffset
  ).littleEndian()

  /** 简单的 AXML 文件结构（头部 + 字符串池） */
  val axmlFileHeader = object : Pattern<Pair<ChunkHeader, StringPoolHeader>> {
    override fun read(s: org.duangsuse.bin.Reader): Pair<ChunkHeader, StringPoolHeader> {
      val fileHeader = chunkHeader.read(s)
      if (fileHeader.type != 0x0008) error("Not a valid AXML file")
      
      val poolHeader = chunkHeader.read(s)
      if (poolHeader.type != 0x0001) error("String pool not found")
      
      val poolInfo = stringPoolHeader.read(s)
      return fileHeader to poolInfo
    }

    override fun write(s: org.duangsuse.bin.Writer, x: Pair<ChunkHeader, StringPoolHeader>) {
      chunkHeader.write(s, x.first)
      chunkHeader.write(s, ChunkHeader(3).apply { 
        type = 0x0001; headerSize = 28; totalSize = 0 // 简化处理
      })
      stringPoolHeader.write(s, x.second)
    }

    override fun writeSize(x: Pair<ChunkHeader, StringPoolHeader>): Cnt {
      return 8L + 8L + 20L // Header + PoolHeader + PoolInfo
    }
  }
}