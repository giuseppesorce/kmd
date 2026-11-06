# KMD

KMD is a Markdown editor built with Kotlin Multiplatform and Compose Multiplatform.

## Targets

- Android application: `androidApp`
- iOS application: `iosApp/iosApp.xcodeproj`
- Desktop application for macOS, Windows, and Linux: `desktopApp`
- Shared Compose UI and editor state: `shared`

## Run

Open the project root in Android Studio and let Gradle sync. Choose the `androidApp` or `desktopApp` run configuration to launch those apps. Open `iosApp/iosApp.xcodeproj` in Xcode to run the iOS app.

The iOS target requires macOS and Xcode. The Android target requires an installed Android SDK.
