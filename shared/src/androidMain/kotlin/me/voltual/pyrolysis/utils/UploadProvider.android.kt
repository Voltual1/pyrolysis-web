// FILE: shared/src/androidMain/kotlin/me/voltual/pyrolysis/utils/UploadProvider.android.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.size
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers
import java.io.FileInputStream
import java.io.InputStream

/**
 * Android 平台流式上传提供器实现
 * 显式传入文件大小以支持 Content-Length，并使用 Ktor 优化的 RawSourceChannel 流式输出
 */
public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    val fileSize = file.size().takeIf { it >= 0L }
    return ChannelProvider(size = fileSize) {
        val inputStream: InputStream = when (val af = file.androidFile) {
            is AndroidFile.FileWrapper -> FileInputStream(af.file)
            is AndroidFile.UriWrapper -> {
                FileKit.context.contentResolver.openInputStream(af.uri)
                    ?: throw IllegalStateException("无法打开 Uri 输入流: ${af.uri}")
            }
        }
        inputStream.toByteReadChannel(Dispatchers.IO)
    }
}