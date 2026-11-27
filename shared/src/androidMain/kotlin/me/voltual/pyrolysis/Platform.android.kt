package me.voltual.pyrolysis

private var androidPlatformId: String = "android"

fun initAndroidPlatform(platformId: String) {
    androidPlatformId = platformId
}

actual fun getPlatformId(): String = androidPlatformId