//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import me.voltual.pyrolysis.core.ui.theme.BBQDropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.voltual.pyrolysis.core.ui.components.SharedLogListItem
import me.voltual.pyrolysis.core.ui.components.SharedPostItem
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import me.voltual.pyrolysis.core.ui.icons.drawable.KakaoPage // 导入 KakaoPage 图标

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPostClick: (Long) -> Unit,
    onLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val hasMoreData by viewModel.hasMoreData.collectAsState()
    
    val allUserFilters by viewModel.allUserFilters.collectAsState()
    val activeNickname by viewModel.activeNickname.collectAsState()
    val activeUserId by viewModel.activeUserId.collectAsState()
    val isUserFilterMode by viewModel.isUserFilterMode.collectAsState()

    var showJumpDialog by remember { mutableStateOf(false) }
    var inputPage by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val listState = rememberLazyListState()

    if (searchMode == SearchMode.POSTS) {
        val shouldLoadMore = remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                val totalItems = listState.layoutInfo.totalItemsCount
                
                lastVisibleItem?.let {
                    it.index >= totalItems - 2 && hasMoreData && !isLoading && searchResults.isNotEmpty()
                } ?: false
            }
        }
        
        LaunchedEffect(shouldLoadMore.value) {
            if (shouldLoadMore.value) {
                viewModel.loadMore()
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("跳转到页面") },
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
                Button(
                    onClick = {
                        val page = inputPage.toIntOrNull() ?: 1
                        if (page in 1..totalPages) {
                            viewModel.jumpToPage(page)
                        }
                        showJumpDialog = false
                        inputPage = ""
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showJumpDialog = false
                    inputPage = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchHeader(
            query = query,
            searchMode = searchMode,
            totalPages = totalPages,
            onQueryChange = viewModel::onQueryChange,
            onSearchSubmit = { viewModel.submitSearch(it)
                keyboardController?.hide() },
            onModeChange = viewModel::onSearchModeChange,
            onJumpClick = { showJumpDialog = true
                inputPage = "" },
            focusRequester = focusRequester,
            allUserFilters = allUserFilters,
            activeUserId = activeUserId,
            activeNickname = activeNickname,
            isUserFilterMode = isUserFilterMode, 
            onSetActiveFilter = viewModel::setActiveUserFilter,
            onRemoveFilter = viewModel::removeUserFilter,
            onClearAllFilters = viewModel::clearAllUserFilters,
            onClearFilter = viewModel::clearUserFilter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        when {
            searchResults.isNotEmpty() -> {
                when (searchMode) {
                    SearchMode.POSTS -> {
                        PostSearchResultsList(
                            results = searchResults,
                            isLoading = isLoading,
                            listState = listState,
                            onPostClick = onPostClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        SearchResultsList(
                            results = searchResults,
                            onPostClick = onPostClick,
                            onLogClick = onLogClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            query.isBlank() && searchHistory.isNotEmpty() -> {
                SearchHistoryList(
                    history = searchHistory,
                    onHistoryClick = { historyQuery ->
                        viewModel.onQueryChange(historyQuery)
                        viewModel.submitSearch(historyQuery)
                    },
                    onClearAll = { viewModel.clearSearchHistory() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading && searchMode == SearchMode.POSTS -> CircularProgressIndicator()
                            errorMessage != null -> Text(
                                errorMessage!!, 
                                color = MaterialTheme.colorScheme.error
                            )
                            query.isNotBlank() && searchMode != SearchMode.POSTS && !isLoading -> 
                                Text("没有找到关于 \"$query\" 的结果")
                            query.isBlank() -> Text("输入关键字开始搜索")
                        }
                    }

                    if (isLoading && searchMode == SearchMode.POSTS && searchResults.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    searchMode: SearchMode,
    totalPages: Int,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onModeChange: (SearchMode) -> Unit,
    onJumpClick: () -> Unit,
    focusRequester: FocusRequester,
    allUserFilters: Map<Long, String>,
    activeUserId: Long?,
    activeNickname: String?,
    isUserFilterMode: Boolean,  
    onSetActiveFilter: (Long?) -> Unit,
    onRemoveFilter: (Long) -> Unit,
    onClearAllFilters: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showModeMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssistChip(
                    onClick = { showModeMenu = true },
                    label = {
                        Text(
                            text = when(searchMode) {
                                SearchMode.POSTS -> "帖子"
                                SearchMode.HISTORY -> "历史" 
                                SearchMode.LOGS -> "日志"
                            },
                            maxLines = 1
                        )
                    },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                BBQDropdownMenu(
                    expanded = showModeMenu, 
                    onDismissRequest = { showModeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("帖子") }, 
                        onClick = { 
                            onModeChange(SearchMode.POSTS)
                            showModeMenu = false 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("浏览历史") }, 
                        onClick = { 
                            onModeChange(SearchMode.HISTORY)
                            showModeMenu = false 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("日志") }, 
                        onClick = { 
                            onModeChange(SearchMode.LOGS)
                            showModeMenu = false 
                        }
                    )
                }
            }
            
            Box {
                if (allUserFilters.isNotEmpty()) {
                    val chipText = if (isUserFilterMode && activeNickname != null) {
                        "筛选: $activeNickname"
                    } else if (allUserFilters.size == 1) {
                        "已保存1个用户"
                    } else {
                        "已保存${allUserFilters.size}个用户"
                    }
                    
                    val contentColor = if (isUserFilterMode) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    AssistChip(
                        onClick = { showFilterMenu = true },
                        label = {
                            Text(
                                text = chipText,
                                color = contentColor,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isUserFilterMode) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            labelColor = if (isUserFilterMode) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                    
                    BBQDropdownMenu(
                        expanded = showFilterMenu, 
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "用户筛选 (${allUserFilters.size})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            onClick = { },
                            enabled = false
                        )
                        
                        HorizontalDivider()
                        
                        if (isUserFilterMode && activeNickname != null && activeUserId != null) {
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("✓ $activeNickname")
                                        IconButton(
                                            onClick = {
                                                onRemoveFilter(activeUserId)
                                                showFilterMenu = false
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                "移除",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }, 
                                onClick = { 
                                    onSetActiveFilter(null)
                                    showFilterMenu = false
                                }
                            )
                            HorizontalDivider()
                        }
                        
                        allUserFilters.forEach { (userId, nickname) ->
                            if (isUserFilterMode && userId == activeUserId) return@forEach
                            
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(nickname)
                                        IconButton(
                                            onClick = {
                                                onRemoveFilter(userId)
                                                showFilterMenu = false
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                "移除",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }, 
                                onClick = { 
                                    onSetActiveFilter(userId)
                                    showFilterMenu = false
                                }
                            )
                        }
                        
                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = { Text("清除所有用户") }, 
                            onClick = { 
                                onClearAllFilters()
                                showFilterMenu = false
                            }
                        )
                        
                        if (isUserFilterMode) {
                            DropdownMenuItem(
                                text = { Text("取消筛选") }, 
                                onClick = { 
                                    onClearFilter()
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                } else if (searchMode == SearchMode.POSTS && totalPages > 1) {
                    // 使用 KakaoPage ImageVector 替换 R.drawable.kakao_page
                    IconButton(
                        onClick = onJumpClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = KakaoPage,
                            contentDescription = "跳页",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { 
                Text(
                    if (isUserFilterMode && activeNickname != null) 
                        "在 $activeNickname 的帖子中搜索..." 
                    else if (allUserFilters.isNotEmpty())
                        "选择用户筛选或在全部帖子中搜索..."
                    else 
                        "输入关键词搜索..."
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, "搜索", modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, "清空", modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = CircleShape,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onSearchSubmit(query) }
            )
        )
    }
}

@Composable
private fun PostSearchResultsList(
    results: List<SearchResultItem>,
    isLoading: Boolean,
    listState: LazyListState,
    onPostClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemCount = results.size
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results, key = { 
            when (it) {
                is SearchResultItem.PostItem -> "post_${it.post.postid}"
                is SearchResultItem.HistoryItem -> "history_${it.history.postId}"
                is SearchResultItem.LogItem -> "log_${it.log.id}"
            }
        }) { item ->
            when (item) {
                is SearchResultItem.PostItem -> {
                    SharedPostItem(
                        post = item.post, 
                        onClick = { onPostClick(item.post.postid) }
                    )
                }
                else -> {}
            }
        }
        
        if (isLoading && itemCount > 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResultItem>,
    onPostClick: (Long) -> Unit,
    onLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(results, key = { 
            when (it) {
                is SearchResultItem.PostItem -> "post_${it.post.postid}"
                is SearchResultItem.HistoryItem -> "history_${it.history.postId}"
                is SearchResultItem.LogItem -> "log_${it.log.id}"
            }
        }) { item ->
            when (item) {
                is SearchResultItem.PostItem -> {
                    SharedPostItem(
                        post = item.post, 
                        onClick = { onPostClick(item.post.postid) }
                    )
                }
                is SearchResultItem.HistoryItem -> {
                    ListItem(
                        headlineContent = { 
                            Text(
                                item.history.title, 
                                maxLines = 1
                            ) 
                        },
                        supportingContent = { 
                            Text(
                                item.history.previewContent, 
                                maxLines = 2
                            ) 
                        },
                        trailingContent = { 
                            Text(item.history.formattedTime()) 
                        },
                        modifier = Modifier.clickable { 
                            onPostClick(item.history.postId) 
                        }
                    )
                }
                is SearchResultItem.LogItem -> {
                    SharedLogListItem(
                        log = item.log, 
                        modifier = Modifier.clickable { onLogClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryList(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "搜索历史", 
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(onClick = onClearAll) {
                Text("清空")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(history, key = { it }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClick(item) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History, 
                        null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        item, 
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}