//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package me.voltual.pyrolysis.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// 注意：UpdateInfo 需要移动到 shared 模块的 commonMain 中
// 建议路径：shared/src/commonMain/kotlin/me/voltual/pyrolysis/data/UpdateInfo.kt
import me.voltual.pyrolysis.data.UpdateInfo

object KtorClient {
    private const val BASE_URL = "http://apk.xiaoqu.online/"
    private const val UPLOAD_BASE_URL = "https://file.bz6.top/"
    private const val WANYUEYUN_UPLOAD_BASE_URL = "http://wanyueyun-x.xbjstd.cn:9812/"
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY = 1000L
    private const val REQUEST_TIMEOUT = 30000L
    private const val CONNECT_TIMEOUT = 30000L
    private const val SOCKET_TIMEOUT = 30000L

    private val commonJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    // 使用平台特定引擎 (OkHttp for Android, Js for Web)
    val httpClient = HttpClient(getPlatformEngine()) {
        install(ContentNegotiation) {
            json(commonJson)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT
            connectTimeoutMillis = CONNECT_TIMEOUT
            socketTimeoutMillis = SOCKET_TIMEOUT
        }
        defaultRequest {
            url(BASE_URL)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            // Web 端如果遇到跨域问题，可能需要后端配合或调整 ContentType
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
        }
    }

    val uploadHttpClient = HttpClient(getPlatformEngine()) {
        defaultRequest { url(UPLOAD_BASE_URL) }
        install(ContentNegotiation) { json(commonJson) }
    }

    val wanyueyunUploadHttpClient = HttpClient(getPlatformEngine()) {
        defaultRequest { url(WANYUEYUN_UPLOAD_BASE_URL) }
        install(ContentNegotiation) { json(commonJson) }
    }

    // ===== 模型类定义 =====

    @Serializable
    data class BaseResponse(
        val code: Int,
        val msg: String,
        val data: JsonElement? = null,
        val timestamp: Long
    ) {
        fun getDownloadUrl(): String? {
            return if (data is JsonObject) {
                data["download"]?.toString()?.removeSurrounding("\"")
            } else {
                null
            }
        }
    }

    @Serializable
    data class PostDetailResponse(
        val code: Int,
        val msg: String,
        val data: PostDetail,
        val timestamp: Long
    )

    @Serializable
    data class PostDetail(
        val id: Long,
        val title: String,
        val content: String,
        val userid: Long,
        val create_time: String,
        val update_time: String,
        val username: String,
        val nickname: String,
        val usertx: String,
        val hierarchy: String,
        val section_name: String,
        val sub_section_name: String,
        val view: String,
        val thumbs: String,
        val comment: String,
        val img_url: List<String>? = null,
        val video_url: String? = null,
        val ip_address: String,
        val create_time_ago: String,
        val is_thumbs: Int,
        val is_collection: Int
    )

    @Serializable
    data class CommentListResponse(
        val code: Int,
        val msg: String,
        val data: CommentListData,
        val timestamp: Long
    )

