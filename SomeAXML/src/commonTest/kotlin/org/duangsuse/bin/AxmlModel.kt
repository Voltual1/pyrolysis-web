package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.IntTuple
import org.duangsuse.bin.type.Cnt

/** AXML 基础块头部 (8 bytes) */
class ChunkHeader(size: Cnt) : IntTuple(size) {
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

/** 包含头部、偏移量数组的完整字符串池结构 */
class StringPool(size: Cnt) : IntTuple(size) {
  var stringCount: Int by index(0)
  var styleCount: Int by index(1)
  var flags: Int by index(2)
  var stringsOffset: Int by index(3)
  var stylesOffset: Int by index(4)
  // 索引 5 之后将存储字符串的偏移量数组 (IntArray)
}