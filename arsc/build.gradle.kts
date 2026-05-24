plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0"
}

version = "1.0"

kotlin {
    explicitApi()
    jvm()
    	compilerOptions.freeCompilerArgs.addAll(
		"-opt-in=kotlin.ExperimentalUnsignedTypes",
		"-opt-in=dev.rushii.arsc.internal.ArscInternalApi"
	)
	    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(libs.kotlinx.io)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.io)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(kotlin("reflect"))
            }
        }
    }
}

apiValidation {
    ignoredPackages.add("dev.rushii.arsc.internal")
    nonPublicMarkers.add("dev.rushii.arsc.internal.ArscInternalApi")
}