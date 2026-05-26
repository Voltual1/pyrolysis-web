package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.*
import org.duangsuse.bin.pat.basic.*
import org.duangsuse.bin.pat.extra.*
import org.duangsuse.bin.type.Int32

object AxmlPatterns {
  /** 块头部模式 */
  val chunkHeader: Pattern.Sized<ChunkHeader> = Seq<ChunkHeader, Int32>(
    { size -> ChunkHeader(size) },
    int16.widen16(),
    int16.widen16(),
    int32
  ).littleEndian()

  /** 字符串池详细信息模式 */
  val stringPoolHeader: Pattern.Sized<StringPoolHeader> = Seq<StringPoolHeader, Int32>(
    { size -> StringPoolHeader(size) },
    int32, int32, int32, int32, int32
  ).littleEndian()

  /** XML 节点头部模式 */
  val nodeHeader: Pattern.Sized<NodeHeader> = Seq<NodeHeader, Int32>(
    { size -> NodeHeader(size) },
    int16.widen16(), int16.widen16(), int32,
    int32, int32
  ).littleEndian()

  /** 元素起始扩展模式 */
  val startElementExt: Pattern.Sized<StartElementExt> = Seq<StartElementExt, Int32>(
    { size -> StartElementExt(size) },
    int32, int32,
    int16.widen16(), int16.widen16(), int16.widen16(),
    int16.widen16(), int16.widen16(), int16.widen16()
  ).littleEndian()

  /** 属性模式 */
  val attributePattern: Pattern.Sized<Attribute> = Seq<Attribute, Int32>(
    { size -> Attribute(size) },
    int32, int32, int32, int32, int32
  ).littleEndian()

  /** 之前测试用的验证器，修复解构支持 */
  val axmlValidator = object : Pattern<Pair<ChunkHeader, StringPoolHeader>> {
    override fun read(s: org.duangsuse.bin.Reader): Pair<ChunkHeader, StringPoolHeader> {
      val fHeader = chunkHeader.read(s)
      val pHeader = chunkHeader.read(s)
      val pInfo = stringPoolHeader.read(s)
      s.skip(pInfo.stringCount.toLong() * 4L)
      return fHeader to pInfo
    }
    override fun write(s: org.duangsuse.bin.Writer, x: Pair<ChunkHeader, StringPoolHeader>) {}
    override fun writeSize(x: Pair<ChunkHeader, StringPoolHeader>): Long = 0L
  }
}