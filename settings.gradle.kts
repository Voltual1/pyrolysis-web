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
        
        // ================== 核心新增部分 ==================
        // 1. 允许 Gradle 从 Node.js 官方网站直接下载二进制包
        ivy {
            url = uri("https://nodejs.org/dist")
            patternLayout {
                artifact("[v]artifacts/releases/[artifact]-[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            // 声明这个仓库只负责加载 org.nodejs 相关的组，避免拖慢其他依赖的查找速度
            content {
                includeGroup("org.nodejs")
            }
        }
        
        // 2. 备用：部分旧版 Kotlin 插件可能会去这个特殊 Maven 镜像找 node
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
        // ==================================================
    }
}

rootProject.name = "Pyrolysis"
include(":shared")
//include(":android")
include(":arsc")
include(":SomeAXML")
include(":web")
//include(":DanmakuFlameMaster")