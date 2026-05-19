//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.core.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.DisplayMetrics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.restartMainActivity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialColors = remember { ThemeColorStore.loadColors(context) }
    var lightColors by remember { mutableStateOf(initialColors.lightSet) }
    var darkColors by remember { mutableStateOf(initialColors.darkSet) }

    var showSavedMessage by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    var dpi by remember { mutableStateOf(ThemeColorStore.loadDpi(context)) }
    var fontSize by remember { mutableStateOf(ThemeColorStore.loadFontSize(context)) }
    var customDpiEnabled by remember { mutableStateOf(ThemeColorStore.loadCustomDpiEnabled(context)) }
    
    val roundScreenPaddings = remember { ThemeColorStore.loadRoundScreenPaddings(context) }
    var roundScreenEnabled by remember { mutableStateOf(roundScreenPaddings.enabled) }
    var roundLeft by remember { mutableStateOf(roundScreenPaddings.left) }
    var roundTop by remember { mutableStateOf(roundScreenPaddings.top) }
    var roundRight by remember { mutableStateOf(roundScreenPaddings.right) }
    var roundBottom by remember { mutableStateOf(roundScreenPaddings.bottom) }

    var translate by remember { mutableStateOf(false) }

    // 全局背景选择器
    val globalBackgroundPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scope.launch { ThemeColorStore.saveGlobalBackgroundUri(context, it.toString()) }
        }
    }

    // 侧边栏背景选择器
    val lightDrawerBgPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scope.launch { ThemeColorStore.saveDrawerHeaderLightBackgroundUri(context, it.toString()) }
        }
    }

    val darkDrawerBgPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scope.launch { ThemeColorStore.saveDrawerHeaderDarkBackgroundUri(context, it.toString()) }
        }
    }

    val globalBackgroundUri by ThemeColorStore.getGlobalBackgroundUriFlow(context).collectAsState(initial = null)
    val lightDrawerBgUri by ThemeColorStore.getDrawerHeaderLightBackgroundUriFlow(context).collectAsState(initial = null)
    val darkDrawerBgUri by ThemeColorStore.getDrawerHeaderDarkBackgroundUriFlow(context).collectAsState(initial = null)

    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            delay(2000)
            showSavedMessage = false
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("恢复默认主题") },
            text = { Text("确定要恢复所有颜色、设置和背景图片为默认值吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            lightColors = ThemeColorStore.DEFAULT_COLORS.lightSet
                            darkColors = ThemeColorStore.DEFAULT_COLORS.darkSet
                            dpi = 1.0f
                            fontSize = 1.0f
                            customDpiEnabled = false
                            ThemeColorStore.saveGlobalBackgroundUri(context, null)
                            ThemeColorStore.saveDrawerHeaderLightBackgroundUri(context, null)
                            ThemeColorStore.saveDrawerHeaderDarkBackgroundUri(context, null)
                        }
                        showResetDialog = false
                    }
                ) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("取消") } }
        )
    }

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showSavedMessage) {
                Text(
                    text = "主题已保存！正在应用...",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Filled.Refresh, "恢复默认设置")
                }
                IconButton(onClick = { translate = !translate }) {
                    Icon(Icons.Filled.Language, "翻译")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    GlobalBackgroundEditor(
                        title = "主页背景图片",
                        backgroundUri = globalBackgroundUri,
                        onSelectImage = { globalBackgroundPickerLauncher.launch(arrayOf("image/*")) },
                        onReset = { scope.launch { ThemeColorStore.saveGlobalBackgroundUri(context, null) } }
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

                item {
                    Text(
                        text = "显示设置",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    SwitchWithText(
                        text = "启用圆屏适配",
                        checked = roundScreenEnabled,
                        onCheckedChange = { roundScreenEnabled = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                val paddingFields = listOf(
                    "左" to { v: Float -> roundLeft = v } to roundLeft,
                    "上" to { v: Float -> roundTop = v } to roundTop,
                    "右" to { v: Float -> roundRight = v } to roundRight,
                    "下" to { v: Float -> roundBottom = v } to roundBottom
                )

                paddingFields.forEach { (label, setter, current) ->
                    item {
                        OutlinedTextField(
                            value = current.toString(),
                            onValueChange = { setter(it.toFloatOrNull() ?: current) },
                            label = { Text("$label内边距 (dp)") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            enabled = roundScreenEnabled
                        )
                    }
                }

                item {
                    SwitchWithText(
                        text = "启用自定义屏幕密度和字体大小",
                        checked = customDpiEnabled,
                        onCheckedChange = { customDpiEnabled = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = dpi.toString(),
                        onValueChange = { dpi = it.toFloatOrNull() ?: dpi },
                        label = { Text("屏幕密度 (DPI 缩放)") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        enabled = customDpiEnabled
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = fontSize.toString(),
                        onValueChange = { fontSize = it.toFloatOrNull() ?: fontSize },
                        label = { Text("字体大小 (倍数)") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        enabled = customDpiEnabled
                    )
                }

                item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

                item {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("日间模式") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("夜间模式") })
                    }
                }

                when (selectedTab) {
                    0 -> renderColorList(context, scope, lightColors, lightDrawerBgUri, lightDrawerBgPickerLauncher, translate, isDark = false) { lightColors = it }
                    1 -> renderColorList(context, scope, darkColors, darkDrawerBgUri, darkDrawerBgPickerLauncher, translate, isDark = true) { darkColors = it }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                saveThemeAndRestart(
                    context = context,
                    colors = CustomColorSet(lightColors, darkColors),
                    dpi = dpi,
                    fontScale = fontSize,
                    customDpiEnabled = customDpiEnabled,
                    roundScreenEnabled = roundScreenEnabled,  
                    roundLeft = roundLeft,                  
                    roundTop = roundTop,                    
                    roundRight = roundRight,
                    roundBottom = roundBottom                             
                )
                showSavedMessage = true
            },
            icon = { Icon(Icons.Filled.Save, "保存") },
            text = { Text("保存并应用") },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

private fun LazyListScope.renderColorList(
    context: Context,
    scope: CoroutineScope,
    colorSet: CustomColorSet.ColorSet,
    drawerBgUri: String?,
    picker: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    translate: Boolean,
    isDark: Boolean,
    onUpdate: (CustomColorSet.ColorSet) -> Unit
) {
    item {
        DrawerBackgroundEditor(
            title = "侧边栏背景 (${if (isDark) "夜间" else "日间"})",
            description = "仅修改侧边栏头部背景图片",
            backgroundUri = drawerBgUri,
            onSelectImage = { picker.launch(arrayOf("image/*")) },
            onReset = {
                scope.launch {
                    if (isDark) ThemeColorStore.saveDrawerHeaderDarkBackgroundUri(context, null)
                    else ThemeColorStore.saveDrawerHeaderLightBackgroundUri(context, null)
                }
            }
        )
    }
    item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) }
    items(colorSet.toList(), key = { (if (isDark) "dark_" else "light_") + it.first }) { (name, color) ->
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ColorEditItem(
                colorName = name,
                currentColor = color,
                onColorChange = { newColor -> onUpdate(colorSet.copyWith(name, newColor)) },
                translate = translate
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun GlobalBackgroundEditor(title: String, backgroundUri: String?, onSelectImage: () -> Unit, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
        Card(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (backgroundUri != null) {
                Image(painter = rememberAsyncImagePainter(model = backgroundUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(text = "未选择图片", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSelectImage, modifier = Modifier.weight(1f)) { Text("选择图片") }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("恢复默认") }
        }
    }
}

private fun saveThemeAndRestart(
    context: Context,
    colors: CustomColorSet,
    dpi: Float,
    fontScale: Float,
    customDpiEnabled: Boolean,
    roundScreenEnabled: Boolean,  
    roundLeft: Float,            
    roundTop: Float,             
    roundRight: Float,           
    roundBottom: Float           
) {
    val scope = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope ?: kotlinx.coroutines.MainScope()
    scope.launch {
        val oldDpi = ThemeColorStore.loadDpi(context)
        val oldFontScale = ThemeColorStore.loadFontSize(context)
        val oldCustomDpiEnabled = ThemeColorStore.loadCustomDpiEnabled(context)

        ThemeColorStore.saveColors(context, colors)
        ThemeColorStore.saveDpi(context, dpi)
        ThemeColorStore.saveFontSize(context, fontScale)
        ThemeColorStore.saveCustomDpiEnabled(context, customDpiEnabled)
        ThemeColorStore.saveRoundScreenPaddings(context, roundScreenEnabled, roundLeft, roundTop, roundRight, roundBottom)

        withContext(Dispatchers.Main) {
            ThemeManager.applyCustomColors(context)
            if (oldDpi != dpi || oldFontScale != fontScale || oldCustomDpiEnabled != customDpiEnabled) {
                (context as? Activity)?.let {
                    if (customDpiEnabled) {
                        val resources = it.resources
                        val configuration = Configuration(resources.configuration)
                        val metrics = resources.displayMetrics
                        val newDensityDpi = (dpi * DisplayMetrics.DENSITY_DEFAULT).toInt()
                        configuration.densityDpi = newDensityDpi
                        configuration.fontScale = fontScale
                        metrics.densityDpi = newDensityDpi
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(configuration, metrics)
                    }
                }
                delay(300)
                restartMainActivity(context)
            }
        }
    }
}

@Composable
private fun DrawerBackgroundEditor(title: String, description: String, backgroundUri: String?, onSelectImage: () -> Unit, onReset: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Card(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer)) {
                if (backgroundUri != null) {
                    Image(painter = rememberAsyncImagePainter(model = backgroundUri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSelectImage, modifier = Modifier.weight(1f)) { Text("选择图片") }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("恢复默认") }
        }
    }
}

@Composable
fun ColorEditItem(colorName: String, currentColor: Color, onColorChange: (Color) -> Unit, translate: Boolean) {
    var hexValue by remember(currentColor) { mutableStateOf(currentColor.toHexString()) }
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        HsvColorPickerDialog(
            initialColor = currentColor,
            onColorSelected = { newColor ->
                onColorChange(newColor)
                hexValue = newColor.toHexString()
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).background(currentColor).border(1.dp, MaterialTheme.colorScheme.outline))
        Text(text = if (translate) colorNameTranslations[colorName] ?: colorName else colorName, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hexValue,
                onValueChange = {
                    val newHex = it.take(6)
                    hexValue = newHex
                    newHex.toColorOrNull()?.let { c -> onColorChange(c) }
                },
                label = { Text("HEX") },
                modifier = Modifier.width(100.dp),
                maxLines = 1,
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showColorPicker = true }) { Icon(Icons.Filled.ColorLens, null) }
        }
    }
}

@Composable
fun HsvColorPickerDialog(initialColor: Color, onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    // 纯 Kotlin 实现的 RGB to HSV 转换
    val r = initialColor.red
    val g = initialColor.green
    val b = initialColor.blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min

    var h = 0f
    if (delta > 0) {
        h = when (max) {
            r -> 60 * (((g - b) / delta) % 6)
            g -> 60 * (((b - r) / delta) + 2)
            else -> 60 * (((r - g) / delta) + 4)
        }
        if (h < 0) h += 360f
    }
    val s = if (max == 0f) 0f else delta / max
    val v = max

    var hue by remember { mutableStateOf(h) }
    var saturation by remember { mutableStateOf(s) }
    var value by remember { mutableStateOf(v) }
    val currentColor = remember(hue, saturation, value) { Color.hsv(hue, saturation, value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).background(currentColor).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
                    Column {
                        Text("HEX: ${currentColor.toHexString()}")
                        Text("RGB: ${(currentColor.red * 255).toInt()}, ${(currentColor.green * 255).toInt()}, ${(currentColor.blue * 255).toInt()}")
                    }
                }
                Text("色相 (0-360°)", style = MaterialTheme.typography.labelMedium)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text("饱和度 (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
                Text("亮度 (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
            }
        },
        confirmButton = { Button(onClick = { onColorSelected(currentColor) }) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/**
 * 纯 Kotlin 实现：Color 转 HEX 字符串
 */
fun Color.toHexString(): String {
    val argb = this.toArgb()
    val r = (argb shr 16 and 0xFF).toString(16).padStart(2, '0')
    val g = (argb shr 8 and 0xFF).toString(16).padStart(2, '0')
    val b = (argb and 0xFF).toString(16).padStart(2, '0')
    return (r + g + b).uppercase()
}

/**
 * 纯 Kotlin 实现：HEX 字符串转 Color
 */
fun String.toColorOrNull(): Color? {
    if (length != 6) return null
    return try {
        val r = substring(0, 2).toInt(16)
        val g = substring(2, 4).toInt(16)
        val b = substring(4, 6).toInt(16)
        Color(r, g, b, 255)
    } catch (e: Exception) { null }
}

val colorNameTranslations = mapOf(
    "primary" to "主要颜色", "onPrimary" to "主要文字颜色",
    "primaryContainer" to "主要容器颜色", "onPrimaryContainer" to "主要容器文字颜色",
    "secondary" to "次要颜色", "onSecondary" to "次要文字颜色",
    "secondaryContainer" to "次要容器颜色", "onSecondaryContainer" to "次要容器文字颜色",
    "surface" to "表面颜色", "onSurface" to "表面文字颜色",
    "surfaceVariant" to "表面变体颜色", "onSurfaceVariant" to "表面变体文字颜色",
    "outline" to "轮廓颜色", "error" to "错误颜色", "onError" to "错误文字颜色",
    "background" to "背景颜色", "onBackground" to "背景文字颜色",
    "messageLikeBg" to "点赞消息背景色", "messageCommentBg" to "评论消息背景色",
    "messageDefaultBg" to "普通消息背景色", "billingIncome" to "收入颜色",
    "billingExpense" to "支出颜色"
)