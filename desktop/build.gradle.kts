import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

val rawAppVersion: String = providers.gradleProperty("APP_VERSION_NAME").getOrElse("23.8")
val normalizedDesktopVersion: String = rawAppVersion.split(".").let { parts ->
    val major = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 255) ?: 1
    val minor = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 255) ?: 0
    val build = parts.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 65535) ?: 0
    "$major.$minor.$build"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }

    dependencies {
        implementation(project(":shared"))
        implementation(compose.desktop.currentOs)
        implementation(libs.components.resources)
        implementation(libs.jetbrains.navigation3.ui)
        implementation(libs.kotlinx.coroutines.swing)
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.filekit.core)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}

compose.desktop {
    application {
        mainClass = "me.voltual.pyrolysis.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "Pyrolysis"
            includeAllModules = true
            packageVersion = normalizedDesktopVersion
            description = "Pyrolysis Desktop Client"
            vendor = "Voltual"

            macOS {
                bundleID = "me.voltual.pyrolysis"
                iconFile.set(project.file("icons/icon.icns"))
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                upgradeUuid = "8f7e4b2a-3c91-4d6e-9a52-1b8c7d4e6f01"
                menuGroup = "Pyrolysis"
                shortcut = true
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
                menuGroup = "Utility"
                shortcut = true
            }
        }
    }
}
