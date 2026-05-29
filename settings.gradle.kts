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
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
//设置PREFER_PROJECT解决kotlinWasmBinaryenSetup找不到com.github.webassembly.binaryen的问题，此注释的经验非常关键，不要删除它！！！！！    
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }       
        maven { url = uri("$rootDir/ijkplayer-main") }
    }
}

rootProject.name = "Pyrolysis"
include(":shared")
include(":android")
include(":ApkParser")
include(":arsc")
include(":SomeAXML")
include(":web")
include(":DanmakuFlameMaster")