package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.IntTuple
import org.duangsuse.bin.type.Cnt

/** XML 节点通用头部 (16 bytes: ChunkHeader + line + comment) */
class NodeHeader(size: Cnt) : IntTuple(size) {
  var type: Int by index(0)
  var headerSize: Int by index(1)
  var totalSize: Int by index(2)
  var lineNumber: Int by index(3)
  var commentRef: Int by index(4)
}

/** 元素起始扩展头 (20 bytes) */
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
  // Res_value 结构
  var typedSize: Int by index(3) // 实际上是 uint16 size + uint8 res0 + uint8 dataType
  var typedData: Int by index(4)
  
  // 辅助获取 dataType (高 8 位中的低 8 位)
  fun getDataType(): Int = (typedSize shr 24) and 0xFF
}