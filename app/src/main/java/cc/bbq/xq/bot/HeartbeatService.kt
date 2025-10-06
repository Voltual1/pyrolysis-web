//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.bot

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import cc.bbq.xq.bot.data.BotConfigDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class HeartbeatService : Service() {
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var botConfigDataStore: BotConfigDataStore

    override fun onCreate() {
        super.onCreate()
        botConfigDataStore = (application as BBQApplication).botConfigDataStore
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startHeartbeat()
        return START_STICKY
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        
        heartbeatJob = scope.launch {
            while (true) {
                Log.d("HEARTBEAT", "Starting new heartbeat cycle.")
                
                // =======================================================
                // == 核心改造：并发处理用户和机器人的心跳 ==
                // =======================================================
                supervisorScope {
                    val userToken = AuthManager.getCredentials(this@HeartbeatService)?.third
                    val botToken = AuthManager.getBotCredentials(this@HeartbeatService)?.third

                    if (!userToken.isNullOrBlank()) {
                        launch {
                            try {
                                val response = RetrofitClient.instance.heartbeat(token = userToken)
                                Log.d("HEARTBEAT", "User Account Heartbeat Status: ${response.code()}")
                            } catch (e: Exception) {
                                Log.e("HEARTBEAT", "User Account Heartbeat Failed: ${e.message}")
                            }
                        }
                    }

                    if (!botToken.isNullOrBlank()) {
                        launch {
                            try {
                                val response = RetrofitClient.instance.heartbeat(token = botToken)
                                Log.d("HEARTBEAT", "Bot Account Heartbeat Status: ${response.code()}")
                            } catch (e: Exception) {
                                Log.e("HEARTBEAT", "Bot Account Heartbeat Failed: ${e.message}")
                            }
                        }
                    }
                }
                // =======================================================

                checkAndWakeUpBot()
                
                delay(60 * 1000)
            }
        }
    }

    private suspend fun checkAndWakeUpBot() {
        try {
            val config = botConfigDataStore.configFlow.first()
            val botStatus = BotService.botStatus.value

            if (config.isBotEnabled && botStatus == BotStatus.IDLE) {
                Log.i("HEARTBEAT", "Bot is enabled but idle. Waking it up...")
                BotService.startService(this@HeartbeatService)
            }
        } catch (e: Exception) {
            Log.e("HEARTBEAT", "Failed to check and wake up bot", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}