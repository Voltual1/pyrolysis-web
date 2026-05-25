package dev.rushii.arsc

/**
 * 根据资源 ID 从 ArscFile 中解析实际值的辅助类
 */
public class ArscResolver(public val arscFile: ArscFile) {
    /**
     * 解析资源值，返回 ArscValue 或 null
     */
    public fun resolve(resId: Int): ArscValue? {
        val pkgId = (resId shr 24) and 0xFF
        val typeId = (resId shr 16) and 0xFF
        val entryId = resId and 0xFFFF

        val pkg = arscFile.packages.find { it.id.toInt() == pkgId } ?: return null
        // 注意：ARSC 中 typeId 是从 1 开始的
        val type = pkg.types.values.find { it.id.toInt() == typeId } ?: return null
        // 简单取第一个配置（默认配置）
        return type.configs.firstNotNullOfOrNull { config ->
            config.resources.find { it.specId.toInt() == entryId }?.value
        }
    }

    public fun resolveString(resId: Int): String? {
        val value = resolve(resId)
        return (value as? ArscValue.Plain.String)?.data
    }
}