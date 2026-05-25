package org.duangsuse.bin.axml

import org.duangsuse.bin.Reader
import org.duangsuse.bin.type.Int32

class AxmlExtractor(private val reader: Reader) {
  companion object {
    const val RES_ID_LABEL = 0x01010001
    const val RES_ID_ICON = 0x01010002
    const val CHUNK_TYPE_AXML = 0x0003
    const val CHUNK_TYPE_STR_POOL = 0x0001
    const val CHUNK_TYPE_RES_MAP = 0x0180
    const val CHUNK_TYPE_START_TAG = 0x0102
  }

  data class AppResourceInfo(var labelRes: Int32? = null, var iconRes: Int32? = null)

  private var resourceMap: IntArray = intArrayOf()

  fun extract(): AppResourceInfo {
    val result = AppResourceInfo()
    reader.byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian

    // 1. 文件头
    val fileHeader = AxmlPatterns.chunkHeader.read(reader)
    if (fileHeader.type != CHUNK_TYPE_AXML) return result
    val totalSize = fileHeader.totalSize.toLong()

    // 2. 迭代块
    while (reader.position < totalSize) {
      val currentPos = reader.position
      val header = AxmlPatterns.chunkHeader.read(reader)
      val nextChunkPos = currentPos + header.totalSize.toLong()

      when (header.type) {
        CHUNK_TYPE_STR_POOL -> {
          // 简单跳过字符串池内容
          reader.skip(header.totalSize.toLong() - 8L)
        }
        CHUNK_TYPE_RES_MAP -> {
          val count = (header.totalSize - 8) / 4
          resourceMap = IntArray(count) { reader.readInt32() }
        }
        CHUNK_TYPE_START_TAG -> {
          // 重新定位到块开头读取完整的 NodeHeader
          reader.skip(-8L)
          val node = AxmlPatterns.nodeHeader.read(reader)
          val ext = AxmlPatterns.startElementExt.read(reader)
          
          // 遍历属性
          for (i in 0 until ext.attributeCount) {
            val attr = AxmlPatterns.attributePattern.read(reader)
            if (attr.name >= 0 && attr.name < resourceMap.size) {
              val resId = resourceMap[attr.name]
              if (resId == RES_ID_LABEL) result.labelRes = attr.typedData
              if (resId == RES_ID_ICON) result.iconRes = attr.typedData
            }
          }
          
          if (result.labelRes != null && result.iconRes != null) return result
          reader.skip(nextChunkPos - reader.position)
        }
        else -> {
          reader.skip(header.totalSize.toLong() - 8L)
        }
      }
      
      // 强制对齐到下一个块，防止解析出错
      if (reader.position < nextChunkPos) {
        reader.skip(nextChunkPos - reader.position)
      }
    }
    return result
  }
}