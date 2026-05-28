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
import androidx.compose.ui.platform.ClipData // 导入 Compose 统一的 ClipData
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.core.ui.theme.BBQSnackbarHost
import me.voltual.pyrolysis.core.utils.cleanUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    imageUrl: String,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState, 
    onClose: () -> Unit
) {
    // 1. 使用 Compose 多平台统一的 LocalClipboard
    val clipboard = LocalClipboard.current
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
                // 点击背景关闭预览
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 2. 缩放与平移手势
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
                    // 3. 点击、双击与长按手势（替代 combinedClickable 避免与 transform 冲突）
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClose() }, // 单击关闭
                            onDoubleTap = {
                                // 双击切换放大状态：如果在放大状态则还原，在原图状态则放大到 3 倍
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 3f
                                }
                            },
                            onLongPress = {
                                scope.launch {
                                    // 4. 方案二：使用多平台统一的 ClipData.withPlainText
                                    val clipData = ClipData.withPlainText(cleanedImageUrl)
                                    clipboard.setClipData(clipData)
                                    
                                    internalSnackbarHostState.showSnackbar(
                                        message = "已复制图片链接"
                                    )
                                }
                            }
                        )
                    }
                    // 5. 应用图形变换
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