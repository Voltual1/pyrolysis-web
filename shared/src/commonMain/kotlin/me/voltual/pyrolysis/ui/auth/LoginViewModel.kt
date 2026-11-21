//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.auth

import androidx.lifecycle.*
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.LanzouClient
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- 平台选择状态 ---
    private val _selectedStore = MutableStateFlow(AppStore.XIAOQU_SPACE)
    val selectedStore: StateFlow<AppStore> = _selectedStore.asStateFlow()

    fun onStoreSelected(store: AppStore) {
        _selectedStore.value = store
        _errorMessage.value = null
    }

    // --- 通用状态 ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    // --- 登录状态 ---
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    // --- 注册状态（小趣空间） ---
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _captcha = MutableStateFlow("")
    val captcha: StateFlow<String> = _captcha.asStateFlow()

    private val _verificationCodeUrl = MutableStateFlow<String?>(null)
    val verificationCodeUrl: StateFlow<String?> = _verificationCodeUrl.asStateFlow()

    // --- 事件处理 ---
    fun onUsernameChange(newUsername: String) { _username.value = newUsername }
    fun onPasswordChange(newPassword: String) { _password.value = newPassword }
    fun onEmailChange(newEmail: String) { _email.value = newEmail }
    fun onCaptchaChange(newCaptcha: String) { _captcha.value = newCaptcha }

    // --- 业务逻辑 ---

    fun login() {
        if (_username.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "账号和密码不能为空"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                when (_selectedStore.value) {
                    AppStore.XIAOQU_SPACE -> loginXiaoqu()
                    AppStore.LANZOU_CLOUD -> loginLanzou()
                    else -> {
                        _errorMessage.value = "不支持登录"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "登录异常: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loginXiaoqu() {
        try {
            val deviceId = authRepository.deviceId.first()

            val loginResult = withContext(Dispatchers.Default) {
                KtorClient.ApiServiceImpl.login(
                    username = _username.value,
                    password = _password.value,
                    device = deviceId
                )
            }

            if (loginResult.isSuccess) {
                val loginResponse = loginResult.getOrNull()
                if (loginResponse?.code == 1) {
                    loginResponse.data?.let {
                        saveCredentialsAndNotifySuccess(
                            usertoken = it.usertoken,
                            userId = it.id
                        )
                    } ?: run {
                        _errorMessage.value = "登录失败: 无法获取用户信息"
                    }
                } else {
                    _errorMessage.value = loginResponse?.msg ?: "登录失败"
                }
            } else {
                _errorMessage.value = loginResult.exceptionOrNull()?.message ?: "登录失败"
            }
        } catch (e: Exception) {
            _errorMessage.value = "网络错误: ${e.message}"
        }
    }

    /**
     * 蓝云存储登录流：
     * 1. 调用 /api/auth/guest 建立或刷新会话，拿到 ly_session Cookie 与 csrf_token
     * 2. 调用 /api/lanzou/bind 绑定蓝奏云账号密码，持久化全部凭证至 DataStore
     */
    private suspend fun loginLanzou() {
        try {
            val existingCredentials = authRepository.credentials.first()
            val existingCookie = existingCredentials.lanzouSessionCookie.takeIf { it.isNotBlank() }

            val guestResult = withContext(Dispatchers.Default) {
                LanzouClient.ApiServiceImpl.createOrRefreshGuestSession(existingCookie)
            }

            val (guestData, sessionCookie) = guestResult.getOrElse { e ->
                _errorMessage.value = "建立蓝云会话失败: ${e.message}"
                return
            }

            val csrfToken = guestData.csrf_token
            if (sessionCookie.isBlank() || csrfToken.isBlank()) {
                _errorMessage.value = "未能获取到有效的蓝云会话令牌 (Cookie/CSRF)"
                return
            }

            val bindResult = withContext(Dispatchers.Default) {
                LanzouClient.ApiServiceImpl.bindLanzouAccount(
                    sessionCookie = sessionCookie,
                    csrfToken = csrfToken,
                    lanzouUsername = _username.value.trim(),
                    lanzouPassword = _password.value
                )
            }

            bindResult.onSuccess { bindResp ->
                val folderName = bindResp.lanzou?.folder_name
                    ?: bindResp.user?.folder_name
                    ?: "网盘存储"
                authRepository.saveLanzouCredentials(
                    lanzouUsername = _username.value.trim(),
                    lanzouPassword = _password.value,
                    sessionCookie = sessionCookie,
                    csrfToken = csrfToken,
                    folderName = folderName
                )
                _loginSuccess.value = true
            }.onFailure { e ->
                _errorMessage.value = e.message ?: "蓝奏云账号绑定失败"
            }
        } catch (e: Exception) {
            _errorMessage.value = "蓝云登录网络错误: ${e.message}"
        }
    }

    // --- 注册逻辑（仅小趣空间） ---

    fun register() {
        if (_selectedStore.value != AppStore.XIAOQU_SPACE) {
            _errorMessage.value = "当前平台不支持在此注册"
            return
        }

        if (_username.value.isBlank() || _password.value.isBlank() || _email.value.isBlank() || _captcha.value.isBlank()) {
            _errorMessage.value = "请填写所有注册信息"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val deviceId = authRepository.deviceId.first()

                val registerResult = withContext(Dispatchers.Default) {
                    KtorClient.ApiServiceImpl.register(
                        username = _username.value,
                        password = _password.value,
                        email = _email.value,
                        device = deviceId,
                        captcha = _captcha.value
                    )
                }
                if (registerResult.isSuccess) {
                    val registerResponse = registerResult.getOrNull()
                    if (registerResponse?.code == 1) {
                        loginAfterRegister()
                    } else {
                        _errorMessage.value = registerResponse?.msg ?: "注册失败"
                        loadVerificationCode()
                    }
                } else {
                    _errorMessage.value = registerResult.exceptionOrNull()?.message ?: "注册失败"
                    loadVerificationCode()
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loginAfterRegister() {
        try {
            val deviceId = authRepository.deviceId.first()
            val loginResult = withContext(Dispatchers.Default) {
                KtorClient.ApiServiceImpl.login(
                    username = _username.value,
                    password = _password.value,
                    device = deviceId
                )
            }
            if (loginResult.isSuccess) {
                val loginResponse = loginResult.getOrNull()
                if (loginResponse?.code == 1) {
                    loginResponse.data?.let {
                        saveCredentialsAndNotifySuccess(
                            usertoken = it.usertoken,
                            userId = it.id
                        )
                    } ?: run {
                        _errorMessage.value = "登录失败: 无法获取用户信息"
                    }
                } else {
                    _errorMessage.value = "注册成功，但自动登录失败: ${loginResponse?.msg}"
                }
            } else {
                _errorMessage.value = "注册成功，但自动登录时网络错误: ${loginResult.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _errorMessage.value = "注册成功，但自动登录时网络错误: ${e.message}"
        }
    }

    fun loadVerificationCode() {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        _verificationCodeUrl.value = "http://apk.xiaoqu.online/api/get_image_verification_code?appid=1&type=2&t=$timestamp"
    }

    private suspend fun saveCredentialsAndNotifySuccess(usertoken: String, userId: Long) {
        authRepository.saveCredentials(
            username = _username.value,
            password = _password.value,
            token = usertoken,
            userId = userId
        )
        _loginSuccess.value = true
    }
}