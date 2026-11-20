// FILE: shared/src/androidMain/kotlin/me/voltual/pyrolysis/utils/PlatformUploadHelper.android.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile

public actual suspend fun uploadMultipartFileNative(
    url: String,
    file: PlatformFile,
    fileParamName: String,
    extraParams: Map<String, String>
): String? {
    // Android 端返回 null，使用经过优化的 OkHttp ChannelProvider 流式上传
    return null
}