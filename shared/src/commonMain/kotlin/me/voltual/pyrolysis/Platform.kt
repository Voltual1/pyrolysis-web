package me.voltual.pyrolysis

enum class PlatformType(val id: String) {
    ANDROID("android"),
    ANDROID_FDROID("android-fdroid"),
    WEB_WASM("web-wasm");

    val isAndroid: Boolean
        get() = this == ANDROID || this == ANDROID_FDROID
}

internal expect fun getPlatformType(): PlatformType

internal fun getPlatformId(): String = getPlatformType().id