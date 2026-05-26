package me.voltual.apkparser.test

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import me.voltual.apkparser.ApkParser
import kotlin.test.Test
import kotlin.test.assertNotNull

class ApkParserTest {

    @Test
    fun testParseRealApk() {
        val path = Path("src/commonTest/resources/test.apk")
        
        // 检查文件是否存在
        if (!SystemFileSystem.exists(path)) {
            println("【提示】未找到测试 APK 文件，路径: ${path.toString()}")
            println("请将测试用的 APK 命名为 'test.apk' 并放置在 'src/commonTest/resources/' 目录下再进行完整解析测试。")
            return // 依然需要结束当前测试方法，但在此之前已经完成了信息打印
        }

        val source = SystemFileSystem.source(path).buffered()
        val parser = ApkParser(source)
        val metadata = parser.parse()

        println("解析结果:")
        println("应用名称: ${metadata.label}")
        println("图标路径: ${metadata.iconPath}")

        assertNotNull(metadata.label)
        assertNotNull(metadata.iconPath)
    }
}