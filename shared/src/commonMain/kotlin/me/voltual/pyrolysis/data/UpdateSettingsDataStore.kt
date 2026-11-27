//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

enum class UpdateNotificationMode(val displayName: String) {
    CURRENT_CHANNEL_ONLY("仅显示包含当前渠道更新的通知"),
    ALWAYS("显示更新通知"),
    NEVER("不显示更新通知");

    companion object {
        fun fromName(name: String?): UpdateNotificationMode? {
            return entries.find { it.name == name }
        }
    }
}

@Single
class UpdateSettingsDataStore(private val dataStore: DataStore<Preferences>) {
    
    companion object {
        private val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        private val UPDATE_NOTIFICATION_MODE = stringPreferencesKey("update_notification_mode")
    }

    val notificationMode: Flow<UpdateNotificationMode> = dataStore.data.map { preferences ->
        val savedMode = UpdateNotificationMode.fromName(preferences[UPDATE_NOTIFICATION_MODE])
        if (savedMode != null) {
            savedMode
        } else {
            // 向后兼容旧版布尔开关：若曾手动关闭则映射为 NEVER，否则默认 CURRENT_CHANNEL_ONLY
            val legacyAutoCheck = preferences[AUTO_CHECK_UPDATES]
            if (legacyAutoCheck == false) {
                UpdateNotificationMode.NEVER
            } else {
                UpdateNotificationMode.CURRENT_CHANNEL_ONLY
            }
        }
    }

    suspend fun setNotificationMode(mode: UpdateNotificationMode) {
        dataStore.edit {
            it[UPDATE_NOTIFICATION_MODE] = mode.name
            it[AUTO_CHECK_UPDATES] = mode != UpdateNotificationMode.NEVER
        }
    }
}