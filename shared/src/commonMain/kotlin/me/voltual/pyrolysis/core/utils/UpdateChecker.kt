//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.core.utils

import me.voltual.pyrolysis.PlatformType
import me.voltual.pyrolysis.getPlatformType
import me.voltual.pyrolysis.shared.BuildConfig
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.data.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 定义一个密封类来封装检查更新的结果
sealed class UpdateCheckResult {
    data class Success(val updateInfo: UpdateInfo) : UpdateCheckResult()
    data class NoUpdate(val message: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {

    /**
     * 过滤出匹配指定渠道的 Release 构建产物列表
     */
    fun filterAssetsForPlatform(
        assets: List<UpdateInfo.Asset>,
        platformType: PlatformType = getPlatformType()
    ): List<UpdateInfo.Asset> {
        return when (platformType) {
            PlatformType.ANDROID -> assets.filter { asset ->
                val lowerName = asset.name.lowercase()
                lowerName.endsWith(".apk") && !lowerName.contains("fdroid")
            }
            PlatformType.ANDROID_FDROID -> assets.filter { asset ->
                val lowerName = asset.name.lowercase()
                lowerName.endsWith(".apk") && lowerName.contains("fdroid")
            }
            PlatformType.JVM -> assets.filter { asset ->
                val lowerName = asset.name.lowercase()
                lowerName.endsWith(".dmg") ||
                    lowerName.endsWith(".msi") ||
                    lowerName.endsWith(".exe") ||
                    lowerName.endsWith(".deb") ||
                    lowerName.endsWith(".rpm") ||
                    lowerName.endsWith(".appimage") ||
                    lowerName.contains("desktop") ||
                    lowerName.contains("jvm")
            }
            PlatformType.WEB_WASM -> assets.filter { asset ->
                asset.name.lowercase().endsWith("-wasm.zip")
            }
        }
    }

    /**
     * 判断给定 UpdateInfo 是否包含当前渠道的可用更新包
     */
    fun hasAssetForPlatform(
        updateInfo: UpdateInfo,
        platformType: PlatformType = getPlatformType()
    ): Boolean {
        return filterAssetsForPlatform(updateInfo.assets, platformType).isNotEmpty()
    }

    /**
     * 多段数字版本号比较：如果 remoteVersion > localVersion 返回 true
     */
    private fun isNewerVersion(remoteVersion: String, localVersion: String): Boolean {
        val remoteParts = remoteVersion.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = localVersion.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    /**
     * 检查更新
     * @param requireChannelAsset 为 true 时，只有当更新包列表中包含当前渠道产物才返回 Success
     */
    fun checkForUpdates(
        requireChannelAsset: Boolean = false,
        onUpdateResult: (UpdateCheckResult) -> Unit
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = KtorClient.ApiServiceImpl.getLatestRelease()
                if (result.isSuccess) {
                    val update = result.getOrNull()
                    if (update != null) {
                        val currentVersion = BuildConfig.VERSION_NAME
                        val newVersion = update.tag_name.replace(Regex("[^\\d.]"), "")
                        if (isNewerVersion(newVersion, currentVersion)) {
                            if (requireChannelAsset && !hasAssetForPlatform(update)) {
                                withContext(Dispatchers.Main) {
                                    onUpdateResult(UpdateCheckResult.NoUpdate("当前渠道暂无新版本"))
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    onUpdateResult(UpdateCheckResult.Success(update))
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onUpdateResult(UpdateCheckResult.NoUpdate("当前已是最新版本"))
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onUpdateResult(UpdateCheckResult.Error("获取更新信息失败"))
                        }
                    }
                } else {
                    val errorMsg = "检查更新失败" + ": ${result.exceptionOrNull()?.message}"
                    withContext(Dispatchers.Main) {
                        onUpdateResult(UpdateCheckResult.Error(errorMsg))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = "检查更新出错" + ": ${e.message}"
                withContext(Dispatchers.Main) {
                    onUpdateResult(UpdateCheckResult.Error(errorMsg))
                }
            }
        }
    }
}