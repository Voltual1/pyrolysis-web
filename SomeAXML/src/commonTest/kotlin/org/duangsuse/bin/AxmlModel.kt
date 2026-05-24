package org.duangsuse.bin.axml

import org.duangsuse.bin.pat.Tuple
import org.duangsuse.bin.type.Cnt

/** AXML 基础块头部 (8 bytes) */
class ChunkHeader(size: Cnt) : Tuple<Any>(size) {
  var type: Int by index(0)
  var headerSize: Int by index(1)
  var totalSize: Int by index(2)
}

/** 字符串池头部 (20 bytes, 不含 ChunkHeader) */
class StringPoolHeader(size: Cnt) : Tuple<Any>(size) {
  var stringCount: Int by index(0)
  var styleCount: Int by index(1)
  var flags: Int by index(2)
  var stringsOffset: Int by index(3)
  var stylesOffset: Int by index(4)
}