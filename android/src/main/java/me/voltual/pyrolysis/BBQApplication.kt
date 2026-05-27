@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package me.voltual.pyrolysis

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.voltual.pyrolysis.core.database.*
import me.voltual.pyrolysis.core.ui.theme.ThemeColorStore
import me.voltual.pyrolysis.core.ui.theme.ThemeManager
import me.voltual.pyrolysis.data.content.Preferences
import me.voltual.pyrolysis.feature.store.repository.privacyModule
import me.voltual.pyrolysis.feature.store.worker.WorkerManager
import me.voltual.pyrolysis.feature.store.worker.workmanagerModule
import me.voltual.pyrolysis.manager.network.downloadClientModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinApplication
import me.voltual.pyrolysis.manager.installer.*
import org.koin.dsl.koinConfiguration
import java.lang.ref.WeakReference

@KoinApplication
class BBQApplication : Application(), KoinStartup {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 正常的延迟注入（只要在调用它们时不早于 Koin 初始化即可）
    val wm: WorkerManager by inject()
    val db: FdroidDatabase by inject()
    val installer: AppInstaller by inject() // 移动到实例作用域
    
    val themeStore: ThemeColorDataStore by inject() 

    // 数据库单例
    lateinit var database: AppDatabase
        private set

    // 用于适配 InstallWorker 的 Activity 引用
    private var activityRef: WeakReference<AppCompatActivity> = WeakReference(null)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 核心：自动追踪当前活跃的 Activity
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is AppCompatActivity) {
                    activityRef = WeakReference(activity)
                }
            }
            override fun onActivityPaused(activity: Activity) {
                if (activityRef.get() == activity) {
                    activityRef.clear()
                }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // 优先初始化存储
        Preferences.init(this)
        
        // 确保 wm 安全调用（KoinStartup 保证了此时 Koin 已就绪）
        wm.prune()    
        
        ThemeManager.syncThemeState(this)
        // 使用 runBlocking 加载持久化的颜色集到内存管理器
        runBlocking {
            ThemeManager.updateCustomColors(themeStore.colorsFlow.first())
        }
    }
    
    
	@Suppress("DSL_MARKER_APPLIED_TO_WRONG_TARGET")
    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@BBQApplication)
        modules(
            downloadClientModule,
            workmanagerModule,
            databaseModule,
            privacyModule,
            installerModule,
            appModule,
        )
    }

    companion object {
        // 使用 lateinit 确保安全，并在 onCreate 中赋值
        lateinit var instance: BBQApplication
            private set

        // 适配 InstallWorker 调用 BBQApplication.mainActivity
        var mainActivity: AppCompatActivity?
            get() = if (::instance.isInitialized) instance.activityRef.get() else null
            set(_) {} 

        // 将全局静态桥梁全部改为 运行时读属性（Getter）
        // 这样可以确保外部调用时，Koin 和 Application 已经完全初始化好了
        val wm: WorkerManager get() = instance.wm
        val db: FdroidDatabase get() = instance.db
        val context: Context get() = instance
        val installer: AppInstaller get() = instance.installer // 通过 instance 间接获取
        
        val latestSyncs: MutableMap<Long, Long> = mutableMapOf()
    }
}