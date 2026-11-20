// FILE: shared/src/commonMain/kotlin/me/voltual/pyrolysis/utils/ApkParserFactory.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import me.voltual.apkparser.ApkParser

/**
 * 跨平台工厂函数：以流式低内存开销方式从 PlatformFile 构建 ApkParser
 */
public expect suspend fun createStreamingApkParser(file: PlatformFile): ApkParser