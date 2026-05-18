//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:(kotlin.time.ExperimentalTime::class)
package me.voltual.pyrolysis.ui.community

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 浏览历史实体类
 * 已移除 java.util.Date 和 SimpleDateFormat，全面适配 kotlinx-datetime
 */
@Entity(tableName = "browse_history")
data class BrowseHistory(
    @PrimaryKey val postId: Long,
    val title: String,
    val previewContent: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    /**
     * 格式化时间字符串 (yyyy-MM-dd HH:mm)
     * 使用 Kotlin 原生方式处理日期，避免 Java 平台依赖
     */
    fun formattedTime(): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        
        return with(localDateTime) {
            val yearStr = year.toString()
            val monthStr = monthNumber.toString().padStart(2, '0')
            val dayStr = dayOfMonth.toString().padStart(2, '0')
            val hourStr = hour.toString().padStart(2, '0')
            val minuteStr = minute.toString().padStart(2, '0')
            
            "$yearStr-$monthStr-$dayStr $hourStr:$minuteStr"
        }
    }
}