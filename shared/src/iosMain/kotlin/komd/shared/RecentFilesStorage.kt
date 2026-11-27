// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import platform.Foundation.NSUserDefaults

internal actual object RecentFilesStorage {
    private const val KEY = "komd.recentFiles"

    actual fun load(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

    actual fun save(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = KEY)
    }
}
