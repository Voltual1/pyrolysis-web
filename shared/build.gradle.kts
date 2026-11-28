// shared/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl 

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.github.gmazzo.buildconfig") version "5.3.0"
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.kotlin.serialization)
}

val appVersionName: String = providers.gradleProperty("APP_VERSION_NAME").get()
val appVersionCode: Int = providers.gradleProperty("APP_VERSION_CODE").get().toInt()
val shellVersionAndroid: String = providers.gradleProperty("SHELL_VERSION_ANDROID").get()
val shellVersionFdroid: String = providers.gradleProperty("SHELL_VERSION_FDROID").get()
val shellVersionWasm: String = providers.gradleProperty("SHELL_VERSION_WASM").get()
val shellVersionJvm: String = providers.gradleProperty("SHELL_VERSION_JVM").get()

buildConfig {
    useKotlinOutput()
    packageName("me.voltual.pyrolysis.shared")
    
    buildConfigField("VERSION_NAME", appVersionName)
    buildConfigField("VERSION_CODE", appVersionCode)
    buildConfigField("SHELL_VERSION_ANDROID", shellVersionAndroid)
    buildConfigField("SHELL_VERSION_FDROID", shellVersionFdroid)
    buildConfigField("SHELL_VERSION_WASM", shellVersionWasm)
    buildConfigField("SHELL_VERSION_JVM", shellVersionJvm)
}

kotlin {
    android {
        namespace = "me.voltual.pyrolysis.shared"
        compileSdk = 37
        minSdk = 24
        
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        useEsModules()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)              
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
                implementation(libs.jetbrains.material3.adaptiveNavigation3)
                implementation(libs.compose.material3)
                implementation(libs.ktor.client.core)
                implementation(libs.markdown)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.room3.runtime)
                implementation(libs.sqlite)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.io)                
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.components.resources)
                implementation(libs.kotlinx.datetime)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.material.icons.core)
                implementation(libs.material.icons.extended)
                
                // FileKit
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)
                implementation(libs.coil.compose)
                
                // DataStore library
                implementation("androidx.datastore:datastore-core:1.3.0-alpha09")
                implementation("androidx.datastore:datastore-preferences-core:1.3.0-alpha09")

                // Cryptography Kotlin
                implementation(libs.cryptography.core)
                implementation(libs.cryptography.provider.optimal)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ijkplayer)
                implementation(libs.koin.android.compose)
//                implementation(libs.sqlite.bundled)
                implementation(project(":DanmakuFlameMaster"))
                implementation(libs.compose.adaptive)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqlite.bundled)
                implementation(project(":ApkParser"))
                implementation(libs.coil.network.ktor)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js.wasm)    
            implementation(project(":ApkParser"))                
            // 这两个库会接管 Wasm 层的初始化钩子，彻底干掉那个无处安放的文件系统异常！
            implementation(libs.coil.compose.wasm)
            implementation(libs.coil.network.ktor.wasm)                
            implementation(libs.sqlite.web)
            implementation(libs.kotlinx.io)             
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "me.voltual.pyrolysis"
    generateResClass = always
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room3.compiler)
    add("kspJvm", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}