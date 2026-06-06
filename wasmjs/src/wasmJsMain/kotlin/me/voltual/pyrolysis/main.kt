//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import me.voltual.pyrolysis.Res
import me.voltual.pyrolysis.ui.*
import me.voltual.pyrolysis.unifont
import me.voltual.pyrolysis.core.ui.theme.BBQTheme
import me.voltual.pyrolysis.core.ui.icons.drawable.Fire
import org.koin.core.context.startKoin
import androidx.compose.ui.platform.LocalUriHandler

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(commonModule, platformModule)
    }

    val composeRoot = document.getElementById("ComposeApp")!!

    ComposeViewport(composeRoot) {
        @OptIn(ExperimentalResourceApi::class)
        val unifontState = preloadFont(Res.font.unifont)
        val unifont = unifontState.value
        val fontFamilyResolver = LocalFontFamilyResolver.current
        
        BBQTheme {
            if (unifont != null) {
                LaunchedEffect(unifont) {
                    fontFamilyResolver.preload(FontFamily(listOf(unifont)))
                }

                PyrolysisApp(
                    platformEntryProvider = { key ->
                        when (key) {
                        is Player -> {           
                                val uriHandler = LocalUriHandler.current
                                val navigator = LocalNavigator.current
                                
                                LaunchedEffect(key.bvid) {
                                    if (key.bvid.isNotBlank()) {
                                        // 1. 在浏览器新标签页（或当前页）打开 Bilibili 链接
                                        uriHandler.openUri("https://www.bilibili.com/video/${key.bvid}")
                                    }
                                    // 2. 联动回退导航栈，防止 Wasm 页面卡在一个空白的 Player 路由上
                                    navigator.goBack()
                                }

                                // 渲染一个过渡的加载中 UI 或保持空白
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator() // 或者直接放空 Box()
                                }
                            }
                        } else {
                            null // 其他页面依然走公共保底逻辑
                        }
                    }
                )
            } else {
                // WasmJS 启动页
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Fire,
                            contentDescription = "Loading",
                            modifier = Modifier.size(100.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}