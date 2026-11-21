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
 * - Android 端直接利用系统 PackageManager 解析（极致性能，零内存溢出）
 * - WasmJS 端利用纯 Kotlin 流式 ApkParser 解析
 */
public expect suspend fun extractApkMetadata(file: PlatformFile): UnifiedApkMetadata