    @Serializable
    data class CommentListData(
        val list: List<Comment>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class Comment(
        val id: Long,
        val content: String,
        val userid: Long,
        val time: String,
        val username: String,
        val nickname: String,
        val usertx: String,
        val hierarchy: String,
        val parentid: Long? = null,
        val parentnickname: String? = null,
        val parentcontent: String? = null,
        val image_path: List<String>? = null,
        val sub_comments_count: Int
    )

    @Serializable
    data class PostListResponse(
        val code: Int,
        val msg: String,
        val data: PostListData,
        val timestamp: Long
    )

    @Serializable
    data class PostListData(
        val list: List<Post>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class Post(
        val postid: Long,
        val title: String,
        val content: String,
        val userid: Long,
        val create_time: String,
        val update_time: String,
        val username: String,
        val nickname: String,
        val usertx: String,
        val hierarchy: String,
        val section_name: String,
        val sub_section_name: String,
        val view: String,
        val thumbs: String,
        val comment: String,
        val img_url: List<String>? = null
    )

    @Serializable
    data class LoginResponse(
        val code: Int,
        val msg: String,
        val data: LoginData? = null,
        val timestamp: Long
    )

    @Serializable
    data class LoginData(
        val usertoken: String,
        val id: Long,
        val username: String,
        val is_section_moderator: Int = 0
    )

    @Serializable
    data class HeartbeatResponse(
        val code: Int,
        val msg: String,
        val timestamp: Long
    )

    @Serializable
    data class UserInfoResponse(
        val code: Int,
        val msg: String,
        val data: UserData,
        val timestamp: Long
    )

    @Serializable
    data class UserData(
        val id: Long,
        val username: String,
        val usertx: String,
        val nickname: String,
        val hierarchy: String,
        val money: Int,
        val followerscount: String,
        val fanscount: String,
        val postcount: String,
        val likecount: String,
        val exp: Int,
        val create_time: String = "",
        val signlasttime: String = "",
        val series_days: Int = 0,
        val sign_today: Boolean = false  
    )

    @Serializable
    data class AppListResponse(
        val code: Int,
        val msg: String,
        val data: AppListData,
        val timestamp: Long
    )

    @Serializable
    data class AppListData(
        val list: List<AppItem>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class AppItem(
        val id: Long,
        val appname: String,
        val app_icon: String,
        val app_size: String,
        val download_count: Int,
        val create_time: String,
        val nickname: String,
        val apps_version_id: Long
    )

    @Serializable
    data class AppDetailResponse(
        val code: Int,
        val msg: String,
        val data: AppDetail,
        val timestamp: Long
    )

    @Serializable
    data class MessageNotificationResponse(
        val code: Int,
        val msg: String,
        val data: MessageNotificationData,
        val timestamp: Long
    )

    @Serializable
    data class MessageNotificationData(
        val list: List<MessageNotification>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class MessageNotification(
        val id: Long,
        val title: String,
        val content: String,
        val send_to: Long,
        val appid: Int,
        val type: Int,
        val time: String,
        val postid: Long?,
        val pic_url: String?,
        val user_id: Long,
        val status: Int,
        val is_admin: Int
    )

    @Serializable
    data class AppDetail(
        val id: Long,
        val appname: String,
        val app_icon: String,
        val app_size: String,
        val app_explain: String?,
        val app_introduce: String?,
        val app_introduction_image: String?,
        val is_pay: Int,
        val pay_money: Int,
        val download: String?,
        val create_time: String,
        val update_time: String,
        val userid: Long,
        val appid: Int,
        val category_id: Int,
        val sub_category_id: Int,
        val category_name: String,
        val category_icon: String,
        val sub_category_name: String,
        val username: String,
        val nickname: String,
        val usertx: String,
        val sex: Int,
        val signature: String,
        val exp: Int,
        val version: String,
        val apps_version_id: Long,
        val version_create_time: String,
        val app_introduction_image_array: List<String>?,
        val ip_address: String,
        val sexName: String,
        val badge: List<JsonObject>,
        val vip: Boolean,
        val hierarchy: String,
        val is_user_pay: Boolean,
        val download_count: Int,
        val comment_count: Int,
        val user_pay_count: Int,
        val reward_count: Int,
        val posturl: String?
    )

    @Serializable
    data class BrowseHistoryResponse(
        val code: Int,
        val msg: String,
        val data: BrowseHistoryData,
        val timestamp: Long
    )

    @Serializable
    data class BrowseHistoryData(
        val list: List<BrowseHistoryItem>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class BrowseHistoryItem(
        val postid: Long,
        val title: String,
        val content: String,
        val userid: Long,
        val create_time: String,
        val update_time: String,
        val username: String,
        val nickname: String,
        val usertx: String,
        val hierarchy: String,
        val section_name: String,
        val sub_section_name: String,
        val view: String,
        val thumbs: String,
        val comment: String,
        val img_url: List<String>? = null
    )

    @Serializable
    data class AppCommentListResponse(
        val code: Int,
        val msg: String,
        val data: AppCommentListData,
        val timestamp: Long
    )

    @Serializable
    data class AppCommentListData(
        val list: List<AppComment>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class AppComment(
        val id: Long,
        val content: String,
        val userid: Long,
        val time: String,
        val username: String,
        val nickname: String,
        val usertx: String,
        val hierarchy: String,
        val parentid: Long? = null,
        val parentnickname: String? = null,
        val parentcontent: String? = null,
        val image_path: List<String>? = null
    )

    @Serializable
    data class UserListResponse(
        val code: Int,
        val msg: String,
        val data: UserListData,
        val timestamp: Long
    )

    @Serializable
    data class UserListData(
        val list: List<UserItem>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class UserItem(
        val id: Long,
        val username: String,
        val nickname: String,
        val usertx: String,
        val sex: Int,
        val signature: String,
        val title: List<String>,
        val badge: List<JsonObject>,
        val vip: Boolean,
        val hierarchy: String,
        val status: Int,
        val sexName: String
    )

    @Serializable
    data class UserInformationResponse(
        val code: Int,
        val msg: String,
        val data: UserInformationData,
        val timestamp: Long
    )

    @Serializable
    data class UserInformationData(
        val id: Long,
        val follow_status: String,
        val username: String,
        val usertx: String,
        val nickname: String,
        val money: Int,
        val exp: Int,
        val followerscount: String,
        val fanscount: String,
        val postcount: String,
        val likecount: String,
        val hierarchy: String,
        val last_activity_time: String?,
        val series_days: Int?,
        val commentcount: String?
    )

    @Serializable
    data class BillingResponse(
        val code: Int,
        val msg: String,
        val data: BillingData,
        val timestamp: Long
    )

    @Serializable
    data class BillingData(
        val list: List<BillingItem>,
        val pagecount: Int,
        val current_number: Int
    )

    @Serializable
    data class BillingItem(
        val id: Long,
        val transaction_type: Int,
        val transaction_date: String,
        val transaction_amount: String,
        val remark: String,
        val type: Int
    )

    @Serializable
    data class RankingUser(
        val id: Long,
        val username: String,
        val nickname: String,
        val usertx: String,
        val money: Int? = null,
        val exp: Int? = null,
        val title: List<String>?,
        val badge: List<JsonObject>?
    )

    @Serializable
    data class RankingListResponse(
        val code: Int,
        val msg: String,
        val data: List<RankingUser>,
        val timestamp: Long
    )

    @Serializable
    data class AppCategory(
        val categoryId: Int?,
        val subCategoryId: Int?,
        val categoryName: String
    )

    @Serializable
    data class UploadResponse(
        val code: Int,
        val msg: String,
        val exists: Int? = null,
        val downurl: String?,
        val viewurl: String?
    )

    @Serializable
    data class WanyueyunUploadResponse(
        val code: Int,
        val msg: String,
        val data: String?
    )

    object JsonConverter {
        fun toJson(appDetail: AppDetail): String {
            return commonJson.encodeToString(AppDetail.serializer(), appDetail)
        }
        fun fromJson(jsonString: String): AppDetail? {
            return try {
                commonJson.decodeFromString(AppDetail.serializer(), jsonString)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend inline fun <reified T> safeApiCall(block: suspend () -> HttpResponse): Result<T> {
        var attempts = 0
        while (attempts < MAX_RETRIES) {
            try {
                val response = block()
                if (!response.status.isSuccess()) {
                    throw Exception("Request failed with status: ${response.status}")
                }
                return Result.success(response.body())
            } catch (e: Exception) {
                attempts++
                if (attempts >= MAX_RETRIES) return Result.failure(e)
                delay(RETRY_DELAY)
            }
        }
        return Result.failure(Exception("Request failed after $MAX_RETRIES attempts."))
    }

    private suspend inline fun <reified T> request(
        url: String,
        method: HttpMethod = HttpMethod.Post,
        parameters: Parameters = Parameters.Empty
    ): Result<T> {
        return safeApiCall {
            httpClient.request(url) {
                this.method = method
                this.setBody(FormDataContent(parameters))
            }
        }
    }

    interface ApiService {
        suspend fun login(appid: Int = 1, username: String, password: String, device: String): Result<LoginResponse>
        suspend fun heartbeat(appid: Int = 1, token: String): Result<HeartbeatResponse>
        suspend fun getPostsList(appid: Int = 1, limit: Int, page: Int, sort: String = "create_time", sortOrder: String = "desc", sectionId: Int? = null, userId: Long? = null): Result<PostListResponse>
        suspend fun getUserInfo(appid: Int = 1, token: String): Result<UserInfoResponse>
        suspend fun getPostDetail(appid: Int = 1, token: String, postId: Long): Result<PostDetailResponse>
        suspend fun getPostComments(appid: Int = 1, postId: Long, limit: Int, page: Int, sort: String = "time", sortOrder: String = "desc"): Result<CommentListResponse>
        suspend fun createPost(appid: Int = 1, token: String, title: String, content: String, sectionId: Int, imageUrls: String? = null, paidReading: Int = 0, downloadMethod: Int = 0): Result<BaseResponse>
        suspend fun getAppsList(appid: Int = 1, limit: Int, page: Int, sort: String = "update_time", sortOrder: String = "desc", categoryId: Int? = null, subCategoryId: Int? = null, appName: String? = null, userId: Long? = null): Result<AppListResponse>
        suspend fun getAppsInformation(appid: Int = 1, token: String, appsId: Long, appsVersionId: Long): Result<AppDetailResponse>
        suspend fun getAppsCommentList(appid: Int = 1, appsId: Long, appsVersionId: Long, limit: Int, page: Int, sortOrder: String = "asc"): Result<AppCommentListResponse>
        suspend fun postComment(appid: Int = 1, token: String, content: String, postId: Long? = null, parentId: Long? = null, imageUrl: String? = null): Result<BaseResponse>
        suspend fun getMessageNotifications(appid: Int = 1, token: String, limit: Int, page: Int): Result<MessageNotificationResponse>
        suspend fun getBrowseHistory(appid: Int = 1, token: String, limit: Int = 10, page: Int): Result<BrowseHistoryResponse>
        suspend fun getLikesRecords(appid: Int = 1, token: String, limit: Int = 10, page: Int): Result<PostListResponse>
        suspend fun searchPosts(appid: Int = 1, query: String, limit: Int = 10, page: Int, userId: Long? = null): Result<PostListResponse>
        suspend fun getHotPostsList(appid: Int = 1, limit: Int, page: Int, sortOrder: String = "desc", sort: String = "score"): Result<PostListResponse>
        suspend fun getMyFollowingPosts(appid: Int = 1, token: String, limit: Int, page: Int): Result<PostListResponse>
        suspend fun likePost(appid: Int = 1, token: String, postId: Long): Result<BaseResponse>
        suspend fun deletePost(appid: Int = 1, token: String, postId: Long): Result<BaseResponse>
        suspend fun getFanList(appid: Int = 1, token: String, limit: Int = 10, page: Int): Result<UserListResponse>
        suspend fun getFollowList(appid: Int = 1, token: String, limit: Int = 10, page: Int): Result<UserListResponse>
        suspend fun getUserInformation(appid: Int = 1, userId: Long, token: String): Result<UserInformationResponse>
        suspend fun deleteComment(appid: Int = 1, token: String, commentId: Long): Result<BaseResponse>
        suspend fun getUserBilling(appid: Int = 1, token: String, limit: Int, page: Int): Result<BillingResponse>
        suspend fun payForApp(appid: Int = 1, token: String, appsId: Long, appsVersionId: Long, money: Int, type: Int = 0): Result<BaseResponse>
        suspend fun rewardPost(appid: Int = 1, token: String, postId: Long, money: Int, payment: Int = 0): Result<BaseResponse>
        suspend fun postAppComment(appid: Int = 1, token: String, content: String, appsId: Long, appsVersionId: Long, parentId: Long? = null, imageUrl: String? = null): Result<BaseResponse>
        suspend fun deleteAppComment(appid: Int = 1, token: String, commentId: Long): Result<BaseResponse>
        suspend fun userSignIn(appid: Int = 1, token: String): Result<BaseResponse>
        suspend fun register(appid: Int = 1, username: String, password: String, email: String, device: String, captcha: String): Result<BaseResponse>
        suspend fun modifyUserInfo(appid: Int = 1, token: String, nickname: String? = null, qq: String? = null): Result<BaseResponse>
        suspend fun releaseApp(appid: Int = 1, usertoken: String, appname: String, icon: String?, app_size: String, app_introduce: String, app_introduction_image: String?, file: String, app_explain: String?, app_version: String, is_pay: Int, pay_money: String?, category_id: Int, sub_category_id: Int): Result<BaseResponse>
        suspend fun updateApp(appid: Int = 1, usertoken: String, apps_id: Long, appname: String, icon: String?, app_size: String, app_introduce: String, app_introduction_image: String, file: String, app_explain: String?, app_version: String, is_pay: Int, pay_money: String?, category_id: Int, sub_category_id: Int): Result<BaseResponse>
        suspend fun deleteApp(appid: Int = 1, usertoken: String, apps_id: Long, app_version_id: Long): Result<BaseResponse>
        suspend fun getRankingList(appid: Int = 1, sort: String = "money", sortOrder: String = "desc", limit: Int = 15, page: Int): Result<RankingListResponse>
        suspend fun followUser(appid: Int = 1, token: String, followedId: Long): Result<BaseResponse>
        suspend fun uploadAvatar(appid: Int, token: String, file: ByteArray, filename: String): Result<BaseResponse>
        suspend fun getLatestRelease(): Result<UpdateInfo>
    }

    object ApiServiceImpl : ApiService {
        override suspend fun login(appid: Int, username: String, password: String, device: String): Result<LoginResponse> =
            request("api/login", parameters = Parameters.build {
                append("appid", appid.toString()); append("username", username); append("password", password); append("device", device)
            })

        override suspend fun heartbeat(appid: Int, token: String): Result<HeartbeatResponse> =
            request("api/user_heartbeat", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token)
            })

        override suspend fun getPostsList(appid: Int, limit: Int, page: Int, sort: String, sortOrder: String, sectionId: Int?, userId: Long?): Result<PostListResponse> =
            request("api/get_posts_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("limit", limit.toString()); append("page", page.toString())
                append("sort", sort); append("sortOrder", sortOrder)
                sectionId?.let { append("sectionid", it.toString()) }; userId?.let { append("userid", it.toString()) }
            })

        override suspend fun getUserInfo(appid: Int, token: String): Result<UserInfoResponse> =
            request("api/get_user_other_information", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token)
            })

        override suspend fun getPostDetail(appid: Int, token: String, postId: Long): Result<PostDetailResponse> =
            request("api/get_post_information", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("postid", postId.toString())
            })

        override suspend fun getPostComments(appid: Int, postId: Long, limit: Int, page: Int, sort: String, sortOrder: String): Result<CommentListResponse> =
            request("api/get_list_comments", parameters = Parameters.build {
                append("appid", appid.toString()); append("postid", postId.toString()); append("limit", limit.toString()); append("page", page.toString()); append("sort", sort); append("sortOrder", sortOrder)
            })

        override suspend fun createPost(appid: Int, token: String, title: String, content: String, sectionId: Int, imageUrls: String?, paidReading: Int, downloadMethod: Int): Result<BaseResponse> =
            request("api/post", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("title", title); append("content", content); append("subsectionid", sectionId.toString())
                imageUrls?.let { append("network_picture", it) }; append("paid_reading", paidReading.toString()); append("file_download_method", downloadMethod.toString())
            })

        override suspend fun getAppsList(appid: Int, limit: Int, page: Int, sort: String, sortOrder: String, categoryId: Int?, subCategoryId: Int?, appName: String?, userId: Long?): Result<AppListResponse> =
            request("api/get_apps_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("limit", limit.toString()); append("page", page.toString()); append("sort", sort); append("sortOrder", sortOrder)
                categoryId?.let { append("category_id", it.toString()) }; subCategoryId?.let { append("sub_category_id", it.toString()) }; appName?.let { append("appname", it) }; userId?.let { append("userid", it.toString()) }
            })

        override suspend fun getAppsInformation(appid: Int, token: String, appsId: Long, appsVersionId: Long): Result<AppDetailResponse> =
            request("api/get_apps_information", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("apps_id", appsId.toString()); append("apps_version_id", appsVersionId.toString())
            })

        override suspend fun getAppsCommentList(appid: Int, appsId: Long, appsVersionId: Long, limit: Int, page: Int, sortOrder: String): Result<AppCommentListResponse> =
            request("api/get_apps_comment_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("apps_id", appsId.toString()); append("apps_version_id", appsVersionId.toString()); append("limit", limit.toString()); append("page", page.toString()); append("sortOrder", sortOrder)
            })

        override suspend fun postComment(appid: Int, token: String, content: String, postId: Long?, parentId: Long?, imageUrl: String?): Result<BaseResponse> =
            request("api/post_comment", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("content", content)
                postId?.let { append("postid", it.toString()) }; parentId?.let { append("parentid", it.toString()) }; imageUrl?.let { append("img", it) }
            })

