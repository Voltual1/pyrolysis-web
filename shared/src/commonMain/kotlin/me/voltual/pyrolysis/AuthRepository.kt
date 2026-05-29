//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.voltual.pyrolysis.core.proto.UserCredentials

/**
 * 伪持久化版本的 AuthRepository。
 * 纯内存操作，不读写任何磁盘文件，App 重启后数据重置。
 * 函数签名与原版完全一致，方便在依赖注入中随时“偷梁换柱”。
 */
 //警告这是快速开发！！！！！不可用于生产环境
class AuthRepository {

    // 使用 MutableStateFlow 来完美模拟 DataStore 的响应式数据流
    private val _credentialsStore = MutableStateFlow(UserCredentials.getDefaultInstance())

    // --- 1. 保存逻辑 ---

    suspend fun saveCredentials(
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        // 使用 update 保证线程安全，完美模拟 DataStore.updateData
        _credentialsStore.update { current ->
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

    // 暴露为只读 Flow，UI 订阅它时就和订阅真正的 DataStore 一模一样
    val credentials: Flow<UserCredentials> = _credentialsStore
    
    val userId: Flow<Long> = credentials.map { it.userId }

    val deviceId: Flow<String> = credentials.map { it.deviceId.ifEmpty { generateDeviceId() } }

    // --- 3. 清理逻辑 ---

    suspend fun clearCredentials() {
        _credentialsStore.value = UserCredentials.getDefaultInstance()
    }

    private fun generateDeviceId(): String = (1..15).map { (0..9).random() }.joinToString("")
}