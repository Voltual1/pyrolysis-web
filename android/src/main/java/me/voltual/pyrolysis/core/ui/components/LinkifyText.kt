// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:Suppress("DEPRECATION")
package me.voltual.pyrolysis.core.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import me.voltual.pyrolysis.ui.*

/**
 * 内部帖子链接正则 (Kotlin Regex)
 */
private val INTERNAL_POST_LINK_REGEX = Regex("""http://apk\.xiaoqu\.online/post/(\d+)\.html""")

/**
 * B站视频链接正则 (Kotlin Regex)
 */
private val BILI_VIDEO_LINK_REGEX = Regex("""【视频：([a-zA-Z0-9]+)】""")

/**
 * 通用 URL 正则 (Kotlin Regex)
 */
private val GENERAL_URL_REGEX = Regex(
    """(?:(?:https?|ftp)://|www\.)[\w\-_]+(?:\.[\w\-_]+)+(?:[\w\-.,@?^=%&:/~+#]*[\w\-@?^=%&;/~+#])?"""
)

private data class LinkMatch(
    val range: IntRange,
    val text: String,
    val type: LinkType
)

private enum class LinkType {
    POST,
    BILIVIDEO,
    URL
}

/**
 * 自动识别文本中的帖子链接、B站视频标记和普通URL，并使其可点击。
 * - 已移除 java.util.regex 依赖，全面适配 Kotlin Regex。
 * - 使用 Navigation 3 的 LocalNavigator 进行内部导航。
 */
@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val linkColor = MaterialTheme.colorScheme.primary

    val textStyle = if (style.color == Color.Unspecified) {
        style.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        style
    }
        
    val annotatedString = remember(text, linkColor) {
        val processedText = text.replace("<br>", "\n")
        buildAnnotatedString {
            append(processedText)

            // 1. 获取所有匹配项并转换为序列
            val postMatches = INTERNAL_POST_LINK_REGEX.findAll(processedText).map { result ->
                LinkMatch(
                    range = result.range,
                    text = result.groups[1]?.value ?: "",
                    type = LinkType.POST
                )
            }

            val biliMatches = BILI_VIDEO_LINK_REGEX.findAll(processedText).map { result ->
                LinkMatch(
                    range = result.range,
                    text = result.groups[1]?.value ?: "",
                    type = LinkType.BILIVIDEO
                )
            }

            val urlMatches = GENERAL_URL_REGEX.findAll(processedText).map { result ->
                LinkMatch(
                    range = result.range,
                    text = result.value,
                    type = LinkType.URL
                )
            }

            // 2. 合并、排序并去重（防止通用 URL 误伤特定格式链接）
            val allMatches = (postMatches + biliMatches + urlMatches)
                .sortedBy { it.range.first }
                .fold(mutableListOf<LinkMatch>()) { acc, current ->
                    // 如果当前匹配项的起始位置不在已知匹配项范围内，则添加
                    if (acc.none { current.range.first in it.range }) {
                        acc.add(current)
                    }
                    acc
                }

            // 3. 应用样式和注解
            allMatches.forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
                addStringAnnotation(
                    tag = match.type.name,
                    annotation = match.text,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }

    SelectionContainer(modifier = modifier) {
        ClickableText(
            text = annotatedString,
            style = textStyle,
            onClick = { offset ->
                // 处理内部帖子链接
                annotatedString.getStringAnnotations(tag = LinkType.POST.name, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        annotation.item.toLongOrNull()?.let { postId ->
                            navigator.navigate(PostDetail(postId))
                        }
                        return@ClickableText
                    }

                // 处理B站视频链接
                annotatedString.getStringAnnotations(tag = LinkType.BILIVIDEO.name, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        navigator.navigate(Player(annotation.item))
                        return@ClickableText
                    }

                // 处理普通URL
                annotatedString.getStringAnnotations(tag = LinkType.URL.name, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val urlToOpen = annotation.item.let {
                            if (!it.startsWith("http://") && !it.startsWith("https://")) "http://$it" else it
                        }
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                            context.startActivity(intent)
                        }.onFailure { e ->
                            Log.e("LinkifyText", "无法打开URL: $urlToOpen", e)
                        }
                    }
            }
        )
    }
}