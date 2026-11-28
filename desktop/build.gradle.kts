import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }

    dependencies {
        implementation(project(":shared"))
        implementation(compose.desktop.currentOs)
        implementation(libs.components.resources)
        implementation(libs.kotlinx.coroutines.swing)
        implementation(libs.koin.core)
        implementation(libs.filekit.core)
    }
}

compose.desktop {
    application {
        mainClass = "me.voltual.pyrolysis.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "Pyrolysis"
            packageVersion = providers.gradleProperty("APP_VERSION_NAME").getOrElse("1.0.0")
        }
    }
}