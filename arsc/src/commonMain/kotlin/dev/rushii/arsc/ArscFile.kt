package dev.rushii.arsc

import dev.rushii.arsc.internal.*
import kotlinx.io.*

public class ArscFile(public val packages: List<ArscPackage>) {

    public constructor(bytes: ByteArray) : this(Buffer().apply { write(bytes) })

    public constructor(source: Source) : this(parseContent(source))

    public fun finalize(): ByteArray {
        val globalBuffer = Buffer()
        val globalStrings = packages.flatMap { pkg ->
            pkg.types.values.flatMap { type ->
                type.configs.flatMap { cfg ->
                    cfg.resources.mapNotNull { (it.value as? ArscValue.Plain.String)?.data }
                }
            }
        }.distinct()
        
        val globalStringPool = ArscStringPool(globalStrings, emptyList(), ArscStringPool.UTF_8_FLAG)
        globalBuffer.writeU32(packages.size.toUInt())
        val writtenGlobalPool = ArscStringPool.write(globalBuffer, globalStringPool)

        for (pkg in packages) {
            ArscPackage.write(globalBuffer, pkg, writtenGlobalPool)
        }

        val finalBuffer = Buffer()
        val header = ArscHeader(
            ArscHeaderType.Table, 
            8u.toUShort(), 
            (8L + globalBuffer.size).toUInt()
        )
        ArscHeader.write(finalBuffer, header)
        finalBuffer.write(globalBuffer, globalBuffer.size)
        
        return finalBuffer.readByteArray()
    }

    override fun toString(): String = "Arsc[packages=$packages]"

    private companion object {
        private fun parseContent(source: Source): List<ArscPackage> {
            val header = ArscHeader.parse(source, 0L)
            if (header.type != ArscHeaderType.Table) {
                throw ArscError(0, header.type, "Not a valid ARSC table")
            }
            val packageCount = source.readU32()
            val globalStringPool = ArscStringPool.parse(source)
            return List(packageCount.toInt()) {
                ArscPackage.parse(source, globalStringPool)
            }
        }
    }
}