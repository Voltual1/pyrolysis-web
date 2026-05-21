//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import me.voltual.pyrolysis.core.database.*
import me.voltual.pyrolysis.core.database.dao.*
import me.voltual.pyrolysis.core.proto.UserCredentials
import me.voltual.pyrolysis.core.proto.UserCredentialsSerializer
import me.voltual.pyrolysis.data.*
import me.voltual.pyrolysis.feature.store.repository.*
import me.voltual.pyrolysis.feature.store.worker.workmanagerModule
import me.voltual.pyrolysis.ui.auth.LoginViewModel
import me.voltual.pyrolysis.ui.billing.BillingViewModel
import me.voltual.pyrolysis.ui.community.*
import me.voltual.pyrolysis.ui.home.HomeViewModel
import me.voltual.pyrolysis.ui.log.LogViewModel
import me.voltual.pyrolysis.ui.message.MessageViewModel
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.ui.plaza.*
import me.voltual.pyrolysis.ui.player.PlayerViewModel
import me.voltual.pyrolysis.ui.rank.RankingListViewModel
import me.voltual.pyrolysis.ui.search.SearchViewModel
import me.voltual.pyrolysis.ui.settings.PrefsVM
import me.voltual.pyrolysis.ui.settings.repos.RepoPageVM
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsViewModel
import me.voltual.pyrolysis.ui.settings.storage.StoreManagerViewModel
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsViewModel
import me.voltual.pyrolysis.ui.user.*
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// 常量定义，集中管理命名空间限定符
private val AUTH_STORE_QUALIFIER = named("auth_store")
private val DRAFT_STORE_QUALIFIER = named("draft_store")
private val PAYMENT_STORE_QUALIFIER = named("payment_store")
private val PLAZA_STORE_QUALIFIER = named("plaza_store")

val appModule = module {

    // =========================================================================
    // 1. 核心基础设施 & 数据库 (Core Infrastructure & Database)
    // =========================================================================
    
    single { BBQApplication.instance.database }
    single { get<AppDatabase>().logDao() }  
    single { get<AppDatabase>().browseHistoryDao() } 
    single { get<AppDatabase>().networkCacheDao() }  
    single { get<AppDatabase>().postDraftDao() }         

    // Crypto (Tink 安全加密)
    single<Aead> {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(androidContext(), "master_keyset", "tink_auth_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://auth_master_key")
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    // =========================================================================
    // 2. 数据存储层 (DataStores)
    // =========================================================================
    
    single { DeviceNameDataStore(androidContext()) }
    single { UserFilterDataStore(get()) }    
    single { UserAgreementDataStore(androidContext()) }    
    single { SearchHistoryDataStore(androidApplication()) }
    single { StorageSettingsDataStore(androidApplication()) }

    // Proto DataStore (用户凭据加密存储)
    single<DataStore<UserCredentials>>(AUTH_STORE_QUALIFIER) {
        DataStoreFactory.create(
            serializer = UserCredentialsSerializer(get()),
            produceFile = { androidContext().dataStoreFile("user_credentials_v2.pb") }
        )
    }

    // Preferences DataStores
    single<DataStore<Preferences>>(DRAFT_STORE_QUALIFIER) {
        PreferenceDataStoreFactory.create(
            produceFile = { androidContext().dataStoreFile("post_drafts.preferences_pb") }
        )
    }

    single<DataStore<Preferences>>(PAYMENT_STORE_QUALIFIER) {
        PreferenceDataStoreFactory.create(
            produceFile = { androidContext().dataStoreFile("payment_requests.preferences_pb") }
        )
    }

    single<DataStore<Preferences>>(PLAZA_STORE_QUALIFIER) {
        PreferenceDataStoreFactory.create(
            produceFile = { androidContext().dataStoreFile("plaza_preferences.preferences_pb") }
        )
    }
    
    single { PostDraftDataStore(get(DRAFT_STORE_QUALIFIER)) } 

    // =========================================================================
    // 3. 业务仓库层 (Repositories)
    // =========================================================================
    
    single { AuthRepository(get(AUTH_STORE_QUALIFIER)) }
    single { PostDraftRepository() }
    single { InstallsRepository(get()) }  
    single { ExtrasRepository(get()) }    
    single { DownloadedRepository(get()) }    
    single { InstalledRepository(get(), get()) }
    single { RepositoriesRepository(get(), get(), get()) }
    single { ProductsRepository(get(), get(), get(), get()) }
    single { PrivacyRepository(get(), get(), get(), get(), get()) }
    single { XiaoQuRepository(KtorClient.ApiServiceImpl, get()) }
    
    // 多渠道应用商店映射桥接
    single<Map<AppStore, IAppStoreRepository>> {
        mutableMapOf<AppStore, IAppStoreRepository>().apply {
            put(AppStore.XIAOQU_SPACE, get<XiaoQuRepository>())
        }
    }

    // =========================================================================
    // 4. 表现层 (ViewModels)
    // =========================================================================
    
    // 用户与认证相关
    viewModel { LoginViewModel(get()) } 
    viewModel { UserProfileViewModel(get(), get()) }
    viewModel { UserListViewModel(get()) }
    viewModel { UserDetailViewModel(get()) }
    viewModel { SignInSettingsViewModel(get()) }

    // 社区交流相关
    viewModel { CommunityViewModel() }
    viewModel { FollowingPostsViewModel(get()) } 
    viewModel { HotPostsViewModel() }
    viewModel { MyLikesViewModel(get()) } 
    viewModel { MyPostsViewModel(get()) }
    viewModel { MyCommentsViewModel(androidApplication(), get()) }
    viewModel { MyReviewsViewModel(androidApplication(), get()) }
    viewModel { PostCreateViewModel(get(), get(), get()) }
    viewModel { PostDetailViewModel(get()) } 
    viewModel { BrowseHistoryViewModel(androidApplication()) }

    // 应用商店、广场与搜索
    viewModel { HomeViewModel(get()) }
    viewModel { PlazaViewModel(get(PLAZA_STORE_QUALIFIER), get(), get(), get(), get()) }
    viewModel { AppDetailComposeViewModel(androidApplication(), get()) }
    viewModel { AppReleaseViewModel(androidApplication(), get()) }
    viewModel { VersionListViewModel(androidApplication(), get()) }
    viewModel { AppPageVM(get(), get(), get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { SearchVM(get(), get(), get()) }    
    viewModel { ExploreVM(get(), get(), get()) }
    viewModel { RepoPageVM(get(), get()) }
    viewModel { RankingListViewModel() }

    // 系统工具、账单及设置
    viewModel { BillingViewModel(get()) }
    viewModel { PaymentViewModel(get(), get(PAYMENT_STORE_QUALIFIER)) }
    viewModel { MessageViewModel(get()) }
    viewModel { PlayerViewModel(androidApplication()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { StoreManagerViewModel(androidApplication()) }
    viewModel { UpdateSettingsViewModel() }
    viewModel { PrefsVM(get(), get(), get()) }
}