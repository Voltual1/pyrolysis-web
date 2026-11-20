// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/utils/PlatformUploadHelper.wasmJs.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.BrowserFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.await
import kotlin.js.Promise

@JsFun("""(url, file, filename, fileKey, extraKey, extraValue) => {
    return new Promise((resolve, reject) => {
        const formData = new FormData();
        if (extraKey && extraValue) {
            formData.append(extraKey, extraValue);
        }
        formData.append(fileKey, file, filename);
        fetch(url, {
            method: "POST",
            body: formData
        }).then(response => {
            if (!response.ok) {
                reject(new Error("HTTP Error " + response.status));
            } else {
                return response.text();
            }
        }).then(text => {
            resolve(text);
        }).catch(err => {
            reject(err);
        });
    });
}""")
private external fun jsNativeFetchUpload(
    url: String,
    file: JsAny,
    filename: String,
    fileKey: String,
    extraKey: String?,
    extraValue: String?
): Promise<JsString>

public actual suspend fun uploadMultipartFileNative(
    url: String,
    file: PlatformFile,
    fileParamName: String,
    extraParams: Map<String, String>
): String? {
    val browserFile: BrowserFile = when (val wf = file.webFile) {
        is WebFile.FileWrapper -> wf.file
        is WebFile.OriginPrivateFile -> wf.handle.getFile().await()
        else -> throw IllegalArgumentException("PlatformFile 不包含有效的浏览器文件对象")
    }

    val extraKey = extraParams.keys.firstOrNull()
    val extraValue = if (extraKey != null) extraParams[extraKey] else null

    val responseText = jsNativeFetchUpload(
        url = url,
        file = browserFile,
        filename = file.name,
        fileKey = fileParamName,
        extraKey = extraKey,
        extraValue = extraValue
    ).await<JsString>()

    return responseText.toString()
}