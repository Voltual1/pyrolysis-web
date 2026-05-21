//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class UserFilterDataStore(private val dataStore: DataStore<Preferences>) {
    
    companion object {
        private val FILTER_USER_IDS_SET = stringSetPreferencesKey("filter_user_ids_set")
        private const val NICKNAME_KEY_PREFIX = "filter_nickname_"
        private val ACTIVE_FILTER_USER_ID = longPreferencesKey("active_filter_user_id")
        private val IS_FILTER_ACTIVE = booleanPreferencesKey("is_filter_active")
        
        private fun getNicknameKey(userId: Long): Preferences.Key<String> {
            return stringPreferencesKey("$NICKNAME_KEY_PREFIX$userId")
        }
    }
    
    suspend fun addOrUpdateUserFilter(userId: Long, nickname: String) {
        dataStore.edit { preferences ->
            val existingUserIds = preferences[FILTER_USER_IDS_SET]?.toMutableSet() ?: mutableSetOf()
            existingUserIds.add(userId.toString())
            preferences[FILTER_USER_IDS_SET] = existingUserIds
            preferences[getNicknameKey(userId)] = nickname
            
            val currentActiveUserId = preferences[ACTIVE_FILTER_USER_ID]
            if (currentActiveUserId == null) {
                preferences[ACTIVE_FILTER_USER_ID] = userId
                preferences[IS_FILTER_ACTIVE] = true
            }
        }
    }
    
    suspend fun removeUserFilter(userId: Long) {
        dataStore.edit { preferences ->
            val existingUserIds = preferences[FILTER_USER_IDS_SET]?.toMutableSet() ?: mutableSetOf()
            existingUserIds.remove(userId.toString())
            preferences[FILTER_USER_IDS_SET] = existingUserIds
            preferences.remove(getNicknameKey(userId))
            
            val currentActiveUserId = preferences[ACTIVE_FILTER_USER_ID]
            if (currentActiveUserId == userId) {
                val nextUserId = existingUserIds.firstOrNull()?.toLongOrNull()
                preferences[ACTIVE_FILTER_USER_ID] = nextUserId ?: 0L
                preferences[IS_FILTER_ACTIVE] = nextUserId != null
            }
        }
    }
    
    suspend fun setActiveUserFilter(userId: Long?) {
        dataStore.edit { preferences ->
            if (userId != null) {
                val existingUserIds = preferences[FILTER_USER_IDS_SET] ?: setOf()
                if (existingUserIds.contains(userId.toString())) {
                    preferences[ACTIVE_FILTER_USER_ID] = userId
                    preferences[IS_FILTER_ACTIVE] = true
                }
            } else {
                preferences[ACTIVE_FILTER_USER_ID] = 0L
                preferences[IS_FILTER_ACTIVE] = false
            }
        }
    }
    
    suspend fun clearAllUserFilters() {
        dataStore.edit { preferences ->
            val existingUserIds = preferences[FILTER_USER_IDS_SET] ?: setOf()
            existingUserIds.forEach { userIdStr ->
                userIdStr.toLongOrNull()?.let { userId ->
                    preferences.remove(getNicknameKey(userId))
                }
            }
            preferences.remove(FILTER_USER_IDS_SET)
            preferences.remove(ACTIVE_FILTER_USER_ID)
            preferences[IS_FILTER_ACTIVE] = false
        }
    }
    
    val userIdsFlow: Flow<Set<Long>> = dataStore.data
        .map { preferences ->
            preferences[FILTER_USER_IDS_SET]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        }
    
    val allUserFiltersFlow: Flow<Map<Long, String>> = dataStore.data
        .map { preferences ->
            val userIds = preferences[FILTER_USER_IDS_SET] ?: emptySet()
            val result = mutableMapOf<Long, String>()
            userIds.forEach { userIdStr ->
                userIdStr.toLongOrNull()?.let { userId ->
                    val nickname = preferences[getNicknameKey(userId)]
                    if (nickname != null) result[userId] = nickname
                }
            }
            result
        }
    
    val activeUserIdFlow: Flow<Long?> = dataStore.data
        .map { preferences ->
            val activeUserId = preferences[ACTIVE_FILTER_USER_ID]
            if (activeUserId != null && activeUserId != 0L) activeUserId else null
        }
    
    val activeNicknameFlow: Flow<String?> = dataStore.data
        .map { preferences ->
            val activeUserId = preferences[ACTIVE_FILTER_USER_ID]
            if (activeUserId != null && activeUserId != 0L) {
                preferences[getNicknameKey(activeUserId)]
            } else null
        }
    
    val activeUserFilterFlow: Flow<Pair<Long?, String?>> = dataStore.data
        .map { preferences ->
            val activeUserId = preferences[ACTIVE_FILTER_USER_ID]
            if (activeUserId != null && activeUserId != 0L) {
                val nickname = preferences[getNicknameKey(activeUserId)]
                Pair(activeUserId, nickname)
            } else Pair(null, null)
        }
    
    val isFilterActiveFlow: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_FILTER_ACTIVE] ?: false }
    
    suspend fun hasUserFilter(userId: Long): Boolean {
        val preferences = dataStore.data.first()
        return preferences[FILTER_USER_IDS_SET]?.contains(userId.toString()) ?: false
    }
    
    suspend fun getNickname(userId: Long): String? {
        val preferences = dataStore.data.first()
        return preferences[getNicknameKey(userId)]
    }
}