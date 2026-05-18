package me.voltual.pyrolysis.data.unified

import kotlinx.serialization.Serializable
import me.voltual.pyrolysis.AppStore

/**
 * 统一的用户信息模型
 */
@Serializable
data class UnifiedUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

/**
 * 统一的下载源模型
 */
@Serializable
data class UnifiedDownloadSource(
    val name: String,
    val url: String,
    val isOfficial: Boolean
)

/**
 * 统一的评论模型
 */
@Serializable
data class UnifiedComment(
    val id: String,
    val content: String,
    val sendTime: Long,
    val sender: UnifiedUser,
    val childCount: Int,
    val childComments: List<UnifiedComment> = emptyOfList(),
    val fatherReply: UnifiedComment? = null,
    val raw: String, // 在 KMP 中建议存为 String 或 JsonElement
    val appId: String? = null,
    val versionId: Long? = null,
    val rating: Int? = null,
    val isCountedInRating: Boolean? = null 
)

private fun <T> emptyOfList(): List<T> = emptyList()

/**
 * 统一的更新用户资料参数
 */
data class UpdateUserProfileParams(
    val nickname: String? = null,
    val qqNumber: String? = null,
    val displayName: String? = null,
    val description: String? = null,
    val deviceName: String? = null
)

/**
 * 统一的应用详情模型
 */
@Serializable
data class UnifiedAppDetail(
    val id: String,
    val store: AppStore,
    val packageName: String,
    val name: String,
    val versionCode: Long,
    val versionName: String,
    val iconUrl: String,
    val type: String,
    val previews: List<String>?,
    val description: String?,
    val updateLog: String?,
    val developer: String?,
    val size: String?,
    val uploadTime: Long,
    val user: UnifiedUser,
    val tags: List<String>?,
    val downloadCount: Int,
    val isFavorite: Boolean,
    val favoriteCount: Int,
    val reviewCount: Int,
    val downloadUrl: String?,
    val raw: String,
    // 微思应用商店专用字段
    val minsdkDisplay: String? = null,
    val targetsdkDisplay: String? = null,
    val cpuArchDisplay: String? = null,
    val osCompatibilityDisplay: String? = null,
    val displayCompatibilityDisplay: String? = null,
    val watchCount: Int? = null,
    val upnote: String? = null,
    val versionTypeDisplay: String? = null
)

/**
 * 统一的应用列表项模型
 */
@Serializable
data class UnifiedAppItem(
    val uniqueId: String,
    val navigationId: String,
    val navigationVersionId: Long,
    val store: AppStore,
    val name: String,
    val iconUrl: String,
    val versionName: String,
    val info: String? = null
)

/**
 * 统一的分类模型
 */
@Serializable
data class UnifiedCategory(
    val id: String,
    val name: String,
    val icon: String? = null
)

/**
 * 统一的用户详情模型
 */
@Serializable
data class UnifiedUserDetail(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val description: String? = null,
    val hierarchy: String? = null,
    val followersCount: String? = null,
    val fansCount: String? = null,
    val postCount: String? = null,
    val likeCount: String? = null,
    val followStatus: Int? = null, // 简化为 Int，对应 FollowStatus.value
    val money: Int? = null,
    val commentCount: Int? = null,
    val seriesDays: Int? = null,
    val lastActivityTime: String? = null,
    val userOfficial: String? = null,
    val userBadge: String? = null,
    val userStatus: Int? = null,
    val userStatusReason: String? = null,
    val banTime: Long? = null,
    val joinTime: Long? = null,
    val userPermission: Int? = null,
    val bindQq: Long? = null,
    val bindEmail: String? = null,
    val bindBilibili: Int? = null,
    val verifyEmail: Int? = null,
    val lastLoginDevice: String? = null,
    val lastOnlineTime: Long? = null,
    val uploadCount: Int? = null,
    val replyCount: Int? = null,
    val store: AppStore,
    val raw: String? = null
)

/**
 * 统一的发布应用参数 (KMP 适配版)
 */
data class UnifiedAppReleaseParams(
    val store: AppStore,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeInMb: Double,
    // 将 File 替换为 ByteArray 以便跨平台，但保留字段名
    val iconFile: ByteArray? = null, 
    val iconUrl: String? = null,
    val apkFile: ByteArray? = null,   
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
    val screenshots: List<ByteArray>? = null 
)

// 小趣空间关注状态密封类
sealed class FollowStatus(val value: Int) {
    data object MutualFollow : FollowStatus(0)      // 已互关
    data object FollowedYou : FollowStatus(1)       // 关注了你
    data object YouFollowed : FollowStatus(2)       // 已关注
    data object NotFollowed : FollowStatus(3)       // 未关注
    
    companion object {
        fun fromValue(value: Int): FollowStatus {
            return when (value) {
                0 -> MutualFollow
                1 -> FollowedYou
                2 -> YouFollowed
                3 -> NotFollowed
                else -> NotFollowed
            }
        }
    }
}