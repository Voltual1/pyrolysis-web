//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.voltual.pyrolysis.core.proto.UserCredentials

/**
 * AuthRepository 是 AuthManager 的依赖注入版本。
 * 它与 AuthManager 共享底层的 "user_credentials_v2.pb" 文件。
 */
class AuthRepository(
    private val credentialsStore: DataStore<UserCredentials>
) {

    // --- 1. 保存逻辑 ---

    suspend fun saveCredentials(
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        credentialsStore.updateData { current ->
            current.toBuilder()
                .setUsername(username)
                .setPassword(password)
                .setToken(token)
                .setUserId(userId)
                .setDeviceId(current.deviceId.ifEmpty { generateDeviceId() })
                .build()
        }
    }

    // --- 2. 读取逻辑 ---

    val credentials: Flow<UserCredentials> = credentialsStore.data
    
    val userId: Flow<Long> = credentials.map { it.userId }

    val deviceId: Flow<String> = credentials.map { it.deviceId.ifEmpty { generateDeviceId() } }

    // --- 3. 清理逻辑 ---

    suspend fun clearCredentials() {
        credentialsStore.updateData { UserCredentials.getDefaultInstance() }
    }

    private fun generateDeviceId(): String = (1..15).map { (0..9).random() }.joinToString("")
}