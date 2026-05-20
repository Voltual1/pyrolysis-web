//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ktor.http.Url
import me.voltual.pyrolysis.data.UpdateInfo

@Composable
fun UpdateDialog(updateInfo: UpdateInfo, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current // 跨平台的本地 URI 处理器

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // 使整个对话框内容可滚动
                horizontalAlignment = Alignment.CenterHorizontally // 内容水平居中对齐
            ) {
                Text(
                    text = "发现新版本：${updateInfo.tag_name}",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = updateInfo.body,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "32位设备请下载v7a，64位下载v8a",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                updateInfo.assets.filter { it.name.endsWith(".apk") }.forEach { asset ->
                    Button(
                        onClick = {
                            runCatching {
                                // 使用 Ktor 的 Url 解析并验证下载链接
                                val downloadUrl = Url(asset.browser_download_url)
                                // 唤起系统浏览器/下载器
                                uriHandler.openUri(downloadUrl.toString())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "下载 ${asset.name}")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // "稍后更新" 按钮
                TextButton(onClick = onDismiss) {
                    Text("下次一定")
                }
            }
        }
    }
}