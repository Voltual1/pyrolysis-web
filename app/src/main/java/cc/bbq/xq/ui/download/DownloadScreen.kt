//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.download

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import cc.bbq.xq.service.download.DownloadStatus
import cc.bbq.xq.service.download.DownloadTask
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQCard
import cc.bbq.xq.ui.theme.BBQIconButton
import cc.bbq.xq.ui.theme.BBQSnackbarHost
import cc.bbq.xq.util.FileActionUtil
import androidx.compose.foundation.shape.CircleShape

@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = koinViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val status by viewModel.downloadStatus.collectAsState()
    val downloadTasks by viewModel.downloadTasks.collectAsState()
    val hasActiveTask = status !is DownloadStatus.Idle
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            BBQSnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 下载任务列表
            if (downloadTasks.isEmpty()) {
                EmptyDownloadState()
            } else {
                LazyColumn {
                    items(downloadTasks) { task ->
                        DownloadTaskItem(
                            task = task, 
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState,
                            scope = scope
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    viewModel: DownloadViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val status = remember(task) { createDownloadStatusFromTask(task) }

    BBQCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题和链接
            Text(
                text = task.fileName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 状态显示
            when (status) {
                is DownloadStatus.Idle -> Text("等待开始", style = MaterialTheme.typography.bodySmall)
                is DownloadStatus.Pending -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("准备中...", style = MaterialTheme.typography.bodySmall)
                }
                is DownloadStatus.Downloading -> {
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${formatFileSize(context, status.downloadedBytes)} / ${formatFileSize(context, status.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = status.speed,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                is DownloadStatus.Paused -> {
                    Text("已暂停", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "已下载: ${formatFileSize(context, status.downloadedBytes)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                is DownloadStatus.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("下载完成", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = formatFileSize(context, status.file.length()),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                is DownloadStatus.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载失败", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 操作按钮行
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 浏览链接按钮（所有状态都可用）
                BBQButton(
                    onClick = { viewModel.openUrlInBrowser(task.url) },
                    text = { Text("浏览链接") }
                )
                Spacer(modifier = Modifier.width(8.dp))

                // 查看文件按钮（仅成功状态）
                // 查看文件按钮（仅成功状态）
if (status is DownloadStatus.Success) {
    BBQButton(
        onClick = {
            try {
                FileActionUtil.openFile(context, status.file)
            } catch (e: FileActionUtil.FileNotFoundException) {
                // 使用 Snackbar 显示错误信息
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "文件不存在: ${status.file.name}",
                        withDismissAction = true
                    )
                }
            } catch (e: ActivityNotFoundException) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "未找到可打开此文件的应用",
                        withDismissAction = true
                    )
                }
            } catch (e: SecurityException) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = e.message ?: "需要安装权限",
                        withDismissAction = true
                    )
                }
                // 可以在这里跳转到设置页面
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "打开文件失败: ${e.message ?: "未知错误"}",
                        withDismissAction = true
                    )
                }
            }
        },
        text = { Text("查看文件") }
    )
    Spacer(modifier = Modifier.width(8.dp))
}

                // 删除任务按钮（所有状态都可用）
                BBQButton(
                    onClick = { viewModel.deleteDownloadTask(task) },
                    text = { Text("删除任务") }
                )
            }
        }
    }
}

// 辅助函数：从数据库任务创建状态对象
private fun createDownloadStatusFromTask(task: DownloadTask): DownloadStatus {
    return when (task.status) {
        DownloadStatus.Idle::class.java.simpleName -> DownloadStatus.Idle
        DownloadStatus.Pending::class.java.simpleName -> DownloadStatus.Pending
        DownloadStatus.Downloading::class.java.simpleName -> DownloadStatus.Downloading(
            progress = task.progress,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speed = task.speed ?: ""
        )
        DownloadStatus.Paused::class.java.simpleName -> DownloadStatus.Paused(
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes
        )
        DownloadStatus.Success::class.java.simpleName -> {
            val file = java.io.File(task.savePath, task.fileName)
            DownloadStatus.Success(file = file)
        }
        DownloadStatus.Error::class.java.simpleName -> DownloadStatus.Error(
            message = task.errorMessage ?: "未知错误"
        )
        else -> DownloadStatus.Idle
    }
}

@Composable
fun EmptyDownloadState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无下载任务",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PendingDownloadState() {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("准备中...")
        }
    }
}

@Composable
fun DownloadingState(
    status: DownloadStatus.Downloading,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "正在下载文件...",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatFileSize(context, status.downloadedBytes)} / ${formatFileSize(context, status.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BBQIconButton(
                    onClick = onCancel,
                    icon = Icons.Default.Close,
                    contentDescription = "取消下载",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(AppShapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(status.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status.speed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorState(status: DownloadStatus.Error) {
    BBQCard(
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "下载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

// 辅助函数：格式化文件大小
fun formatFileSize(context: Context, size: Long): String {
    return Formatter.formatFileSize(context, size)
}