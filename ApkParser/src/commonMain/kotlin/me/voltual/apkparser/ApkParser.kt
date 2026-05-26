package me.voltual.apkparser

import kotlinx.io.*
import no.synth.kmpzip.kotlinx.ZipInputStream
import org.duangsuse.bin.io.Reader
import org.duangsuse.bin.Nat8Reader
import org.duangsuse.bin.axml.AxmlExtractor
import dev.rushii.arsc.ArscFile
import dev.rushii.arsc.ArscResolver
import dev.rushii.arsc.internal.ArscInternalApi

@OptIn(ArscInternalApi::class)
public class ApkParser(private val apkSource: Source) {

    /**
     * 解析 APK 并提取元数据
     */
    public fun parse(): ApkMetadata {
        var manifestBytes: ByteArray? = null
        var arscBytes: ByteArray? = null

        // 1. 使用 kmp-zip 遍历 APK 查找必要文件
        val zis = ZipInputStream(apkSource)
        while (true) {
            val entry = zis.nextEntry ?: break
            when (entry.name) {
                "AndroidManifest.xml" -> {
                    manifestBytes = zis.readBytes()
                }
                "resources.arsc" -> {
                    arscBytes = zis.readBytes()
                }
            }
            if (manifestBytes != null && arscBytes != null) break
        }

        if (manifestBytes == null) error("APK 中未找到 AndroidManifest.xml")
        if (arscBytes == null) error("APK 中未找到 resources.arsc")

        // 2. 解析 AndroidManifest.xml 提取资源 ID
        val manifestReader = createAxmlReader(manifestBytes)
        val extractor = AxmlExtractor(manifestReader)
        val resInfo = extractor.extract()

        // 3. 解析 resources.arsc 还原资源值
        val arscFile = ArscFile(arscBytes)
        val resolver = ArscResolver(arscFile)

        val label = resInfo.labelRes?.let { resolver.resolveString(it) }
        val iconPath = resInfo.iconRes?.let { resolver.resolveString(it) }

        // 注意：如果需要包名，我们可以进一步扩展 AxmlExtractor 来提取 manifest 标签的属性
        // 目前先根据您的需求返回 label 和 iconPath
        return ApkMetadata(
            packageName = null, // 待扩展
            label = label,
            iconPath = iconPath,
            versionCode = null,
            versionName = null
        )
    }

    /**
     * 辅助函数：将 ByteArray 包装为 SomeAXML 所需的 Reader
     */
    private fun createAxmlReader(bytes: ByteArray): Reader {
        val buffer = Buffer().apply { write(bytes) }
        val nat8Reader = object : Nat8Reader {
            override val source: Source = buffer
            override val estimate: Long = bytes.size.toLong()
            override fun skip(n: Long) { source.skip(n) }
            override fun close() { /* Buffer 不需要关闭 */ }
        }
        return Reader(nat8Reader).apply {
            byteOrder = org.duangsuse.bin.ByteOrder.LittleEndian
        }
    }
}