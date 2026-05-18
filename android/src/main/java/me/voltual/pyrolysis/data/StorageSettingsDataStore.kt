//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * 定义 Kotlin 原生存储异常，隔离底层 Java IO 细节
 */
class PyrolysisStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

// 扩展属性定义 DataStore
private val Context.storageSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "storage_settings")

@Single
class StorageSettingsDataStore(context: Context) {

    private val dataStore: DataStore<Preferences> = context.storageSettingsDataStore

    private companion object {
        val IS_SUPER_CACHE_ENABLED = booleanPreferencesKey("is_super_cache_enabled")
    }

    /**
     * 超级缓存是否启用的流
     * 使用 Kotlin 风格的处理逻辑，捕获异常并转换为默认值或自定义异常
     */
    val isSuperCacheEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { throwable ->
            // 这里的逻辑处理底层文件读取异常
            // 在 Kotlin 中，我们通过类名字符串或特定的异常检查来规避显式的 java.io 导入
            if (throwable::class.simpleName?.contains("IOException") == true) {
                emit(emptyPreferences())
            } else {
                throw PyrolysisStorageException("无法读取存储配置", throwable)
            }
        }
        .map { preferences ->
            preferences[IS_SUPER_CACHE_ENABLED] ?: false
        }

    /**
     * 更新超级缓存状态
     * 使用 runCatching 确保操作的安全性，并包装为 Kotlin 友好的结果
     */
    suspend fun updateSuperCacheEnabled(isEnabled: Boolean) {
        runCatching {
            dataStore.edit { preferences ->
                preferences[IS_SUPER_CACHE_ENABLED] = isEnabled
            }
        }.onFailure { throwable ->
            throw PyrolysisStorageException("更新存储配置失败", throwable)
        }
    }
}