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
public class ApkParser(private val apkSource: Source) {

    public fun parse(): ApkMetadata {
        var manifestBytes: ByteArray? = null
        var arscBytes: ByteArray? = null

        val zis = ZipInputStream(apkSource)
        while (true) {
            val entry = zis.nextEntry ?: break
            when (entry.name) {
                "AndroidManifest.xml" -> manifestBytes = zis.readBytes()
                "resources.arsc" -> arscBytes = zis.readBytes()
            }
            if (manifestBytes != null && arscBytes != null) break
        }

        if (manifestBytes == null) error("AndroidManifest.xml not found")
        
        // 1. 先解析 AXML 的字符串池
        val manifestBuffer = Buffer().apply { write(manifestBytes) }
        val axmlStringPool = parseAxmlStringPool(manifestBuffer.peek())

        // 2. 解析 AXML 内容
        val manifestReader = createAxmlReader(manifestBytes)
        val extractor = AxmlExtractor(manifestReader)
        extractor.setAxmlStrings(axmlStringPool)
        val resInfo = extractor.extract()

        // 3. 处理资源解析
        var label = resInfo.labelRaw
        var iconPath = resInfo.iconRaw

        if ((resInfo.labelRes != null || resInfo.iconRes != null) && arscBytes != null) {
            val arscFile = ArscFile(arscBytes)
            val resolver = ArscResolver(arscFile)
            
            if (label == null && resInfo.labelRes != null) {
                label = resolver.resolveString(resInfo.labelRes!!)
            }
            if (iconPath == null && resInfo.iconRes != null) {
                iconPath = resolver.resolveString(resInfo.iconRes!!)
            }
        }

        return ApkMetadata(
            packageName = resInfo.packageName,
            label = label,
            iconPath = iconPath,
            versionCode = null,
            versionName = null
        )
    }

    /**
     * 专门从 AXML 头部提取 StringPool 的工具函数
     */
    private fun parseAxmlStringPool(source: Source): List<String> {
        // 跳过 AXML Header (8 bytes)
        source.skip(8)
        // 解析 StringPool
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