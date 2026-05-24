package dev.rushii.arsc

import dev.rushii.arsc.internal.ArscStringPool
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringPoolTest {
	@Test
	fun `utf8 string pool roundtrip`() {
		val strings = listOf("hello", "world", "kotlin")
		val pool = ArscStringPool(
			strings = strings,
			styles = emptyList(),
			flags = ArscStringPool.UTF_8_FLAG
		)

		val buffer = Buffer()
		ArscStringPool.write(buffer, pool)

		val parsedPool = ArscStringPool.parse(buffer)
		assertEquals(strings.size, parsedPool.strings.size)
		assertEquals(strings[0], parsedPool.strings[0])
		assertEquals(strings[1], parsedPool.strings[1])
		assertEquals(strings[2], parsedPool.strings[2])
		assertTrue(parsedPool.flags and ArscStringPool.UTF_8_FLAG != 0u)
	}

	@Test
	fun `utf16 string pool roundtrip`() {
		val strings = listOf("安卓", "资源")
		val pool = ArscStringPool(
			strings = strings,
			styles = emptyList(),
			flags = 0u // UTF-16
		)

		val buffer = Buffer()
		ArscStringPool.write(buffer, pool)

		val parsedPool = ArscStringPool.parse(buffer)
		assertEquals(strings[0], parsedPool.strings[0])
		assertEquals(strings[1], parsedPool.strings[1])
	}
}