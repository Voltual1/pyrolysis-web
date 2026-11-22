// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/utils/PlatformUploadHelper.wasmJs.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.BrowserFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.await
import kotlin.js.Promise

@JsFun("""(originPrivateFile) => originPrivateFile.handle.getFile()""")
private external fun getOriginPrivateBrowserFile(originPrivateFile: JsAny): Promise<BrowserFile>

@JsFun("""(url, file, filename, fileKey, extraJson, headersJson) => {
    return new Promise((resolve, reject) => {
        const formData = new FormData();
        if (extraJson) {
            const extras = JSON.parse(extraJson);
            for (const [k, v] of Object.entries(extras)) {
                formData.append(k, v);
            }
        }
        formData.append(fileKey, file, filename);

        const headers = {};
        if (headersJson) {
            const parsedHeaders = JSON.parse(headersJson);
            for (const [k, v] of Object.entries(parsedHeaders)) {
                headers[k] = v;
            }
        }

        fetch(url, {
            method: "POST",
            headers: headers,
            body: formData
        }).then(async response => {
            const text = await response.text();
            if (!response.ok) {
                reject(new Error("HTTP " + response.status + ": " + text));
            } else {
                resolve(text);
            }
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
    extraJson: String?,
    headersJson: String?
): Promise<JsString>

public actual suspend fun uploadMultipartFileNative(
    url: String,
    file: PlatformFile,
    fileParamName: String,
    extraParams: Map<String, String>,
    headers: Map<String, String>
): String? {
    val browserFile: BrowserFile = when (val wf = file.webFile) {
        is WebFile.FileWrapper -> wf.file
        is WebFile.OriginPrivateFile -> getOriginPrivateBrowserFile(wf.unsafeCast()).await()
        else -> throw IllegalArgumentException("PlatformFile 不包含有效的浏览器文件对象")
    }

    val extraJson = if (extraParams.isNotEmpty()) {
        extraParams.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            "\"${k.escapeJson()}\":\"${v.escapeJson()}\""
        }
    } else null

    val headersJson = if (headers.isNotEmpty()) {
        headers.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            "\"${k.escapeJson()}\":\"${v.escapeJson()}\""
        }
    } else null

    val responseText = jsNativeFetchUpload(
        url = url,
        file = browserFile,
        filename = file.name,
        fileKey = fileParamName,
        extraJson = extraJson,
        headersJson = headersJson
    ).await<JsString>()

    return responseText.toString()
}

private fun String.escapeJson(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")