//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.bot.ui.message

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cc.bbq.xq.bot.ui.compose.MessageItem
import cc.bbq.xq.bot.ui.compose.PageJumpDialog
import cc.bbq.xq.bot.ui.compose.PaginationControls

@Composable
fun MessageCenterScreen(
    viewModel: MessageViewModel,
  //  navController: NavController,
    onMessageClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showPageDialog by remember { mutableStateOf(false) }
    val dialogShape = remember { RoundedCornerShape(4.dp) }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 消息列表
        Box(modifier = Modifier.weight(1f)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.messages.isEmpty()) {
                Text(
                    text = "暂无消息",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.messages) { message ->
                        MessageItem(
                            message = message,
                            onClick = {
                                if (message.postid != null) {
                                    onMessageClick(message.postid)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 分页控制栏
        PaginationControls(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onPrevClick = { viewModel.prevPage() },
            onNextClick = { viewModel.nextPage() },
            onPageClick = { showPageDialog = true },
            isPrevEnabled = state.currentPage > 1 && !state.isLoading,
            isNextEnabled = state.currentPage < state.totalPages && !state.isLoading
        )
    }

    // 分页跳转对话框
    if (showPageDialog) {
        PageJumpDialog(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onDismiss = { showPageDialog = false },
            onConfirm = { page ->
                viewModel.goToPage(page)
                showPageDialog = false
            },
            shape = dialogShape
        )
    }
}