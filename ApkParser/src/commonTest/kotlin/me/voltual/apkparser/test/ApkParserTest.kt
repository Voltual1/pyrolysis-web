package me.voltual.apkparser.test

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import me.voltual.apkparser.ApkParser
import kotlin.test.Test
import kotlin.test.assertNotNull

//class ApkParserTest {

    @Test
    fun testParseRealApk() {
        // 假设您的测试资源里有一个测试用的 apk
        val path = Path("src/commonTest/resources/test.apk")
        if (!SystemFileSystem.exists(path)) return

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