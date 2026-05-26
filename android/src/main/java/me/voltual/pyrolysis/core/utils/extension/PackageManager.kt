/*
 * This file is adapted from Neo Store (https://github.com/NeoApplications/Neo-Store)
 * Modified by Voltual to fit Pyrolysis architecture.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */
package me.voltual.pyrolysis.core.utils.extension

import android.content.pm.InstallSourceInfo
import android.content.pm.PackageManager
import android.os.Build
import me.voltual.pyrolysis.BuildConfig
import me.voltual.pyrolysis.core.utils.extension.android.Android

fun PackageManager.isNSPackageUpdateOwner(packageName: String): Boolean {
    val owner = when {
        // 如果是自身应用 ID，直接通过
        packageName == BuildConfig.APPLICATION_ID -> BuildConfig.APPLICATION_ID

        // Android 14+ (API 34): 引入了更新所有者 (Update Owner) 概念
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            getInstallSourceInfo(packageName).let { info ->
                info.updateOwnerPackageName ?: info.installingPackageName
            }
        }

        // Android 11+ (API 30): getInstallerPackageName 开始废弃
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            getInstallSourceInfo(packageName).installingPackageName
        }

        else -> {
            @Suppress("DEPRECATION")
            getInstallerPackageName(packageName)
        }
    }
    return owner == BuildConfig.APPLICATION_ID
}

fun PackageManager.isInstalled(packageName: String): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, 0)
    }
    true
}.getOrElse { false }