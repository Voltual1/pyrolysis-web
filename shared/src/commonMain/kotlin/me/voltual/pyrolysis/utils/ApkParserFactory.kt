// FILE: shared/src/commonMain/kotlin/me/voltual/pyrolysis/utils/ApkParserFactory.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile

/**
 * 统一的 APK 元数据结果模型
 */
data class UnifiedApkMetadata(
    val packageName: String?,
    val label: String?,
    val versionCode: Long?,
    val versionName: String?,
    val iconBytes: ByteArray?
)

/**
 * 跨平台工厂函数：
 * - Android 端使用系统 PackageManager
 * - WasmJS 端使用流式 ApkParser
 */
public expect suspend fun extractApkMetadata(file: PlatformFile): UnifiedApkMetadata