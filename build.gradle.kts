// 根目录 build.gradle.kts
plugins {
    // 应用 base 插件来统一管理生命周期任务
    id("base") 
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// 使用 named 而不是 register，因为 base 插件已经创建了 clean
tasks.named<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}