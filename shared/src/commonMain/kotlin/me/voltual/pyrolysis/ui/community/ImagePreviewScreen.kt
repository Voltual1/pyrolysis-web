//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager // 迎回老朋友
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.core.ui.theme.BBQSnackbarHost
import me.voltual.pyrolysis.core.utils.cleanUrl

// 在函数头上直接压制弃用警告，眼不见心不烦
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    imageUrl: String,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState, 
    onClose: () -> Unit
) {
    // 愉快地继续使用旧版 API，直接传 AnnotatedString 极其省心
    val clipboardManager = LocalClipboardManager.current
    val internalSnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val cleanedImageUrl = imageUrl.cleanUrl()

    // 缩放、平移状态管理
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { BBQSnackbarHost(internalSnackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 1. 缩放与平移手势
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 8f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    // 2. 完美的手势融合：单击关闭、双击放大、长按复制
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClose() },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 3f
                                }
                            },
                            onLongPress = {
                                scope.launch {
                                    // 老 API 直接塞字符串，一行搞定
                                    clipboardManager.setText(AnnotatedString(cleanedImageUrl))
                                    
                                    internalSnackbarHostState.showSnackbar(
                                        message = "已复制图片链接"
                                    )
                                }
                            }
                        )
                    }
                    // 3. 应用图形变换
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cleanedImageUrl)
                        .size(coil3.size.Size.ORIGINAL)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}