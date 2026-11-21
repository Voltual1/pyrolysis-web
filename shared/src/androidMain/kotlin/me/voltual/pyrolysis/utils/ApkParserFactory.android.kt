// FILE: shared/src/androidMain/kotlin/me/voltual/pyrolysis/utils/ApkParserFactory.android.kt
package me.voltual.pyrolysis.utils

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

public actual suspend fun extractApkMetadata(file: PlatformFile): UnifiedApkMetadata = withContext(Dispatchers.IO) {
    val context = FileKit.context
    val pm = context.packageManager

    val filePath = when (val af = file.androidFile) {
        is AndroidFile.FileWrapper -> af.file.absolutePath
        is AndroidFile.UriWrapper -> {
            // 若为 content:// Uri，复制到临时文件供 PackageParser 安全读取
            val tempFile = File.createTempFile("apk_parse_", ".apk", context.cacheDir)
            try {
                context.contentResolver.openInputStream(af.uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                tempFile.absolutePath
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    }

    val flags = PackageManager.GET_ACTIVITIES
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageArchiveInfo(filePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageArchiveInfo(filePath, flags)
    } ?: throw IllegalStateException("无法解析该 APK 文件包信息")

    packageInfo.applicationInfo?.let { appInfo ->
        appInfo.sourceDir = filePath
        appInfo.publicSourceDir = filePath
    }

    val packageName = packageInfo.packageName
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    val versionName = packageInfo.versionName

    val label = packageInfo.applicationInfo?.let { appInfo ->
        try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            null
        }
    }

    // 提取应用图标字节
    var iconBytes: ByteArray? = null
    try {
        val iconDrawable: Drawable? = packageInfo.applicationInfo?.loadIcon(pm)
        if (iconDrawable != null) {
            val bitmap = drawableToBitmap(iconDrawable)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            iconBytes = stream.toByteArray()
        }
    } catch (_: Exception) {
    }

    // 清理临时文件（若为 Uri 复制生成的）
    if (file.androidFile is AndroidFile.UriWrapper) {
        try {
            File(filePath).delete()
        } catch (_: Exception) {}
    }

    UnifiedApkMetadata(
        packageName = packageName,
        label = label,
        versionCode = versionCode,
        versionName = versionName,
        iconBytes = iconBytes
    )
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}