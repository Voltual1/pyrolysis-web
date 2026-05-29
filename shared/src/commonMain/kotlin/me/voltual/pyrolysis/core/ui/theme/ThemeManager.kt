//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    // 默认是浅色还是深色，作为初始兜底（不影响后续随系统或手动切换）
    var isAppDarkTheme by mutableStateOf(false)
    
    var customColorSet by mutableStateOf<CustomColorSet?>(null)
    
    fun updateCustomColors(colors: CustomColorSet) {
        customColorSet = colors
    }
    
    /**
     * 纯运行时切换主题（不需要传参，完美向前兼容旧调用）
     */
    fun toggleTheme() {
        isAppDarkTheme = !isAppDarkTheme
    }        
            
}