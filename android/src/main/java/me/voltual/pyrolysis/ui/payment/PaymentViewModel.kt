//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.payment

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.call.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.AuthManager
import me.voltual.pyrolysis.KtorClient
import org.koin.android.annotation.KoinViewModel
import kotlin.random.Random
import kotlin.time.Clock

private val Context.dataStore by preferencesDataStore(name = "payment_requests")

/**
 * 支付类型密封类
 */
enum class PaymentType {
    APP_PURCHASE,
    POST_REWARD
}

/**
 * 支付状态数据对象
 */
sealed class PaymentStatus {
    data object INITIAL : PaymentStatus()
    data object PROCESSING : PaymentStatus()
    data object SUCCESS : PaymentStatus()
    data object FAILED : PaymentStatus()
}

/**
 * 支付信息模型
 */
data class PaymentInfo(
    val type: PaymentType,
    val appId: Long = 0L,
    val appName: String = "",
    val versionId: Long = 0L,
    val price: Int = 0,
    val postId: Long = 0L,
    val postTitle: String = "",
    val locked: Boolean = true,
    val iconUrl: String = "",
    val previewContent: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val postTime: String = ""
)

/**
 * 统一的下载事件
 */
data class DownloadEvent(
    val url: String,
    val fileName: String,
    val headers: Map<String, String> = emptyMap()
)

