//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    // 使用 null 表示“跟随系统”，运行时切换后变为具体的 true 或 false
    private var _isDarkThemeSetting by mutableStateOf<Boolean?>(null)

    var customColorSet by mutableStateOf<CustomColorSet?>(null)
    
    fun updateCustomColors(colors: CustomColorSet) {
        customColorSet = colors
    }

    /**
     * 获取当前最终的深色模式状态（在 @Composable 中调用）
     */
    @Composable
    fun isAppDarkTheme(): Boolean {
        // 如果用户没手动切过（null），就拿 Compose 自带的系统深色状态，否则用用户的设置
        return _isDarkThemeSetting ?: isSystemInDarkTheme()
    }

    /**
     * 切换主题
     * @param isSystemDark 当前系统的深色模式状态（因为是非 Composable 环境，需要外面传进来，或者通过当前状态反转）
     */
    fun toggleTheme(isSystemDark: Boolean) {
        val currentMode = _isDarkThemeSetting ?: isSystemDark
        _isDarkThemeSetting = !currentMode
    }
}