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

    // 1. 文件头 (8 bytes)
    val fileHeader = AxmlPatterns.chunkHeader.read(reader)
    if (fileHeader.type != CHUNK_TYPE_AXML) return result
    val totalSize = fileHeader.totalSize.toLong()

    // 2. 迭代块
    while (reader.position < totalSize) {
      val currentChunkStart = reader.position
      val header = AxmlPatterns.chunkHeader.read(reader)
      val nextChunkPos = currentChunkStart + header.totalSize.toLong()

      when (header.type) {
        CHUNK_TYPE_STR_POOL -> {
          // 跳过字符串池
          reader.skip(header.totalSize.toLong() - 8L)
        }
        CHUNK_TYPE_RES_MAP -> {
          val count = (header.totalSize - 8) / 4
          resourceMap = IntArray(count) { reader.readInt32() }
        }
        CHUNK_TYPE_START_TAG -> {
          // 此时已经读了 8 字节 (ChunkHeader)，NodeHeader 还需要 12 字节
          // 我们手动读取后续字段，而不是 skip(-8)
          val lineNumber = reader.readInt32()
          val commentRef = reader.readInt32()
          
          // 读取 StartElementExt (24 bytes)
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
          
          // 如果找到了 application 的相关资源，可以继续找，也可以返回
          // 注意：AndroidManifest 中可能有多个 tag，这里简单处理
        }
        else -> {
          // 其他块直接跳过
        }
      }
      
      // 确保指针移动到下一个块的起始位置
      if (reader.position < nextChunkPos) {
        reader.skip(nextChunkPos - reader.position)
      } else if (reader.position > nextChunkPos) {
        // 如果读多了（理论上不应该），报错或修正
        // error("Read past chunk boundary")
      }
    }
    return result
  }
}