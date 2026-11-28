// FILE: shared/src/jvmMain/kotlin/me/voltual/pyrolysis/utils/UploadProvider.jvm.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.size
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers
import java.io.FileInputStream

/**
 * JVM 平台流式上传提供器实现
 * 显式传入文件大小以支持 Content-Length，并使用 Ktor 优化的 RawSourceChannel 流式输出
 */
public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    val fileSize = file.size().takeIf { it >= 0L }
    return ChannelProvider(size = fileSize) {
        FileInputStream(file.file).toByteReadChannel(Dispatchers.IO)
    }
}