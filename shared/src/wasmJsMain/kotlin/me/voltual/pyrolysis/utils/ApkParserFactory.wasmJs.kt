// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/utils/ApkParserFactory.wasmJs.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import me.voltual.apkparser.ApkParser

public actual suspend fun extractApkMetadata(file: PlatformFile): UnifiedApkMetadata {
    val bytes = file.readBytes()
    val parser = ApkParser(bytes)
    val metadata = parser.parse()

    var iconBytes: ByteArray? = null
    metadata.iconPath?.let { path ->
        iconBytes = parser.getFileBytes(path)
    }

    return UnifiedApkMetadata(
        packageName = metadata.packageName,
        label = metadata.label,
        versionCode = metadata.versionCode,
        versionName = metadata.versionName,
        iconBytes = iconBytes
    )
}