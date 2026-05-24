// shared/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl 

plugins {
    // 关键变更：替换旧的 android.library
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

kotlin {
    // 关键变更：使用 android {} 而不是 androidTarget {}
    // 并且配置直接写在这里，不再需要顶层的 android {} 块
    android {
        namespace = "me.voltual.pyrolysis.shared"
        compileSdk = 36
        minSdk = 24
        
        // 如果需要使用 Android 资源（R.string 等），必须显式开启
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    js(IR) {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
            }
        }

        val androidMain by getting {
            dependencies {
                // 原本在顶层 dependencies {} 里的 android 依赖现在移到这里
                implementation(libs.ktor.client.okhttp)
            }
        }

        val webMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

// 注意：原有的顶层 android {} 块已被删除，配置已移入 kotlin.android {}