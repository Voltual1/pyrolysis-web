//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.data.unified

import me.voltual.pyrolysis.AppStore
import kotlinx.io.files.Path

/**
 * 统一的用户信息模型
 */
data class UnifiedUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?
)

/**
 * 统一的评论模型
 */
data class UnifiedComment(
    val id: String,
    val content: String,
    val sendTime: Long,
    val sender: UnifiedUser,
    val childCount: Int,
    val childComments: List<UnifiedComment> = emptyList(),
    val fatherReply: UnifiedComment? = null,
    val raw: Any,
    val appId: String? = null,
    val versionId: Long? = null,
    val rating: Int? = null,
    val isCountedInRating: Boolean? = null
)

/**
 * 统一的更新用户资料参数
 */
data class UpdateUserProfileParams(
    val nickname: String? = null,
    val qqNumber: String? = null,
    val description: String? = null,
    val deviceName: String? = null
)

/**
 * 统一的应用详情模型
 */
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
    val raw: Any
)

/**
 * 统一的收藏/点赞状态数据类
 */
data class UnifiedFavoriteState(
    val isFavorite: Boolean,
    val favoriteCount: Int = -1
)

/**
 * 统一的应用列表项模型
 */
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
 * 统一的应用分类模型
 */
data class UnifiedCategory(
    val id: String,
    val name: String,
    val icon: String? = null
)

/**
 * 统一的下载源模型
 */
data class UnifiedDownloadSource(
    val name: String,
    val url: String,
    val isOfficial: Boolean
)

/**
 * 统一的用户详情模型
 */
data class UnifiedUserDetail(
    val id: Long,
    val username: String,
    val displayName: String,
    val bindQq: Long? = null,
    val avatarUrl: String?,
    val description: String? = null,
    val hierarchy: String? = null,
    val followersCount: String? = null,
    val fansCount: String? = null,
    val postCount: String? = null,
    val likeCount: String? = null,
    val followStatus: FollowStatus? = null,
    val money: Int? = null,
    val commentCount: Int? = null,
    val seriesDays: Int? = null,
    val lastActivityTime: String? = null,
    val store: AppStore,
    val raw: Any? = null
)

/**
 * 统一的发布应用参数
 */
data class UnifiedAppReleaseParams(
    val store: AppStore,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeInMb: Double,
    val iconPath: Path?,
    val iconUrl: String?,
    val apkPath: Path?,
    val apkUrl: String?,
    val introduce: String? = null,
    val explain: String? = null,
    val introImages: List<String>? = null,
    val categoryId: Int? = null,
    val subCategoryId: Int? = null,
    val isPay: Int? = null,
    val payMoney: String? = null,
    val isUpdate: Boolean = false,
    val appId: Long? = null,
    val appVersionId: Long? = null
)

/**
 * 小趣空间关注状态
 */
sealed class FollowStatus(val value: Int) {
    data object MutualFollow : FollowStatus(0)
    data object FollowedYou : FollowStatus(1)
    data object YouFollowed : FollowStatus(2)
    data object NotFollowed : FollowStatus(3)

    companion object {
        fun fromValue(value: Int): FollowStatus = when (value) {
            0 -> MutualFollow
            1 -> FollowedYou
            2 -> YouFollowed
            else -> NotFollowed
        }
    }
}
