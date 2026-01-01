//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.download

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.service.download.DownloadStatus
import cc.bbq.xq.service.download.DownloadTask
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQCard
import cc.bbq.xq.ui.theme.BBQIconButton
// 关键：导入 FileActionUtil
import cc.bbq.xq.util.FileActionUtil
import androidx.compose.foundation.shape.CircleShape // 添加 CircleShape 的导入
import kotlinx.coroutines.delay

@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = viewModel()
) {
    val status by viewModel.downloadStatus.collectAsState()
    // 从 ViewModel 获取所有下载任务
    val downloadTasks by viewModel.downloadTasks.collectAsState()

    // 删除对话框状态
    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (selectedTask, setSelectedTask) = remember { mutableStateOf<DownloadTask?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 如果没有下载任务，则显示当前下载状态
            if (downloadTasks.isEmpty()) {
                AnimatedContent(targetState = status, label = "DownloadStatus") { currentStatus ->
                    when (currentStatus) {
                        is DownloadStatus.Idle -> EmptyDownloadState()
                        is DownloadStatus.Pending -> PendingDownloadState()
                        is DownloadStatus.Downloading -> DownloadingState(
                            status = currentStatus,
                            onCancel = { viewModel.cancelDownload() }
                        )
                        is DownloadStatus.Paused -> PausedState(currentStatus)
                        is DownloadStatus.Success -> SuccessState(currentStatus)
                        is DownloadStatus.Error -> ErrorState(currentStatus)
                    }
                }
            } else {
                // 如果有下载任务，则显示下载任务列表
                LazyColumn {
                    items(downloadTasks) { task ->
                        DownloadTaskItem(
                            task = task,
                            viewModel = viewModel,
                            onLongClick = { downloadTask ->
                                setSelectedTask(downloadTask)
                                setShowDeleteDialog(true)
                            }
                        )
                    }
                }
            }
        }

        // 删除确认对话框
        DeleteConfirmationDialog(
            show = showDeleteDialog,
            task = selectedTask,
            onConfirm = { task ->
                viewModel.deleteDownloadTask(task)
                setShowDeleteDialog(false)
                setSelectedTask(null)
            },
            onDismiss = {
                setShowDeleteDialog(false)
                setSelectedTask(null)
            }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    show: Boolean,
    task: DownloadTask?,
    onConfirm: (DownloadTask) -> Unit,
    onDismiss: () -> Unit
) {
    if (show && task != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "删除下载任务",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "确定要删除以下下载任务吗？",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = task.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "保存位置：${task.savePath}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val statusText = when (task.status) {
                                DownloadStatus.Idle::class.java.simpleName -> "空闲"
                                DownloadStatus.Pending::class.java.simpleName -> "等待中"
                                DownloadStatus.Downloading::class.java.simpleName -> "下载中"
                                DownloadStatus.Paused::class.java.simpleName -> "已暂停"
                                DownloadStatus.Success::class.java.simpleName -> "已完成"
                                DownloadStatus.Error::class.java.simpleName -> "下载失败"
                                else -> "未知状态"
                            }
                            Text(
                                text = "状态：$statusText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "此操作将删除下载任务记录和已下载的文件（如果存在），且无法恢复。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(task) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("删除")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    viewModel: DownloadViewModel,
    onLongClick: (DownloadTask) -> Unit // 添加长按回调
) {
    val context = LocalContext.current
    val status = when (task.status) {
        DownloadStatus.Idle::class.java.simpleName -> DownloadStatus.Idle
        DownloadStatus.Pending::class.java.simpleName -> DownloadStatus.Pending
        DownloadStatus.Downloading::class.java.simpleName -> DownloadStatus.Downloading(
            progress = task.progress,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speed = "" // 速度信息在数据库中没有存储，需要从服务中获取
        )
        DownloadStatus.Paused::class.java.simpleName -> DownloadStatus.Paused(
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes
        )
        DownloadStatus.Success::class.java.simpleName -> {
            val file = java.io.File(task.savePath, task.fileName)
            DownloadStatus.Success(file = file)
        }
        DownloadStatus.Error::class.java.simpleName -> DownloadStatus.Error(message = "未知错误")
        else -> DownloadStatus.Idle
    }

    BBQCard(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .pointerInput(Unit) {
            // 使用正确的长按检测方式
            detectTapGestures(
                onLongPress = {
                    onLongClick(task)
                }
            )
        }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 根据下载状态显示不同的内容
                when (status) {
                    is DownloadStatus.Idle -> Text("等待下载")
                    is DownloadStatus.Pending -> Text("准备中...")
                    is DownloadStatus.Downloading -> Text("${(status.progress * 100).toInt()}%")
                    is DownloadStatus.Paused -> Text("已暂停")
                    is DownloadStatus.Success -> Text("已完成")
                    is DownloadStatus.Error -> Text("下载失败")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "保存至：${task.savePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 添加“浏览链接”按钮
                BBQButton(
                    onClick = {
                        // 打开浏览器访问下载链接
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.url))
                        context.startActivity(intent)
                    },
                    text = { Text("浏览链接") }
                )
            }

            if (status is DownloadStatus.Success) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BBQButton(
                        onClick = {
                            // 使用 FileActionUtil 打开文件
                            FileActionUtil.openFile(context, (status as DownloadStatus.Success).file)
                        },
                        text = { Text("查看文件") }
                    )
                }
            }
        }
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
fun PausedState(status: DownloadStatus.Paused) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("下载已暂停", style = MaterialTheme.typography.titleMedium)
                Text(
                    "已下载: ${formatFileSize(LocalContext.current, status.downloadedBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SuccessState(status: DownloadStatus.Success) {
    val context = LocalContext.current
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "下载完成",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status.file.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BBQButton(
                    onClick = {
                        // 使用 FileActionUtil 打开文件
                        FileActionUtil.openFile(context, status.file)
                    },
                    text = { Text("查看文件") }
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