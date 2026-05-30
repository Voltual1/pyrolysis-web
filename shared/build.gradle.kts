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
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // AGP 9.0 KMP 库专用 Android 配置块
    android {
        namespace = "me.voltual.pyrolysis"
        compileSdk = 37
        minSdk = 24 // 直接设置，不需要 defaultConfig
        
        // 开启 Android 资源支持（如果以后要放图片、字符串到 shared）
        androidResources {
            enable = true
        }

        // 替代旧的 compileOptions 和 kotlinOptions
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

/*    js(IR) {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }*/

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)              
                    implementation(libs.compose.navigation3)
                        implementation(libs.compose.navigation3.ui) 
                implementation(libs.compose.material3)
                    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
                        implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging)
                    implementation(project(":ApkParser"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.koin.android.compose)
                implementation(libs.kotlinx.coroutines.core)
                            implementation(libs.components.resources)
                implementation(libs.kotlinx.datetime)
                implementation(libs.koin.core)
                    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

                implementation(libs.koin.annotations)
                            implementation(libs.material.icons.core)
                            implementation(libs.material.icons.extended)
                    // FileKit
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
    implementation(libs.filekit.dialogs.compose)
        implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
           // DataStore library
  implementation("androidx.datastore:datastore-core:1.3.0-alpha09")
  // The Preferences DataStore library
  implementation("androidx.datastore:datastore-preferences-core:1.3.0-alpha09")
            }
        }
//这里的datastore一定要+"-core"并且得是1.3.0-alpha01之后才支持wasm+js
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

/*        val webMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }*/
    }
}

dependencies {
    // 针对 commonMain 源码集运行 KSP
    add("kspCommonMainMetadata", libs.room.compiler)    
}