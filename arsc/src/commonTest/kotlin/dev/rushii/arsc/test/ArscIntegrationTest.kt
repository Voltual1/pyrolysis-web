package dev.rushii.arsc.test

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import dev.rushii.arsc.*
import dev.rushii.arsc.internal.ArscInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ArscInternalApi::class)
class ArscIntegrationTest {

    @Test
    fun testLookupExtractedResources() {
        val path = Path("src/commonTest/resources/resources.arsc")
        if (!SystemFileSystem.exists(path)) {
            println("resources.arsc not found, skipping test")
            return
        }

        val bytes = SystemFileSystem.source(path).buffered().use { it.readByteArray() }
        val arscFile = ArscFile(bytes)
        val resolver = ArscResolver(arscFile)

        // 从 AXML 中提取的真实 ID
        val labelResId = 0x7f110046
        val iconResId = 0x7f0e0000

        val label = resolver.resolveString(labelResId)
        val icon = resolver.resolveString(iconResId)

        println("App label: $label")
        println("Icon path: $icon")

        assertNotNull(label, "Label should be resolved")
        assertNotNull(icon, "Icon should be resolved")
    }
}