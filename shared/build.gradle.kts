// shared/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
        publishLibraryVariants("release", "debug")
    }

    js(IR) {
        browser()
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp) // Android 继续使用 OkHttp
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js) // Web 使用 JS 引擎
            }
        }
        
        val wasmJsMain by getting {
            dependsOn(jsMain)
        }
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