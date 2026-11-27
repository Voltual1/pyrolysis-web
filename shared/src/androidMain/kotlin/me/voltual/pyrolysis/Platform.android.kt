package me.voltual.pyrolysis

private var currentAndroidPlatform: PlatformType = PlatformType.ANDROID

fun initAndroidPlatform(platform: PlatformType) {
    require(platform.isAndroid) { "Expected an Android platform type, got: $platform" }
    currentAndroidPlatform = platform
}

internal actual fun getPlatformType(): PlatformType = currentAndroidPlatform