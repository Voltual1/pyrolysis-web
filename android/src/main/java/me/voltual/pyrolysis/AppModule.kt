//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import me.voltual.pyrolysis.core.database.*
import me.voltual.pyrolysis.core.database.dao.*
import me.voltual.pyrolysis.ui.auth.LoginViewModel
import me.voltual.pyrolysis.ui.billing.BillingViewModel
import org.koin.android.ext.koin.androidContext
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import me.voltual.pyrolysis.ui.community.CommunityViewModel
import me.voltual.pyrolysis.ui.community.FollowingPostsViewModel
import me.voltual.pyrolysis.ui.community.HotPostsViewModel
import me.voltual.pyrolysis.data.DeviceNameDataStore
import me.voltual.pyrolysis.ui.user.UserProfileViewModel
import me.voltual.pyrolysis.ui.community.MyLikesViewModel
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.ui.user.MyReviewsViewModel
import me.voltual.pyrolysis.feature.store.worker.workmanagerModule
import me.voltual.pyrolysis.ui.log.LogViewModel
import me.voltual.pyrolysis.ui.user.UserListViewModel
import me.voltual.pyrolysis.ui.settings.PrefsVM
import me.voltual.pyrolysis.ui.message.MessageViewModel
import me.voltual.pyrolysis.ui.community.PostCreateViewModel
import me.voltual.pyrolysis.ui.plaza.*
import me.voltual.pyrolysis.ui.player.PlayerViewModel
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsViewModel
import me.voltual.pyrolysis.ui.settings.repos.RepoPageVM
import me.voltual.pyrolysis.ui.search.SearchViewModel
import me.voltual.pyrolysis.ui.user.MyPostsViewModel
import me.voltual.pyrolysis.ui.user.UserDetailViewModel
import me.voltual.pyrolysis.ui.settings.storage.StoreManagerViewModel 
import me.voltual.pyrolysis.data.StorageSettingsDataStore 
import me.voltual.pyrolysis.data.SearchHistoryDataStore
import me.voltual.pyrolysis.data.PostDraftDataStore
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import me.voltual.pyrolysis.ui.community.BrowseHistoryViewModel
import me.voltual.pyrolysis.ui.community.PostDetailViewModel
import me.voltual.pyrolysis.ui.rank.RankingListViewModel
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsViewModel
import me.voltual.pyrolysis.ui.home.HomeViewModel
import me.voltual.pyrolysis.data.UserFilterDataStore
import me.voltual.pyrolysis.data.UserAgreementDataStore
import me.voltual.pyrolysis.ui.user.MyCommentsViewModel
import me.voltual.pyrolysis.feature.store.repository.*
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import me.voltual.pyrolysis.core.proto.UserCredentials
import me.voltual.pyrolysis.core.proto.UserCredentialsSerializer

