package org.duangsuse.bin.axml

import org.duangsuse.bin.Reader
import org.duangsuse.bin.type.Int32
import dev.rushii.arsc.internal.ArscInternalApi

@OptIn(ArscInternalApi::class)
class AxmlExtractor(private val reader: Reader) {
    companion object {
        const val RES_ID_LABEL = 0x01010001
        const val RES_ID_ICON = 0x01010002
        const val RES_ID_VERSION_CODE = 0x0101021b
        const val RES_ID_VERSION_NAME = 0x0101021c
        
        const val TYPE_REFERENCE = 0x01
        const val TYPE_STRING = 0x03

        const val CHUNK_TYPE_AXML = 0x0003
        const val CHUNK_TYPE_RES_MAP = 0x0180
        const val CHUNK_TYPE_START_TAG = 0x0102
    }

    data class FullAppInfo(
        var labelRes: Int32? = null,
        var labelRaw: String? = null,
        var iconRes: Int32? = null,
        var iconRaw: String? = null,
        var packageName: String? = null,
        var versionCode: Long? = null,
        var versionNameRaw: String? = null,
        var versionNameRes: Int32? = null
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
                CHUNK_TYPE_RES_MAP -> {
                    val count = (header.totalSize - 8) / 4
                    resourceMap = IntArray(count) { reader.readInt32() }
                }
                CHUNK_TYPE_START_TAG -> {
                    reader.readInt32() // lineNumber
                    reader.readInt32() // commentRef
                    val ext = AxmlPatterns.startElementExt.read(reader)
                    val tagName = axmlStrings.getOrNull(ext.name)

                    for (i in 0 until ext.attributeCount) {
                        val attr = AxmlPatterns.attributePattern.read(reader)
                        val attrName = axmlStrings.getOrNull(attr.name)
                        val resId = if (attr.name >= 0 && attr.name < resourceMap.size) resourceMap[attr.name] else -1

                        when {
                            resId == RES_ID_LABEL || attrName == "label" -> {
                                if (attr.dataType == TYPE_STRING) result.labelRaw = axmlStrings.getOrNull(attr.typedData)
                                else if (attr.dataType == TYPE_REFERENCE) result.labelRes = attr.typedData
                            }
                            resId == RES_ID_ICON || attrName == "icon" -> {
                                if (attr.dataType == TYPE_STRING) result.iconRaw = axmlStrings.getOrNull(attr.typedData)
                                else if (attr.dataType == TYPE_REFERENCE) result.iconRes = attr.typedData
                            }
                            resId == RES_ID_VERSION_CODE || attrName == "versionCode" -> {
                                // versionCode 几乎总是整型
                                result.versionCode = attr.typedData.toLong()
                            }
                            resId == RES_ID_VERSION_NAME || attrName == "versionName" -> {
                                if (attr.dataType == TYPE_STRING) result.versionNameRaw = axmlStrings.getOrNull(attr.typedData)
                                else if (attr.dataType == TYPE_REFERENCE) result.versionNameRes = attr.typedData
                            }
                            attrName == "package" && tagName == "manifest" -> {
                                result.packageName = axmlStrings.getOrNull(attr.typedData)
                            }
                        }
                    }
                }
                else -> reader.skip(header.totalSize.toLong() - 8L)
            }
            if (reader.position < nextChunkPos) reader.skip(nextChunkPos - reader.position)
        }
        return result
    }

    fun setAxmlStrings(strings: List<String>) { this.axmlStrings = strings }
}