        override suspend fun getMessageNotifications(appid: Int, token: String, limit: Int, page: Int): Result<MessageNotificationResponse> =
            request("api/get_message_notifications", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun getBrowseHistory(appid: Int, token: String, limit: Int, page: Int): Result<BrowseHistoryResponse> =
            request("api/browse_history", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun getLikesRecords(appid: Int, token: String, limit: Int, page: Int): Result<PostListResponse> =
            request("api/get_likes_records", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun searchPosts(appid: Int, query: String, limit: Int, page: Int, userId: Long?): Result<PostListResponse> =
            request("api/get_posts_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("keyword", query); append("limit", limit.toString()); append("page", page.toString())
                userId?.let { append("userid", it.toString()) }
            })

        override suspend fun getHotPostsList(appid: Int, limit: Int, page: Int, sortOrder: String, sort: String): Result<PostListResponse> =
            request("api/get_posts_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("limit", limit.toString()); append("page", page.toString()); append("sortOrder", sortOrder); append("sort", sort)
            })

        override suspend fun getMyFollowingPosts(appid: Int, token: String, limit: Int, page: Int): Result<PostListResponse> =
            request("api/get_my_following_posts", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun likePost(appid: Int, token: String, postId: Long): Result<BaseResponse> =
            request("api/like_posts", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("postid", postId.toString())
            })

        override suspend fun deletePost(appid: Int, token: String, postId: Long): Result<BaseResponse> =
            request("api/delete_post", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("postid", postId.toString())
            })

