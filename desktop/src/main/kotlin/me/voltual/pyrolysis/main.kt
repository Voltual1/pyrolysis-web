//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.request.crossfade
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.voltual.pyrolysis.core.database.LogDao
import me.voltual.pyrolysis.core.database.LogEntry
import me.voltual.pyrolysis.core.ui.icons.drawable.Fire
import me.voltual.pyrolysis.core.ui.theme.ThemeColorDataStore
import me.voltual.pyrolysis.core.ui.theme.ThemeManager
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import me.voltual.pyrolysis.shared.BuildConfig
import okio.Path.Companion.toOkioPath
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.io.File
import javax.swing.JOptionPane

fun main() {
    // 1. 初始化 FileKit（必须在访问 FileKit.filesDir / databasesDir / cacheDir 前调用）
    FileKit.init(appId = "me.voltual.pyrolysis")

    // 2. 启动 Koin 依赖注入容器
    val koinApp = startKoin {
        modules(commonModule, platformModule)
    }
    val koin = koinApp.koin

    // 3. 注册全局未捕获异常处理（对齐 Android 端日志入库）
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val crashReport = buildDesktopCrashReport(thread, throwable)
        System.err.println(crashReport)
        runCatching {
            val logDao: LogDao = koin.get()
            runBlocking {
                logDao.insert(
                    LogEntry(
                        type = "CRASH",
                        requestBody = "Desktop 线程 [${thread.name}] 崩溃",
                        responseBody = crashReport,
                        status = "FAILURE"
                    )
                )
            }
        }
        runCatching {
            JOptionPane.showMessageDialog(
                null,
                "程序遇到未捕获异常：${throwable.message ?: throwable::class.simpleName}\n详情已写入日志。",
                "Pyrolysis 崩溃报告",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    // 4. 同步预加载自定义主题色（对齐 BBQApplication）
    runCatching {
        val themeStore: ThemeColorDataStore = koin.get()
        runBlocking {
            ThemeManager.updateCustomColors(themeStore.colorsFlow.first())
        }
    }

    // 5. 适配 macOS 系统外观
    System.setProperty("apple.awt.application.appearance", "system")

    // 6. 启动 Compose Desktop 窗口
    application {
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(FileKit.cacheDir.file, "image_cache").toOkioPath())
                        .maxSizeBytes(256L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .build()
        }

        val themeStore: ThemeColorDataStore = koinInject()
        val currentColors by themeStore.colorsFlow.collectAsState(initial = null)
        LaunchedEffect(currentColors) {
            currentColors?.let {
                ThemeManager.updateCustomColors(it)
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Pyrolysis",
            icon = rememberVectorPainter(Fire),
            state = rememberWindowState(width = 1100.dp, height = 760.dp)
        ) {
            PyrolysisApp(
                platformEntryProvider = { _, _ -> null }
            )
        }
    }
}

private fun buildDesktopCrashReport(thread: Thread, throwable: Throwable): String {
    val stackTrace = throwable.stackTraceToString()
    val systemInfo = """
        操作系统: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})
        Java 版本: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})
        App 版本: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})
        崩溃线程: ${thread.name}
    """.trimIndent()
    return """
        崩溃信息: ${throwable.message}

        运行环境:
        $systemInfo

        堆栈跟踪:
        $stackTrace
    """.trimIndent()
}