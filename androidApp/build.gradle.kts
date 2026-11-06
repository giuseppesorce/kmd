import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "komd.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "komd.android"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}
