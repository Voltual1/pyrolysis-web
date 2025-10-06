//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.


package cc.bbq.xq.bot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.qubotConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "qubot_config")

data class BotConfig(
    val isBotEnabled: Boolean,
    val apiKey: String,
    val modelName: String,
    val apiEndpoint: String, // 补全：apiEndpoint 字段
    val isSuperCacheEnabled: Boolean, // 新增
    val promptTemplate: String,
    val pollingIntervalSeconds: Long
)

class BotConfigDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.qubotConfigDataStore

    companion object {
        val IS_BOT_ENABLED = booleanPreferencesKey("is_bot_enabled")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val API_ENDPOINT = stringPreferencesKey("api_endpoint") // 补全：API_ENDPOINT Key
        val IS_SUPER_CACHE_ENABLED = booleanPreferencesKey("is_super_cache_enabled") // 新增
        val PROMPT_TEMPLATE = stringPreferencesKey("prompt_template")
        val POLLING_INTERVAL_SECONDS = longPreferencesKey("polling_interval_seconds")

        const val DEFAULT_MODEL_NAME = "……:free"
        const val DEFAULT_API_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions" // 补全：默认端点
        const val DEFAULT_POLLING_INTERVAL: Long = 60
        val DEFAULT_PROMPT_TEMPLATE = """
        你是一个活跃在小趣空间社区的AI助手。小趣空间是一个智能手表应用市场和社区平台。

        环境信息：
        - 当前帖子标题：{title}
        - 帖子内容：{content}
        - 作者：{author}
        - 版块：{section}

        请生成一个友好、有帮助的评论：
        1. 针对帖子内容进行有意义的回应
        2. 保持积极友好的语气
        3. 避免重复帖子中已有的内容
        4. 长度控制在50-150字之间
        5. 不要使用markdown格式
        6. 如果是技术问题，提供建设性建议
        7. 如果是分享类内容，表达欣赏和共鸣

        请用自然的中文回复：
        """.trimIndent()
    }

    val configFlow: Flow<BotConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isBotEnabled = preferences[IS_BOT_ENABLED] ?: false
            val apiKey = preferences[API_KEY] ?: ""
            val modelName = preferences[MODEL_NAME] ?: DEFAULT_MODEL_NAME
            val apiEndpoint = preferences[API_ENDPOINT] ?: DEFAULT_API_ENDPOINT // 补全：读取端点
            val isSuperCacheEnabled = preferences[IS_SUPER_CACHE_ENABLED] ?: false // 新增
            val promptTemplate = preferences[PROMPT_TEMPLATE] ?: DEFAULT_PROMPT_TEMPLATE
            val pollingInterval = preferences[POLLING_INTERVAL_SECONDS] ?: DEFAULT_POLLING_INTERVAL

            BotConfig(
                isBotEnabled = isBotEnabled,
                apiKey = apiKey,
                modelName = modelName,
                apiEndpoint = apiEndpoint, // 补全：构造函数赋值
                promptTemplate = promptTemplate,
                isSuperCacheEnabled = isSuperCacheEnabled, // 新增
                pollingIntervalSeconds = pollingInterval
            )
        }

    suspend fun updateBotEnabled(isEnabled: Boolean) {
        dataStore.edit { it[IS_BOT_ENABLED] = isEnabled }
    }

    suspend fun updateApiKey(key: String) {
        dataStore.edit { it[API_KEY] = key }
    }

    suspend fun updateModelName(name: String) {
        dataStore.edit { it[MODEL_NAME] = name }
    }

    // 补全：完整的 updateApiEndpoint 方法
    suspend fun updateApiEndpoint(endpoint: String) {
        dataStore.edit { it[API_ENDPOINT] = endpoint }
    }

    suspend fun updatePromptTemplate(template: String) {
        dataStore.edit { it[PROMPT_TEMPLATE] = template }
    }
    
    // 新增
    suspend fun updateSuperCacheEnabled(isEnabled: Boolean) {
        dataStore.edit { it[IS_SUPER_CACHE_ENABLED] = isEnabled }
    }

    suspend fun updatePollingInterval(seconds: Long) {
        dataStore.edit { it[POLLING_INTERVAL_SECONDS] = seconds }
    }
}