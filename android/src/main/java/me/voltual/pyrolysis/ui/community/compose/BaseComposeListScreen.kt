//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis.ui.community.compose

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.voltual.pyrolysis.R
import me.voltual.pyrolysis.network.KtorClient
import me.voltual.pyrolysis.core.ui.theme.BBQDropdownMenu
import me.voltual.pyrolysis.AuthManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.voltual.pyrolysis.core.ui.theme.BBQPullRefreshIndicator
import me.voltual.pyrolysis.ui.LocalTopAppBarController
import me.voltual.pyrolysis.ui.TopAppBarAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseComposeListScreen(
    title: String = "",
    currentRoute: String = "", // 新增参数：传入当前页面的路由标识
    posts: List<KtorClient.Post>,
    isLoading: Boolean,
    errorMessage: String,
    onItemClick: (KtorClient.Post) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    historyClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    totalPages: Int = 1,
    onJumpToPage: (Int) -> Unit = {},
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showJumpDialog by remember { mutableStateOf(false) }
    var inputPage by remember { mutableStateOf("") }
    var menuExpanded by mutableStateOf(false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val topAppBarController = LocalTopAppBarController.current

    // 动态同步顶栏状态
    LaunchedEffect(title, menuExpanded, totalPages, currentRoute) { // 将 currentRoute 加入感知依赖
        // 1. 设置自定义标题组件
        topAppBarController.titleContent = {
            val titleScrollState = rememberScrollState()
            Box(modifier = Modifier.wrapContentSize()) {
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .horizontalScroll(titleScrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { menuExpanded = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = title, 
                            maxLines = 1,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                // 下拉菜单：根据 currentRoute 动态过滤当前路由
                BBQDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(200.dp)
                ) {
                    if (currentRoute != "community") {
                        DropdownMenuItem(text = { Text("社区") }, onClick = { menuExpanded = false; onNavigate("community") })
                    }
                    if (currentRoute != "hot_posts") {
                        DropdownMenuItem(text = { Text("热点") }, onClick = { menuExpanded = false; onNavigate("hot_posts") })
                    }
                    if (currentRoute != "following_posts") {
                        DropdownMenuItem(text = { Text("关注的人") }, onClick = { menuExpanded = false; onNavigate("following_posts") })
                    }
                    if (currentRoute != "my_likes") {
                        DropdownMenuItem(text = { Text("我喜欢的") }, onClick = { menuExpanded = false; onNavigate("my_likes") })
                    }
                    
                    // 特殊处理：我的帖子带有动态参数（如 "my_posts/123"），使用 startsWith 来匹配前缀
                    if (!currentRoute.startsWith("my_posts")) {
                        DropdownMenuItem(
                            text = { Text("我的帖子") },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    val currentUserId = AuthManager.getUserId(context).first()
                                    if (currentUserId > 0) onNavigate("my_posts/$currentUserId")
                                    else snackbarHostState.showSnackbar(context.getString(R.string.login_first))
                                }
                            }
                        )
                    }
                }
            }
        }

        // 2. 设置右侧动作按钮
        topAppBarController.updateActions(
            listOf(
                TopAppBarAction(
                    icon = { tint -> Icon(painterResource(id = R.drawable.ic_menu_message), "消息", tint = tint) },
                    description = "消息中心",
                    onClick = onMessageClick
                ),
                TopAppBarAction(
                    icon = { tint -> Icon(painterResource(id = R.drawable.kakao_page), "跳页", tint = tint) },
                    description = "跳页",
                    onClick = { showJumpDialog = true; inputPage = "" }
                )
            )
        )
    }

    // 下拉刷新逻辑
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(isLoading, errorMessage, posts) {
        if (!isLoading && isRefreshing) {
             isRefreshing = false
        }
    }

    // 弹窗逻辑
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("从哪页开始呢~_~") },
            text = {
                Column {
                    Text("总页数: $totalPages", color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = inputPage,
                        onValueChange = { inputPage = it },
                        label = { Text("输入页码 (1-$totalPages)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val page = inputPage.toIntOrNull() ?: 1
                    if (page in 1..totalPages) onJumpToPage(page)
                    showJumpDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("取消") }
            }
        )
    }

    // 主内容
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        },
        state = pullRefreshState,
        indicator = {
            BBQPullRefreshIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        modifier = modifier.fillMaxSize() 
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                errorMessage.isNotEmpty() -> ErrorView(errorMessage, onRefresh, Modifier.fillMaxSize())
                isLoading && posts.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                posts.isEmpty() -> EmptyView(onRefresh, Modifier.fillMaxSize())
                else -> {
                    PostListComposable(
                        posts = posts,
                        isLoading = isLoading,
                        onItemClick = onItemClick,
                        onLoadMore = onLoadMore,
                        modifier = Modifier.fillMaxSize(),
                        isRefreshing = isRefreshing
                    )
                }
            }
            if (isLoading && posts.isNotEmpty() && !isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        } 
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = TextStyle(color = MaterialTheme.colorScheme.error), modifier = Modifier.padding(16.dp))
            Button(onClick = onRetry) { Text(text = "重试") }
        }
    }
}

@Composable
private fun EmptyView(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "暂无内容", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRefresh) { Text(text = "刷新") }
        }
    }
}