plugins {
    // 使用 Version Catalog 引用 Kotlin JVM 插件
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0" 
}

// 如果子模块有独立版本号可保留，若与根项目一致，建议直接删掉这一行，由根项目统一管理
version = "1.0.0"

kotlin {
    // 开启显式 API 模式（现代化公共库开发推荐）
    explicitApi()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-opt-in=dev.rushii.arsc.internal.ArscInternalApi"
        )
    }
}

apiValidation {
    ignoredPackages.add("dev.rushii.arsc.internal")
    nonPublicMarkers.add("dev.rushii.arsc.internal.ArscInternalApi")
}

tasks.test {
    // 现代 Gradle 默认且推荐的测试运行器
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib"))   
    implementation(libs.kotlinx.io)
    testImplementation(kotlin("test"))
}