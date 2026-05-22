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
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.KtorClient.AppItem
import me.voltual.pyrolysis.KtorClient.Comment
import me.voltual.pyrolysis.KtorClient.AppComment
import me.voltual.pyrolysis.KtorClient.AppDetail
import me.voltual.pyrolysis.KtorClient.UserInformationData

// --- KtorClient (小趣空间) Mappers ---

fun KtorClient.AppItem.toUnifiedAppItem(): UnifiedAppItem {
    return UnifiedAppItem(
        uniqueId = "${AppStore.XIAOQU_SPACE}-${this.id}-${this.apps_version_id}",
        navigationId = this.id.toString(),
        navigationVersionId = this.apps_version_id,
        store = AppStore.XIAOQU_SPACE,
        name = this.appname,
        iconUrl = this.app_icon,
        versionName = ""
    )
}

fun createUnifiedUserFromKtor(id: Long, name: String, avatar: String): UnifiedUser {
    return UnifiedUser(
        id = id.toString(),
        displayName = name,
        avatarUrl = avatar
    )
}

fun KtorClient.Comment.toUnifiedComment(): UnifiedComment {
    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.time.toLongOrNull() ?: 0L,
        sender = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        childCount = this.sub_comments_count,
        // KtorClient.Comment 没有直接包含子评论列表，设为空
        childComments = emptyList(), 
        fatherReply = null,
        raw = this
    )
}

fun KtorClient.AppComment.toUnifiedComment(): UnifiedComment {
    val father = if (this.parentid != null && this.parentid != 0L) {
        UnifiedComment(
            id = this.parentid.toString(),
            content = this.parentcontent ?: "",
            sendTime = 0L,
            sender = createUnifiedUserFromKtor(0L, this.parentnickname ?: "未知用户", ""),
            childCount = 0,
            fatherReply = null,
            raw = this
        )
    } else null

    return UnifiedComment(
        id = this.id.toString(),
        content = this.content,
        sendTime = this.time.toLongOrNull() ?: 0L,
        sender = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        childCount = 0,
        fatherReply = father,
        raw = this
    )
}

fun KtorClient.AppDetail.toUnifiedAppDetail(): UnifiedAppDetail {
    return UnifiedAppDetail(
        id = this.id.toString(),
        store = AppStore.XIAOQU_SPACE,
        packageName = "", 
        name = this.appname,
        versionCode = 0L,
        versionName = this.version,
        iconUrl = this.app_icon,
        type = this.category_name,
        previews = this.app_introduction_image_array,
        description = this.app_introduce,
        updateLog = this.app_explain,
        developer = null,
        size = this.app_size,
        uploadTime = this.create_time.toLongOrNull() ?: 0L,
        user = createUnifiedUserFromKtor(this.userid, this.nickname, this.usertx),
        tags = listOf(this.category_name, this.sub_category_name),
        downloadCount = this.download_count,
        isFavorite = false,
        favoriteCount = 0,
        reviewCount = this.comment_count,
        downloadUrl = if (this.is_pay == 0 || this.is_user_pay) this.download else null,
        raw = this
    )
}

/**
 * 将小趣空间用户数据转换为统一用户详情
 * 
 * 注意：数据字段映射已修正（2026年1月9日18点）
 * - followerscount → followersCount: 用户关注的人数（该用户关注了多少人）
 * - fanscount → fansCount: 用户的粉丝数（有多少人关注了该用户）
 * 
 * 此前版本因映射错误导致UI显示相反，现已修复
 */

fun KtorClient.UserInformationData.toUnifiedUserDetail(): UnifiedUserDetail {
// 解析关注状态（注意：响应中的 follow_status 是 String 类型）
    val followStatusValue = this.follow_status.toIntOrNull() ?: 3
    val followStatus = FollowStatus.fromValue(followStatusValue)
    return UnifiedUserDetail(
        id = this.id,
        username = this.username,
        displayName = this.nickname,
        avatarUrl = this.usertx,
        description = null, // 小趣空间无此字段
        hierarchy = this.hierarchy,
        followersCount = this.followerscount,
        fansCount = this.fanscount,
        followStatus = followStatus,  
        postCount = this.postcount,
        likeCount = this.likecount,
        money = this.money,
        commentCount = this.commentcount?.toIntOrNull(), // 从 String? 转换为 Int?
        seriesDays = this.series_days,
        lastActivityTime = this.last_activity_time,
        store = AppStore.XIAOQU_SPACE,
        raw = this
    )
}