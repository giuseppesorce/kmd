./gradlew :desktopApp:packageDmg                       # .dmg (solo su macOS)
./gradlew :desktopApp:packageDistributionForCurrentOS  # tutti i formati del sistema su cui sei
./gradlew :desktopApp:packageReleaseDmg                # come sopra, ma passando prima da ProGuard