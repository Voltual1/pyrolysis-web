//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package me.voltual.pyrolysis.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import me.voltual.pyrolysis.PyrolysisNetworkException // 导入自定义异常

object BiliApiManager {
    private const val BILI_API_BASE_URL = "https://api.bilibili.com/"
    private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    // Ktor HttpClient 实例，移除 OkHttp 引擎指定以适配多平台
    private val httpClient = HttpClient {
        defaultRequest {
            url(BILI_API_BASE_URL)
            header(HttpHeaders.UserAgent, USER_AGENT_WEB)
            header(HttpHeaders.Referrer, "https://www.bilibili.com/")
            header(HttpHeaders.Accept, "application/json")
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000L
            connectTimeoutMillis = 30000L
            socketTimeoutMillis = 30000L
        }
    }

    val instance: BiliApiService = BiliApiServiceImpl()

    object models {
        @Serializable
        data class BiliVideoInfoResponse(
            val code: Int,
            val message: String,
            val data: VideoData? = null
        )

        @Serializable
        data class VideoData(
            val bvid: String,
            val aid: Long,
            val title: String,
            val pic: String,
            val duration: Int,
            val owner: Owner,
            val pages: List<VideoPage>
        )

        @Serializable
        data class Owner(
            val mid: Long,
            val name: String,
            val face: String
        )

        @Serializable
        data class VideoPage(
            val cid: Long,
            val part: String,
            val duration: Int
        )

        @Serializable
        data class BiliPlayUrlResponse(
            val code: Int,
            val message: String,
            val data: PlayUrlData? = null
        )

        @Serializable
        data class PlayUrlData(
            val durl: List<DashUrl>? = null
        )

        @Serializable
        data class DashUrl(
            val url: String,
            val size: Long,
            val backup_url: List<String>? = null
        )
    }

    interface BiliApiService {
        suspend fun getVideoInfo(bvid: String): Result<models.BiliVideoInfoResponse>
        suspend fun getPlayUrl(
            bvid: String,
            cid: Long,
            quality: Int = 80,
            fnval: Int = 1
        ): Result<models.BiliPlayUrlResponse>
        suspend fun getDanmaku(cid: Long): Result<ByteArray>
    }

    private class BiliApiServiceImpl : BiliApiService {
        companion object {
            const val MAX_RETRIES = 3
            const val RETRY_DELAY = 1000L
        }

        /**
         * 使用 PyrolysisNetworkException 替代 java.io.IOException
         */
        private suspend inline fun <reified T> safeApiCall(block: suspend () -> T): Result<T> {
            var attempts = 0
            while (attempts < MAX_RETRIES) {
                try {
                    val result = block()
                    return Result.success(result)
                } catch (e: Exception) {
                    attempts++
                    // 如果是最后一次尝试，或者不是网络相关的异常，则直接抛出
                    if (attempts >= MAX_RETRIES) {
                        return Result.failure(PyrolysisNetworkException("BiliApi 请求在 $MAX_RETRIES 次尝试后失败: ${e.message}", e))
                    }
                    delay(RETRY_DELAY)
                }
            }
            return Result.failure(PyrolysisNetworkException("BiliApi 请求执行了未知的失败路径"))
        }

        override suspend fun getVideoInfo(bvid: String): Result<models.BiliVideoInfoResponse> {
            return safeApiCall {
                httpClient.get("x/web-interface/view") {
                    parameter("bvid", bvid)
                }.body()
            }
        }

        override suspend fun getPlayUrl(
            bvid: String,
            cid: Long,
            quality: Int,
            fnval: Int
        ): Result<models.BiliPlayUrlResponse> {
            return safeApiCall {
                httpClient.get("x/player/playurl") {
                    parameter("bvid", bvid)
                    parameter("cid", cid)
                    parameter("qn", quality)
                    parameter("fnval", fnval)
                }.body()
            }
        }

        override suspend fun getDanmaku(cid: Long): Result<ByteArray> {
            return safeApiCall {
                httpClient.get("x/v1/dm/list.so") {
                    parameter("oid", cid)
                }.body<ByteArray>()
            }
        }
    }

    fun close() {
        httpClient.close()
    }
}