//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。

package cc.bbq.xq.bot

import android.util.Log
import cc.bbq.xq.bot.data.BotConfigDataStore
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url // 核心修改 #1: 导入 @Url 注解
import retrofit2.http.Streaming
import java.io.IOException

object LlmManager {
    private const val TAG = "LlmManager"    

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 创建一个可以动态添加 Authorization Header 的 OkHttp 客户端
    private fun createHttpClient(apiKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKey))
            .build()
    }

    // 核心修改 #3: 创建一个通用的 Retrofit 实例，其 baseUrl 只是一个占位符
    private fun getApiService(apiKey: String): LlmApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKey))
            .build()
        
        return Retrofit.Builder()
            .baseUrl("http://localhost/") // 这个 URL 不会被使用，因为我们用了 @Url
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LlmApiService::class.java)
    }

    /**
     * 主调用函数：生成评论
     * @param apiKey OpenRouter API Key
     * @param model 使用的 LLM 模型
     * @param systemPrompt 系统提示
     * @param userPrompt 用户提示 (包含帖子信息)
     * @return 生成的评论文本，如果失败则返回 null
     */
    suspend fun generateComment(
        apiKey: String,
        apiEndpoint: String, // 新增：接收完整的端点 URL
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): String? {
        // ... API Key 检查 ...

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            )
        )

        return try {
            // 核心修改 #4: 将完整的端点 URL 传递给 Retrofit 方法
            val response = getApiService(apiKey).getChatCompletions(apiEndpoint, request)
            if (response.isSuccessful) {
                response.body()?.let { parseStreamingResponse(it) }
            } else {
                Log.e(TAG, "API Error: ${response.code()} - ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network request failed", e)
            null
        }
    }

    /**
     * 解析 OpenRouter 的流式响应 (text/event-stream)
     */
    private fun parseStreamingResponse(body: ResponseBody): String {
        val result = StringBuilder()
        val reader = body.charStream().buffered()
        val chunkAdapter = moshi.adapter(ChatCompletionChunk::class.java)

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("data:")) {
                        val json = line.substring(5).trim()
                        if (json != "[DONE]") {
                            try {
                                val chunk = chunkAdapter.fromJson(json)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let {
                                    result.append(it)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse stream chunk: $json", e)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading streaming response", e)
        }

        Log.d(TAG, "Generated comment: $result")
        return result.toString()
    }

    // --- 数据模型 ---

    @JsonClass(generateAdapter = true)
    data class ChatRequest(
        @Json(name = "model") val model: String,
        @Json(name = "messages") val messages: List<ChatMessage>,
        @Json(name = "temperature") val temperature: Double = 0.6,
        @Json(name = "stream") val stream: Boolean = true
    )

    @JsonClass(generateAdapter = true)
    data class ChatMessage(
        @Json(name = "role") val role: String,
        @Json(name = "content") val content: String
    )

    // 用于解析流式响应中的每个 JSON 对象
    @JsonClass(generateAdapter = true)
    data class ChatCompletionChunk(
        @Json(name = "choices") val choices: List<ChoiceChunk>
    )

    @JsonClass(generateAdapter = true)
    data class ChoiceChunk(
        @Json(name = "delta") val delta: Delta
    )

    @JsonClass(generateAdapter = true)
    data class Delta(
        @Json(name = "content") val content: String?,
        // 新增：兼容硅基流动的 API
        @Json(name = "reasoning_content") val reasoning_content: String?
    )

    // --- Retrofit 接口 ---

    interface LlmApiService {
        // 核心修改 #5: 使用 @Url 注解来接收一个完整的 URL
        @POST // POST 注解中不再需要路径
        @Streaming
        suspend fun getChatCompletions(
            @Url fullUrl: String,
            @Body request: ChatRequest,
            @Header("HTTP-Referer") referer: String = "https://gitee.com/Voltula/BBQ",
            @Header("X-Title") title: String = "QUBOT"
        ): retrofit2.Response<ResponseBody>
    }

    // --- OkHttp 拦截器 ---

    private class AuthInterceptor(private val apiKey: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .build()
            return chain.proceed(newRequest)
        }
    }
}