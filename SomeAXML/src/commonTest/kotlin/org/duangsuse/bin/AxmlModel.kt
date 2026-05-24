package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.IntTuple
import org.duangsuse.bin.type.Cnt

/** AXML 基础块头部 (使用 IntTuple 确保 items 类型为 Int) */
class ChunkHeader(size: Cnt) : IntTuple(size) {
  var type: Int by index(0)
  var headerSize: Int by index(1)
  var totalSize: Int by index(2)
}

/** 字符串池头部 (使用 IntTuple) */
class StringPoolHeader(size: Cnt) : IntTuple(size) {
  var stringCount: Int by index(0)
  var styleCount: Int by index(1)
  var flags: Int by index(2)
  var stringsOffset: Int by index(3)
  var stylesOffset: Int by index(4)
}