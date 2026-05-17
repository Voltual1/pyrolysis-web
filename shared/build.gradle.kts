// shared/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

kotlin {
    // 1. Android 目标配置
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // 2. Web 目标配置 (JS)
    js(IR) {
        browser()
        useEsModules()
    }

    // 3. Web 目标配置 (Wasm)
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    // 4. 层次结构模板：自动创建 webMain 等中间层
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.androidx.icons.extended)
                
                // 网络与序列化
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.androidx.activity.compose)
            }
        }

        // 5. 创建 webMain 中间层，供 JS 和 WasmJs 共同使用
        val webMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        val jsMain by getting {
            dependsOn(webMain)
        }

        val wasmJsMain by getting {
            dependsOn(webMain)
        }
    }

    // 开启 expect-actual 强校验
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "me.voltual.pyrolysis.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}