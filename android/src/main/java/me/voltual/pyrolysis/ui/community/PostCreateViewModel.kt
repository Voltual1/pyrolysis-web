package me.voltual.pyrolysis.ui.community

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.AuthManager
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.core.database.PostDraftRepository
import me.voltual.pyrolysis.data.PostDraftDataStore
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PostCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val draftRepository = PostDraftRepository()
    private val draftDataStore = PostDraftDataStore(application)

    private val _uiState = MutableStateFlow(PostCreateUiState())
    val uiState: StateFlow<PostCreateUiState> = _uiState.asStateFlow()

    private val _preferencesState = MutableStateFlow(DraftPreferencesState())
    val preferencesState: StateFlow<DraftPreferencesState> = _preferencesState.asStateFlow()

    private val _postStatus = MutableStateFlow<PostStatus>(PostStatus.Idle)
    val postStatus: StateFlow<PostStatus> = _postStatus.asStateFlow()

    private val _showRestoreDialog = MutableStateFlow(false)
    val showRestoreDialog: StateFlow<Boolean> = _showRestoreDialog.asStateFlow()

    private var _snackbarHostState: SnackbarHostState? = null

    fun setSnackbarHostState(snackbarHostState: SnackbarHostState) {
        _snackbarHostState = snackbarHostState
    }

    init {
        viewModelScope.launch {
            draftDataStore.preferencesFlow.first().let { preferences ->
                _preferencesState.value = DraftPreferencesState(
                    autoRestoreDraft = preferences.autoRestoreDraft,
                    noStoreDraft = preferences.noStoreDraft
                )
            }

            draftRepository.draftFlow.first()?.let { draft ->
                if (draft.title.isNotBlank() || draft.content.isNotBlank() || draft.imageUrls.isNotBlank()) {
                    if (_preferencesState.value.autoRestoreDraft) {
                        restoreDraft(draft)
                    } else {
                        _showRestoreDialog.value = true
                    }
                }
            }
        }
        observeAndAutoSave()
    }

    private fun restoreDraft(draft: PostDraftRepository.DraftDto) {
        _uiState.update {
            it.copy(
                title = draft.title,
                content = draft.content,
                imageUrls = draft.imageUrls,
                selectedSubsectionId = draft.subsectionId
            )
        }
    }

    fun onRestoreDialogConfirm() {
        viewModelScope.launch {
            draftRepository.draftFlow.first()?.let { restoreDraft(it) }
            _showRestoreDialog.value = false
        }
    }

    fun onRestoreDialogDismiss() {
        _showRestoreDialog.value = false
    }

    fun setAutoRestoreDraft(enabled: Boolean) {
        viewModelScope.launch {
            draftDataStore.setAutoRestoreDraft(enabled)
            _preferencesState.value = _preferencesState.value.copy(autoRestoreDraft = enabled)
        }
    }

    fun setNoStoreDraft(enabled: Boolean) {
        viewModelScope.launch {
            draftDataStore.setNoStoreDraft(enabled)
            _preferencesState.value = _preferencesState.value.copy(noStoreDraft = enabled)
            if (enabled) draftRepository.clearDraft()
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onContentChange(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun onSubsectionChange(newId: Int) {
        _uiState.update { it.copy(selectedSubsectionId = newId) }
    }

    fun uploadImage(file: PlatformFile) {
        if (_preferencesState.value.noStoreDraft) {
            showSnackbar("草稿存储已禁用")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(showProgressDialog = true, progressMessage = "正在上传图片...") }

            uploadImageKtor(file).onSuccess { imageUrl ->
                _uiState.update { currentState ->
                    // 修复：显式创建新的 Map 以避免类型推断错误
                    val newMap = currentState.imageFileToUrlMap.toMutableMap()
                    newMap[file] = imageUrl
                    currentState.copy(
                        imageUrls = newMap.values.joinToString(","),
                        imageFileToUrlMap = newMap,
                        showProgressDialog = false
                    )
                }
                showSnackbar("图片上传成功")
            }.onFailure { e ->
                showSnackbar("上传失败: ${e.message}")
                _uiState.update { it.copy(showProgressDialog = false) }
            }
        }
    }

    private suspend fun uploadImageKtor(file: PlatformFile): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileBytes = file.readBytes()
            val response: HttpResponse = KtorClient.uploadHttpClient.post("api.php") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            })
                        }
                    )
                )
            }

            val responseBody: KtorClient.UploadResponse = response.body()
            if (responseBody.code == 0 && !responseBody.downurl.isNullOrBlank()) {
                Result.success(responseBody.downurl)
            } else {
                Result.failure(Exception("上传失败: ${responseBody.msg}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeImage(file: PlatformFile) {
        _uiState.update { currentState ->
            val newMap = currentState.imageFileToUrlMap.toMutableMap()
            newMap.remove(file)
            currentState.copy(
                imageUrls = newMap.values.joinToString(","),
                imageFileToUrlMap = newMap
            )
        }
    }

    fun createPost(
        title: String,
        imageUrls: String,
        subsectionId: Int,
        bvNumber: String,
        tempDeviceName: String,
        mode: String,
        refundAppId: Long = 0L,
        refundVersionId: Long = 0L,
        refundPayMoney: Int = 0,
        selectedRefundReason: String = ""
    ) {
        viewModelScope.launch {
            _postStatus.value = PostStatus.Loading
            val credentials = AuthManager.getCredentials(getApplication()).first()
            if (credentials.userId == 0L || credentials.token.isEmpty()) {
                _postStatus.value = PostStatus.Error("请先登录")
                return@launch
            }

            val videoPart = if (bvNumber.isNotBlank()) "【视频：$bvNumber】" else ""
            val finalContent = if (mode == "refund") {
                "${_uiState.value.content}\n\n问题类型:$selectedRefundReason\n退还金额:$refundPayMoney\n機型：$tempDeviceName｜$videoPart"
            } else {
                "${_uiState.value.content} 機型：$tempDeviceName｜$videoPart"
            }

            KtorClient.ApiServiceImpl.createPost(
                token = credentials.token,
                title = title,
                content = finalContent,
                sectionId = if (mode == "refund") 21 else subsectionId,
                imageUrls = imageUrls.ifBlank { null }
            ).onSuccess {
                _postStatus.value = PostStatus.Success
                if (!_preferencesState.value.noStoreDraft) draftRepository.clearDraft()
            }.onFailure { e ->
                _postStatus.value = PostStatus.Error(e.message ?: "发帖失败")
            }
        }
    }

    fun resetPostStatus() { _postStatus.value = PostStatus.Idle }

    @OptIn(FlowPreview::class)
    private fun observeAndAutoSave() {
        _uiState.debounce(1000).onEach { state ->
            if (_preferencesState.value.noStoreDraft) return@onEach
            if (state.title.isNotBlank() || state.content.isNotBlank()) {
                draftRepository.saveDraft(PostDraftRepository.DraftDto(state.title, state.content, emptyList(), state.imageUrls, state.selectedSubsectionId))
            }
        }.launchIn(viewModelScope)
    }

    private fun showSnackbar(message: String) {
        viewModelScope.launch { _snackbarHostState?.showSnackbar(message) }
    }
}

data class PostCreateUiState(
    val title: String = "",
    val content: String = "",
    val selectedSubsectionId: Int = 11,
    val imageUrls: String = "",
    val imageFileToUrlMap: Map<PlatformFile, String> = emptyMap(),
    val showProgressDialog: Boolean = false,
    val progressMessage: String = ""
)

data class DraftPreferencesState(val autoRestoreDraft: Boolean = false, val noStoreDraft: Boolean = false)

sealed class PostStatus {
    data object Idle : PostStatus()
    data object Loading : PostStatus()
    data object Success : PostStatus()
    data class Error(val message: String) : PostStatus()
}