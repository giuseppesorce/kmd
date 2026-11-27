// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

internal actual val shortcutUsesCommand: Boolean =
    System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