@KoinViewModel
class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.applicationContext.dataStore

    private val _isLoadingBalance = MutableStateFlow(false)
    val isLoadingBalance: StateFlow<Boolean> = _isLoadingBalance.asStateFlow()

    private val _paymentInfo = MutableStateFlow<PaymentInfo?>(null)
    val paymentInfo: StateFlow<PaymentInfo?> = _paymentInfo

    private val _coinsBalance = MutableStateFlow<Int?>(null)
    val coinsBalance: StateFlow<Int?> = _coinsBalance

    private val _verificationResult = MutableStateFlow<String?>(null)
    val verificationResult: StateFlow<String?> = _verificationResult

    private val _paymentStatus = MutableStateFlow<PaymentStatus>(PaymentStatus.INITIAL)
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _paymentRequestId = MutableStateFlow<String?>(null)
    val paymentRequestId: StateFlow<String?> = _paymentRequestId

    private var _downloadUrl: String? = null
    private var _downloadFileName: String? = null
    
    private val _downloadEvent = MutableSharedFlow<DownloadEvent>()
    val downloadEvent = _downloadEvent.asSharedFlow()

    fun getDownloadUrl(): String? = _downloadUrl
    fun getDownloadFileName(): String? = _downloadFileName

    /**
     * Kotlin 原生随机 ID 生成器，替代 java.util.UUID
     */
    private fun generateRandomId(): String {
        val charPool = "0123456789abcdef"
        return (1..32)
            .map { Random.nextInt(0, charPool.length).let { charPool[it] } }
            .joinToString("")
    }

    fun setPaymentInfo(
        type: PaymentType,
        appId: Long = 0L,
        appName: String = "",
        versionId: Long = 0L,
        price: Int = 0,
        postId: Long = 0L,
        postTitle: String = "",
        locked: Boolean = true,
        iconUrl: String = "",
        previewContent: String = "",
        authorName: String = "",
        authorAvatar: String = "",
        postTime: String = ""
    ) {
        _paymentInfo.value = PaymentInfo(
            type = type,
            appId = appId,
            appName = appName,
            versionId = versionId,
            price = price,
            postId = postId,
            postTitle = postTitle,
            locked = locked,
            iconUrl = iconUrl,
            previewContent = previewContent,
            authorName = authorName,
            authorAvatar = authorAvatar,
            postTime = postTime
        )
        
        if (type == PaymentType.APP_PURCHASE && appName.isNotEmpty()) {
            _downloadFileName = "${appName.replace(" ", "_")}_v${versionId}.apk"
        }
    }
    
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun startDownload(url: String, fileName: String?) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            _downloadEvent.emit(
                DownloadEvent(
                    url = url,
                    fileName = fileName ?: "download_$now.apk"
                )
            )
        }
    }

    fun loadPostInfo(postId: Long) {
        viewModelScope.launch {
            val credentials = AuthManager.getCredentials(getApplication()).first()
            val token = credentials.token.ifEmpty {
                _errorMessage.value = "用户未登录"
                return@launch
            }

            KtorClient.ApiServiceImpl.getPostDetail(token = token, postId = postId).onSuccess { result ->
                val post = result.data
                _paymentInfo.value = PaymentInfo(
                    type = PaymentType.POST_REWARD,
                    postId = postId,
                    postTitle = post.title,
                    previewContent = post.content.take(30),
                    locked = false,
                    authorName = post.nickname,
                    authorAvatar = post.usertx,
                    postTime = post.create_time_ago
                )
            }.onFailure { error ->
                _errorMessage.value = "加载帖子失败: ${error.message}"
            }
        }
    }

    fun fetchCoinsBalance() {
        viewModelScope.launch {
            _isLoadingBalance.value = true
            _coinsBalance.value = null
            
            val credentials = AuthManager.getCredentials(getApplication()).first()
            if (credentials.userId == 0L) {
                _errorMessage.value = "用户未登录"
                _isLoadingBalance.value = false
                return@launch
            }

            KtorClient.ApiServiceImpl.getUserInformation(
                userId = credentials.userId,
                token = credentials.token
            ).onSuccess { result ->
                _coinsBalance.value = result.data.money
            }.onFailure { error ->
                _errorMessage.value = "获取余额失败: ${error.message}"
            }.also {
                _isLoadingBalance.value = false
            }
        }
    }

    fun verifyPaymentInfo() {
        viewModelScope.launch {
            val info = _paymentInfo.value ?: return@launch
            _verificationResult.value = null
            _errorMessage.value = null

            when (info.type) {
                PaymentType.APP_PURCHASE -> {
                    KtorClient.ApiServiceImpl.getAppsInformation(
                        token = "",
                        appsId = info.appId,
                        appsVersionId = info.versionId
                    ).onSuccess { result ->
                        _verificationResult.value = "验证成功: ${result.data.appname} (¥${result.data.pay_money})"
                    }.onFailure { error ->
                        _errorMessage.value = "应用验证失败: ${error.message}"
                    }
                }
                PaymentType.POST_REWARD -> {
                    KtorClient.ApiServiceImpl.getPostDetail(
                        token = "",
                        postId = info.postId
                    ).onSuccess { result ->
                        _verificationResult.value = "验证成功: ${result.data.title}"
                    }.onFailure { error ->
                        _errorMessage.value = "帖子验证失败: ${error.message}"
                    }
                }
            }
        }
    }

    fun executePayment(amount: Int) {
        viewModelScope.launch {
            val info = _paymentInfo.value ?: return@launch

            val requestId = generateRandomId()
            _paymentRequestId.value = requestId

            if (getPaymentRequestId(requestId) != null) {
                _errorMessage.value = "该支付请求已提交，请勿重复操作"
                _paymentStatus.value = PaymentStatus.FAILED
                return@launch
            }

            _paymentStatus.value = PaymentStatus.PROCESSING
            _errorMessage.value = null

            savePaymentRequestId(requestId)

            val credentials = AuthManager.getCredentials(getApplication()).first()
            if (credentials.token.isEmpty()) {
                _errorMessage.value = "用户未登录"
                _paymentStatus.value = PaymentStatus.FAILED
                removePaymentRequestId(requestId)
                return@launch
            }

            val response = when (info.type) {
                PaymentType.APP_PURCHASE -> KtorClient.ApiServiceImpl.payForApp(
                    token = credentials.token,
                    appsId = info.appId,
                    appsVersionId = info.versionId,
                    money = amount,
                    type = 0
                )
                PaymentType.POST_REWARD -> KtorClient.ApiServiceImpl.rewardPost(
                    token = credentials.token,
                    postId = info.postId,
                    money = amount,
                    payment = 0
                )
            }

            response.onSuccess { result ->
                if (result.code == 1) {
                    _paymentStatus.value = PaymentStatus.SUCCESS
                    _coinsBalance.value = (_coinsBalance.value ?: 0) - amount
                    if (info.type == PaymentType.APP_PURCHASE) {
                        _downloadUrl = result.getDownloadUrl()
                    }
                } else {
                    _errorMessage.value = "支付失败: ${result.msg}"
                    _paymentStatus.value = PaymentStatus.FAILED
                }
            }.onFailure { error ->
                _errorMessage.value = "支付错误: ${error.message}"
                _paymentStatus.value = PaymentStatus.FAILED
            }

            removePaymentRequestId(requestId)
            _paymentRequestId.value = null
        }
    }

    fun resetPaymentStatus() {
        _paymentStatus.value = PaymentStatus.INITIAL
        _errorMessage.value = null
        _downloadUrl = null
        _downloadFileName = null
    }

    private suspend fun savePaymentRequestId(requestId: String) {
        dataStore.edit { it[stringPreferencesKey(requestId)] = requestId }
    }

    private suspend fun getPaymentRequestId(requestId: String): String? {
        return dataStore.data.map { it[stringPreferencesKey(requestId)] }.first()
    }

    private suspend fun removePaymentRequestId(requestId: String) {
        dataStore.edit { it.remove(stringPreferencesKey(requestId)) }
    }
}