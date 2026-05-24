package dev.rushii.arsc

import dev.rushii.arsc.internal.ArscHeader
import dev.rushii.arsc.internal.ArscHeaderType
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderRoundtripTest {
	@Test
	fun `header write and parse`() {
		val buffer = Buffer()
		val originalHeader = ArscHeader(
			type = ArscHeaderType.StringPool,
			headerSize = 28u,
			bodySize = 1024u
		)

		ArscHeader.write(buffer, originalHeader)
		
		assertEquals(8L, buffer.size)
		
		val parsedHeader = ArscHeader.parse(buffer, 0L)
		assertEquals(originalHeader.type, parsedHeader.type)
		assertEquals(originalHeader.headerSize, parsedHeader.headerSize)
		assertEquals(originalHeader.bodySize, parsedHeader.bodySize)
	}
}