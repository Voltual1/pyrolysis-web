package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile

public actual suspend fun uploadMultipartFileNative(
    url: String,
    file: PlatformFile,
    fileParamName: String,
    extraParams: Map<String, String>,
    headers: Map<String, String>
): String? {
    return null
}