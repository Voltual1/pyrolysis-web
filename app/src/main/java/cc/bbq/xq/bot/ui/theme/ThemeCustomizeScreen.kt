//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.bot.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import coil.compose.rememberAsyncImagePainter
import android.content.res.Configuration
import android.net.Uri
import android.util.DisplayMetrics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// 从原 Activity 中提取的主题管理对象
object ThemeCustomizeManager {

    var restartApp: (() -> Unit)? = null

    fun saveThemeAndRestart(
        context: Context,
        colors: CustomColorSet,
        dpi: Float,
        fontScale: Float,
        lightUri: String?,
        darkUri: String?
    ) {
        val scope = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope ?: kotlinx.coroutines.MainScope()
        scope.launch {
            ThemeColorStore.saveColors(context, colors)
            ThemeColorStore.saveDpi(context, dpi)
            ThemeColorStore.saveFontSize(context, fontScale)
            ThemeColorStore.saveDrawerHeaderLightBackgroundUri(context, lightUri)
            ThemeColorStore.saveDrawerHeaderDarkBackgroundUri(context, darkUri)

            withContext(Dispatchers.Main) {
                ThemeManager.applyCustomColors(context)
                (context as? Activity)?.let {
                    applyDpiAndFontScale(it, dpi, fontScale)
                }
                // 延迟一小段时间以确保设置已应用，然后触发重启
                delay(300)
                restartApp?.invoke()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyDpiAndFontScale(activity: Activity, dpi: Float, fontScale: Float) {
        val resources = activity.resources
        val configuration = Configuration(resources.configuration)
        val metrics = resources.displayMetrics
        val newDensityDpi = (dpi * DisplayMetrics.DENSITY_DEFAULT).toInt()
        configuration.densityDpi = newDensityDpi
        configuration.fontScale = fontScale
        metrics.densityDpi = newDensityDpi
        resources.updateConfiguration(configuration, metrics)
    }
}

@Composable
private fun DrawerHeaderPreview(modifier: Modifier = Modifier, backgroundUri: String?) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (backgroundUri != null) {
            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(model = backgroundUri),
                contentDescription = "Drawer Header Background Preview",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 显示默认背景或占位符
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizeScreen(
    onSave: (CustomColorSet, Float, Float, String?, String?) -> Unit,
    modifier: Modifier = Modifier // 新增：接收外部 modifier，移除 onBack
) {
    val context = LocalContext.current

    val initialColors = remember { ThemeColorStore.loadColors(context) }
    var lightColors by remember { mutableStateOf(initialColors.lightSet) }
    var darkColors by remember { mutableStateOf(initialColors.darkSet) }

    var showSavedMessage by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    var dpi by remember { mutableStateOf(ThemeColorStore.loadDpi(context)) }
    var fontSize by remember { mutableStateOf(ThemeColorStore.loadFontSize(context)) }

    val initialLightUri by ThemeColorStore.getDrawerHeaderLightBackgroundUriFlow(context).collectAsState(initial = null)
    var lightDrawerBgUri by remember(initialLightUri) { mutableStateOf(initialLightUri) }

    val initialDarkUri by ThemeColorStore.getDrawerHeaderDarkBackgroundUriFlow(context).collectAsState(initial = null)
    var darkDrawerBgUri by remember(initialDarkUri) { mutableStateOf(initialDarkUri) }

    var selectedTab by remember { mutableStateOf(0) }

    val lightImagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            lightDrawerBgUri = it.toString()
        }
    }

    val darkImagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            darkDrawerBgUri = it.toString()
        }
    }

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
            text = { Text("确定要恢复所有颜色、设置和侧边栏背景为默认值吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        lightColors = ThemeColorStore.DEFAULT_COLORS.lightSet
                        darkColors = ThemeColorStore.DEFAULT_COLORS.darkSet
                        dpi = 1.0f
                        fontSize = 1.0f
                        lightDrawerBgUri = null
                        darkDrawerBgUri = null
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("取消") } }
        )
    }

    // 使用 Box 布局来悬浮 FAB
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showSavedMessage) {
                Text(
                    text = "主题已保存！正在应用...",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // 添加重置按钮到内容区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Filled.Refresh, "恢复默认设置")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // 为 FAB 留出空间
            ) {
                item {
                    Text(
                        text = "显示设置",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = dpi.toString(),
                        onValueChange = { dpi = it.toFloatOrNull() ?: dpi },
                        label = { Text("屏幕密度 (DPI 缩放)") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = fontSize.toString(),
                        onValueChange = { fontSize = it.toFloatOrNull() ?: fontSize },
                        label = { Text("字体大小 (倍数)") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

                item {
                    TabRow(selectedTabIndex = selectedTab, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("日间模式") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("夜间模式") })
                    }
                }

                when (selectedTab) {
                    0 -> {
                        item {
                            DrawerBackgroundEditor(
                                title = "侧边栏背景 (日间)",
                                backgroundUri = lightDrawerBgUri,
                                onSelectImage = { lightImagePickerLauncher.launch(arrayOf("image/*")) },
                                onReset = { lightDrawerBgUri = null }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) }
                        items(lightColors.toList(), key = { "light_" + it.first }) { (name, color) ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ColorEditItem(
                                    colorName = name,
                                    currentColor = color,
                                    onColorChange = { newColor -> lightColors = lightColors.copyWith(name, newColor) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                    1 -> {
                        item {
                            DrawerBackgroundEditor(
                                title = "侧边栏背景 (夜间)",
                                backgroundUri = darkDrawerBgUri,
                                onSelectImage = { darkImagePickerLauncher.launch(arrayOf("image/*")) },
                                onReset = { darkDrawerBgUri = null }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) }
                        items(darkColors.toList(), key = { "dark_" + it.first }) { (name, color) ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ColorEditItem(
                                    colorName = name,
                                    currentColor = color,
                                    onColorChange = { newColor -> darkColors = darkColors.copyWith(name, newColor) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        // 将 FAB 悬浮在内容上方
        ExtendedFloatingActionButton(
            onClick = {
                onSave(CustomColorSet(lightColors, darkColors), dpi, fontSize, lightDrawerBgUri, darkDrawerBgUri)
                showSavedMessage = true
            },
            icon = { Icon(Icons.Filled.Save, "保存") },
            text = { Text("保存并应用") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

// 其他辅助 Composable 函数保持不变...
@Composable
private fun DrawerBackgroundEditor(
    title: String,
    backgroundUri: String?,
    onSelectImage: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            DrawerHeaderPreview(
                modifier = Modifier.fillMaxSize(),
                backgroundUri = backgroundUri
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSelectImage, modifier = Modifier.weight(1f)) {
                Text("选择图片")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Text("恢复默认")
            }
        }
    }
}

@Composable
fun ColorEditItem(
    colorName: String,
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    var hexValue by remember(currentColor) { mutableStateOf(currentColor.toHex()) }
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        HsvColorPickerDialog(
            initialColor = currentColor,
            onColorSelected = { newColor ->
                onColorChange(newColor)
                hexValue = newColor.toHex()
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(currentColor)
                .border(1.dp, MaterialTheme.colorScheme.outline)
        )
        Text(
            text = colorName,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hexValue,
                onValueChange = {
                    val newHex = it.take(6)
                    hexValue = newHex
                    if (newHex.isValidHex()) {
                        onColorChange(Color(android.graphics.Color.parseColor("#$newHex")))
                    }
                },
                label = { Text("HEX") },
                modifier = Modifier.width(100.dp),
                maxLines = 1,
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showColorPicker = true }) {
                Icon(
                    imageVector = Icons.Filled.ColorLens,
                    contentDescription = "选择颜色"
                )
            }
        }
    }
}

@Composable
fun HsvColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val hsvArray = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArray)

    var hue by remember { mutableStateOf(hsvArray[0]) }
    var saturation by remember { mutableStateOf(hsvArray[1]) }
    var value by remember { mutableStateOf(hsvArray[2]) }

    val currentColor = remember(hue, saturation, value) {
        Color.hsv(hue, saturation, value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    )
                    Column {
                        Text("HEX: ${currentColor.toHex()}")
                        Text("RGB: ${currentColor.red.to255()}, ${currentColor.green.to255()}, ${currentColor.blue.to255()}")
                    }
                }

                Text("色相 (0-360°)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("饱和度 (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("亮度 (0-100%)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(currentColor) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun Color.toHex(): String {
    return String.format("%06X", this.toArgb() and 0xFFFFFF)
}

fun String.isValidHex(): Boolean =
    this.length == 6 && this.matches(Regex("[0-9A-Fa-f]{6}"))

fun Float.to255(): Int = (this * 255).roundToInt()