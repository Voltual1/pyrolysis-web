//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.bot

import cc.bbq.xq.bot.ui.auth.LoginViewModel
import cc.bbq.xq.bot.ui.billing.BillingViewModel
import cc.bbq.xq.bot.ui.bot.BotSettingsViewModel
import cc.bbq.xq.bot.ui.community.CommunityViewModel
import cc.bbq.xq.bot.ui.community.FollowingPostsViewModel
import cc.bbq.xq.bot.ui.community.HotPostsViewModel
import cc.bbq.xq.bot.ui.community.MyLikesViewModel
import cc.bbq.xq.bot.ui.payment.PaymentViewModel
import cc.bbq.xq.bot.ui.log.LogViewModel
import cc.bbq.xq.bot.ui.user.UserListViewModel
import cc.bbq.xq.bot.ui.message.MessageViewModel
import cc.bbq.xq.bot.ui.plaza.AppDetailComposeViewModel
import cc.bbq.xq.bot.ui.community.PostCreateViewModel 
import cc.bbq.xq.bot.ui.plaza.AppReleaseViewModel
import cc.bbq.xq.bot.ui.plaza.PlazaViewModel
import cc.bbq.xq.bot.ui.player.PlayerViewModel
import cc.bbq.xq.bot.ui.search.SearchViewModel
import cc.bbq.xq.bot.ui.user.MyPostsViewModel
import cc.bbq.xq.bot.ui.user.UserDetailViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ViewModel definitions
    viewModel { LoginViewModel(androidApplication()) }
    viewModel { BillingViewModel(androidApplication()) }
    viewModel { BotSettingsViewModel(androidApplication()) }
    viewModel { CommunityViewModel() }
    viewModel { FollowingPostsViewModel(androidApplication()) }
    viewModel { HotPostsViewModel() }
    viewModel { MyLikesViewModel(androidApplication()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { MessageViewModel(androidApplication()) }
    viewModel { AppDetailComposeViewModel(androidApplication()) }
    viewModel { AppReleaseViewModel(androidApplication()) }
    viewModel { (initialMode: Boolean) -> PlazaViewModel(androidApplication(), initialMode) }
    viewModel { PlayerViewModel(androidApplication()) }
    viewModel { SearchViewModel() }
    viewModel { UserListViewModel(androidApplication()) }
    viewModel { PostCreateViewModel(androidApplication()) }
    viewModel { MyPostsViewModel() }
    viewModel { PaymentViewModel(androidApplication()) }
    viewModel { UserDetailViewModel(androidApplication()) }

    // Singletons (if needed)
    single { AuthManager }
    single { RetrofitClient.instance }
    single { BBQApplication.instance.database }
    single { BBQApplication.instance.botConfigDataStore }
    single { BBQApplication.instance.processedPostsDataStore }
    single { BBQApplication.instance.searchHistoryDataStore }
}