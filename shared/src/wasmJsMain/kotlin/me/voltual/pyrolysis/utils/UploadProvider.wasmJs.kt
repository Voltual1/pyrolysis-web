// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/utils/UploadProvider.wasmJs.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.BrowserFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.github.vinceglb.filekit.size
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import org.khronos.webgl.get

/**
 * Wasm 平台流式上传提供器实现
 */
@OptIn(DelicateCoroutinesApi::class)
public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    val fileSize = file.size().takeIf { it >= 0L }
    val browserFile: BrowserFile = when (val wf = file.webFile) {
        is WebFile.FileWrapper -> wf.file
        else -> throw IllegalArgumentException("PlatformFile 不包含有效的浏览器文件对象")
    }

    return ChannelProvider(size = fileSize) {
        GlobalScope.writer(Dispatchers.Default) {
            val reader = getReader(browserFile)
            try {
                while (true) {
                    val result = readChunk(reader).await<JsAny>()
                    if (isDone(result)) break

                    val uint8Array = getValue(result)
                    if (uint8Array != null) {
                        val length = uint8Array.length
                        val byteArray = ByteArray(length)
                        for (i in 0 until length) {
                            byteArray[i] = uint8Array[i]
                        }
                        channel.writeFully(byteArray)
                        channel.flush()
                    }
                }
            } catch (e: Exception) {
                channel.cancel(e)
            } finally {
                releaseReaderLock(reader)
            }
        }.channel
    }
}