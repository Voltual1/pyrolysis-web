// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }       
        maven { url = uri("$rootDir/ijkplayer-main") }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "Pyrolysis"
//include(":shared")
include(":android")
//include(":web")
include(":DanmakuFlameMaster")