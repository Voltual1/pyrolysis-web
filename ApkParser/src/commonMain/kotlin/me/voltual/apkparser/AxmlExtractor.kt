package org.duangsuse.bin.axml

import org.duangsuse.bin.Reader
import org.duangsuse.bin.type.Int32

class AxmlExtractor(private val reader: Reader) {
  companion object {
    const val RES_ID_LABEL = 0x01010001
    const val RES_ID_ICON = 0x01010002
    const val RES_ID_VERSION_CODE = 0x0101021b
    const val RES_ID_VERSION_NAME = 0x0101021c
    
    const val CHUNK_TYPE_AXML = 0x0003
    const val CHUNK_TYPE_STR_POOL = 0x0001
    const val CHUNK_TYPE_RES_MAP = 0x0180
    const val CHUNK_TYPE_START_TAG = 0x0102
  }

  data class FullAppInfo(
    var labelRes: Int32? = null,
    var iconRes: Int32? = null,
    var packageName: String? = null,
    var versionCode: Int32? = null,
    var versionNameRes: Int32? = null,
    var versionNameRaw: String? = null
  )

  private var resourceMap: IntArray = intArrayOf()

  fun extract(): FullAppInfo {
    val result = FullAppInfo()
    reader.byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian

    val fileHeader = AxmlPatterns.chunkHeader.read(reader)
    if (fileHeader.type != CHUNK_TYPE_AXML) return result
    val totalSize = fileHeader.totalSize.toLong()

    while (reader.position < totalSize) {
      val currentChunkStart = reader.position
      val header = AxmlPatterns.chunkHeader.read(reader)
      val nextChunkPos = currentChunkStart + header.totalSize.toLong()

      when (header.type) {
        CHUNK_TYPE_STR_POOL -> {
            // 以后如果需要解析原始字符串（非资源引用），可以在这里解析 StringPool
            reader.skip(header.totalSize.toLong() - 8L)
        }
        CHUNK_TYPE_RES_MAP -> {
          val count = (header.totalSize - 8) / 4
          resourceMap = IntArray(count) { reader.readInt32() }
        }
        CHUNK_TYPE_START_TAG -> {
          reader.readInt32() // lineNumber
          reader.readInt32() // commentRef
          val ext = AxmlPatterns.startElementExt.read(reader)
          
          for (i in 0 until ext.attributeCount) {
            val attr = AxmlPatterns.attributePattern.read(reader)
            
            // 1. 处理带资源 ID 的属性 (Android 命名空间)
            if (attr.name >= 0 && attr.name < resourceMap.size) {
              val resId = resourceMap[attr.name]
              when (resId) {
                RES_ID_LABEL -> result.labelRes = attr.typedData
                RES_ID_ICON -> result.iconRes = attr.typedData
                RES_ID_VERSION_CODE -> result.versionCode = attr.typedData
                RES_ID_VERSION_NAME -> result.versionNameRes = attr.typedData
              }
            }
            
            // 2. 处理不带资源 ID 的属性 (如 manifest 标签的 package)
            // 注意：这里需要 StringPool 支持才能拿到 "package" 字符串，目前先跳过
          }
        }
        else -> { }
      }
      if (reader.position < nextChunkPos) reader.skip(nextChunkPos - reader.position)
    }
    return result
  }
}