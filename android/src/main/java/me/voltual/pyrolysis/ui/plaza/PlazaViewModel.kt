//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.plaza

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import me.voltual.pyrolysis.core.database.entity.ProductIconDetails
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.data.unified.UnifiedAppItem
import me.voltual.pyrolysis.data.unified.UnifiedCategory
import me.voltual.pyrolysis.core.database.entity.CategoryDetails
import me.voltual.pyrolysis.data.entity.*
import me.voltual.pyrolysis.core.database.entity.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.feature.store.repository.*
import kotlinx.coroutines.withContext

// --- 统一的数据模型包装 ---
data class PlazaData(val popularApps: List<UnifiedAppItem>)

class PlazaViewModel(
    private val dataStore: DataStore<Preferences>, // 注入 DataStore
    private val productsRepo: ProductsRepository, 
    private val reposRepo: RepositoriesRepository,
    private val extrasRepo: ExtrasRepository,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : ViewModel() { // 变为普通 ViewModel

    // ---  State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _appStore = MutableStateFlow(AppStore.XIAOQU_SPACE)
    val appStore: StateFlow<AppStore> = _appStore.asStateFlow()

    private val _categories = MutableStateFlow<List<UnifiedCategory>>(emptyList())
    val categories: StateFlow<List<UnifiedCategory>> = _categories.asStateFlow()

    private val _plazaData = MutableStateFlow(PlazaData(emptyList()))
    val plazaData: StateFlow<PlazaData> = _plazaData.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UnifiedAppItem>>(emptyList())
    val searchResults: StateFlow<List<UnifiedAppItem>> = _searchResults.asStateFlow()
    
    private var _currentStoreNameState: String = AppStore.XIAOQU_SPACE.name

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _autoScrollMode = MutableStateFlow<Boolean>(false)
    val autoScrollMode: StateFlow<Boolean> = _autoScrollMode.asStateFlow()

    private val _currentCategoryId = MutableStateFlow<String?>(null)
    val currentCategoryId: StateFlow<String?> = _currentCategoryId.asStateFlow()

    // --- 内部状态管理 ---
    private var isSearchMode = false
    private var currentQuery = ""
    private var currentUserId: String? = null
    private var isMyResourceMode: Boolean = false
    private var currentMode: String = "public"

    private var _isInitialized = false
    private var _currentIsMyResourceMode: Boolean = false
    private var _currentUserIdState: String? = null
    private var _currentModeState: String = ""

    private var savedCurrentPage: Int = 1
    private var savedCurrentQuery: String = ""

    private val currentRepository: IAppStoreRepository
        get() = repositories[_appStore.value] ?: throw IllegalStateException("No repository found for the selected app store")

    private val AUTO_SCROLL_MODE_KEY = booleanPreferencesKey("auto_scroll_mode")

    init {
        viewModelScope.launch {
            readAutoScrollMode().collect {
                _autoScrollMode.value = it
            }
        }
    }
    
    val sortFilterState: StateFlow<SortFilterState> = combine(
        reposRepo.getAllEnabled(),
        combine(
            productsRepo.getAllCategories(),
            productsRepo.getAllCategoryDetails(),
        ) { cats, catDetails ->
            cats.map { cat ->
                catDetails.find { it.name == cat }
                    ?: CategoryDetails(cat, cat)
            }
        }.distinctUntilChanged(),
        reposRepo.getRepoAntiFeaturePairs().distinctUntilChanged(),
        productsRepo.getAllLicensesDistinct().distinctUntilChanged(),
    ) { enabledRepos, categories, antifeaturePairs, licenses ->
        SortFilterState(
            enabledRepos = enabledRepos,
            categories = categories,
            antifeaturePairs = antifeaturePairs,
            licenses = licenses,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = SortFilterState(),
    )
    
    val dataState: StateFlow<DataState> = combine(
        reposRepo.getAllMap(),
        extrasRepo.getAllFavorites().distinctUntilChanged(),
        productsRepo.getIconDetailsMap().distinctUntilChanged(),
    ) { reposMap, favorites, iconDetails ->
        DataState(
            reposMap = reposMap,
            favorites = favorites,
            iconDetails = iconDetails,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DataState(),
    )
    
    fun setFavorite(packageName: String, setBoolean: Boolean) {
        viewModelScope.launch {
            extrasRepo.setFavorite(packageName, setBoolean)
        }
    }
    
    data class DataState(
        val reposMap: Map<Long, Repository> = emptyMap(),
        val favorites: List<String> = emptyList(),
        val iconDetails: Map<String, ProductIconDetails> = emptyMap(),
    )

    fun initialize(isMyResource: Boolean, userId: String?, mode: String = "public", storeName: String = AppStore.XIAOQU_SPACE.name) {
        val needsReinit = _currentIsMyResourceMode != isMyResource ||
                          _currentUserIdState != userId ||
                          _currentModeState != mode ||
                          _currentStoreNameState != storeName
        
        if (needsReinit) {
            _currentIsMyResourceMode = isMyResource
            _currentUserIdState = userId
            _currentModeState = mode
            _currentStoreNameState = storeName
            _isInitialized = false
            
            this.isMyResourceMode = isMyResource
            this.currentUserId = userId
            this.currentMode = mode
            
            val targetStore = try {
                AppStore.valueOf(storeName)
            } catch (e: Exception) {
                AppStore.XIAOQU_SPACE
            }
            if (_appStore.value != targetStore) {
                _appStore.value = targetStore
            }
            
            resetStateAndLoadCategories()
        } else {
            loadDataIfNeeded()
        }
    }

    fun setAppStore(store: AppStore) {
        if (_appStore.value == store && _categories.value.isNotEmpty()) return
        _appStore.value = store
        resetStateAndLoadCategories()
    }

    fun loadCategory(categoryId: String?) {
        if (isSearchMode) {
            cancelSearch()
        }
        if (_currentCategoryId.value == categoryId) return

        _currentCategoryId.value = categoryId
        loadPage(1)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        isSearchMode = true
        currentQuery = query
        savedCurrentQuery = query
        _plazaData.value = PlazaData(emptyList())
        _searchResults.value = emptyList()
        loadPage(1)
    }

    fun cancelSearch() {
        isSearchMode = false
        currentQuery = ""
        savedCurrentQuery = ""
        _searchResults.value = emptyList()
        loadPage(1)
    }

    fun nextPage() {
        val next = _currentPage.value + 1
        loadPage(next, append = autoScrollMode.value)
    }

    fun prevPage() {
        val prev = _currentPage.value - 1
        loadPage(prev)
    }

    fun goToPage(page: Int) {
        savedCurrentPage = page
        loadPage(page)
    }

    fun loadMore() {
        if (autoScrollMode.value && !_isLoading.value) {
            val current = _currentPage.value
            val total = _totalPages.value
            if (current < total) {
                loadPage(current + 1, append = true)
            }
        }
    }

    private fun loadDataIfNeeded() {
        if (!_isInitialized) {
            resetStateAndLoadCategories()
        }
    }

    private fun resetStateAndLoadCategories() {
        _isLoading.value = true
        _currentCategoryId.value = null 
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when {
                    _appStore.value == AppStore.LOCAL -> {
                        when (currentMode) {
                            "my_upload" -> _currentCategoryId.value = "-3"
                            "my_favourite" -> _currentCategoryId.value = "-4"
                            "my_history" -> _currentCategoryId.value = "-5"
                            else -> _currentCategoryId.value = null
                        }
                    }              
                    else -> _currentCategoryId.value = null
                }
                
                if (currentMode in listOf("my_upload", "my_favourite", "my_history")) {
                    val supportedModes = when (_appStore.value) {
                        AppStore.LOCAL -> listOf("my_upload", "my_favourite", "my_history")
                        else -> emptyList()
                    }
                    
                    if (currentMode !in supportedModes) {
                        _categories.value = emptyList()
                        _isLoading.value = false
                        _errorMessage.value = "当前商店不支持${currentMode}功能"
                        return@launch
                    }
                    
                    _categories.value = emptyList()
                    _isLoading.value = false
                    withContext(Dispatchers.Main) {
                        loadPage(1, append = false)
                    }
                } else {
                    val categoriesResult = currentRepository.getCategories()
                    if (categoriesResult.isSuccess) {
                        val categoryList = categoriesResult.getOrThrow()
                        _categories.value = categoryList
                        
                        if (_currentCategoryId.value == null) {
                            _currentCategoryId.value = categoryList.firstOrNull()?.id 
                        }
                        
                        _isLoading.value = false
                        withContext(Dispatchers.Main) {
                            loadPage(1, append = false)
                        }
                    } else {
                        handleFailure(categoriesResult.exceptionOrNull())
                        _categories.value = emptyList()
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                handleFailure(e)
                _isLoading.value = false
            }
        }
    }

    private fun loadPage(page: Int, append: Boolean = false) {
        if (_isLoading.value && !append) return
        
        val total = _totalPages.value
        if (page < 1 || (page > total && total > 0 && !append)) return

        _isLoading.value = true 
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalUserId = currentUserId

                val result = if (isSearchMode) {
                    currentRepository.searchApps(currentQuery, page, finalUserId)
                } else {
                    currentRepository.getApps(_currentCategoryId.value, page, finalUserId) 
                }

                if (result.isSuccess) {
                    val (items, totalPages) = result.getOrThrow()
                    _totalPages.value = if(totalPages > 0) totalPages else 1
                    _currentPage.value = page
                    savedCurrentPage = page

                    if (isSearchMode) {
                        val currentList = if (append) _searchResults.value else emptyList()
                        _searchResults.value = currentList + items
                    } else {
                        val currentList = if (append) _plazaData.value.popularApps else emptyList()
                        _plazaData.value = PlazaData(currentList + items)
                    }
                    
                    if (!_isInitialized && page == 1 && !append) {
                         _isInitialized = true
                    }
                } else {
                    handleFailure(result.exceptionOrNull())
                }
            } catch (e: Exception) {
                handleFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleFailure(exception: Throwable?) {
        val message = "操作失败: ${exception?.message ?: "未知错误"}"
        _errorMessage.value = message
    }

    private fun readAutoScrollMode(): Flow<Boolean> {
        return dataStore.data
            .map { preferences ->
                preferences[AUTO_SCROLL_MODE_KEY] ?: false
            }
            .catch { emit(false) }
    }

    fun setAutoScrollMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[AUTO_SCROLL_MODE_KEY] = enabled
            }
            _autoScrollMode.value = enabled
        }
    }
}

/**
 * UI 层的过滤状态模型
 */
data class SortFilterState(
    val enabledRepos: List<Repository> = emptyList(),
    val categories: List<CategoryDetails> = emptyList(),
    val antifeaturePairs: List<Pair<String, String>> = emptyList(),
    val licenses: List<String> = emptyList()
)