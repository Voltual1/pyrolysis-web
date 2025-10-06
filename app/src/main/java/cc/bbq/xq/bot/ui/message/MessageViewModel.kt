//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.bot.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.bot.AuthManager
import cc.bbq.xq.bot.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MessageState(
    val messages: List<RetrofitClient.models.MessageNotification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(MessageState())
    val state: StateFlow<MessageState> = _state.asStateFlow()
    
    private val token by lazy { AuthManager.getCredentials(application.applicationContext)?.third }
    
    private var isInitialized = false

    // 加载指定页的消息（替换当前内容）
    fun loadPage(page: Int = 1) {
        // 如果已经初始化并且请求的是同一页，则避免重复加载
        if (isInitialized && page == _state.value.currentPage) return

        _state.value = _state.value.copy(
            isLoading = true,
            error = null
        )
        
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getMessageNotifications(
                    token = token!!,
                    limit = 5,
                    page = page
                )
                
                if (response.isSuccessful) {
                    response.body()?.data?.let { data ->
                        _state.value = MessageState(
                            messages = data.list,
                            currentPage = page,
                            totalPages = data.pagecount
                        )
                        isInitialized = true
                    }
                } else {
                    _state.value = _state.value.copy(
                        error = "加载失败: ${response.code()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "网络错误: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    // 下一页
    fun nextPage() {
        val nextPage = _state.value.currentPage + 1
        if (nextPage > _state.value.totalPages) return
        loadPage(nextPage)
    }
    
    // 上一页
    fun prevPage() {
        val prevPage = _state.value.currentPage - 1
        if (prevPage < 1) return
        loadPage(prevPage)
    }
    
    // 跳转到指定页
    fun goToPage(page: Int) {
        if (page in 1.._state.value.totalPages) {
            loadPage(page)
        }
    }
}
