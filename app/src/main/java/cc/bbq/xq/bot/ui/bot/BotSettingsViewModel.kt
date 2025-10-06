//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.bot.ui.bot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.bot.AuthManager // 补全 import
import cc.bbq.xq.bot.BBQApplication
import cc.bbq.xq.bot.BotService
import cc.bbq.xq.bot.BotStatus
import cc.bbq.xq.bot.data.BotConfig
import cc.bbq.xq.bot.data.BotConfigDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BotSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val botConfigDataStore = (application as BBQApplication).botConfigDataStore
    
    private val database = (application as BBQApplication).database

    val config: StateFlow<BotConfig?> = botConfigDataStore.configFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val botStatus: StateFlow<BotStatus> = BotService.botStatus

    private val _botAccountUsername = MutableStateFlow<String?>(null)
    val botAccountUsername: StateFlow<String?> = _botAccountUsername.asStateFlow()

    init {
        checkBotAccountStatus()
    }

    fun checkBotAccountStatus() {
        viewModelScope.launch {
            val botCredentials = AuthManager.getBotCredentials(getApplication())
            _botAccountUsername.value = botCredentials?.first
        }
    }
    
    // 新增：处理超级缓存开关的逻辑
    fun onSuperCacheEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            botConfigDataStore.updateSuperCacheEnabled(isEnabled)
        }
    }
    
    // 新增：清空网络缓存的方法
    fun clearNetworkCache() {
        viewModelScope.launch {
            database.networkCacheDao().clearAll()
        }
    }

    fun logoutBotAccount() {
        viewModelScope.launch {
            AuthManager.clearBotCredentials(getApplication())
            checkBotAccountStatus()
        }
    }
    
    fun onBotEnabledChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            botConfigDataStore.updateBotEnabled(isEnabled)
            if (isEnabled) {
                startBot()
            } else {
                stopBot()
            }
        }
    }

    fun updateApiKey(key: String) = viewModelScope.launch {
        botConfigDataStore.updateApiKey(key)
    }

    fun updateModelName(name: String) = viewModelScope.launch {
        botConfigDataStore.updateModelName(name)
    }

    fun updateApiEndpoint(endpoint: String) = viewModelScope.launch {
        botConfigDataStore.updateApiEndpoint(endpoint)
    }

    fun updatePollingInterval(seconds: String) {
        val interval = seconds.toLongOrNull() ?: BotConfigDataStore.DEFAULT_POLLING_INTERVAL
        viewModelScope.launch {
            botConfigDataStore.updatePollingInterval(interval)
        }
    }

    fun updatePromptTemplate(template: String) = viewModelScope.launch {
        botConfigDataStore.updatePromptTemplate(template)
    }

    private fun startBot() {
        BotService.startService(getApplication())
    }

    private fun stopBot() {
        BotService.stopService(getApplication())
    }
}