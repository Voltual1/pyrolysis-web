package me.voltual.pyrolysis

import me.voltual.pyrolysis.shared.BuildConfig

enum class PlatformType(val id: String) {
    ANDROID("android"),
    ANDROID_FDROID("android-fdroid"),
    WEB_WASM("web-wasm");

    val isAndroid: Boolean
        get() = this == ANDROID || this == ANDROID_FDROID

    val shellVersion: String
        get() = when (this) {
            ANDROID -> BuildConfig.SHELL_VERSION_ANDROID
            ANDROID_FDROID -> BuildConfig.SHELL_VERSION_FDROID
            WEB_WASM -> BuildConfig.SHELL_VERSION_WASM
        }

    val formattedVersion: String
        get() = "${BuildConfig.VERSION_NAME} ($id $shellVersion · Build ${BuildConfig.VERSION_CODE})"
}

internal expect fun getPlatformType(): PlatformType

internal fun getPlatformId(): String = getPlatformType().id