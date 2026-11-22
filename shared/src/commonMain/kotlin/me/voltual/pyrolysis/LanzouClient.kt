// FILE: shared/src/commonMain/kotlin/me/voltual/pyrolysis/LanzouClient.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package me.voltual.pyrolysis

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.voltual.pyrolysis.utils.createUploadProvider
import me.voltual.pyrolysis.utils.uploadMultipartFileNative
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LanzouNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

object LanzouClient {
    private val commonJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(commonJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10.minutes.inWholeMilliseconds
            connectTimeoutMillis = 30.seconds.inWholeMilliseconds
            socketTimeoutMillis = 10.minutes.inWholeMilliseconds
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    @Serializable
    data class HealthResponse(
        val success: Boolean = false,
        val service: String? = null,
        val version: String? = null,
        val mode: String? = null,
        val max_file_size: Long = 104857600L
    )

    @Serializable
    data class LanzouUser(
        val id: Long = 0,
        val username: String = "",
        val has_lanzou: Boolean = false,
        val lanzou_username: String = "",
        val folder_id: String = "",
        val folder_name: String = "",
        val api_key_prefix: String = ""
    )

    @Serializable
    data class LanzouBindDetail(
        val lanzou_username: String = "",
        val lanzou_uid: String = "",
        val folder_id: String = "",
        val folder_name: String = ""
    )

    @Serializable
    data class GuestAuthResponse(
        val success: Boolean = false,
        val created: Boolean = false,
        val user: LanzouUser? = null,
        val csrf_token: String = "",
        val api_key: String? = null,
        val message: String? = null
    )

    @Serializable
    data class BindResponse(
        val success: Boolean = false,
        val message: String? = null,
        val user: LanzouUser? = null,
        val lanzou: LanzouBindDetail? = null
    )

    @Serializable
    data class BindRequest(
        val lanzou_username: String,
        val lanzou_password: String
    )

    @Serializable
    data class LanzouApkInfo(
        val app_name: String? = null,
        val package_name: String? = null,
        val version_name: String? = null,
        val version_code: String? = null,
        val min_sdk: String? = null,
        val min_android: String? = null,
        val icon_url: String? = null,
        val icon_name: String? = null,
        val icon_size: Long? = null,
        val icon_type: String? = null,
        val icon_error: String? = null
    )

    @Serializable
    data class LanzouUploadData(
        val file_name: String = "",
        val file_size: Long = 0L,
        val folder_id: String = "",
        val folder_name: String = "",
        val file_id: String = "",
        val f_id: String = "",
        val share_url: String = "",
        val password: String = "",
        val apk: LanzouApkInfo? = null
    )

    @Serializable
    data class LanzouUploadResponse(
        val success: Boolean = false,
        val code: String? = null,
        val message: String? = null,
        val request_id: String? = null,
        val data: LanzouUploadData? = null
    )

    interface ApiService {
        suspend fun createOrRefreshGuestSession(existingCookie: String? = null): Result<Pair<GuestAuthResponse, String>>
        suspend fun bindLanzouAccount(
            sessionCookie: String,
            csrfToken: String,
            lanzouUsername: String,
            lanzouPassword: String
        ): Result<BindResponse>
        suspend fun uploadFile(
            sessionCookie: String,
            csrfToken: String,
            file: PlatformFile
        ): Result<LanzouUploadResponse>
    }

    object ApiServiceImpl : ApiService {
        private fun getEndpoint(path: String): String {
            val base = ApiUrlProvider.lanzouBaseUrl.removeSuffix("/")
            val cleanPath = path.removePrefix("/")
            return "$base/$cleanPath"
        }

        override suspend fun createOrRefreshGuestSession(existingCookie: String?): Result<Pair<GuestAuthResponse, String>> {
            return try {
                val response = httpClient.post(getEndpoint("api/auth/guest")) {
                    contentType(ContentType.Application.Json)
                    if (!existingCookie.isNullOrBlank()) {
                        header(HttpHeaders.Cookie, existingCookie)
                    }
                    setBody("{}")
                }

                val bodyText = response.bodyAsText()
                val parsed = commonJson.decodeFromString<GuestAuthResponse>(bodyText)
                if (!response.status.isSuccess() || !parsed.success) {
                    return Result.failure(LanzouNetworkException(parsed.message ?: "建立蓝云会话失败 (${response.status})"))
                }

                val setCookieHeaders = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
                val extractedSession = setCookieHeaders
                    .mapNotNull { raw ->
                        raw.split(";")
                            .map { it.trim() }
                            .firstOrNull { it.startsWith("ly_session=") }
                    }
                    .firstOrNull()
                    ?: existingCookie.orEmpty()

                Result.success(parsed to extractedSession)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun bindLanzouAccount(
            sessionCookie: String,
            csrfToken: String,
            lanzouUsername: String,
            lanzouPassword: String
        ): Result<BindResponse> {
            return try {
                val response = httpClient.post(getEndpoint("api/lanzou/bind")) {
                    contentType(ContentType.Application.Json)
                    if (sessionCookie.isNotBlank()) {
                        header(HttpHeaders.Cookie, sessionCookie)
                    }
                    if (csrfToken.isNotBlank()) {
                        header("X-CSRF-Token", csrfToken)
                    }
                    setBody(BindRequest(lanzou_username = lanzouUsername, lanzou_password = lanzouPassword))
                }

                val bodyText = response.bodyAsText()
                val parsed = commonJson.decodeFromString<BindResponse>(bodyText)
                if (!response.status.isSuccess() || !parsed.success) {
                    return Result.failure(LanzouNetworkException(parsed.message ?: "绑定蓝奏云账号失败 (${response.status})"))
                }

                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun uploadFile(
            sessionCookie: String,
            csrfToken: String,
            file: PlatformFile
        ): Result<LanzouUploadResponse> {
            return try {
                val uploadUrl = getEndpoint("api/upload")
                val requestHeaders = buildMap {
                    if (sessionCookie.isNotBlank()) put(HttpHeaders.Cookie, sessionCookie)
                    if (csrfToken.isNotBlank()) put("X-CSRF-Token", csrfToken)
                }

                val nativeText = uploadMultipartFileNative(
                    url = uploadUrl,
                    file = file,
                    fileParamName = "file",
                    headers = requestHeaders
                )

                if (nativeText != null) {
                    val parsed = commonJson.decodeFromString<LanzouUploadResponse>(nativeText)
                    if (parsed.success && parsed.data != null) {
                        Result.success(parsed)
                    } else {
                        Result.failure(LanzouNetworkException(parsed.message ?: "蓝云上传失败"))
                    }
                } else {
                    val response = httpClient.submitFormWithBinaryData(
                        url = uploadUrl,
                        formData = formData {
                            append("file", createUploadProvider(file), Headers.build {
                                append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            })
                        }
                    ) {
                        if (sessionCookie.isNotBlank()) {
                            header(HttpHeaders.Cookie, sessionCookie)
                        }
                        if (csrfToken.isNotBlank()) {
                            header("X-CSRF-Token", csrfToken)
                        }
                    }

                    val bodyText = response.bodyAsText()
                    val parsed = commonJson.decodeFromString<LanzouUploadResponse>(bodyText)
                    if (response.status.isSuccess() && parsed.success && parsed.data != null) {
                        Result.success(parsed)
                    } else {
                        Result.failure(LanzouNetworkException(parsed.message ?: "蓝云上传失败 (${response.status})"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun close() {
        httpClient.close()
    }
}