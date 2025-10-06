//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.bot

import android.app.Application
import cc.bbq.xq.bot.data.BotConfigDataStore
import cc.bbq.xq.bot.data.ProcessedPostsDataStore
import cc.bbq.xq.bot.ui.theme.ThemeManager
import cc.bbq.xq.bot.ui.theme.ThemeColorStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import cc.bbq.xq.bot.data.db.AppDatabase // 导入 AppDatabase
import cc.bbq.xq.bot.data.SearchHistoryDataStore // 导入

class BBQApplication : Application() {

    // DataStore 单例
    lateinit var botConfigDataStore: BotConfigDataStore
        private set
    lateinit var processedPostsDataStore: ProcessedPostsDataStore
        private set
        
    lateinit var searchHistoryDataStore: SearchHistoryDataStore // 新增
        private set

    // 新增：数据库单例
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
    super.onCreate()
    instance = this

    // 初始化所有单例
    botConfigDataStore = BotConfigDataStore(this)
    processedPostsDataStore = ProcessedPostsDataStore(this)
    database = AppDatabase.getDatabase(this) // 初始化数据库
    searchHistoryDataStore = SearchHistoryDataStore(this) // 新增
    // 初始化主题管理器
    ThemeManager.initialize(this)

    // 加载并应用保存的自定义颜色
    ThemeManager.customColorSet = ThemeColorStore.loadColors(this)

    // 初始化 Koin
    startKoin {
        androidContext(this@BBQApplication)
        modules(appModule) // 稍后定义 appModule
    }
}
    companion object {
        lateinit var instance: BBQApplication
            private set
    }
}