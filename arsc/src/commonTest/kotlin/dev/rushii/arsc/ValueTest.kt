package dev.rushii.arsc

import dev.rushii.arsc.internal.ArscStringPool
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueTest {
	@Test
	fun `raw value roundtrip`() {
		val buffer = Buffer()
		val pool = ArscStringPool(emptyList(), emptyList(), 0u)
		val writtenPool = ArscStringPool.WrittenPool(emptyMap(), emptyMap())
		
		val rawValue = ArscValue.Plain.Raw(type = 0x10u, data = 0x12345678u)
		
		ArscValue.Plain.write(buffer, rawValue, writtenPool)
		
		val parsedValue = ArscValue.Plain.parse(buffer, pool) as ArscValue.Plain.Raw
		assertEquals(0x10u, parsedValue.type)
		assertEquals(0x12345678u, parsedValue.data)
	}
}