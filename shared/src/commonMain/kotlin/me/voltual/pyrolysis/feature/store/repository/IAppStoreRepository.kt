package me.voltual.pyrolysis.feature.store.repository

import me.voltual.pyrolysis.data.unified.*

interface IAppStoreRepository {
    suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail>
    suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit>
    suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String>
    suspend fun getCategories(): Result<List<UnifiedCategory>>
    suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>>
    suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail>
    suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>>
    suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit>
    suspend fun deleteComment(commentId: String): Result<Unit>
    suspend fun deleteComment(appId: String, commentId: String): Result<Unit>
    suspend fun deleteApp(appId: String, versionId: Long): Result<Unit>
    suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit>
    suspend fun uploadImage(imageBytes: ByteArray, filename: String): Result<String>
    suspend fun uploadApk(apkBytes: ByteArray, filename: String, serviceType: String): Result<String>
    suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>>
    suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>>
    suspend fun deleteReview(reviewId: String): Result<Unit>
    suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>>
}