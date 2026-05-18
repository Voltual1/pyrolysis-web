package me.voltual.pyrolysis.data.unified

import kotlinx.serialization.Serializable
import me.voltual.pyrolysis.AppStore

@Serializable
data class UnifiedUserDetail(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val hierarchy: String,
    val money: Int,
    val followersCount: String,
    val fansCount: String,
    val postCount: String,
    val likeCount: String,
    val store: AppStore,
    val raw: Any? = null
)

@Serializable
data class UnifiedCategory(
    val id: String,
    val name: String
)

@Serializable
data class UnifiedAppItem(
    val id: String,
    val name: String,
    val iconUrl: String,
    val size: String,
    val downloadCount: Int,
    val versionId: Long,
    val authorName: String,
    val releaseTime: String
)

@Serializable
data class UnifiedAppDetail(
    val id: String,
    val name: String,
    val iconUrl: String,
    val size: String,
    val description: String,
    val updateLog: String,
    val screenshots: List<String>,
    val downloadUrl: String?,
    val authorName: String,
    val authorAvatar: String,
    val categoryName: String,
    val versionName: String,
    val versionId: Long,
    val releaseTime: String,
    val commentCount: Int,
    val isPay: Boolean,
    val payMoney: Int,
    val isUserPaid: Boolean
)

@Serializable
data class UnifiedComment(
    val id: String,
    val content: String,
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String,
    val time: String,
    val parentAuthorName: String? = null,
    val parentContent: String? = null,
    val imageUrl: String? = null
)

data class UnifiedAppReleaseParams(
    val store: AppStore,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeInMb: Double,
    val iconBytes: ByteArray? = null, // 替换 File
    val iconUrl: String? = null,
    val apkBytes: ByteArray? = null, // 替换 File
    val apkUrl: String? = null,
    val introduce: String? = null,
    val explain: String? = null,
    val introImages: List<String>? = null,
    val categoryId: Int? = null,
    val subCategoryId: Int? = null,
    val isPay: Int? = null,
    val payMoney: String? = null,
    val isUpdate: Boolean = false,
    val appId: Long? = null,
    val appVersionId: Long? = null,
    // 弦开放平台相关
    val appTypeId: Int? = null,
    val appVersionTypeId: Int? = null,
    val appTags: String? = null,
    val sdkMin: Int? = null,
    val sdkTarget: Int? = null,
    val developer: String? = null,
    val source: String? = null,
    val describe: String? = null,
    val updateLog: String? = null,
    val uploadMessage: String? = null,
    val keyword: String? = null,
    val isWearOs: Int? = null,
    val abi: Int? = null,
    val screenshotBytes: List<ByteArray> = emptyList() // 替换 File
)

data class UpdateUserProfileParams(
    val nickname: String? = null,
    val qqNumber: String? = null
)

@Serializable
data class UnifiedDownloadSource(
    val name: String,
    val url: String,
    val isOfficial: Boolean
)