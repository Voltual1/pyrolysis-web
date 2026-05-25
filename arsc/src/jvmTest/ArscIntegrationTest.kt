package dev.rushii.arsc.test

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import dev.rushii.arsc.*
import dev.rushii.arsc.internal.ArscInternalApi
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ArscInternalApi::class)
class ArscIntegrationTest {

    @Test
    fun testLookupExtractedResources() {
        // 1. 加载 resources.arsc 文件
        val path = Path("src/commonTest/resources/resources.arsc")
        if (!SystemFileSystem.exists(path)) {
            println("警告: resources.arsc 不存在，跳过测试")
            return
        }

        val bytes = SystemFileSystem.source(path).buffered().use { it.readByteArray() }
        
        // 2. 解析 ARSC 文件
        val arscFile = ArscFile(bytes)
        
        // 我们从 AXML 提取到的 ID
        val labelResId = 0x7f110046
        val iconResId = 0x7f0e0000

        println("=== ARSC 资源检索测试 ===")
        
        // 3. 查找 Label (预期是一个 String)
        val labelValue = findResourceValue(arscFile, labelResId)
        assertNotNull(labelValue, "未能找到 Label 资源")
        
        if (labelValue is ArscValue.Plain.String) {
            println("应用名称 (Label): ${labelValue.data}")
            // 假设应用名称包含某个关键字
            // assertTrue(labelValue.data.isNotEmpty())
        } else {
            println("Label 资源类型不是 String: $labelValue")
        }

        // 4. 查找 Icon (预期是一个指向图片路径的 String)
        val iconValue = findResourceValue(arscFile, iconResId)
        assertNotNull(iconValue, "未能找到 Icon 资源")
        
        if (iconValue is ArscValue.Plain.String) {
            println("图标路径 (Icon): ${iconValue.data}")
            assertTrue(iconValue.data.endsWith(".xml") || iconValue.data.endsWith(".png"), "图标应该是图片或 VectorDrawable")
        } else {
            println("Icon 资源类型不是 String: $iconValue")
        }
        
        println("========================")
    }

    /**
     * 根据资源 ID 在 ArscFile 中查找资源值的辅助函数
     */
    private fun findResourceValue(arscFile: ArscFile, resId: Int): ArscValue? {
        val targetPkgId = (resId shr 24) and 0xFF
        val targetTypeId = (resId shr 16) and 0xFF
        val targetEntryId = resId and 0xFFFF

        // 遍历包 (Package)
        for (pkg in arscFile.packages) {
            if (pkg.id.toInt() != targetPkgId) continue

            // 遍历类型 (Type)
            for (type in pkg.types.values) {
                if (type.id.toInt() != targetTypeId) continue

                // 遍历配置 (Config)，通常我们取第一个匹配的（默认配置）
                for (config in type.configs) {
                    // 在配置中查找对应的 Entry ID
                    val resource = config.resources.find { it.specId.toInt() == targetEntryId }
                    if (resource != null) {
                        return resource.value
                    }
                }
            }
        }
        return null
    }
}