plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.filekit.dialogs.compose)
}

// CFBundleDocumentTypes: which documents the app opens. LSHandlerRank "Alternate" lists KoMD in
// "Open With…" without stealing the default handler; the user can still make it the default.
// UTImportedTypeDeclarations: describes net.daringfireball.markdown for macOS versions or setups
// where no other installed app declares it, so .md/.markdown always resolve to that UTI.
val macMarkdownDocumentTypes = """
    <key>CFBundleDocumentTypes</key>
    <array>
        <dict>
            <key>CFBundleTypeName</key>
            <string>Markdown document</string>
            <key>CFBundleTypeRole</key>
            <string>Editor</string>
            <key>LSHandlerRank</key>
            <string>Alternate</string>
            <key>LSItemContentTypes</key>
            <array>
                <string>net.daringfireball.markdown</string>
            </array>
            <key>CFBundleTypeExtensions</key>
            <array>
                <string>md</string>
                <string>markdown</string>
                <string>mdown</string>
                <string>mkd</string>
                <string>mkdn</string>
            </array>
        </dict>
    </array>
    <key>UTImportedTypeDeclarations</key>
    <array>
        <dict>
            <key>UTTypeIdentifier</key>
            <string>net.daringfireball.markdown</string>
            <key>UTTypeDescription</key>
            <string>Markdown document</string>
            <key>UTTypeConformsTo</key>
            <array>
                <string>public.plain-text</string>
            </array>
            <key>UTTypeTagSpecification</key>
            <dict>
                <key>public.filename-extension</key>
                <array>
                    <string>md</string>
                    <string>markdown</string>
                    <string>mdown</string>
                    <string>mkd</string>
                    <string>mkdn</string>
                </array>
                <key>public.mime-type</key>
                <string>text/markdown</string>
            </dict>
        </dict>
    </array>
""".trimIndent()

compose.desktop {
    application {
        mainClass = "komd.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
            packageName = "KoMD"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/main/resources/KoMD.icns"))
                bundleID = "komd.desktop"
                // Registers KoMD as an editor for Markdown files, so it shows up in Finder's
                // "Open With…" menu and can be picked as the default app (Get Info → Open with).
                infoPlist {
                    extraKeysRawXml = macMarkdownDocumentTypes
                }
            }
            windows {
                iconFile.set(project.file("src/main/resources/KoMD.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/KoMD.png"))
            }
        }
    }
}
