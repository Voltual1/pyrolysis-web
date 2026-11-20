// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/utils/ApkParserFactory.wasmJs.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import me.voltual.apkparser.ApkParser

public actual suspend fun createStreamingApkParser(file: PlatformFile): ApkParser {
    // Web 平台：通过 readBytes 解析必要元数据
    val bytes = file.readBytes()
    return ApkParser(bytes)
}