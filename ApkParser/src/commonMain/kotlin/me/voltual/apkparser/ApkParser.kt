// FILE: me.voltual.apkparser/ApkParser.kt
package me.voltual.apkparser

import kotlinx.io.*
import no.synth.kmpzip.kotlinx.ZipInputStream
import org.duangsuse.bin.io.Reader
import org.duangsuse.bin.Nat8Reader
import org.duangsuse.bin.axml.AxmlExtractor
import dev.rushii.arsc.ArscFile
import dev.rushii.arsc.ArscResolver
import dev.rushii.arsc.internal.ArscStringPool
import dev.rushii.arsc.internal.ArscInternalApi

@OptIn(ArscInternalApi::class)
public class ApkParser(private val sourceProvider: () -> Source) {

    /**
     * 兼容已有的 ByteArray 构造方式
     */
    public constructor(apkBytes: ByteArray) : this({ Buffer().apply { write(apkBytes) } })

    /**
     * 流式解析 APK 元数据。
     * 只需解压读取 AndroidManifest.xml 与 resources.arsc，完毕后立即截断流，防止整包加载进内存。
     */
    public fun parse(): ApkMetadata {
        var manifestBytes: ByteArray? = null
        var arscBytes: ByteArray? = null

        val zis = ZipInputStream(sourceProvider())

        try {
            while (true) {
                val entry = zis.nextEntry ?: break
                when (entry.name) {
                    "AndroidManifest.xml" -> manifestBytes = zis.readBytes()
                    "resources.arsc" -> arscBytes = zis.readBytes()
                }
                if (manifestBytes != null && arscBytes != null) break
            }
        } finally {
            zis.close()
        }

        if (manifestBytes == null) error("AndroidManifest.xml not found")

        // 1. 解析 AXML 内部字符串池
        val manifestBuffer = Buffer().apply { write(manifestBytes) }
        val axmlStringPool = parseAxmlStringPool(manifestBuffer.peek())

        // 2. 提取 AXML 信息
        val manifestReader = createAxmlReader(manifestBytes)
        val extractor = AxmlExtractor(manifestReader)
        extractor.setAxmlStrings(axmlStringPool)
        val resInfo = extractor.extract()

        // 3. 准备资源解析
        var label = resInfo.labelRaw
        var iconPath = resInfo.iconRaw
        var versionName = resInfo.versionNameRaw

        if (arscBytes != null && (resInfo.labelRes != null || resInfo.iconRes != null || resInfo.versionNameRes != null)) {
            val arscFile = ArscFile(arscBytes)
            val resolver = ArscResolver(arscFile)

            if (label == null) label = resInfo.labelRes?.let { resolver.resolveString(it) }
            if (iconPath == null) iconPath = resInfo.iconRes?.let { resolver.resolveString(it) }
            if (versionName == null) versionName = resInfo.versionNameRes?.let { resolver.resolveString(it) }
        }

        return ApkMetadata(
            packageName = resInfo.packageName,
            label = label,
            iconPath = iconPath,
            versionCode = resInfo.versionCode,
            versionName = versionName
        )
    }

    /**
     * 从 APK 中提取指定路径的文件（如应用图标）。
     * @param path 文件在 APK 压缩包内的路径。
     * @return 文件的字节数组，如果未找到则返回 null。
     */
    public fun getFileBytes(path: String): ByteArray? {
        val zis = ZipInputStream(sourceProvider())
        val targetPath = path.replace("\\", "/").removePrefix("/")
        try {
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name.replace("\\", "/").removePrefix("/") == targetPath) {
                    return zis.readBytes()
                }
            }
        } finally {
            zis.close()
        }
        return null
    }

    private fun parseAxmlStringPool(source: Source): List<String> {
        source.skip(8)
        return ArscStringPool.parse(source).strings
    }

    private fun createAxmlReader(bytes: ByteArray): Reader {
        val buffer = Buffer().apply { write(bytes) }
        return Reader(object : Nat8Reader {
            override val source: Source = buffer
            override val estimate: Long = bytes.size.toLong()
            override fun skip(n: Long) { source.skip(n) }
            override fun close() {}
        }).apply { byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian }
    }
}