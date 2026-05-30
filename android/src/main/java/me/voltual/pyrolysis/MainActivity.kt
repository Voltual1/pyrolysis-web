//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import android.content.Context
import android.content.Intent
import android.app.ActivityOptions
import androidx.compose.ui.focus.FocusManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.data.UpdateInfo
import me.voltual.pyrolysis.data.UpdateSettingsDataStore
import me.voltual.pyrolysis.core.database.LogEntry
import me.voltual.pyrolysis.core.database.LogDao
import me.voltual.pyrolysis.data.UserAgreementDataStore
import me.voltual.pyrolysis.ui.*
import me.voltual.pyrolysis.core.ui.components.UserAgreementDialog
import me.voltual.pyrolysis.core.ui.theme.*
import me.voltual.pyrolysis.core.utils.UpdateCheckResult
import me.voltual.pyrolysis.core.ui.components.UpdateDialog
import me.voltual.pyrolysis.core.utils.UpdateChecker
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val agreementDataStore: UserAgreementDataStore by inject()
    private val authRepository: AuthRepository by inject()    
    private val themeStore: ThemeColorDataStore by inject()
    
    companion object {
        private const val TAG = "NeoActivity"
        const val ACTION_UPDATES = "${BuildConfig.APPLICATION_ID}.intent.action.UPDATES"
        const val ACTION_INSTALL = "${BuildConfig.APPLICATION_ID}.intent.action.INSTALL"
        const val EXTRA_UPDATES = "${BuildConfig.APPLICATION_ID}.intent.extra.UPDATES"
        const val EXTRA_CACHE_FILE_NAME = "${BuildConfig.APPLICATION_ID}.intent.extra.CACHE_FILE_NAME"
    }
    
    fun launchLockPrompt(action: () -> Unit) {
    // TODO: 待重新实现生物识别逻辑
}

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_BBQ_Main)
        super.onCreate(savedInstanceState)

        setContent {
            val navigationState = rememberNavigationState(
                startRoute = Home,
                topLevelRoutes = topLevelRoutes
            )
            val focusManager = LocalFocusManager.current 
            val topAppBarController = remember { TopAppBarController() }
            val navigator = remember(focusManager, topAppBarController, navigationState) {
        Navigator(navigationState, focusManager, topAppBarController)
    }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalNavigationState provides navigationState,
                LocalTopAppBarController provides topAppBarController,
            ) {
                val snackbarHostState = remember { SnackbarHostState() }

                val userAccepted by agreementDataStore.isUserAgreementAccepted.collectAsState(initial = true)
                val xiaoquAccepted by agreementDataStore.isXiaoquAccepted.collectAsState(initial = true)

                var isAgreementDataLoaded by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(150)
                    isAgreementDataLoaded = true
                }

                val showAgreementDialog = isAgreementDataLoaded && !(userAccepted && xiaoquAccepted)

                BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
                    MainScreenContent(
                        navigationState = navigationState,
                        navigator = navigator,
                        snackbarHostState = snackbarHostState,
                        showAgreementDialog = showAgreementDialog,
                        onAgreementDismiss = { finish() }
                    )
                }
            }
        }

        lifecycleScope.launch {
            delay(10000)
            val userCredentials = authRepository.credentials.first()
            if (userCredentials.token.isNotEmpty()) {
                startHeartbeatService(this@MainActivity, userCredentials.token)
            }
        }
    }

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val crashReport = getCrashReport(throwable)
            val logDao: LogDao by inject()
            CoroutineScope(Dispatchers.IO).launch {
                val logEntry = LogEntry(
                    type = "CRASH",
                    requestBody = "MainActivity 崩溃",
                    responseBody = crashReport,
                    status = "FAILURE"
                )
                logDao.insert(logEntry)
            }.invokeOnCompletion {
                CrashLogActivity.start(BBQApplication.instance, crashReport)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    private fun getCrashReport(throwable: Throwable): String {
        val stackTrace = throwable.stackTraceToString()
        val deviceInfo = """
            设备型号: ${android.os.Build.MODEL}
            Android 版本: ${android.os.Build.VERSION.RELEASE}
            App 版本: ${BuildConfig.VERSION_NAME}
        """.trimIndent()
        return """
            崩溃信息: ${throwable.message}
            
            设备信息:
            $deviceInfo
            
            堆栈跟踪:
            $stackTrace
        """.trimIndent()
    }

}

fun startHeartbeatService(context: Context, token: String) {
    Intent(context, HeartbeatService::class.java).apply {
        putExtra("TOKEN", token)
        context.startService(this)
    }
}

private fun tryAutoLogin(
    username: String,
    password: String,
    authRepository: AuthRepository, 
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val deviceId = authRepository.deviceId.first()
            val result = KtorClient.ApiServiceImpl.login(
                username = username,
                password = password,
                device = deviceId
            )

            withContext(Dispatchers.Main) {
                when {
                    result.isSuccess -> {
                        val loginResponse = result.getOrNull()
                        if (loginResponse != null && loginResponse.code == 1) {
                            val loginData = loginResponse.data
                            if (loginData != null) {
                                authRepository.saveCredentials(
                                    username,
                                    password,
                                    loginData.usertoken,
                                    loginData.id
                                )
                            } else {
                                authRepository.clearCredentials()
                                snackbarHostState.showSnackbar("登录数据为空")
                                navigator.navigate(Login)
                            }
                        } else {
                            authRepository.clearCredentials()
                            val errorMsg = loginResponse?.msg ?: "登录失败"
                            snackbarHostState.showSnackbar(errorMsg)
                            navigator.navigate(Login)
                        }
                    }
                    else -> {
                        authRepository.clearCredentials()
                        val exception = result.exceptionOrNull()
                        val errorMsg = when (exception) {
                            is IOException -> {
                                when {
                                    exception.message?.contains("429") == true -> "请求太频繁"
                                    exception.message?.contains("500") == true -> "服务器错误"
                                    else -> "网络错误: ${exception.message}"
                                }
                            }
                            else -> "登录异常: ${exception?.message ?: "未知错误"}"
                        }
                        snackbarHostState.showSnackbar(errorMsg)
                        navigator.navigate(Login)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                authRepository.clearCredentials()
                snackbarHostState.showSnackbar("登录异常: ${e.message}")
                navigator.navigate(Login)
            }
        }
    }
}