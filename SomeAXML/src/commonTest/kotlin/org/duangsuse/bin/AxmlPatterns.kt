package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.Pattern
import org.duangsuse.bin.pat.atom.*
import org.duangsuse.bin.pat.basic.*
import org.duangsuse.bin.pat.extra.*

object AxmlPatterns {
  val chunkHeader: Pattern.Sized<ChunkHeader> = Seq<ChunkHeader, Int>(
    ::ChunkHeader,
    int16.widen16(),
    int16.widen16(),
    int32
  ).littleEndian()

  val stringPoolHeader: Pattern.Sized<StringPoolHeader> = Seq<StringPoolHeader, Int>(
    ::StringPoolHeader,
    int32, int32, int32, int32, int32
  ).littleEndian()

  val nodeHeader: Pattern.Sized<NodeHeader> = Seq<NodeHeader, Int>(
    ::NodeHeader,
    int16.widen16(), int16.widen16(), int32, // ChunkHeader
    int32, // lineNumber
    int32  // commentRef
  ).littleEndian()

  val startElementExt: Pattern.Sized<StartElementExt> = Seq<StartElementExt, Int>(
    ::StartElementExt,
    int32, int32, 
    int16.widen16(), int16.widen16(), int16.widen16(),
    int16.widen16(), int16.widen16(), int16.widen16()
  ).littleEndian()

  val attributePattern: Pattern.Sized<Attribute> = Seq<Attribute, Int>(
    ::Attribute,
    int32, int32, int32, // ns, name, rawValue
    int32, // size + res0 + type
    int32  // data
  ).littleEndian()
}