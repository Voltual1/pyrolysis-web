// FILE: me.voltual.pyrolysis.ui.plaza/AppReleaseViewModel.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(kotlin.time.ExperimentalTime::class)
package me.voltual.pyrolysis.ui.plaza

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.size
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.feature.store.repository.IAppStoreRepository
import me.voltual.pyrolysis.feature.store.repository.XiaoQuRepository
import me.voltual.pyrolysis.data.unified.UnifiedAppReleaseParams
import me.voltual.pyrolysis.utils.createStreamingApkParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.math.roundToInt

class AppReleaseViewModel(
    private val xiaoQuRepo: XiaoQuRepository
) : ViewModel() {

    private val _selectedStore = MutableStateFlow(AppStore.XIAOQU_SPACE)
    val selectedStore = _selectedStore.asStateFlow()

    fun onStoreSelected(store: AppStore) {
        _selectedStore.value = store
        clearProcessFeedback()
    }

    private fun getCurrentRepo(): IAppStoreRepository = xiaoQuRepo

    // --- 通用状态 ---
    val appName = mutableStateOf("")
    val packageName = mutableStateOf("")
    val versionName = mutableStateOf("")
    val versionCode = mutableStateOf(0L)
    val appSize = mutableStateOf("")
    val localIconFile = mutableStateOf<PlatformFile?>(null)

    // --- 小趣空间发布状态 ---
    val isUpdateMode = mutableStateOf(false)
    private var appId: Long = 0
    private var appVersionId: Long = 0
    val selectedApkUploadService = mutableStateOf(ApkUploadService.LANZOU_CLOUD)
    val appIntroduce = mutableStateOf("资源介绍【密码:】 ")
    val appExplain = mutableStateOf("适配性能描述 •\n包名：\n版本：")
    val isPay = mutableStateOf(0)
    val payMoney = mutableStateOf("")
    val selectedCategoryIndex = mutableStateOf(0)
    val apkDownloadUrl = mutableStateOf("")
    val iconUrl = mutableStateOf<String?>(null)
    val introductionImageUrls = mutableStateListOf<String>()

    val categories = listOf(
        AppCategory(45, 47, "影音阅读"), AppCategory(45, 55, "音乐听歌"),
        AppCategory(45, 61, "休闲娱乐"), AppCategory(45, 58, "文件管理"),
        AppCategory(45, 59, "图像摄影"), AppCategory(45, 53, "输入方式"),
        AppCategory(45, 54, "生活出行"), AppCategory(45, 50, "社交通讯"),
        AppCategory(45, 56, "上网浏览"), AppCategory(45, 60, "其他类型"),
        AppCategory(45, 62, "跑酷竞技"), AppCategory(45, 49, "系统工具"),
        AppCategory(45, 48, "桌面插件"), AppCategory(45, 65, "学习教育")
    ).filter { it.categoryId != null && it.subCategoryId != null }
     .distinctBy { it.subCategoryId }
     .sortedBy { it.categoryName }

    val isApkUploading = mutableStateOf(false)
    val isIconUploading = mutableStateOf(false)
    val isIntroImagesUploading = mutableStateOf(false)
    val isReleasing = mutableStateOf(false)
    private val _processFeedback = MutableStateFlow<Result<String>?>(null)
    val processFeedback = _processFeedback.asStateFlow()

    private val MAX_INTRO_IMAGES_XIAOQU = 3

    /**
     * 处理选择的 APK 文件：
     * 1. 若选择「蓝云托管」：直接流式上传 APK 至蓝云，复用后端解析的元数据与 icon_url，避免本地重复解析和重复上传图标。
     * 2. 若选择「外部直链」：本地流式解析 APK 元数据并提取图标上传至小趣图床，APK 链接由用户手动填写。
     */
    fun parseAndUploadApk(file: PlatformFile) {
        viewModelScope.launch(Dispatchers.Default) {
            when (selectedApkUploadService.value) {
                ApkUploadService.LANZOU_CLOUD -> uploadApkAndUseServerMetadata(file)
                ApkUploadService.EXTERNAL_LINK -> parseApkLocallyAndUploadIcon(file)
            }
        }
    }

    private suspend fun uploadApkAndUseServerMetadata(file: PlatformFile) {
        isApkUploading.value = true
        _processFeedback.value = Result.success("正在流式上传 APK 至蓝云并解析信息...")

        val rawSize = file.size()
        val mb = if (rawSize > 0) rawSize / 1024.0 / 1024.0 else 0.0
        val formattedSize = ((mb * 100).roundToInt() / 100.0).toString()

        val result = xiaoQuRepo.uploadApkToLanzou(file)
        result.onSuccess { uploadData ->
            val serverApk = uploadData.apk
            withContext(Dispatchers.Main) {
                apkDownloadUrl.value = uploadData.share_url
                appSize.value = formattedSize
                localIconFile.value = file

                if (uploadData.password.isNotBlank()) {
                    val currentIntro = appIntroduce.value
                    appIntroduce.value = if (currentIntro.contains("【密码:】")) {
                        currentIntro.replace("【密码:】", "【密码:${uploadData.password}】")
                    } else if (!currentIntro.contains(uploadData.password)) {
                        "$currentIntro【密码:${uploadData.password}】"
                    } else {
                        currentIntro
                    }
                }

                if (serverApk != null) {
                    appName.value = serverApk.app_name ?: "未知应用"
                    packageName.value = serverApk.package_name ?: ""
                    versionName.value = serverApk.version_name ?: "N/A"
                    versionCode.value = serverApk.version_code?.toLongOrNull() ?: 0L
                    if (!serverApk.icon_url.isNullOrBlank()) {
                        iconUrl.value = serverApk.icon_url
                    }
                    appExplain.value = "适配性能描述 •\n包名：${serverApk.package_name.orEmpty()}\n版本：${serverApk.version_name.orEmpty()}"
                }
                _processFeedback.value = Result.success("蓝云上传并解析成功！")
            }
        }.onFailure { e ->
            withContext(Dispatchers.Main) {
                _processFeedback.value = Result.failure(e)
            }
        }

        isApkUploading.value = false
    }

    private suspend fun parseApkLocallyAndUploadIcon(file: PlatformFile) {
    _processFeedback.value = Result.success("正在解析 APK 元数据...")

    val parsed = try {
        extractApkMetadata(file)
    } catch (e: Exception) {
        _processFeedback.value = Result.failure(Exception("APK 解析失败: ${e.message}"))
        return
    }

    val rawSize = file.size()
    val mb = if (rawSize > 0) rawSize / 1024.0 / 1024.0 else 0.0
    val formattedSize = ((mb * 100).roundToInt() / 100.0).toString()

    withContext(Dispatchers.Main) {
        appName.value = parsed.label ?: "未知应用"
        packageName.value = parsed.packageName ?: ""
        versionName.value = parsed.versionName ?: "N/A"
        versionCode.value = parsed.versionCode ?: 0L
        appSize.value = formattedSize
        localIconFile.value = file
        appExplain.value = "适配性能描述 •\n包名：${parsed.packageName.orEmpty()}\n版本：${parsed.versionName.orEmpty()}"
    }

    if (parsed.iconBytes != null) {
        isIconUploading.value = true
        val now = Clock.System.now().toEpochMilliseconds()
        val iconFileName = "icon_${now}.png"
        val imageResult = xiaoQuRepo.uploadImage(parsed.iconBytes, iconFileName, "icon")
        imageResult.onSuccess { url ->
            withContext(Dispatchers.Main) {
                iconUrl.value = url
                _processFeedback.value = Result.success("本地解析与图标上传完成，请填写外部 APK 链接")
            }
        }.onFailure { e ->
            withContext(Dispatchers.Main) {
                _processFeedback.value = Result.failure(e)
            }
        }
        isIconUploading.value = false
    } else {
        withContext(Dispatchers.Main) {
            _processFeedback.value = Result.success("本地解析完成，请填写外部 APK 链接")
        }
    }
}

    fun uploadIntroductionImages(files: List<PlatformFile>) {
        if (_selectedStore.value != AppStore.XIAOQU_SPACE) return

        viewModelScope.launch(Dispatchers.Default) {
            val currentCount = introductionImageUrls.size
            if (currentCount >= MAX_INTRO_IMAGES_XIAOQU) {
                _processFeedback.value = Result.failure(Exception("最多只能上传 $MAX_INTRO_IMAGES_XIAOQU 张介绍图"))
                return@launch
            }

            isIntroImagesUploading.value = true
            val remainingSlots = MAX_INTRO_IMAGES_XIAOQU - currentCount
            val filesToUpload = files.take(remainingSlots)

            val uploadJobs = filesToUpload.map { file ->
                launch {
                    val imageResult = xiaoQuRepo.uploadImage(file, "intro")
                    imageResult.onSuccess { url ->
                        withContext(Dispatchers.Main) {
                            if (introductionImageUrls.size < MAX_INTRO_IMAGES_XIAOQU) {
                                introductionImageUrls.add(url)
                            }
                        }
                    }.onFailure { e ->
                        withContext(Dispatchers.Main) { _processFeedback.value = Result.failure(e) }
                    }
                }
            }
            uploadJobs.joinAll()
            isIntroImagesUploading.value = false
        }
    }

    fun clearProcessFeedback() { _processFeedback.value = null }
    fun removeIntroductionImage(url: String) { introductionImageUrls.remove(url) }

    fun releaseApp(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            isReleasing.value = true
            _processFeedback.value = Result.success("正在准备发布...")

            try {
                val params = UnifiedAppReleaseParams(
                    store = _selectedStore.value,
                    appName = appName.value,
                    packageName = packageName.value,
                    versionName = versionName.value,
                    versionCode = versionCode.value,
                    sizeInMb = appSize.value.toDoubleOrNull() ?: 0.0,
                    iconPath = null,
                    iconUrl = iconUrl.value,
                    apkPath = null,
                    apkUrl = apkDownloadUrl.value,
                    introduce = appIntroduce.value,
                    explain = appExplain.value,
                    introImages = introductionImageUrls.toList(),
                    categoryId = categories.getOrNull(selectedCategoryIndex.value)?.categoryId,
                    subCategoryId = categories.getOrNull(selectedCategoryIndex.value)?.subCategoryId,
                    isPay = isPay.value,
                    payMoney = if (isPay.value == 1) payMoney.value else "",
                    isUpdate = isUpdateMode.value,
                    appId = appId,
                    appVersionId = appVersionId
                )

                getCurrentRepo().releaseApp(params).onSuccess {
                    _processFeedback.value = Result.success("发布成功！")
                    withContext(Dispatchers.Main) { onSuccess() }
                }.onFailure { e ->
                    _processFeedback.value = Result.failure(e)
                }
            } catch (e: Exception) {
                _processFeedback.value = Result.failure(e)
            } finally {
                isReleasing.value = false
            }
        }
    }

    fun populateFromAppDetail(appDetail: KtorClient.AppDetail) {
        if (_selectedStore.value != AppStore.XIAOQU_SPACE) return
        isUpdateMode.value = true
        appId = appDetail.id
        appVersionId = appDetail.apps_version_id
        appName.value = appDetail.appname
        apkDownloadUrl.value = appDetail.download ?: ""
        packageName.value = appDetail.app_explain?.split("\n")?.find { it.startsWith("包名：") }?.substringAfter("包名：")?.trim() ?: ""
        versionName.value = appDetail.version
        versionCode.value = appDetail.apps_version_id
        appSize.value = appDetail.app_size.replace("MB", "").trim()
        appIntroduce.value = appDetail.app_introduce?.replace("<br>", "\n") ?: ""
        appExplain.value = appDetail.app_explain ?: ""
        isPay.value = appDetail.is_pay
        payMoney.value = if (appDetail.pay_money > 0) appDetail.pay_money.toString() else ""
        selectedCategoryIndex.value = categories.indexOfFirst {
            it.categoryId == appDetail.category_id && it.subCategoryId == appDetail.sub_category_id
        }.takeIf { it != -1 } ?: 0
        iconUrl.value = appDetail.app_icon
        introductionImageUrls.clear()
        appDetail.app_introduction_image_array?.let { introductionImageUrls.addAll(it) }
    }
}