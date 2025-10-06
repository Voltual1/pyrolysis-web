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
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import cc.bbq.xq.bot.data.BotConfigDataStore
import cc.bbq.xq.bot.data.ProcessedPostsDataStore
import cc.bbq.xq.bot.data.db.LogRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.coroutineContext

class BotService : Service() {

    private lateinit var botConfigDataStore: BotConfigDataStore
    private lateinit var processedPostsDataStore: ProcessedPostsDataStore
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var logRepository: LogRepository
    private lateinit var moshi: Moshi

    private var botJob: Job? = null
    private val botScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "BotService"
        const val ACTION_START = "cc.bbq.xq.bot.action.START"
        const val ACTION_STOP = "cc.bbq.xq.bot.action.STOP"

        private val _botStatus = MutableStateFlow(BotStatus.IDLE)
        val botStatus: StateFlow<BotStatus> = _botStatus.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, BotService::class.java).apply {
                action = ACTION_START
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BotService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        botConfigDataStore = (application as BBQApplication).botConfigDataStore
        processedPostsDataStore = (application as BBQApplication).processedPostsDataStore
        logRepository = LogRepository()
        apiService = RetrofitClient.instance
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        _botStatus.value = BotStatus.IDLE
        Log.i(TAG, "BotService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startBot()
            ACTION_STOP -> stopBot()
        }
        return START_STICKY
    }

    private fun startBot() {
        if (botJob?.isActive == true) {
            Log.w(TAG, "Bot is already running.")
            return
        }
        Log.i(TAG, "Starting bot logic...")

        botJob = botScope.launch {
            _botStatus.value = BotStatus.RUNNING
            while (isActive) {
                performBotCycle()
                val interval = try {
                    botConfigDataStore.configFlow.first().pollingIntervalSeconds
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get interval from config, using default.", e)
                    BotConfigDataStore.DEFAULT_POLLING_INTERVAL
                }
                Log.d(TAG, "Cycle finished. Waiting for ${interval} seconds...")
                delay(interval * 1000)
            }
        }
    }

    private fun stopBot() {
        Log.i(TAG, "Stopping bot...")
        _botStatus.value = BotStatus.STOPPING
        botJob?.cancel()
        botJob = null
        _botStatus.value = BotStatus.IDLE
        stopSelf()
    }

    private suspend fun performBotCycle() {
        Log.i(TAG, "Starting new bot cycle.")
        try {
            val config = botConfigDataStore.configFlow.first()
            if (!config.isBotEnabled) {
                Log.w(TAG, "Bot is disabled in settings. Skipping cycle.")
                return
            }

            // =======================================================
            // == 核心改造：智能选择凭证 ==
            // =======================================================
            val botCredentials = AuthManager.getBotCredentials(this)
            val userCredentials = AuthManager.getCredentials(this)

            val credentialsToUse = botCredentials ?: userCredentials
            val accountType = if (botCredentials != null) "Bot Account" else "User Account"

            if (credentialsToUse == null || credentialsToUse.third.isBlank()) {
                Log.e(TAG, "No valid credentials (Bot or User) found. Skipping cycle.")
                _botStatus.value = BotStatus.ERROR
                return
            }
            // =======================================================
            
            if (config.apiKey.isBlank()) {
                Log.e(TAG, "API Key not set. Skipping cycle.")
                _botStatus.value = BotStatus.ERROR
                return
            }

            _botStatus.value = BotStatus.RUNNING
            val token = credentialsToUse.third
            Log.d(TAG, "Performing cycle with: $accountType")

            val response = apiService.getPostsList(limit = 10, page = 1)
            if (!response.isSuccessful || response.body() == null) {
                Log.e(TAG, "Failed to fetch post list: ${response.code()}")
                return
            }

            val posts = response.body()!!.data.list
            Log.d(TAG, "Fetched ${posts.size} posts.")

            for (post in posts) {
                if (!coroutineContext.isActive) break

                if (processedPostsDataStore.containsPostId(post.postid)) {
                    Log.d(TAG, "Skipping already processed post: ${post.postid}")
                    continue
                }

                Log.i(TAG, "Found new post to process: ${post.postid} - '${post.title}'")
                processSinglePost(post, token, config)
                
                delay(5000)
            }

        } catch (e: Exception) {
            Log.e(TAG, "An error occurred during bot cycle. Will retry on next cycle.", e)
            _botStatus.value = BotStatus.ERROR
        }
    }

    private suspend fun processSinglePost(
        post: RetrofitClient.models.Post,
        token: String,
        config: cc.bbq.xq.bot.data.BotConfig
    ) {
        val detailResponse = apiService.getPostDetail(token = token, postId = post.postid)
        if (!detailResponse.isSuccessful || detailResponse.body() == null) {
            Log.e(TAG, "Failed to fetch detail for post ${post.postid}")
            return
        }
        val postDetail = detailResponse.body()!!.data

        val userPrompt = config.promptTemplate
            .replace("{title}", postDetail.title)
            .replace("{content}", postDetail.content.take(500))
            .replace("{author}", postDetail.nickname)
            .replace("{section}", "${postDetail.section_name} / ${postDetail.sub_section_name}")

        val llmRequest = LlmManager.ChatRequest(
            model = config.modelName,
            messages = listOf(
                LlmManager.ChatMessage("system", "You are a helpful assistant."),
                LlmManager.ChatMessage("user", userPrompt)
            )
        )
        val llmRequestJson = moshi.adapter(LlmManager.ChatRequest::class.java).toJson(llmRequest)
        val comment = LlmManager.generateComment(
            apiKey = config.apiKey,
            apiEndpoint = config.apiEndpoint,
            model = llmRequest.model,
            systemPrompt = llmRequest.messages[0].content,
            userPrompt = llmRequest.messages[1].content
        )

        if (comment.isNullOrBlank()) {
            Log.e(TAG, "LLM failed to generate comment for post ${post.postid}")
            logRepository.insertLog("LLM_REQUEST", llmRequestJson, "NULL or BLANK response", "FAILURE")
            return
        }
        logRepository.insertLog("LLM_REQUEST", llmRequestJson, comment, "SUCCESS")

        Log.i(TAG, "Generated comment for post ${post.postid}: '$comment'")

        val commentResponse = apiService.postComment(
            token = token,
            postId = post.postid,
            content = comment,
            parentId = 0L,
            imageUrl = ""
        )
        val commentRequestJson = "postId: ${post.postid}, content: $comment"

        if (commentResponse.isSuccessful && commentResponse.body()?.code == 1) {
            Log.i(TAG, "Successfully posted comment to post ${post.postid}")
            processedPostsDataStore.addPostId(post.postid)
            logRepository.insertLog("POST_COMMENT", commentRequestJson, commentResponse.body().toString(), "SUCCESS")
        } else {
            val errorMsg = commentResponse.body()?.toString() ?: commentResponse.errorBody()?.string() ?: "Unknown Error"
            Log.e(TAG, "Failed to post comment to post ${post.postid}: $errorMsg")
            logRepository.insertLog("POST_COMMENT", commentRequestJson, errorMsg, "FAILURE")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "BotService destroyed.")
        botJob?.cancel()
        botScope.cancel()
        _botStatus.value = BotStatus.IDLE
    }

    override fun onBind(intent: Intent?): IBinder? = null
}