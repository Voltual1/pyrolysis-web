/*
 * This file is adapted from Neo Store (https://github.com/NeoApplications/Neo-Store)
 * Modified by Voltual to fit Pyrolysis architecture.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */
package me.voltual.pyrolysis.manager.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.core.net.toUri
import me.voltual.pyrolysis.ARG_PACKAGE_NAME
import me.voltual.pyrolysis.MainActivity
import me.voltual.pyrolysis.BBQApplication
import me.voltual.pyrolysis.manager.installer.AppInstaller
import me.voltual.pyrolysis.manager.installer.type.BaseInstaller.Companion.translatePackageInstallerError
import me.voltual.pyrolysis.core.utils.Utils
import me.voltual.pyrolysis.core.utils.extension.android.Android
import me.voltual.pyrolysis.core.utils.notifyStatus
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InstallerReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        private const val TAG = "InstallerReceiver"
        const val KEY_ACTION = "installerAction"
        const val KEY_PACKAGE_LABEL = "packageLabel"
        const val ACTION_UNINSTALL = "uninstall"
        const val INSTALLED_NOTIFICATION_TIMEOUT: Long = 5000
        const val NOTIFICATION_TAG_PREFIX = "install-"

        /**
         * 兼容性扩展函数：处理 Parcelable 获取
         */
        inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(key, T::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(key) as? T
            }
        }
    }

    val installer: AppInstaller by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)

        val sessionInstaller = context.packageManager.packageInstaller
        val session = if (sessionId > 0) sessionInstaller.getSessionInfo(sessionId) else null

        val packageName = session?.appPackageName ?: intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)

        Log.i(TAG, "Status: $status, Package: $packageName")

        val pending = goAsync()
        val appScope = (context.applicationContext as BBQApplication).applicationScope
        
        appScope.launch {
            try {
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> {
                        packageName?.let { installer.reportSuccess(it) }
                    }

                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val isNotInUserInteraction = !installer.isInUserInteraction(packageName) &&
                                !(Android.sdk(Build.VERSION_CODES.R) && session?.isStagedSessionActive == true)
                        
                        if (Utils.inForeground() && isNotInUserInteraction) {
                            installer.reportUserInteraction(packageName)
                            
                            // 使用兼容性函数修复警告
                            val promptIntent = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)

                            promptIntent?.let {
                                it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                // 模拟来源以绕过某些系统的严格检查
                                it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                Log.i(TAG, "Initiating install dialog for Package: $packageName")
                                context.startActivity(it)
                            }
                        }
                    }

                    PackageInstaller.STATUS_FAILURE_ABORTED,
                    PackageInstaller.STATUS_FAILURE_CONFLICT,
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                    PackageInstaller.STATUS_FAILURE_INVALID,
                    PackageInstaller.STATUS_FAILURE_STORAGE -> {
                        val cancelIntent = Intent(context, ActionReceiver::class.java).apply {
                            this.action = ActionReceiver.COMMAND_CANCEL_INSTALL
                            putExtra(ARG_PACKAGE_NAME, packageName)
                        }
                        context.sendBroadcast(cancelIntent)
                        installer.reportFailure(translatePackageInstallerError(status))
                    }
                }
                
                if (!(Utils.inForeground() && status == PackageInstaller.STATUS_PENDING_USER_ACTION)) {
                    notifyStatus(context, intent)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

fun installIntent(context: Context, intent: Intent): PendingIntent {
    // 同样使用兼容性逻辑获取 Intent 内部的 Intent
    val promptIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_INTENT)
    }
    
    val name = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
    val cacheFileName = intent.getStringExtra(MainActivity.EXTRA_CACHE_FILE_NAME)

    return PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_INSTALL
            data = "package:$name".toUri()
            putExtra(Intent.EXTRA_INTENT, promptIntent)
            putExtra(MainActivity.EXTRA_CACHE_FILE_NAME, cacheFileName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}