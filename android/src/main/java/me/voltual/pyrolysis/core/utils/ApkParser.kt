//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.pyrolysis.core.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import kotlin.datetime.Clock
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import kotlin.math.roundToInt

data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int,        
    val targetSdkVersion: Int,    
    val sizeInMb: Double,
    val tempApkPath: Path,
    val tempIconPath: Path?,
    val tempIconFileUri: Uri?
)

object ApkParser {

    private val fileSystem = FileSystem.SYSTEM

    private fun generateUniqueFileName(prefix: String, extension: String): String {
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val randomSuffix = (100..999).random()
        return "${prefix}_${timestamp}_${randomSuffix}.$extension"
    }

    fun parse(context: Context, apkUri: Uri): ApkInfo? {
        val tempApkFileName = generateUniqueFileName("release", "apk")
        val tempApkPath = uriToTempPath(context, apkUri, tempApkFileName) ?: return null
        val archivePath = tempApkPath.toString()

        try {
            val pm = context.packageManager
            val flags = PackageManager.GET_META_DATA
            val packageInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(archivePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(archivePath, flags)
            }
            
            val appInfo = packageInfo?.applicationInfo
            if (appInfo == null) {
                fileSystem.delete(tempApkPath)
                return null
            }

            appInfo.sourceDir = archivePath
            appInfo.publicSourceDir = archivePath

            val appName = appInfo.loadLabel(pm).toString()
            val packageName = packageInfo.packageName
            val versionName = packageInfo.versionName ?: "N/A"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            val minSdkVersion = packageInfo.applicationInfo?.minSdkVersion ?: 0
            val targetSdkVersion = packageInfo.applicationInfo?.targetSdkVersion ?: 0
            
            val iconDrawable = appInfo.loadIcon(pm)
            val tempIconFileName = generateUniqueFileName("icon", "png")
            val tempIconPath = drawableToTempPath(context, iconDrawable, tempIconFileName)
            
            // 巧妙避开 java.io.File，直接通过字符串构建 file:// 协议的 Uri
            val tempIconFileUri = tempIconPath?.let { Uri.parse("file://$it") }

            // 使用 Okio 的 FileSystem 获取文件大小
            val sizeInBytes = fileSystem.metadata(tempApkPath).size ?: 0L
            val sizeInMb = (sizeInBytes / 1024.0 / 1024.0 * 100).roundToInt() / 100.0

            return ApkInfo(
                appName = appName,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                minSdkVersion = minSdkVersion,     
                targetSdkVersion = targetSdkVersion,
                sizeInMb = sizeInMb,
                tempApkPath = tempApkPath,
                tempIconPath = tempIconPath,
                tempIconFileUri = tempIconFileUri
            )
        } catch (e: Exception) {
            e.printStackTrace()
            fileSystem.delete(tempApkPath)
            return null
        }
    }
    
    private fun uriToTempPath(context: Context, uri: Uri, fileName: String): Path? {
        return try {
            // 利用 Kotlin 类型推断隐藏 InputStream 声明，直接调用 Okio 的 source()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempPath = context.cacheDir.absolutePath.toPath() / fileName
            
            fileSystem.write(tempPath) {
                writeAll(inputStream.source().buffer())
            }
            tempPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawableToTempPath(context: Context, drawable: Drawable?, fileName: String): Path? {
        if (drawable == null) return null
        return try {
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bmp = Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }

            val tempPath = context.cacheDir.absolutePath.toPath() / fileName
            
            fileSystem.write(tempPath) {
                // 直接从 Okio 的 BufferedSink 中获取 outputStream 给 Bitmap 压缩使用
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream())
            }
            tempPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}