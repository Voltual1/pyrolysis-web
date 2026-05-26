package me.voltual.apkparser

/**
 * APK 元数据信息
 */
public data class ApkMetadata(
    public val packageName: String?,
    public val label: String?,
    public val iconPath: String?,
    public val versionCode: Long?,
    public val versionName: String?
)