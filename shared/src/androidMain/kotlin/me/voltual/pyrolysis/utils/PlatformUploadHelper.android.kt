// FILE: shared/src/androidMain/kotlin/me/voltual/pyrolysis/utils/PlatformUploadHelper.android.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile

public actual suspend fun uploadMultipartFileNative(
    url: String,
    file: PlatformFile,
    fileParamName: String,
    extraParams: Map<String, String>,
    headers: Map<String, String>
): String? {
    // Android 端返回 null，直接使用 Ktor + OkHttp 的流式 ChannelProvider
    return null
}