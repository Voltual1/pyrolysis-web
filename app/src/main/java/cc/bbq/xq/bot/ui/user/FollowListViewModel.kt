//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.bot.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.bot.AuthManager
import cc.bbq.xq.bot.RetrofitClient
import cc.bbq.xq.bot.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserListState(
    val users: List<RetrofitClient.models.UserItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

class FollowListViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager() // 修复：调用无参构造函数

    private val _uiState = MutableStateFlow(UserListState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        if (_uiState.value.users.isEmpty()) {
            loadData(1)
        }
    }

    fun refresh() {
        loadData(1)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (!currentState.isLoading && currentState.currentPage < currentState.totalPages) {
            loadData(currentState.currentPage + 1)
        }
    }

    private fun loadData(page: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val token = AuthManager.getCredentials(getApplication())?.third ?: ""

            try {
                val response = userManager.getFollowList(token = token, page = page)
                if (response.isSuccessful && response.body()?.code == 1) {
                    val data = response.body()!!.data
                    _uiState.value = _uiState.value.copy(
                        users = if (page == 1) data.list else _uiState.value.users + data.list,
                        totalPages = data.pagecount,
                        currentPage = page,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = response.body()?.msg ?: "未知错误",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "网络错误",
                    isLoading = false
                )
            }
        }
    }
}