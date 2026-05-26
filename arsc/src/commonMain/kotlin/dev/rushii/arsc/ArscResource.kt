package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public data class ArscResource(
    public val specId: UInt = 0u,
    public val flags: UShort,
    public val name: String,
    public val value: ArscValue,
) {
    @ArscInternalApi
    public companion object {
        private const val FLAG_COMPLEX: UShort = 0x0001u

        public fun write(
            sink: Sink,
            resource: ArscResource,
            writtenGlobalPool: ArscStringPool.WrittenPool,
            writtenKeyPool: ArscStringPool.WrittenPool
        ) {
            val nameIndex = writtenKeyPool.strings[resource.name] ?: 0
            val isComplex = resource.value is ArscValue.Bag
            val entrySize = if (isComplex) 16u else 8u
            
            sink.writeU16(entrySize.toUShort())
            sink.writeU16(resource.flags)
            sink.writeU32(nameIndex.toUInt())

            when (val v = resource.value) {
                is ArscValue.Plain -> ArscValue.Plain.write(sink, v, writtenGlobalPool)
                is ArscValue.Bag -> {
                    sink.writeU32(v.parent); sink.writeU32(v.values.size.toUInt())
                    v.values.forEach { (key, plain) ->
                        sink.writeU32(key)
                        ArscValue.Plain.write(sink, plain, writtenGlobalPool)
                    }
                }
            }
        }

        public fun parse(source: Source, count: Int, globalPool: ArscStringPool, keyPool: ArscStringPool): MutableList<ArscResource> {
            val offsets = (0 until count).map { source.readU32() }
            val resources = mutableListOf<ArscResource>()
            offsets.forEachIndexed { idx, offset ->
                if (offset == UInt.MAX_VALUE) return@forEachIndexed
                val size = source.readU16()
                val flags = source.readU16()
                val nameIdx = source.readU32()
                val value = if (flags and FLAG_COMPLEX != 0u.toUShort()) {
                    val parent = source.readU32()
                    val c = source.readU32()
                    val map = (0 until c.toInt()).associate { source.readU32() to ArscValue.Plain.parse(source, globalPool) }
                    ArscValue.Bag(parent, map)
                } else ArscValue.Plain.parse(source, globalPool)
                resources += ArscResource(idx.toUInt(), flags, keyPool.strings[nameIdx.toInt()], value)
            }
            return resources
        }
    }
}