//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.KtorClient

class MyLikesViewModel(
    private val authRepository: AuthRepository // 注入 Repository，不再需要 Context
) : ViewModel() {
    
    private val _posts = MutableStateFlow(emptyList<KtorClient.Post>())
    val posts: StateFlow<List<KtorClient.Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private var currentPage = 1
    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    fun jumpToPage(page: Int) {
        if (page in 1.._totalPages.value) {
            _posts.value = emptyList()
            currentPage = page
            loadLikesRecords()
        }
    }

    fun loadInitialData() {
        loadLikesRecords()
    }

    fun loadNextPage() {
        if (currentPage < _totalPages.value && !_isLoading.value) {
            currentPage++
            loadLikesRecords()
        }
    }

    fun refresh() {
        currentPage = 1
        loadLikesRecords()
    }

    /**
     * 加载点赞记录
     */
    private fun loadLikesRecords() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""

            // 直接从 Repository 获取 token，不再需要 Context
            val userCredentials = authRepository.credentials.first()
            val token = userCredentials.token

            // 发起请求并处理结果
            withContext(Dispatchers.IO) {
                KtorClient.ApiServiceImpl.getLikesRecords(
                    token = token,
                    limit = PAGE_SIZE,
                    page = currentPage
                )
            }.onSuccess { response ->
                if (response.code == 1) {
                    val data = response.data
                    _totalPages.value = data.pagecount
                    
                    val currentList = if (currentPage == 1) emptyList() else _posts.value
                    val combinedList = currentList + data.list
                    
                    // 利用 Kotlin 的 distinctBy 进行数据去重
                    _posts.value = combinedList.distinctBy { it.postid }
                } else {
                    _errorMessage.value = "操作失败: ${response.msg.ifEmpty { "服务器错误" }}"
                }
            }.onFailure { exception ->
                // 此时 exception 可能是 PyrolysisNetworkException 或其他 Kotlin 异常
                _errorMessage.value = "加载失败: ${exception.message ?: "未知错误"}"
            }

            _isLoading.value = false
        }
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}