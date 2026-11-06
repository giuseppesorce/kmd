plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "komd.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
            packageName = "KoMD"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/main/resources/KoMD.icns"))
            }
        }
    }
}
