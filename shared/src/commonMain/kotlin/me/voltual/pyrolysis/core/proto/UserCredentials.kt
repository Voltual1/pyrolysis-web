//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis.core.proto

import kotlinx.serialization.Serializable

/**
 * 纯 Kotlin 伪造的 UserCredentials 实体类。
 * 摆脱了 Google Protobuf 对 JVM 的依赖，确保 KMP 全平台（iOS/Desktop/Wasm）顺利编译。
 * 支持 kotlinx.serialization 序列化。
 */
@Serializable
class UserCredentials internal constructor(
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val userId: Long = 0L,
    val deviceId: String = "",
    val lanzouUsername: String = "",
    val lanzouPassword: String = "",
    val lanzouSessionCookie: String = "",
    val lanzouCsrfToken: String = "",
    val lanzouFolderName: String = ""
) {
    fun toBuilder(): Builder = Builder(this)

    class Builder {
        private var username = ""
        private var password = ""
        private var token = ""
        private var userId = 0L
        private var deviceId = ""
        private var lanzouUsername = ""
        private var lanzouPassword = ""
        private var lanzouSessionCookie = ""
        private var lanzouCsrfToken = ""
        private var lanzouFolderName = ""

        constructor()

        constructor(credentials: UserCredentials) {
            this.username = credentials.username
            this.password = credentials.password
            this.token = credentials.token
            this.userId = credentials.userId
            this.deviceId = credentials.deviceId
            this.lanzouUsername = credentials.lanzouUsername
            this.lanzouPassword = credentials.lanzouPassword
            this.lanzouSessionCookie = credentials.lanzouSessionCookie
            this.lanzouCsrfToken = credentials.lanzouCsrfToken
            this.lanzouFolderName = credentials.lanzouFolderName
        }

        fun setUsername(value: String) = apply { this.username = value }
        fun setPassword(value: String) = apply { this.password = value }
        fun setToken(value: String) = apply { this.token = value }
        fun setUserId(value: Long) = apply { this.userId = value }
        fun setDeviceId(value: String) = apply { this.deviceId = value }
        fun setLanzouUsername(value: String) = apply { this.lanzouUsername = value }
        fun setLanzouPassword(value: String) = apply { this.lanzouPassword = value }
        fun setLanzouSessionCookie(value: String) = apply { this.lanzouSessionCookie = value }
        fun setLanzouCsrfToken(value: String) = apply { this.lanzouCsrfToken = value }
        fun setLanzouFolderName(value: String) = apply { this.lanzouFolderName = value }

        fun build(): UserCredentials = UserCredentials(
            username = username,
            password = password,
            token = token,
            userId = userId,
            deviceId = deviceId,
            lanzouUsername = lanzouUsername,
            lanzouPassword = lanzouPassword,
            lanzouSessionCookie = lanzouSessionCookie,
            lanzouCsrfToken = lanzouCsrfToken,
            lanzouFolderName = lanzouFolderName
        )
    }

    companion object {
        private val DEFAULT_INSTANCE = UserCredentials()
		// 模拟 Protobuf 的 getDefaultInstance()
        fun getDefaultInstance(): UserCredentials = DEFAULT_INSTANCE
    }
}