package me.voltual.pyrolysis.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.data.DeviceConfig
import me.voltual.pyrolysis.data.DeviceNameDataStore
import me.voltual.pyrolysis.data.unified.UnifiedUserDetail
import me.voltual.pyrolysis.data.unified.UpdateUserProfileParams
import me.voltual.pyrolysis.feature.store.repository.IAppStoreRepository
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class UserProfileViewModel(
    private val repositories: Map<AppStore, IAppStoreRepository>,
    private val deviceNameDataStore: DeviceNameDataStore
) : ViewModel() {

    // ... (保持其它不变) ...
    data class UserProfileUiState(
        val isLoading: Boolean = false,
        val userDetail: UnifiedUserDetail? = null,
        val currentDevice: DeviceConfig = DeviceConfig(),
        val isUploading: Boolean = false,
        val allDevices: List<DeviceConfig> = emptyList(),
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                deviceNameDataStore.deviceListFlow,
                deviceNameDataStore.currentConfigFlow
            ) { list, current ->
                _uiState.update { it.copy(allDevices = list, currentDevice = current) }
            }.collect()
        }
    }

    fun loadUserProfile(store: AppStore) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val repository = repositories[store] ?: return@launch
            val result = repository.getCurrentUserDetail()
            _uiState.update { it.copy(isLoading = false, userDetail = result.getOrNull()) }
        }
    }

    fun switchDevice(config: DeviceConfig) {
        viewModelScope.launch {
            deviceNameDataStore.updateDeviceList(
                uiState.value.allDevices.map { it.copy(isSelected = it == config) }
            )
        }
    }

    fun importDeviceConfig(jsonStr: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val count = deviceNameDataStore.importConfigsFromJson(jsonStr)
            onResult(count > 0, count)
        }
    }

    fun updateProfile(
        store: AppStore, 
        params: UpdateUserProfileParams, 
        currentConfig: DeviceConfig, 
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updatedList = uiState.value.allDevices.map {
                if (it.isSelected) currentConfig.copy(isSelected = true) else it
            }
            deviceNameDataStore.updateDeviceList(updatedList)
            
            val result = repositories[store]?.updateUserProfile(params)
            onResult(result?.isSuccess == true, if (result?.isSuccess == true) "已同步云端并保存本地" else "本地已保存，云端失败")
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun uploadAvatar(store: AppStore, file: PlatformFile, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            try {
                val repository = repositories[store] ?: return@launch onResult(false, "不支持的平台")
                val bytes = file.readBytes()
                val result = repository.uploadAvatar(bytes, file.name)
                if (result.isSuccess) {
                    loadUserProfile(store)
                    onResult(true, "头像上传成功")
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "上传失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "文件处理失败")
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }
}