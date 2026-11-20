// FILE: shared/src/androidMain/kotlin/me/voltual/pyrolysis/utils/ApkParserFactory.android.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source
import kotlinx.io.buffered
import me.voltual.apkparser.ApkParser

public actual suspend fun createStreamingApkParser(file: PlatformFile): ApkParser {
    // Android 端：使用本地文件系统的直接输入流，无需预读整个文件
    return ApkParser { file.source().buffered() }
}