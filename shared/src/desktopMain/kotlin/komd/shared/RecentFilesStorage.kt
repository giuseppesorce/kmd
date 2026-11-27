// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import java.util.prefs.Preferences

internal actual object RecentFilesStorage {
    private const val KEY = "recentFiles"
    private val prefs: Preferences by lazy { Preferences.userRoot().node("komd") }

    actual fun load(): String? = runCatching { prefs.get(KEY, null) }.getOrNull()

    actual fun save(value: String) {
        runCatching {
            prefs.put(KEY, value)
            prefs.flush()
        }
    }
}