        override suspend fun getFanList(appid: Int, token: String, limit: Int, page: Int): Result<UserListResponse> =
            request("api/get_fan_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun getFollowList(appid: Int, token: String, limit: Int, page: Int): Result<UserListResponse> =
            request("api/get_follow_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun getUserInformation(appid: Int, userId: Long, token: String): Result<UserInformationResponse> =
            request("api/get_user_information", parameters = Parameters.build {
                append("appid", appid.toString()); append("userid", userId.toString()); append("usertoken", token)
            })

        override suspend fun deleteComment(appid: Int, token: String, commentId: Long): Result<BaseResponse> =
            request("api/delete_comment", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("commentid", commentId.toString())
            })

        override suspend fun getUserBilling(appid: Int, token: String, limit: Int, page: Int): Result<BillingResponse> =
            request("api/get_user_billing", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun payForApp(appid: Int, token: String, appsId: Long, appsVersionId: Long, money: Int, type: Int): Result<BaseResponse> =
            request("api/pay_for_apps", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("apps_id", appsId.toString()); append("apps_version_id", appsVersionId.toString()); append("money", money.toString()); append("type", type.toString())
            })

        override suspend fun rewardPost(appid: Int, token: String, postId: Long, money: Int, payment: Int): Result<BaseResponse> =
            request("api/reward_posts", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("postid", postId.toString()); append("money", money.toString()); append("payment", payment.toString())
            })

