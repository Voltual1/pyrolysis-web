// FILE: shared/src/commonMain/kotlin/me/voltual/pyrolysis/utils/PlatformUploadHelper.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile

/**
 * 针对 WasmJS 等平台提供原生零内存开销的多部分表单直传能力。
 * 若平台无需绕行（如 Android），返回 null，使用常规 Ktor 流式管道。
 */
public expect suspend fun uploadMultipartFileNative(
    url: String,
    file: PlatformFile,
    fileParamName: String = "file",
    extraParams: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap()
): String?