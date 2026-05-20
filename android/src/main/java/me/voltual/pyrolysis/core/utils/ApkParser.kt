//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.core.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlin.time.Clock
import kotlin.time.Clock.System
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
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
    val tempIconFile: PlatformFile? // 替换 Uri 为 PlatformFile
)

object ApkParser {

    private val fileSystem = FileSystem.SYSTEM

    private fun generateUniqueFileName(prefix: String, extension: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = (100..999).random()
        return "${prefix}_${timestamp}_${randomSuffix}.$extension"
    }

    /**
     * 解析 APK 文件
     * @param file FileKit 的 PlatformFile 对象
     */
    fun parse(context: Context, file: PlatformFile): ApkInfo? {
        val tempApkFileName = generateUniqueFileName("release", "apk")
        // 将 PlatformFile 写入临时路径，因为 getPackageArchiveInfo 需要物理路径
        val tempApkPath = fileToTempPath(context, file, tempApkFileName) ?: return null
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
            
            // 将临时图标路径包装为 PlatformFile
            val tempIconFile = tempIconPath?.let { PlatformFile(it.toFile()) }

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
                tempIconFile = tempIconFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            fileSystem.delete(tempApkPath)
            return null
        }
    }
    
    /**
     * 将 PlatformFile 写入缓存目录
     */
    private fun fileToTempPath(context: Context, file: PlatformFile, fileName: String): Path? {
        return try {
            val bytes = file.readBytes()
            val tempPath = context.cacheDir.absolutePath.toPath() / fileName
            
            fileSystem.write(tempPath) {
                write(bytes)
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
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream())
            }
            tempPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}