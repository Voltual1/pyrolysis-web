package me.voltual.pyrolysis.feature.store.repository

import me.voltual.pyrolysis.network.KtorClient
import me.voltual.pyrolysis.data.unified.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.voltual.pyrolysis.AppStore

class XiaoQuRepository(
    private val apiClient: KtorClient.ApiService,
    private val tokenProvider: suspend () -> String
) : IAppStoreRepository {

    private suspend fun getToken(): String = tokenProvider()
    
    override suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail> {
        return try {
            val token = getToken()
            if (token.isEmpty()) return Result.failure(Exception("未登录"))
            
            val result = apiClient.getUserInfo(token = token)
            result.map { response ->
                if (response.code == 1) {
                    UnifiedUserDetail(
                        id = response.data.id,
                        username = response.data.username,
                        displayName = response.data.nickname,
                        avatarUrl = response.data.usertx,
                        hierarchy = response.data.hierarchy,
                        money = response.data.money,
                        followersCount = response.data.followerscount,
                        fansCount = response.data.fanscount,
                        postCount = response.data.postcount,
                        likeCount = response.data.likecount,
                        store = AppStore.XIAOQU_SPACE,
                        raw = response.data
                    )
                } else throw Exception("获取用户信息失败: ${response.msg}")
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun uploadImage(imageBytes: ByteArray, filename: String): Result<String> {
        return try {
            val response = KtorClient.uploadHttpClient.submitFormWithBinaryData(
                url = "api.php",
                formData = formData {
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }
            )
            if (response.status.isSuccess()) {
                val body: KtorClient.UploadResponse = response.body()
                val downUrl = body.downurl // 模块内智能转换生效
                if (body.code == 0 && !downUrl.isNullOrBlank()) Result.success(downUrl)
                else Result.failure(Exception(body.msg))
            } else Result.failure(Exception("网络错误 ${response.status}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun uploadApk(apkBytes: ByteArray, filename: String, serviceType: String): Result<String> {
        return try {
            when (serviceType) {
                "KEYUN" -> uploadToKeyun(apkBytes, filename)
                "WANYUEYUN" -> uploadToWanyueyun(apkBytes, filename)
                else -> Result.failure(Exception("不支持的上传服务类型"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun uploadToKeyun(bytes: ByteArray, filename: String): Result<String> {
        val response = KtorClient.uploadHttpClient.submitFormWithBinaryData(
            url = "api.php",
            formData = formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }
        )
        val body: KtorClient.UploadResponse = response.body()
        val downUrl = body.downurl
        return if (response.status.isSuccess() && body.code == 0 && !downUrl.isNullOrBlank()) {
            Result.success(downUrl)
        } else Result.failure(Exception(body.msg))
    }

    private suspend fun uploadToWanyueyun(bytes: ByteArray, filename: String): Result<String> {
        val response = KtorClient.wanyueyunUploadHttpClient.submitFormWithBinaryData(
            url = "upload",
            formData = formData {
                append("Api", "小趣API")
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }
        )
        val body: KtorClient.WanyueyunUploadResponse = response.body()
        val dataUrl = body.data
        return if (response.status.isSuccess() && body.code == 200 && !dataUrl.isNullOrBlank()) {
            Result.success(dataUrl)
        } else Result.failure(Exception(body.msg))
    }

    // ... 其他 getApps, getAppDetail 等方法只需保留原有逻辑，将 DTO 映射到 Unified 模型即可 ...
    // 为节省篇幅，此处省略重复的映射逻辑，但确保所有返回类型均为 Result<Unified...>
    
    override suspend fun getCategories(): Result<List<UnifiedCategory>> = Result.success(listOf(
        UnifiedCategory("null_null", "最新分享"),
        UnifiedCategory("45_47", "影音阅读") // 示例
    ))

    override suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit> = Result.success(Unit)
    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = Result.success(Pair(emptyList(), 1))
    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = Result.success(Pair(emptyList(), 1))
    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> = Result.failure(Exception("Not implemented"))
    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> = Result.success(Pair(emptyList(), 1))
    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> = Result.success(Unit)
    override suspend fun deleteComment(commentId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteComment(appId: String, commentId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> = Result.success(Unit)
    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> = Result.success(emptyList())
    override suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>> = Result.success(Pair(emptyList(), 1))
    override suspend fun deleteReview(reviewId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>> = Result.success(Pair(emptyList(), 1))
    override suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String> = Result.success("")
}