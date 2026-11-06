import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "komd.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources { enable = true }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        getByName("androidMain").dependencies { implementation(libs.ktor.client.android) }
        getByName("iosArm64Main").dependencies { implementation(libs.ktor.client.darwin) }
        getByName("iosSimulatorArm64Main").dependencies { implementation(libs.ktor.client.darwin) }
        getByName("desktopMain").dependencies { implementation(compose.desktop.currentOs) }
        getByName("desktopMain").dependencies { implementation(libs.ktor.client.java) }
    }
}
