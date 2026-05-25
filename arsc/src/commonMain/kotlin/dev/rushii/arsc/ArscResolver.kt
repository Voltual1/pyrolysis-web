package dev.rushii.arsc

public class ArscResolver(public val arscFile: ArscFile) {
    public fun resolve(resId: Int): ArscValue? {
        val pkgId = (resId shr 24) and 0xFF
        val typeId = (resId shr 16) and 0xFF
        val entryId = resId and 0xFFFF

        val pkg = arscFile.packages.find { it.id.toInt() == pkgId } ?: return null
        val type = pkg.types.values.find { it.id.toInt() == typeId } ?: return null
        return type.configs.firstNotNullOfOrNull { config ->
            config.resources.find { it.specId.toInt() == entryId }?.value
        }
    }

    public fun resolveString(resId: Int): String? {
        val value = resolve(resId)
        return (value as? ArscValue.Plain.String)?.data
    }
}