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
import androidx.compose.ui.platform.LocalView
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
            val view = LocalView.current 
            val topAppBarController = remember { TopAppBarController() }
            val navigator = remember(navigationState, view) {
                Navigator(navigationState, view, topAppBarController)
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

val topLevelRoutes: Set<NavKey> = setOf(Home)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    navigationState: NavigationState,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    showAgreementDialog: Boolean,
    onAgreementDismiss: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authRepository: AuthRepository = koinInject() 
    val themeStore: ThemeColorDataStore = koinInject()

    val currentRoute = navigationState.currentRoute
    val currentTopLevelRoute = navigationState.topLevelRoute

    val showBackButton = remember(currentRoute) {
        currentRoute != Home && currentRoute != Login
    }
    
    val topAppBarController = LocalTopAppBarController.current

    val isPlayerScreen = remember(currentRoute) { currentRoute is Player }

    val useDarkTheme = ThemeManager.isAppDarkTheme
    val lightBgUri by themeStore.drawerHeaderLightBackgroundUriFlow.collectAsState(initial = null)
    val darkBgUri by themeStore.drawerHeaderDarkBackgroundUriFlow.collectAsState(initial = null)
    val drawerHeaderBackgroundUri = if (useDarkTheme) darkBgUri else lightBgUri

    val isLoggedIn = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val credentials = authRepository.credentials.first()
        isLoggedIn.value = credentials.userId != 0L
        if (isLoggedIn.value) {
            tryAutoLogin(
                username = credentials.username, 
                password = credentials.password, 
                authRepository = authRepository, 
                navigator = navigator, 
                snackbarHostState = snackbarHostState
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.width(360.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .roundScreenPadding()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DrawerHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        backgroundUri = drawerHeaderBackgroundUri
                    )
                    NavigationDrawerItems(
                        navigator = navigator,
                        currentTopLevelRoute = currentTopLevelRoute,
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            }
        },
        gesturesEnabled = true,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                if (!isPlayerScreen) {
                    TopAppBar(
                        title = {
                            val customContent = topAppBarController.titleContent
                            if (customContent != null) {
                                customContent()
                            } else {
                                Text(
                                    text = topAppBarController.customTitle ?: getTitleForDestination(currentRoute),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = { navigator.goBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.open_drawer),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        actions = {
                            if (currentRoute != Login) {
                                IconButton(onClick = {
                                    navigator.navigate(Search(userId = null, nickname = null))
                                }) {
                                    Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { navigator.navigate(CreatePost) }) {
                                    Icon(Icons.Default.Add, "发帖", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { navigator.navigate(BrowseHistory) }) {
                                    Icon(Icons.Default.History, "浏览历史", tint = MaterialTheme.colorScheme.onSurface)
                                }

                                topAppBarController.actions.forEach { action ->
                                    val iconTint = action.tint?.invoke() ?: MaterialTheme.colorScheme.onSurface
                                    IconButton(onClick = action.onClick) {
                                        action.icon(iconTint)
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            snackbarHost = { BBQSnackbarHost(hostState = snackbarHostState) },
            content = { innerPadding ->
                val contentPadding = when {
                    isPlayerScreen -> PaddingValues(0.dp)
                    else -> innerPadding
                }

                val currentBackStack = navigationState.backStacks[currentTopLevelRoute] 
                    ?: navigationState.backStacks[navigationState.startRoute]!! 

                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding) 
                    .roundScreenPadding()
                ) {
                    BBQNavDisplay(
                        backStack = currentBackStack,
                        onBack = { navigator.goBack() },
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showAgreementDialog) {
                        UserAgreementDialog(
                            onAgreed = { /* ... */ },
                            onDismissRequest = onAgreementDismiss
                        )
                    }

                    CheckForUpdates(snackbarHostState)
                }
            }
        )
    }
}

@Composable
fun CheckForUpdates(snackbarHostState: SnackbarHostState) {
    val coroutineScope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val updateSettingsDataStore: UpdateSettingsDataStore = koinInject()
    
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val autoCheckUpdates = updateSettingsDataStore.autoCheckUpdates.first()
        if (autoCheckUpdates) {
            UpdateChecker.checkForUpdates() { result ->
                when (result) {
                    is UpdateCheckResult.Success -> {
                        updateInfo = result.updateInfo
                        showDialog = true
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("当前已是最新版本")
                        }
                    }
                    is UpdateCheckResult.Error -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(result.message)
                        }
                    }
                }
            }
        }
    }

    updateInfo?.let { info ->
        if (showDialog) {
            UpdateDialog(updateInfo = info) {
                showDialog = false
                updateInfo = null
            }
        }
    }
}

fun restartMainActivity(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        context.startActivity(it, options.toBundle())
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

@Composable
fun getTitleForDestination(route: NavKey?): String {
    return when (route) {
        Home -> "首页"
        Login -> "登录"
        is ResourcePlaza -> {
            when {
                route.isMyResource -> "我的资源"
                route.userId != -1L -> "Ta的资源"
                route.mode == "my_upload" -> "我的上传"
                route.mode == "my_favourite" -> "我的收藏"
                route.mode == "my_history" -> "历史足迹"
                else -> "资源广场"
            }
        }
        RankingList -> "天梯竞赛"
        MessageCenter -> "消息中心"
        BrowseHistory -> "浏览历史"
        Billing -> "账单"
        ThemeCustomize -> "主题设置"
        is Search -> "搜索"
        is PostDetail -> "帖子详情"
        is UserDetail -> "用户详情"
        is MyPosts -> "我的帖子"
        CreatePost -> "创建新帖"
        is CreateRefundPost -> "申请退币"
        CreateAppRelease -> "发布应用"
        is UpdateAppRelease -> "更新应用"
        LogViewer -> "日志"
        is AccountProfile -> "账号资料"
        FollowList -> "我的关注"
        FanList -> "我的粉丝"
        MyLikes -> "我喜欢的"
        HotPosts -> "热点"
        FollowingPosts -> "关注的人"
        Community -> "社区"
        is PaymentForApp -> "买应用"
        is PaymentForPost -> "给帖子投币"
        PaymentCenterAdvanced -> "投币"
        is Player -> "视频播放"
        About -> "关于"
        is ImagePreview -> "图片预览"
        StoreManager -> "存储管理"
        is AppDetail -> "应用详情"
        is AppPage -> "应用页"
        UpdateSettings -> "更新设置"
        MyComments -> "我的评论"
        MyReviews -> "我的评价"
        SignInSettings -> "签到设置"
        PrefsReposPage -> "仓库管理"
        Explore -> "仓库探索"
        SearchPage -> "搜索页"
        SortFilterSheet -> "排序和过滤"
        else -> "在~ $route ~里~哦"
    }
}