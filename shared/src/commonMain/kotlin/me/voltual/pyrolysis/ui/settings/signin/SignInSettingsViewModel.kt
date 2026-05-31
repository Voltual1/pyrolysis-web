//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.settings.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.KtorClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.data.SignInSettingsDataStore
import kotlinx.coroutines.Dispatchers

class SignInSettingsViewModel(
    private val authRepository: AuthRepository,
    private val signInSettingsDataStore: SignInSettingsDataStore // 注入
) : ViewModel() {

    val autoSignIn: Flow<Boolean> = signInSettingsDataStore.autoSignIn
    
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState

    suspend fun setAutoSignIn(value: Boolean) {
        signInSettingsDataStore.setAutoSignIn(value)
    }
    
    fun signIn() {
        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            try {
                val userCredentials = authRepository.credentials.first()
                if (userCredentials.userId == 0L || userCredentials.token.isEmpty()) {
                    _signInState.value = SignInState.Error("请先登录")
                    return@launch
                }
                
                val token = userCredentials.token
                val result = withContext(Dispatchers.Default) {
                    KtorClient.ApiServiceImpl.userSignIn(token = token)
                }
                
                result.onSuccess { response ->
                    if (response.code == 200) {
                        _signInState.value = SignInState.Success(response.msg)
                    } else if (response.code == 401) {
                        _signInState.value = SignInState.Error("登录已过期，请重新登录")
                    } else if (response.code == 400 && response.msg.contains("今天已经签到")) {
                        _signInState.value = SignInState.Info("今日已签到")
                    } else {
                        _signInState.value = SignInState.Error(response.msg)
                    }
                }.onFailure { exception ->
                    _signInState.value = SignInState.Error("签到失败: ${exception.message ?: "网络错误"}")
                }
            } catch (e: Exception) {
                _signInState.value = SignInState.Error("签到失败: ${e.message ?: "未知错误"}")
            }
            
            launch {
                kotlinx.coroutines.delay(3000)
                if (_signInState.value !is SignInState.Loading) {
                    _signInState.value = SignInState.Idle
                }
            }
        }
    }
    
    fun resetSignInState() {
        _signInState.value = SignInState.Idle
    }
}

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val message: String) : SignInState()
    data class Error(val message: String) : SignInState()
    data class Info(val message: String) : SignInState()
}