// web/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    js(IR) {
        outputModuleName = "pyrolysis-web"
        browser {
            commonWebpackConfig {
                outputFileName = "pyrolysis.js"
            }
        }
        binaries.executable()
        useEsModules()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "pyrolysis-wasm"
        browser()
        binaries.executable()
    }

    sourceSets {
        val webMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(project(":shared")) // 核心逻辑都在 shared 
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
            }
        }
        
        val jsMain by getting { dependsOn(webMain) }
        val wasmJsMain by getting { dependsOn(webMain) }
    }
}

compose.experimental {
    web.application {}
}