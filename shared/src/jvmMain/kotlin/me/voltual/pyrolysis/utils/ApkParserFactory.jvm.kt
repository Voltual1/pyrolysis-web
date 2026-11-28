// FILE: shared/src/jvmMain/kotlin/me/voltual/pyrolysis/utils/ApkParserFactory.jvm.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSource
import kotlinx.io.buffered
import me.voltual.apkparser.ApkParser
import java.io.FileInputStream

public actual suspend fun extractApkMetadata(file: PlatformFile): UnifiedApkMetadata = withContext(Dispatchers.IO) {
    val targetFile = file.file
    require(targetFile.exists() && targetFile.isFile) {
        "APK 文件不存在或不是有效文件: ${targetFile.absolutePath}"
    }

    val parser = ApkParser {
        FileInputStream(targetFile).asSource().buffered()
    }

    val metadata = parser.parse()
    val iconBytes = metadata.iconPath?.let { path ->
        runCatching { parser.getFileBytes(path) }.getOrNull()
    }

    UnifiedApkMetadata(
        packageName = metadata.packageName,
        label = metadata.label,
        versionCode = metadata.versionCode,
        versionName = metadata.versionName,
        iconBytes = iconBytes
    )
}