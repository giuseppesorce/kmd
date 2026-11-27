// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Bridges [RecentFile] (plain persisted data) and FileKit's [PlatformFile].
 *
 * The bookmark is what makes "reopen after restart" work outside plain desktop paths:
 * security-scoped bookmark on Apple, persistable URI permission on Android, the path on JVM.
 */
@OptIn(ExperimentalEncodingApi::class)
internal suspend fun recentFileOf(file: PlatformFile): RecentFile {
    val bookmark = runCatching { Base64.encode(file.bookmarkData().bytes) }.getOrNull()
    return RecentFile(path = file.path, name = file.name, bookmark = bookmark)
}

/** The file behind [entry], or null if it is no longer reachable (moved, renamed, deleted, permission revoked). */
@OptIn(ExperimentalEncodingApi::class)
internal fun resolveRecentFile(entry: RecentFile): PlatformFile? {
    val fromBookmark = entry.bookmark?.let { encoded ->
        runCatching { PlatformFile.fromBookmarkData(Base64.decode(encoded)) }.getOrNull()
    }
    val file = fromBookmark ?: runCatching { PlatformFile(entry.path) }.getOrNull() ?: return null
    return file.takeIf { runCatching { it.exists() }.getOrDefault(false) }
}