        override suspend fun postAppComment(appid: Int, token: String, content: String, appsId: Long, appsVersionId: Long, parentId: Long?, imageUrl: String?): Result<BaseResponse> =
            request("api/apps_add_comment", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("content", content); append("apps_id", appsId.toString()); append("apps_version_id", appsVersionId.toString())
                parentId?.let { append("parentid", it.toString()) }; imageUrl?.let { append("img", it) }
            })

        override suspend fun deleteAppComment(appid: Int, token: String, commentId: Long): Result<BaseResponse> =
            request("api/delete_apps_comment", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("comment_id", commentId.toString())
            })

        override suspend fun userSignIn(appid: Int, token: String): Result<BaseResponse> =
            request("api/user_sign_in", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token)
            })

        override suspend fun register(appid: Int, username: String, password: String, email: String, device: String, captcha: String): Result<BaseResponse> =
            request("api/register", parameters = Parameters.build {
                append("appid", appid.toString()); append("username", username); append("password", password); append("email", email); append("device", device); append("captcha", captcha)
            })

        override suspend fun modifyUserInfo(appid: Int, token: String, nickname: String?, qq: String?): Result<BaseResponse> =
            request("api/modify_user_information", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token)
                nickname?.let { append("nickname", it) }; qq?.let { append("qq", it) }
            })

        override suspend fun releaseApp(appid: Int, usertoken: String, appname: String, icon: String?, app_size: String, app_introduce: String, app_introduction_image: String?, file: String, app_explain: String?, app_version: String, is_pay: Int, pay_money: String?, category_id: Int, sub_category_id: Int): Result<BaseResponse> =
            request("api/release_apps", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", usertoken); append("appname", appname); icon?.let { append("icon", it) }; append("app_size", app_size); append("app_introduce", app_introduce); app_introduction_image?.let { append("app_introduction_image", it) }; append("file", file); app_explain?.let { append("app_explain", it) }; append("app_version", app_version); append("is_pay", is_pay.toString()); pay_money?.let { append("pay_money", it) }; append("category_id", category_id.toString()); append("sub_category_id", sub_category_id.toString())
            })

        override suspend fun updateApp(appid: Int, usertoken: String, apps_id: Long, appname: String, icon: String?, app_size: String, app_introduce: String, app_introduction_image: String, file: String, app_explain: String?, app_version: String, is_pay: Int, pay_money: String?, category_id: Int, sub_category_id: Int): Result<BaseResponse> =
            request("api/release_new_version", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", usertoken); append("apps_id", apps_id.toString()); append("appname", appname); icon?.let { append("icon", it) }; append("app_size", app_size); append("app_introduce", app_introduce); append("app_introduction_image", app_introduction_image); append("file", file); app_explain?.let { append("app_explain", it) }; append("app_version", app_version); append("is_pay", is_pay.toString()); pay_money?.let { append("pay_money", it) }; append("category_id", category_id.toString()); append("sub_category_id", sub_category_id.toString())
            })

        override suspend fun deleteApp(appid: Int, usertoken: String, apps_id: Long, app_version_id: Long): Result<BaseResponse> =
            request("api/delete_apps", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", usertoken); append("apps_id", apps_id.toString()); append("app_version_id", app_version_id.toString())
            })

        override suspend fun getRankingList(appid: Int, sort: String, sortOrder: String, limit: Int, page: Int): Result<RankingListResponse> =
            request("api/ranking_list", parameters = Parameters.build {
                append("appid", appid.toString()); append("sort", sort); append("sortOrder", sortOrder); append("limit", limit.toString()); append("page", page.toString())
            })

        override suspend fun getLatestRelease(): Result<UpdateInfo> =
            safeApiCall { httpClient.get("https://gitee.com/api/v5/repos/Voltula/bbq/releases/latest") }

        override suspend fun followUser(appid: Int, token: String, followedId: Long): Result<BaseResponse> =
            request("api/follow_users", parameters = Parameters.build {
                append("appid", appid.toString()); append("usertoken", token); append("followedid", followedId.toString())
            })

        override suspend fun uploadAvatar(appid: Int, token: String, file: ByteArray, filename: String): Result<BaseResponse> =
            safeApiCall {
                httpClient.post("api/upload_avatar") {
                    setBody(MultiPartFormDataContent(formData {
                        append("appid", appid.toString()); append("usertoken", token)
                        append("file", file, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                        })
                    }))
                }
            }
    }

    fun close() {
        httpClient.close()
        uploadHttpClient.close()
        wanyueyunUploadHttpClient.close()
    }
}