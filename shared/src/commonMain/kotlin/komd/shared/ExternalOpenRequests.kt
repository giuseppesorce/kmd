// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Files the OS asks KoMD to open from outside the UI: Finder "Open With…", a double click on a
 * document associated with the app, a drag onto the Dock icon or paths passed on the command line.
 *
 * The platform entry point pushes into this queue, [App] drains it. A buffered channel (instead of a
 * SharedFlow) is used on purpose: on macOS the open-document event of a cold launch arrives before
 * the first composition, so requests must wait in the queue until the UI starts collecting them.
 */
object ExternalOpenRequests {
    private val queue = Channel<PlatformFile>(Channel.UNLIMITED)

    fun open(file: PlatformFile) {
        queue.trySend(file)
    }

    internal val requests: Flow<PlatformFile> = queue.receiveAsFlow()
}
