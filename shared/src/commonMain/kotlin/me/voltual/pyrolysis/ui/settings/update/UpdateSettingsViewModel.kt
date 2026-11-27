//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.settings.update

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import me.voltual.pyrolysis.data.UpdateNotificationMode
import me.voltual.pyrolysis.data.UpdateSettingsDataStore
import me.voltual.pyrolysis.data.UpdateInfo
import me.voltual.pyrolysis.core.ui.components.UpdateDialog
import me.voltual.pyrolysis.core.utils.UpdateCheckResult
import me.voltual.pyrolysis.core.utils.UpdateChecker

class UpdateSettingsViewModel(
    private val updateSettingsDataStore: UpdateSettingsDataStore
) : ViewModel() {

    val notificationMode: Flow<UpdateNotificationMode> = updateSettingsDataStore.notificationMode

    suspend fun setNotificationMode(mode: UpdateNotificationMode) {
        updateSettingsDataStore.setNotificationMode(mode)
    }

    fun checkForUpdates(
        requireChannelAsset: Boolean = false,
        onUpdateResult: (UpdateCheckResult) -> Unit
    ) {
        UpdateChecker.checkForUpdates(
            requireChannelAsset = requireChannelAsset,
            onUpdateResult = onUpdateResult
        )
    }
    
    @Composable
    private fun showUpdateDialog(updateInfo: UpdateInfo) {
        UpdateDialog(updateInfo = updateInfo) {
        }
    }
}