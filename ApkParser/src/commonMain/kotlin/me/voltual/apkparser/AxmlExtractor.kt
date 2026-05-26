package org.duangsuse.bin.axml

import org.duangsuse.bin.Reader
import org.duangsuse.bin.type.Int32
import dev.rushii.arsc.internal.ArscStringPool
import dev.rushii.arsc.internal.ArscInternalApi

@OptIn(ArscInternalApi::class)
class AxmlExtractor(private val reader: Reader) {
    companion object {
        const val RES_ID_LABEL = 0x01010001
        const val RES_ID_ICON = 0x01010002
        const val RES_ID_PACKAGE = 0x0101021b // 实际上 package 属性通常没有资源 ID，它是原始属性
        
        const val TYPE_REFERENCE = 0x01
        const val TYPE_STRING = 0x03

        const val CHUNK_TYPE_AXML = 0x0003
        const val CHUNK_TYPE_STR_POOL = 0x0001
        const val CHUNK_TYPE_RES_MAP = 0x0180
        const val CHUNK_TYPE_START_TAG = 0x0102
    }

    data class FullAppInfo(
        var labelRes: Int32? = null,
        var labelRaw: String? = null,
        var iconRes: Int32? = null,
        var iconRaw: String? = null,
        var packageName: String? = null
    )

    private var resourceMap: IntArray = intArrayOf()
    private var axmlStrings: List<String> = emptyList()

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
                    // 关键修复：解析 AXML 内部的字符串池
                    // 我们需要将 reader 回退 8 字节，因为 ArscStringPool.parse 会读取 Header
                    // 但由于 reader.skip 不支持负数，我们直接用 source 重新解析
                    // 这里的技巧是：ArscStringPool.parse 接受 Source
                    // 我们使用 reader.asNat8Reader().source，但它现在的指针在 Header 之后
                    // 所以我们需要一个能解析“已经读了 Header”的 StringPool 方法，或者手动构造
                    
                    // 简单方案：由于我们已经有了 ArscStringPool.parse，
                    // 我们直接在 ApkParser 层面先解析出 AXML 的字符串池，或者在这里特殊处理
                    // 这里我们假设 reader 的 source 可以重新定位（如果是 Buffer 的话）
                    
                    // 为了不破坏流水线，我们在这里手动跳过，在 ApkParser 里统一处理
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
                    
                    // 记录标签名（如果是 manifest 或 application）
                    val tagName = axmlStrings.getOrNull(ext.name)

                    for (i in 0 until ext.attributeCount) {
                        val attr = AxmlPatterns.attributePattern.read(reader)
                        val attrName = axmlStrings.getOrNull(attr.name)
                        val resId = if (attr.name >= 0 && attr.name < resourceMap.size) resourceMap[attr.name] else -1

                        when {
                            resId == RES_ID_LABEL || attrName == "label" -> {
                                if (attr.dataType == TYPE_STRING) {
                                    result.labelRaw = axmlStrings.getOrNull(attr.typedData)
                                } else if (attr.dataType == TYPE_REFERENCE) {
                                    result.labelRes = attr.typedData
                                }
                            }
                            resId == RES_ID_ICON || attrName == "icon" -> {
                                if (attr.dataType == TYPE_STRING) {
                                    result.iconRaw = axmlStrings.getOrNull(attr.typedData)
                                } else if (attr.dataType == TYPE_REFERENCE) {
                                    result.iconRes = attr.typedData
                                }
                            }
                            attrName == "package" && tagName == "manifest" -> {
                                result.packageName = axmlStrings.getOrNull(attr.typedData)
                            }
                        }
                    }
                }
                else -> {
                    reader.skip(header.totalSize.toLong() - 8L)
                }
            }
            if (reader.position < nextChunkPos) reader.skip(nextChunkPos - reader.position)
        }
        return result
    }

    // 注入解析好的字符串池
    fun setAxmlStrings(strings: List<String>) {
        this.axmlStrings = strings
    }
}