val appModule = module {
    // ViewModel definitions
	viewModel { LoginViewModel(get()) } 
    viewModel { BillingViewModel(get()) }
    viewModel { CommunityViewModel() }
	viewModel { FollowingPostsViewModel(get()) } // 移除 androidApplication()，改为 get() 获取 AuthRepository
    viewModel { HotPostsViewModel() }
	viewModel { MyLikesViewModel(get()) } // 移除 androidApplication()，改为 get() 获取 AuthRepository
    viewModel { LogViewModel(androidApplication()) }
    viewModel { MessageViewModel(androidApplication()) }   
    viewModel { AppDetailComposeViewModel(androidApplication(), get()) }
    
    viewModel { AppReleaseViewModel(androidApplication()) }    
    
    viewModel { PlayerViewModel(androidApplication()) }
    
    viewModel { SearchViewModel(get(), get()) }
    
    viewModel { UserListViewModel(androidApplication()) }
    viewModel { PostCreateViewModel(get(), get(), get()) }
    viewModel { MyPostsViewModel(get()) }
    viewModel { VersionListViewModel(androidApplication(), get()) }
    viewModel { UserDetailViewModel(androidApplication()) }
    viewModel { StoreManagerViewModel(androidApplication()) }
    
    viewModel { BrowseHistoryViewModel(androidApplication()) }
	viewModel { PostDetailViewModel(get()) } 
    viewModel { RankingListViewModel() }
    viewModel { UpdateSettingsViewModel() }
	viewModel { SignInSettingsViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { MyCommentsViewModel(androidApplication(), get()) }
    viewModel { MyReviewsViewModel(androidApplication(), get()) }
    viewModel { RepoPageVM(get(), get()) }
    viewModel { PrefsVM(get(), get(), get()) }
    viewModel { SearchVM(get(), get(), get()) }    
    viewModel { ExploreVM(get(), get(), get()) }
    viewModel { AppPageVM(get(), get(), get(),get(),get(),get()) }

    // Singletons    
    single { UserFilterDataStore(get()) }    
    single { InstallsRepository(get()) }  
    single { 
        PostDraftDataStore(get(named("draft_store"))) 
    }
    single { ExtrasRepository(get()) }    
    single { DownloadedRepository(get()) }    
    single { ProductsRepository(get(), get(), get(), get()) }
    single { PrivacyRepository(get(), get(), get(),get(), get()) }
    single { InstalledRepository(get(), get()) }
    single { RepositoriesRepository(get(), get(), get()) }
    single { UserAgreementDataStore(androidContext()) }    
    single { BBQApplication.instance.database }
    single { PostDraftRepository() }
    single { get<AppDatabase>().logDao() }  
    single { get<AppDatabase>().browseHistoryDao() } 
    single { get<AppDatabase>().networkCacheDao() }  
    single { get<AppDatabase>().postDraftDao() }         
    single { SearchHistoryDataStore(androidApplication()) }
    single { StorageSettingsDataStore(androidApplication()) }
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
    single<DataStore<UserCredentials>>(named("auth_store")) {
        DataStoreFactory.create(
            serializer = UserCredentialsSerializer(get()),
            produceFile = { androidContext().dataStoreFile("user_credentials_v2.pb") }
        )
    }
    private val PAYMENT_STORE_QUALIFIER = named("payment_store")

// 提供 Payment DataStore
single<DataStore<Preferences>>(PAYMENT_STORE_QUALIFIER) {
    PreferenceDataStoreFactory.create(
        produceFile = { androidContext().dataStoreFile("payment_requests.preferences_pb") }
    )
}

// 更新 PaymentViewModel 的注入
viewModel { PaymentViewModel(get(), get(PAYMENT_STORE_QUALIFIER)) }
    single<DataStore<Preferences>>(named("draft_store")) {
    PreferenceDataStoreFactory.create(
        produceFile = { androidContext().dataStoreFile("post_drafts.preferences_pb") }
    )
}
    single { 
        AuthRepository(get(named("auth_store"))) 
    }
    
    // 定义 Plaza DataStore 的限定符
private val PLAZA_STORE_QUALIFIER = named("plaza_store")

// 提供 Plaza DataStore
single<DataStore<Preferences>>(PLAZA_STORE_QUALIFIER) {
    PreferenceDataStoreFactory.create(
        produceFile = { androidContext().dataStoreFile("plaza_preferences.preferences_pb") }
    )
}

// 更新 PlazaViewModel 的注入
viewModel { 
    PlazaViewModel(
        get(PLAZA_STORE_QUALIFIER), // 注入 DataStore
        get(),                      // ProductsRepository
        get(),                      // RepositoriesRepository
        get(),                      // ExtrasRepository
        get()                       // Map<AppStore, IAppStoreRepository>
    ) 
}
    viewModel { UserProfileViewModel(get(), get()) }
    
    single { DeviceNameDataStore(androidContext()) }
    single { XiaoQuRepository(KtorClient.ApiServiceImpl) }
    single<Map<AppStore, IAppStoreRepository>> {
    val map = mutableMapOf<AppStore, IAppStoreRepository>()
    map[AppStore.XIAOQU_SPACE] = get<XiaoQuRepository>()
    map
}
}