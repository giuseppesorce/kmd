package komd.shared

internal actual val shortcutUsesCommand: Boolean =
    System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
