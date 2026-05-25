package org.duangsuse.bin.axml

import org.duangsuse.bin.Reader
import org.duangsuse.bin.type.Int32

class AxmlExtractor(private val reader: Reader) {
    // Android 系统资源常量
    companion object {
        const val RES_ID_LABEL = 0x01010001
        const val RES_ID_ICON = 0x01010002
        
        const val CHUNK_TYPE_RES_MAP = 0x0180
        const val CHUNK_TYPE_START_TAG = 0x0102
        const val CHUNK_TYPE_END_TAG = 0x0103
    }

    private var resourceMap: IntArray = intArrayOf()

    data class AppInfo(var labelRes: Int32? = null, var iconRes: Int32? = null)

    fun extractAppLabelAndIcon(): AppInfo {
        val result = AppInfo()
        val strings = mutableListOf<String>() // 假设您已经有了解析字符串池内容的逻辑，这里简化

        // 1. 跳过文件头和字符串池头（已经在测试中演示过）
        val fileHeader = AxmlPatterns.chunkHeader.read(reader)
        val poolHeader = AxmlPatterns.chunkHeader.read(reader)
        val poolInfo = AxmlPatterns.stringPoolHeader.read(reader)
        
        // 跳过字符串池的具体内容（此处应根据 poolInfo.stringsOffset 跳转并读取，为了演示直接跳过）
        reader.skip(fileHeader.totalSize.toLong() - reader.position) 
        // 注意：实际解析中，需要先读完 StringPool 才能解析后面的内容
        // 这里假设 reader 已经定位到了 StringPool 之后的第一个 Chunk
        
        // 重置到字符串池之后开始寻找
        // 实际逻辑中，建议在测试中记录 poolHeader.totalSize
        
        while (reader.position < fileHeader.totalSize) {
            val header = AxmlPatterns.chunkHeader.read(reader)
            val chunkStart = reader.position - 8
            
            when (header.type) {
                CHUNK_TYPE_RES_MAP -> {
                    val count = (header.totalSize - 8) / 4
                    resourceMap = IntArray(count) { reader.readInt32() }
                }
                CHUNK_TYPE_START_TAG -> {
                    // 读取节点头（注意：NodeHeader 包含了 ChunkHeader，这里需要处理偏移）
                    // 重新从 chunkStart 读取完整的 NodeHeader
                    reader.skip(-8)
                    val node = AxmlPatterns.nodeHeader.read(reader)
                    val ext = AxmlPatterns.startElementExt.read(reader)
                    
                    // 我们需要一个方法从字符串池获取字符串，这里假设我们已知 "application" 的索引
                    // 或者通过 resourceMap 判断。但标签名通常不在 resourceMap 里，属性名在。
                    
                    // 遍历属性
                    for (i in 0 until ext.attributeCount) {
                        val attr = AxmlPatterns.attributePattern.read(reader)
                        
                        // 检查属性名是否在 resourceMap 中
                        if (attr.name < resourceMap.size) {
                            val resId = resourceMap[attr.name]
                            if (resId == RES_ID_LABEL) {
                                result.labelRes = attr.typedData
                            } else if (resId == RES_ID_ICON) {
                                result.iconRes = attr.typedData
                            }
                        }
                    }
                    
                    // 如果找到了就提前退出（可选）
                    if (result.labelRes != null && result.iconRes != null) return result
                    
                    // 跳转到块末尾
                    val remaining = (chunkStart + header.totalSize) - reader.position
                    if (remaining > 0) reader.skip(remaining)
                }
                else -> {
                    // 跳过不关心的块
                    reader.skip(header.totalSize.toLong() - 8)
                }
            }
        }
        return result
    }
}