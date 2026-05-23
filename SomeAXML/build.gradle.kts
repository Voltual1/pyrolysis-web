plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "org.duangsuse"
version = "1.0"

kotlin {
    
    sourceSets {
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.io)
            }
        }        
    }
}