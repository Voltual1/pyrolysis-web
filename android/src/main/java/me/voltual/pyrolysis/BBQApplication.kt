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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.voltual.pyrolysis.di.*
import me.voltual.pyrolysis.core.ui.theme.ThemeColorDataStore
import me.voltual.pyrolysis.core.ui.theme.ThemeManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinApplication
import org.koin.dsl.koinConfiguration
import java.lang.ref.WeakReference

@KoinApplication
class BBQApplication : Application(), KoinStartup {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    val themeStore: ThemeColorDataStore by inject() 

    override fun onCreate() {
        super.onCreate()
        instance = this       
        
        runBlocking {
            ThemeManager.updateCustomColors(themeStore.colorsFlow.first())
        }
    }
    
	@Suppress("DSL_MARKER_APPLIED_TO_WRONG_TARGET")
    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@BBQApplication)
        modules(
            commonModule,      // KMP 共享的通用模块
			platformModule,    // KMP 共享的 Android 平台实现
        )
    }

    companion object {
        lateinit var instance: BBQApplication
            private set
        
    }
}
