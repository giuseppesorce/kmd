// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import android.content.Context
import android.content.SharedPreferences
import io.github.vinceglb.filekit.FileKitCore

internal actual object RecentFilesStorage {
    private const val KEY = "recentFiles"

    // FileKit initializes itself with the app context through androidx.startup, so no Activity wiring is needed.
    private val prefs: SharedPreferences? by lazy {
        runCatching { FileKitCore.context.getSharedPreferences("komd", Context.MODE_PRIVATE) }.getOrNull()
    }

    actual fun load(): String? = prefs?.getString(KEY, null)

    actual fun save(value: String) {
        prefs?.edit()?.putString(KEY, value)?.apply()
    }
}
