//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.voltual.pyrolysis.data.unified.toUnifiedUserDetail  
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.data.unified.UnifiedUserDetail 
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserDetailViewModel(
    private val authRepository: AuthRepository // 注入 AuthRepository
) : ViewModel() { // 变为普通 ViewModel

    private val _userData = MutableStateFlow<UnifiedUserDetail?>(null)
    val userData: StateFlow<UnifiedUserDetail?> = _userData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var _isInitialized = false
    private var _currentUserId: Long = -1L
    private var _currentStore: AppStore = AppStore.XIAOQU_SPACE
    private val apiService = KtorClient.ApiServiceImpl

    fun loadUserDetails(userId: Long, store: AppStore = AppStore.XIAOQU_SPACE) {
        if (this._currentUserId != userId || this._currentStore != store) {
            this._currentUserId = userId
            this._currentStore = store
            this._isInitialized = false
            resetState()
            loadDataIfNeeded()
        } else {
            loadDataIfNeeded()
        }
    }

    private fun resetState() {
        _userData.value = null
        _errorMessage.value = null
        _isLoading.value = false
    }

    private fun loadDataIfNeeded() {
        if (!_isInitialized && _currentUserId != -1L && !_isLoading.value) {
            _isInitialized = true
            loadData()
        }
    }

    fun refresh() {
        if (_currentUserId != -1L) {
            _isLoading.value = false
            loadData()
        }
    }
        
    fun followUser(targetUserId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = getToken()
                if (token.isEmpty()) {
                    _errorMessage.value = "请先登录"
                    return@launch
                }
                
                val result = withContext(Dispatchers.IO) {
                    apiService.followUser(
                        token = token,
                        followedId = targetUserId
                    )
                }
                
                when (val response = result.getOrNull()) {
                    is KtorClient.BaseResponse -> {
                        if (response.code == 1) {
                            refresh()
                        } else {
                            _errorMessage.value = "关注失败: ${response.msg}"
                        }
                    }
                    else -> {
                        _errorMessage.value = "网络错误"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "关注失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun unfollowUser(targetUserId: Long) {
        followUser(targetUserId)
    }
    
    private suspend fun getToken(): String {
        return authRepository.credentials.first().token
    }

    private fun loadData() {
        viewModelScope.launch {
            val token = getToken()

            if (_isLoading.value) return@launch

            _isLoading.value = true

            try {
                val result = withContext(Dispatchers.IO) {
                    when (_currentStore) {
                        AppStore.XIAOQU_SPACE -> {
                            apiService.getUserInformation(
                                userId = _currentUserId,
                                token = token
                            )
                        }
                        else -> {
                            Result.failure(IllegalArgumentException("当前应用商店不支持用户详情"))
                        }
                    }
                }

                if (_currentStore == AppStore.XIAOQU_SPACE ) {
                    when (val response = result.getOrNull()) {
                        is KtorClient.UserInformationResponse -> {
                            if (response.code == 1) {
                                _userData.value = response.data.toUnifiedUserDetail()
                                _errorMessage.value = null
                            } else {
                                _errorMessage.value = "加载失败: ${response.msg}"
                            }
                        }
                        else -> {
                            _errorMessage.value = "加载失败: ${result.exceptionOrNull()?.message ?: "网络错误"}"
                        }
                    }
                } else {
                    _errorMessage.value = "当前应用商店不支持用户详情"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}