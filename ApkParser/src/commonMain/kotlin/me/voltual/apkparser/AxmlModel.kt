package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.IntTuple
import org.duangsuse.bin.type.Cnt

/** AXML 基础块头部 (8 bytes) */
open class ChunkHeader(size: Cnt) : IntTuple(size) {
  var type: Int by index(0)
  var headerSize: Int by index(1)
  var totalSize: Int by index(2)
}

/** 字符串池头部 (20 bytes, 不含 ChunkHeader) */
class StringPoolHeader(size: Cnt) : IntTuple(size) {
  var stringCount: Int by index(0)
  var styleCount: Int by index(1)
  var flags: Int by index(2)
  var stringsOffset: Int by index(3)
  var stylesOffset: Int by index(4)
}

/** XML 节点通用头部 (20 bytes) */
class NodeHeader(size: Cnt) : IntTuple(size) {
  var type: Int by index(0)
  var headerSize: Int by index(1)
  var totalSize: Int by index(2)
  var lineNumber: Int by index(3)
  var commentRef: Int by index(4)
}

/** 元素起始扩展头 (24 bytes) */
class StartElementExt(size: Cnt) : IntTuple(size) {
  var ns: Int by index(0)
  var name: Int by index(1)
  var attributeStart: Int by index(2)
  var attributeSize: Int by index(3)
  var attributeCount: Int by index(4)
  var idIndex: Int by index(5)
  var classIndex: Int by index(6)
  var styleIndex: Int by index(7)
}

/** 属性结构 (20 bytes) */
class Attribute(size: Cnt) : IntTuple(size) {
  var ns: Int by index(0)
  var name: Int by index(1)
  var rawValue: Int by index(2)
  var typedSizeAndType: Int by index(3)
  var typedData: Int by index(4)
  
  val dataType: Int get() = (typedSizeAndType shr 24) and 0xFF
}