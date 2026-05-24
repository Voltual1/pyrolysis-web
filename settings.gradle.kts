// settings.gradle.kts
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

allprojects {
    plugins.withType<NodeJsPlugin> {
        the<NodeJsEnvSpec>().apply {
            download = false
            version = "22.12.0" 
        }
    }

    plugins.withType<YarnPlugin> {
        the<YarnRootEnvSpec>().download = false
    }

    plugins.withType<WasmNodeJsPlugin> {
        the<WasmNodeJsEnvSpec>().apply {
            download = false
            version = "22.12.0"
        }
    }

    plugins.withType<WasmYarnPlugin> {
        the<WasmYarnRootEnvSpec>().download = false
    }
}
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
include(":shared")
//include(":android")
include(":arsc")
include(":SomeAXML")
include(":web")
//include(":DanmakuFlameMaster")