package me.voltual.pyrolysis

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import org.koin.core.context.startKoin

fun main() {
    // 1. 初始化 FileKit（必须在访问 FileKit.filesDir / databasesDir 前调用）
    FileKit.init(appId = "me.voltual.pyrolysis")

    // 2. 启动 Koin 依赖注入容器
    startKoin {
        modules(commonModule, platformModule)
    }

    // 3. 适配 macOS 系统外观
    System.setProperty("apple.awt.application.appearance", "system")

    // 4. 启动 Compose Desktop 窗口
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Pyrolysis",
            state = rememberWindowState(width = 1100.dp, height = 760.dp)
        ) {
            PyrolysisApp()
        }
    }
}