import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.shizuku.refine)
    id("kotlin-parcelize")
}

android {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    namespace = "me.voltual.pyrolysis"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.voltual.pyrolysis"
        minSdk = 24
        targetSdk = 36
        versionCode = 511
        versionName = "22.1"
        multiDexEnabled = true
        buildConfigField("String", "LICENSE", "\"GPLv3\"")
        resourceConfigurations += listOf("zh")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: keystoreProperties.getProperty("storeFile") ?: "debug.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword")
            keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
            keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            val abi = output.getFilter(com.android.build.OutputFile.ABI) ?: "universal"
            output.outputFileName = "Pyrolysis${variant.versionName}-$abi-${variant.buildType.name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("/META-INF/INDEX.LIST")
            excludes.add("/META-INF/DEPENDENCIES")
            excludes.add("/google/protobuf/**")
            excludes.add("/src/google/protobuf/**")
            excludes.add("/java/core/java_features_proto-descriptor-set.proto.bin")
            excludes.add("/META-INF/LICENSE*")
            excludes.add("/META-INF/*.txt")
            excludes.add("/DebugProbesKt.bin")
            merges.add("/META-INF/services/**")
        }
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // 基础
    coreLibraryDesugaring(libs.android.desugar)
    implementation(libs.google.material)
    implementation(libs.okhttp)
    implementation(libs.fragment)
    implementation(libs.biometric)
    implementation(libs.simple.storage)
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.navigation3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
    implementation(libs.zxing.core)
    implementation(libs.compose.navigation3.ui)
    implementation(libs.compose.adaptive)
    implementation(libs.compose.adaptive.layout)
    implementation(libs.compose.adaptive.navigation)

    // 图片与异步
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.kotlinx.coroutines.android)
    
    // FileKit 替代 imagepicker
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)
    implementation(libs.filekit.dialogs.compose)

    // 播放器与 UI
    implementation(libs.ijkplayer)
    implementation(project(":DanmakuFlameMaster"))
    implementation(libs.markdown)

    // 存储
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.datastore.core)
    
    //Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.shizuku.refine)
    compileOnly(libs.shizuku.hidden)
    
    //Libsu
    implementation(libs.libsu.core)

    // Koin 注入
    implementation(libs.koin.android.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.annotations)
    implementation(libs.koin.workmanager)
    implementation(libs.koin.startup)
    ksp(libs.koin.ksp.compiler)

    // Ktor 与 序列化
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.work.runtime)
    implementation(libs.ktor.io)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)

    // 安全与数据
    implementation(libs.tink.android)
    implementation(libs.protobuf.kotlin)

    // --- Neo Store 移植依赖 ---
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.compose.html.converter)
}

protobuf {
    protoc {
        artifact = libs.protoc.artifact.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java")
                create("kotlin")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
    }
}