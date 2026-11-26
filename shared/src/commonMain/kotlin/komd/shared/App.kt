package komd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** [baseline] is the last saved (or loaded) text: a document is dirty when its content differs from it. */
private data class Document(val name: String, val content: String, val file: PlatformFile? = null, val baseline: String = content)

private enum class SettingsSection { Languages, Appearance, Text }
internal enum class WorkspaceMode { Split, Editor, Preview }

/** Panels of the side bar, one per rail icon. */
internal enum class SidePanel { Files, Search, Outline, Export }

internal val LocalInterfaceScale = staticCompositionLocalOf { 1f }

private fun welcomeText(strings: AppStrings): String = """# ${strings.welcomeTitle}

${strings.welcomeIntro}

## ${strings.welcomeSection}

${strings.welcomeDirections}

- ${strings.welcomeBullets[0]}
- ${strings.welcomeBullets[1]}
- ${strings.welcomeBullets[2]}

> ${strings.welcomeQuote}

```kotlin
fun hello() = "${strings.hello}, KoMD!"
```
"""

private fun initialDocuments(strings: AppStrings): List<Document> = listOf(
    Document(strings.welcomeName, welcomeText(strings))
)

private fun imageAlignmentLabel(alignment: ImageAlignment, strings: AppStrings): String = when (alignment) {
    ImageAlignment.Left -> strings.left
    ImageAlignment.Center -> strings.center
    ImageAlignment.Right -> strings.right
}

internal fun workspaceModeLabel(mode: WorkspaceMode, strings: AppStrings): String = when (mode) {
    WorkspaceMode.Split -> strings.split
    WorkspaceMode.Editor -> strings.editor
    WorkspaceMode.Preview -> strings.preview
}

/**
 * @param commands when given, the platform shell (desktop menu bar) renders the action registry
 *   as native menus and owns the keyboard shortcuts; otherwise App handles the shortcuts itself.
 * @param onExit invoked when the app should quit, after unsaved changes have been confirmed.
 */
@Composable
fun App(commands: AppCommands? = null, onExit: () -> Unit = {}) {
    val systemDark = isSystemInDarkTheme()
    var darkTheme by remember { mutableStateOf(systemDark) }
    var language by remember { mutableStateOf(UiLanguage.Italian) }
    var interfaceScale by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    KomdTheme(dark = darkTheme) {
        CompositionLocalProvider(
            LocalUiStrings provides stringsFor(language),
            LocalInterfaceScale provides interfaceScale,
            LocalDensity provides Density(density.density, density.fontScale * interfaceScale)
        ) {
            AppContent(
                darkTheme = darkTheme,
                onDarkThemeChange = { darkTheme = it },
                language = language,
                onLanguageChange = { language = it },
                interfaceScale = interfaceScale,
                onInterfaceScaleChange = { interfaceScale = it },
                commands = commands,
                onExit = onExit
            )
        }
    }
}

@Composable
private fun AppContent(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    language: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit,
    interfaceScale: Float,
    onInterfaceScaleChange: (Float) -> Unit,
    commands: AppCommands?,
    onExit: () -> Unit
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var documents by remember { mutableStateOf(initialDocuments(ui)) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var content by remember { mutableStateOf(welcomeText(ui)) }
    // Caret/selection of the editor, kept outside the text field so toolbar inserts land where the user is.
    var selection by remember { mutableStateOf(TextRange(content.length)) }
    var composition by remember { mutableStateOf<TextRange?>(null) }
    val replaceContent: (String) -> Unit = { newContent ->
        content = newContent
        selection = TextRange(newContent.length)
        composition = null
    }
    /** Copy of the documents list with the live editor text written back into the selected document. */
    val syncCurrent: (List<Document>) -> MutableList<Document> = { list ->
        list.toMutableList().also { if (selectedIndex in it.indices) it[selectedIndex] = it[selectedIndex].copy(content = content) }
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    val editorScroll = rememberScrollState()
    val previewScroll = rememberScrollState()
    var syncScroll by remember { mutableStateOf(true) }
    SyncedScrollEffect(enabled = syncScroll, first = editorScroll, second = previewScroll)
    var workspaceMode by remember { mutableStateOf(WorkspaceMode.Split) }
    var sidePanel by remember { mutableStateOf<SidePanel?>(SidePanel.Files) }
    var paletteOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf(SettingsSection.Languages) }
    var fileMessage by remember { mutableStateOf<String?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }
    var showImageUrlDialog by remember { mutableStateOf(false) }
    var imageAlt by remember { mutableStateOf(ui.image) }
    var imageUrl by remember { mutableStateOf("") }
    var imageAlignment by remember { mutableStateOf(ImageAlignment.Left) }
    var showCustomLinkDialog by remember { mutableStateOf(false) }
    var customLinkText by remember { mutableStateOf("") }
    var customLinkUrl by remember { mutableStateOf("") }
    /** Folder of the current document's file, base for relative image paths (null if unsaved or not a filesystem path). */
    val documentDirectory: () -> String? = { documents.getOrNull(selectedIndex)?.file?.path?.let(::parentDirectory) }
    val changeLanguage: (UiLanguage) -> Unit = { selectedLanguage ->
        if (selectedLanguage != language) {
            val nextStrings = stringsFor(selectedLanguage)
            val oldSamples = initialDocuments(ui)
            val newSamples = initialDocuments(nextStrings)
            val selectedSampleIsUntouched = selectedIndex in oldSamples.indices &&
                documents.getOrNull(selectedIndex)?.content == oldSamples[selectedIndex].content &&
                content == oldSamples[selectedIndex].content
            val translatedDocuments = documents.mapIndexed { index, document ->
                val untouchedSample = index in oldSamples.indices && document.content == oldSamples[index].content
                document.copy(
                    name = if (index in oldSamples.indices && document.name == oldSamples[index].name) newSamples[index].name else document.name,
                    content = if (untouchedSample) newSamples[index].content else document.content,
                    baseline = if (untouchedSample) newSamples[index].content else document.baseline
                )
            }.toMutableList()
            if (selectedSampleIsUntouched) replaceContent(newSamples[selectedIndex].content)
            if (selectedIndex in translatedDocuments.indices) {
                translatedDocuments[selectedIndex] = translatedDocuments[selectedIndex].copy(content = content)
            }
            documents = translatedDocuments
            if (imageAlt == ui.image) imageAlt = nextStrings.image
            fileMessage = null
            onLanguageChange(selectedLanguage)
        }
    }
    val scope = rememberCoroutineScope()
    val latestContent by rememberUpdatedState(content)
    // --- Recent files: last MAX_RECENT_FILES files opened or saved, persisted across launches ---
    var recentFiles by remember { mutableStateOf(decodeRecentFiles(RecentFilesStorage.load())) }
    var missingRecent by remember { mutableStateOf<RecentFile?>(null) }
    val updateRecentFiles: ((List<RecentFile>) -> List<RecentFile>) -> Unit = { transform ->
        recentFiles = transform(recentFiles)
        RecentFilesStorage.save(encodeRecentFiles(recentFiles))
    }
    suspend fun rememberRecent(file: PlatformFile) {
        val entry = recentFileOf(file)
        updateRecentFiles { it.withRecent(entry) }
    }
    val selectDocument: (Int) -> Unit = { index ->
        if (index in documents.indices && index != selectedIndex) {
            documents = syncCurrent(documents)
            selectedIndex = index
            replaceContent(documents[index].content)
        }
    }
    suspend fun openDocument(file: PlatformFile) {
        // A file that is already open in a tab is focused instead of being opened twice.
        val alreadyOpen = documents.indexOfFirst { it.file?.path == file.path }
        if (alreadyOpen >= 0) {
            selectDocument(alreadyOpen)
            fileMessage = ui.opened.replace("{name}", file.name)
            rememberRecent(file)
            return
        }
        runCatching { file.readString() }
            .onSuccess { loaded ->
                documents = syncCurrent(documents).also { it += Document(file.name, loaded, file) }
                selectedIndex = documents.lastIndex
                replaceContent(loaded)
                fileMessage = ui.opened.replace("{name}", file.name)
                rememberRecent(file)
            }
            .onFailure { fileMessage = ui.readFailure.replace("{error}", it.message ?: ui.unknownError) }
    }
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("md", "markdown")),
        onError = { fileMessage = ui.openFailure.replace("{error}", it.message ?: ui.unknownError) },
        onResult = { file -> if (file != null) scope.launch { openDocument(file) } }
    )
    // Files handed over by the OS (Finder "Open With…", double click, drag & drop, CLI args): same path as the picker.
    // rememberUpdatedState so the long-lived collector always uses the current strings/language.
    val openExternal by rememberUpdatedState<suspend (PlatformFile) -> Unit> { openDocument(it) }
    LaunchedEffect(Unit) {
        ExternalOpenRequests.requests.collect { file -> openExternal(file) }
    }
    // Action to run once the "Save as" dialog has written the file (e.g. close the document after "Save").
    var afterSaveAs by remember { mutableStateOf<(() -> Unit)?>(null) }
    val saver = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onError = {
            afterSaveAs = null
            fileMessage = ui.saveFailure.replace("{error}", it.message ?: ui.unknownError)
        },
        onResult = { file ->
            val afterSave = afterSaveAs
            afterSaveAs = null
            if (file != null) scope.launch {
                // Image paths are rewritten relative to the new folder (absolute ones too, on the first save).
                val original = latestContent
                val text = parentDirectory(file.path)?.let { rebaseImageSources(original, documentDirectory(), it) } ?: original
                runCatching { file.writeString(text) }
                    .onSuccess {
                        documents = documents.toMutableList().also { list ->
                            if (selectedIndex in list.indices) list[selectedIndex] = list[selectedIndex].copy(name = file.name, content = text, file = file, baseline = text)
                        }
                        if (text != original && content == original) {
                            content = text
                            selection = TextRange(selection.start.coerceAtMost(text.length), selection.end.coerceAtMost(text.length))
                            composition = null
                        }
                        fileMessage = ui.saved.replace("{name}", file.name)
                        rememberRecent(file)
                        afterSave?.invoke()
                    }
                    .onFailure { fileMessage = ui.saveFailure.replace("{error}", it.message ?: ui.unknownError) }
            }
        }
    )
    /** Replaces text and selection in one step and mirrors the text into the documents list. */
    val applyEdit: (InsertResult) -> Unit = { result ->
        content = result.text
        selection = result.selection
        composition = null
        documents = syncCurrent(documents)
    }
    val insertAtCursor: (String) -> Unit = insert@{ snippet ->
        if (documents.isEmpty()) return@insert
        applyEdit(insertSnippet(content, selection, snippet))
    }
    val insertImage: (String, String) -> Unit = { alt, source ->
        insertAtCursor(alignedImageHtml(alt, source, imageAlignment))
    }
    val insertLink: (String, String) -> Unit = { label, target ->
        insertAtCursor(markdownLink(label, target))
    }
    /** Formatting shortcuts (headings, bold, lists...): edit the current line / selection in place. */
    val applyFormat: (FormatAction) -> Unit = format@{ action ->
        if (documents.isEmpty()) return@format
        applyEdit(action.apply(content, selection, ui))
    }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onError = { fileMessage = ui.imageFailure.replace("{error}", it.message ?: ui.unknownError) },
        onResult = { file ->
            if (file != null) {
                insertImage(ui.image, imageSourceForInsert(file.path, documentDirectory()))
                fileMessage = ui.imageAdded.replace("{name}", file.name)
            }
        }
    )
    val linkPicker = rememberFilePickerLauncher(
        type = FileKitType.File(),
        onError = { fileMessage = ui.filePickerFailure.replace("{error}", it.message ?: ui.unknownError) },
        onResult = { file ->
            if (file != null) {
                insertLink(file.name, fileUri(file.path))
                fileMessage = ui.fileLinkAdded.replace("{name}", file.name)
            }
        }
    )

    val openFile = {
        fileMessage = null
        picker.launch()
    }
    /** Reopens a recent file; if it is gone (moved, renamed, deleted) asks to drop it from the list. */
    val openRecent: (RecentFile) -> Unit = { entry ->
        fileMessage = null
        scope.launch {
            val file = resolveRecentFile(entry)
            if (file == null) {
                missingRecent = entry
            } else {
                // A bookmark can follow a moved file (Apple): keep one entry, under the new path.
                if (file.path != entry.path) updateRecentFiles { it.withoutRecent(entry.path) }
                openDocument(file)
            }
        }
    }
    /** Saves the current document: in place when [overwrite] and it has a file, otherwise via "Save as". [onSaved] runs only on success. */
    val saveDocument: (overwrite: Boolean, onSaved: (() -> Unit)?) -> Unit = save@{ overwrite, onSaved ->
        val index = selectedIndex
        if (index !in documents.indices) return@save
        val existing = documents[index].file
        if (overwrite && existing != null) {
            fileMessage = null
            scope.launch {
                val text = latestContent
                runCatching { existing.writeString(text) }
                    .onSuccess {
                        documents = documents.toMutableList().also { list ->
                            if (index in list.indices) list[index] = list[index].copy(content = text, baseline = text)
                        }
                        fileMessage = ui.saved.replace("{name}", existing.name)
                        rememberRecent(existing)
                        onSaved?.invoke()
                    }
                    .onFailure { fileMessage = ui.saveFailure.replace("{error}", it.message ?: ui.unknownError) }
            }
        } else {
            fileMessage = null
            afterSaveAs = onSaved
            saver.launch(
                suggestedName = documents.getOrNull(index)?.name?.substringBeforeLast('.') ?: ui.untitledTitle,
                defaultExtension = "md",
                allowedExtensions = setOf("md", "markdown")
            )
        }
    }
    val saveFile: (Boolean) -> Unit = { overwrite -> saveDocument(overwrite, null) }

    // --- Export HTML: page rendered when the dialog opens, written (plus images/) on its result ---
    var pendingExport by remember { mutableStateOf<HtmlExport?>(null) }
    val htmlSaver = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onError = {
            pendingExport = null
            fileMessage = ui.exportFailure.replace("{error}", it.message ?: ui.unknownError)
        },
        onResult = { file ->
            val export = pendingExport
            pendingExport = null
            if (file != null && export != null) scope.launch {
                runCatching { writeHtmlExport(file, export) }
                    .onSuccess { result ->
                        fileMessage = when {
                            result.skippedImages > 0 -> ui.exportImagesSkipped.replace("{count}", "${result.skippedImages}")
                            result.copiedImages > 0 -> ui.exportedWithImages.replace("{count}", "${result.copiedImages}")
                            else -> ui.exported
                        }.replace("{name}", file.name)
                    }
                    .onFailure { fileMessage = ui.exportFailure.replace("{error}", it.message ?: ui.unknownError) }
            }
        }
    )
    // --- Export PDF (desktop only): print HTML built when the dialog opens, rendered and written on its result ---
    var pendingPdf by remember { mutableStateOf<PendingPdf?>(null) }
    val pdfSaver = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onError = {
            pendingPdf = null
            fileMessage = ui.exportFailure.replace("{error}", it.message ?: ui.unknownError)
        },
        onResult = { file ->
            val pending = pendingPdf
            pendingPdf = null
            if (file != null && pending != null) scope.launch {
                runCatching { writePdfExport(file, pending) }
                    .onSuccess { fileMessage = ui.exported.replace("{name}", file.name) }
                    .onFailure { fileMessage = ui.exportFailure.replace("{error}", it.message ?: ui.unknownError) }
            }
        }
    )
    val exportPdf: () -> Unit = export@{
        if (!pdfExportSupported) return@export
        val document = documents.getOrNull(selectedIndex) ?: return@export
        val title = document.name.substringBeforeLast('.').ifBlank { ui.untitledTitle }
        val directory = documentDirectory()
        fileMessage = null
        pendingPdf = PendingPdf(
            html = buildHtmlExport(latestContent, title, directory, ExportMedium.Print).html,
            baseUri = directory?.let(::directoryFileUri),
        )
        pdfSaver.launch(suggestedName = title, defaultExtension = "pdf", allowedExtensions = setOf("pdf"))
    }
    val exportHtml: () -> Unit = export@{
        val document = documents.getOrNull(selectedIndex) ?: return@export
        val title = document.name.substringBeforeLast('.').ifBlank { ui.untitledTitle }
        fileMessage = null
        pendingExport = buildHtmlExport(latestContent, title, documentDirectory())
        htmlSaver.launch(suggestedName = title, defaultExtension = "html", allowedExtensions = setOf("html", "htm"))
    }
    val newDocument = {
        documents = syncCurrent(documents) + Document(ui.untitledName, "# ${ui.untitledTitle}\n\n")
        selectedIndex = documents.lastIndex
        replaceContent(documents.last().content)
    }

    // --- Close / exit with unsaved-changes confirmation -------------------------------------
    var pendingCloseIndex by remember { mutableStateOf<Int?>(null) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val isDirty: (Int) -> Boolean = { index ->
        val document = documents[index]
        (if (index == selectedIndex) content else document.content) != document.baseline
    }
    val removeDocument: (Int) -> Unit = { index ->
        val list = syncCurrent(documents)
        if (index in list.indices) list.removeAt(index)
        documents = list
        selectedIndex = when {
            list.isEmpty() -> 0
            index < selectedIndex -> selectedIndex - 1
            else -> selectedIndex.coerceAtMost(list.lastIndex)
        }
        replaceContent(list.getOrNull(selectedIndex)?.content ?: "")
        fileMessage = null
    }
    val requestExit: () -> Unit = {
        if (documents.indices.any(isDirty)) {
            showExitConfirm = true
        } else {
            onExit()
        }
    }
    /** Closes the document at [index]; a dirty one is selected first, since "Save" acts on the current document. */
    val closeDocumentAt: (Int) -> Unit = { index ->
        if (index in documents.indices) {
            if (isDirty(index)) {
                selectDocument(index)
                pendingCloseIndex = index
            } else {
                removeDocument(index)
            }
        }
    }
    /** Closes the current document (asking to save if dirty); with no document open, it asks to exit. */
    val closeCurrent: () -> Unit = {
        if (documents.isEmpty()) requestExit() else closeDocumentAt(selectedIndex)
    }

    // --- In-document search -------------------------------------------------------------
    // `searchQuery` is what is typed; `searchedQuery` is the last submitted one, so the
    // matches (and the n/m counter) only change on Enter/arrow, not on every keystroke.
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchedQuery by remember { mutableStateOf("") }
    var currentMatch by remember { mutableIntStateOf(-1) }
    var searchTooShort by remember { mutableStateOf(false) }
    var searchFocusRequests by remember { mutableIntStateOf(0) }
    // Editor reveal: scroll so [revealOffset] sits in the upper third (search hit, outline heading).
    var revealOffset by remember { mutableIntStateOf(-1) }
    var revealRequests by remember { mutableIntStateOf(0) }
    var previewRevealRequests by remember { mutableIntStateOf(0) }
    var editorLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val searchMatches = remember(content, searchedQuery) { findMatches(content, searchedQuery) }
    val shownMatch = if (currentMatch in searchMatches.indices) currentMatch else -1
    val searchStatus = when {
        searchTooShort -> SearchStatus.TooShort
        searchedQuery.isEmpty() || searchedQuery != searchQuery -> SearchStatus.Idle
        searchMatches.isEmpty() -> SearchStatus.NoMatches
        else -> SearchStatus.Found(shownMatch + 1, searchMatches.size)
    }
    val closeSearch: () -> Unit = {
        searchOpen = false
        searchedQuery = ""
        currentMatch = -1
        searchTooShort = false
        if (sidePanel == SidePanel.Search) sidePanel = SidePanel.Files
    }
    val openSearch: () -> Unit = {
        searchOpen = true
        sidePanel = SidePanel.Search
        searchFocusRequests++
    }
    /** Selects match [index] of [matches] and scrolls editor (and preview when it is not driven by sync) to it. */
    val selectMatch: (List<IntRange>, Int) -> Unit = { matches, index ->
        currentMatch = index
        matches.getOrNull(index)?.let { range ->
            // The match becomes the editor selection, so typing replaces it like in any editor.
            selection = TextRange(range.first, range.last + 1)
            composition = null
            revealOffset = range.first
            revealRequests++
            if (!syncScroll || workspaceMode == WorkspaceMode.Preview) previewRevealRequests++
        }
    }
    val runSearch: (Boolean) -> Unit = run@{ forward ->
        if (searchQuery.length < MIN_SEARCH_LENGTH) {
            searchTooShort = true
            return@run
        }
        searchTooShort = false
        val again = searchQuery == searchedQuery && currentMatch in searchMatches.indices
        val matches = if (again) searchMatches else findMatches(content, searchQuery)
        val next = if (again) stepMatch(matches, currentMatch, forward) else firstMatchFrom(matches, selection.min)
        searchedQuery = searchQuery
        selectMatch(matches, next)
    }
    val searchColors = colors.accent.copy(alpha = 0.18f) to colors.accent.copy(alpha = 0.5f)
    val previewHitAnchor = remember { HitAnchor() }
    val previewSearch = if (searchOpen && searchedQuery.isNotEmpty()) {
        PreviewSearch(searchedQuery, shownMatch, searchColors.first, searchColors.second, previewHitAnchor)
    } else null
    // Scrolls the editor so [revealOffset] sits in the upper third of the viewport.
    // Waits one frame so the new selection/highlight is laid out first.
    LaunchedEffect(revealRequests) {
        if (revealRequests == 0) return@LaunchedEffect
        withFrameNanos { }
        if (workspaceMode == WorkspaceMode.Preview) {
            // No editor on screen: an outline jump places the preview proportionally (search hits scroll it themselves).
            if (previewSearch == null && content.isNotEmpty()) {
                val ratio = (caretPosition(content, revealOffset).first - 1).toFloat() / content.lines().size.coerceAtLeast(1)
                previewScroll.animateScrollTo((previewScroll.maxValue * ratio).roundToInt())
            }
            return@LaunchedEffect
        }
        val layout = editorLayout ?: return@LaunchedEffect
        val length = layout.layoutInput.text.length
        if (length == 0 || revealOffset !in 0..length) return@LaunchedEffect
        val top = layout.getBoundingBox(revealOffset.coerceAtMost(length - 1)).top
        editorScroll.animateScrollTo((top - editorScroll.viewportSize / 3f).roundToInt().coerceAtLeast(0))
    }
    val outline = remember(content) { documentOutline(content) }
    val revealOutline: (OutlineEntry) -> Unit = { entry ->
        // Caret at the end of the heading line, ready to keep writing.
        selection = TextRange(content.indexOf('\n', entry.offset).let { if (it < 0) content.length else it })
        composition = null
        revealOffset = entry.offset
        revealRequests++
    }

    // --- Action registry: one declaration per command, projected on menu, bars, palette, shortcuts ---
    val hasDocument = documents.isNotEmpty()
    val showPanel: (SidePanel) -> Unit = { panel ->
        if (panel == SidePanel.Search) openSearch() else {
            if (searchOpen) closeSearch()
            sidePanel = panel
        }
    }
    /** Rail click: opens the panel, or collapses it when it is already the active one. */
    val togglePanel: (SidePanel) -> Unit = { panel ->
        if (sidePanel == panel) {
            if (searchOpen) closeSearch()
            sidePanel = null
        } else showPanel(panel)
    }
    val zoomBy: (Int) -> Unit = { step -> zoom = ((zoom * 10).roundToInt() + step).coerceIn(7, 20) / 10f }
    val stepTab: (Int) -> Unit = { delta ->
        if (documents.size > 1) selectDocument((selectedIndex + delta + documents.size) % documents.size)
    }
    val format: (ActionId, FormatAction, KomdShortcut?) -> KomdAction = { id, action, shortcut ->
        KomdAction(
            id, action.label(ui), KomdMenu.Format,
            section = when (action) {
                FormatAction.H1, FormatAction.H2, FormatAction.H3 -> 0
                FormatAction.Bold, FormatAction.Italic, FormatAction.Strikethrough, FormatAction.InlineCode, FormatAction.Link -> 1
                else -> 2
            },
            shortcut = shortcut, icon = action.icon, enabled = hasDocument
        ) { applyFormat(action) }
    }
    val actions = ActionRegistry(buildList {
        // File
        add(KomdAction(ActionId.NewDocument, ui.newDocument, KomdMenu.File, 0, KomdShortcut(Key.N, "N"), KomdIcons.Plus, run = newDocument))
        add(KomdAction(ActionId.Open, ui.openFile + "…", KomdMenu.File, 0, KomdShortcut(Key.O, "O"), KomdIcons.Folder, run = openFile))
        add(KomdAction(ActionId.Close, ui.closeDocument, KomdMenu.File, 1, KomdShortcut(Key.W, "W"), KomdIcons.Close, run = closeCurrent))
        add(KomdAction(ActionId.Save, ui.save, KomdMenu.File, 1, KomdShortcut(Key.S, "S"), KomdIcons.Save, enabled = hasDocument) { saveFile(true) })
        add(KomdAction(ActionId.SaveAs, ui.saveAs, KomdMenu.File, 1, KomdShortcut(Key.S, "S", shift = true), KomdIcons.Save, enabled = hasDocument) { saveFile(false) })
        add(KomdAction(ActionId.ExportHtml, ui.exportHtml, KomdMenu.File, 2, KomdShortcut(Key.E, "E", shift = true), KomdIcons.Share, enabled = hasDocument, run = exportHtml))
        if (pdfExportSupported) {
            add(KomdAction(ActionId.ExportPdf, ui.exportPdf, KomdMenu.File, 2, KomdShortcut(Key.P, "P", shift = true), KomdIcons.Pdf, enabled = hasDocument, run = exportPdf))
        }
        add(KomdAction(ActionId.Settings, ui.settings + "…", KomdMenu.File, 3, KomdShortcut(Key.Comma, ","), KomdIcons.Gear) { showSettings = true })
        // Edit
        add(KomdAction(ActionId.Find, ui.find, KomdMenu.Edit, 0, KomdShortcut(Key.F, "F"), KomdIcons.Search, enabled = hasDocument, run = openSearch))
        // Format
        add(format(ActionId.H1, FormatAction.H1, KomdShortcut(Key.One, "1")))
        add(format(ActionId.H2, FormatAction.H2, KomdShortcut(Key.Two, "2")))
        add(format(ActionId.H3, FormatAction.H3, KomdShortcut(Key.Three, "3")))
        add(format(ActionId.Bold, FormatAction.Bold, KomdShortcut(Key.B, "B")))
        add(format(ActionId.Italic, FormatAction.Italic, KomdShortcut(Key.I, "I")))
        add(format(ActionId.Strikethrough, FormatAction.Strikethrough, KomdShortcut(Key.X, "X", shift = true)))
        add(format(ActionId.InlineCode, FormatAction.InlineCode, KomdShortcut(Key.E, "E")))
        add(format(ActionId.Link, FormatAction.Link, KomdShortcut(Key.L, "L")))
        add(format(ActionId.BulletList, FormatAction.BulletList, null))
        add(format(ActionId.NumberedList, FormatAction.NumberedList, null))
        add(format(ActionId.TaskList, FormatAction.TaskList, null))
        add(format(ActionId.Quote, FormatAction.Quote, null))
        add(KomdAction(ActionId.InsertTable, ui.table, KomdMenu.Format, 3, icon = KomdIcons.Table, enabled = hasDocument) {
            insertAtCursor("\n| ${ui.column} 1 | ${ui.column} 2 |\n| --- | --- |\n| ${ui.value} | ${ui.value} |\n")
        })
        add(KomdAction(ActionId.InsertImage, ui.insertImage + "…", KomdMenu.Format, 3, icon = KomdIcons.Image, enabled = hasDocument) { showImageOptions = true })
        add(KomdAction(ActionId.InsertCodeBlock, ui.codeBlock, KomdMenu.Format, 3, icon = KomdIcons.CodeBlock, enabled = hasDocument) {
            insertAtCursor("\n```\n${ui.code}\n```\n")
        })
        add(KomdAction(ActionId.InsertCustomLink, ui.customLink + "…", KomdMenu.Format, 3, icon = KomdIcons.Link, enabled = hasDocument) { showCustomLinkDialog = true })
        // View
        add(KomdAction(ActionId.CommandPalette, ui.commandPalette, KomdMenu.View, 0, KomdShortcut(Key.K, "K"), KomdIcons.Command) { paletteOpen = true })
        add(KomdAction(ActionId.ToggleSidebar, ui.toggleSidebar, KomdMenu.View, 0, KomdShortcut(Key.Backslash, "\\"), KomdIcons.Sidebar) {
            if (sidePanel == null) sidePanel = SidePanel.Files else togglePanel(sidePanel!!)
        })
        add(KomdAction(ActionId.ShowFiles, ui.panelFiles, KomdMenu.View, 1, icon = KomdIcons.File) { showPanel(SidePanel.Files) })
        add(KomdAction(ActionId.ShowOutline, ui.panelOutline, KomdMenu.View, 1, icon = KomdIcons.Outline, enabled = hasDocument) { showPanel(SidePanel.Outline) })
        add(KomdAction(ActionId.ShowExport, ui.panelExport, KomdMenu.View, 1, icon = KomdIcons.Share) { showPanel(SidePanel.Export) })
        add(KomdAction(ActionId.ViewEditor, ui.editor, KomdMenu.View, 2, checked = workspaceMode == WorkspaceMode.Editor) { workspaceMode = WorkspaceMode.Editor })
        add(KomdAction(ActionId.ViewSplit, ui.split, KomdMenu.View, 2, icon = KomdIcons.Sidebar, checked = workspaceMode == WorkspaceMode.Split) { workspaceMode = WorkspaceMode.Split })
        add(KomdAction(ActionId.ViewPreview, ui.preview, KomdMenu.View, 2, icon = KomdIcons.Eye, checked = workspaceMode == WorkspaceMode.Preview) { workspaceMode = WorkspaceMode.Preview })
        add(KomdAction(ActionId.ToggleSyncScroll, ui.syncedScroll, KomdMenu.View, 3, checked = syncScroll) { syncScroll = !syncScroll })
        add(KomdAction(ActionId.ZoomIn, ui.zoomIn, KomdMenu.View, 4, KomdShortcut(Key.Equals, "+")) { zoomBy(1) })
        add(KomdAction(ActionId.ZoomOut, ui.zoomOut, KomdMenu.View, 4, KomdShortcut(Key.Minus, "-")) { zoomBy(-1) })
        add(KomdAction(ActionId.ZoomReset, ui.zoomReset, KomdMenu.View, 4, KomdShortcut(Key.Zero, "0")) { zoom = 1f })
        add(KomdAction(ActionId.ToggleTheme, ui.toggleTheme, KomdMenu.View, 5, icon = if (darkTheme) KomdIcons.Sun else KomdIcons.Moon, checked = darkTheme) {
            onDarkThemeChange(!darkTheme)
        })
        add(KomdAction(ActionId.NextTab, ui.nextTab, KomdMenu.View, 6, KomdShortcut(Key.RightBracket, "]", shift = true), enabled = documents.size > 1) { stepTab(1) })
        add(KomdAction(ActionId.PreviousTab, ui.previousTab, KomdMenu.View, 6, KomdShortcut(Key.LeftBracket, "[", shift = true), enabled = documents.size > 1) { stepTab(-1) })
    })

    if (commands != null) {
        val menus = actions.menus { menu ->
            when (menu) {
                KomdMenu.File -> ui.menuFile
                KomdMenu.Edit -> ui.menuEdit
                KomdMenu.Format -> ui.menuFormat
                KomdMenu.View -> ui.menuView
            }
        }
        SideEffect {
            commands.menus = menus
            commands.exitHandler = requestExit
        }
        DisposableEffect(commands) { onDispose { commands.exitHandler = null } }
    }

    val tabs = documents.mapIndexed { index, document -> TabItem(document.name, isDirty(index), document.file?.path) }
    val currentDocument = documents.getOrNull(selectedIndex)
    val editorPane: @Composable (Modifier) -> Unit = { paneModifier ->
        EditorPane(
            TextFieldValue(content, selection, composition),
            { newValue ->
                val textChanged = newValue.text != content
                content = newValue.text
                selection = newValue.selection
                composition = newValue.composition
                if (textChanged) documents = syncCurrent(documents)
            },
            zoom, { zoom = it }, editorScroll, paneModifier,
            highlight = if (previewSearch != null) {
                SearchHighlightTransformation(searchMatches, shownMatch, searchColors.first, searchColors.second)
            } else VisualTransformation.None,
            onTextLayout = { editorLayout = it }
        )
    }
    // Checkbox click in the preview: flip "[ ]" <-> "[x]" on that source line. Same length,
    // so the editor selection stays valid; editor and preview both re-read `content`.
    val toggleTask: (Int) -> Unit = { line ->
        toggleTaskAt(content, line)?.let { updated ->
            content = updated
            documents = syncCurrent(documents)
        }
    }
    val previewPane: @Composable (Modifier) -> Unit = { paneModifier ->
        PreviewPane(content, documentDirectory(), ui, zoom, { zoom = it }, previewScroll, toggleTask, paneModifier, previewSearch, previewRevealRequests)
    }
    val searchField: @Composable (Modifier) -> Unit = { fieldModifier ->
        DocumentSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it; searchTooShort = false },
            onSubmit = runSearch,
            onClose = closeSearch,
            status = searchStatus,
            focusRequests = searchFocusRequests,
            modifier = fieldModifier
        )
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(colors.background).onPreviewKeyEvent { event ->
            // With a shell-provided command bridge the menu accelerators handle the shortcuts.
            if (commands != null) return@onPreviewKeyEvent false
            actions.handleKey(event)
        }
    ) {
        val wide = maxWidth >= 900.dp
        val panelOpen = wide && sidePanel != null
        val splitWidthPx = with(LocalDensity.current) { (maxWidth - if (panelOpen) 288.dp else 52.dp).toPx() }
        val workspace: @Composable ColumnScope.() -> Unit = {
            when {
                currentDocument == null -> EmptyWorkspace(ui, onNew = newDocument, onOpen = openFile, modifier = Modifier.weight(1f).fillMaxWidth())
                wide && workspaceMode == WorkspaceMode.Split -> Row(Modifier.weight(1f).fillMaxWidth()) {
                    editorPane(Modifier.weight(splitRatio).fillMaxHeight())
                    Splitter(availableWidthPx = splitWidthPx) { delta -> splitRatio = (splitRatio + delta).coerceIn(0.2f, 0.8f) }
                    previewPane(Modifier.weight(1f - splitRatio).fillMaxHeight())
                }
                workspaceMode == WorkspaceMode.Preview -> previewPane(Modifier.weight(1f).fillMaxWidth())
                else -> editorPane(Modifier.weight(1f).fillMaxWidth())
            }
        }

        if (wide) {
            Column(Modifier.fillMaxSize()) {
                TitleBar(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onSelect = selectDocument,
                    onClose = closeDocumentAt,
                    onNew = newDocument,
                    paletteLabel = ui.searchOrRun,
                    paletteShortcut = actions[ActionId.CommandPalette].shortcut?.label(),
                    onPalette = { paletteOpen = true },
                    mode = workspaceMode,
                    onModeChange = { workspaceMode = it },
                )
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Rail(
                        active = sidePanel,
                        onSelect = togglePanel,
                        themeAction = actions[ActionId.ToggleTheme],
                        settingsAction = actions[ActionId.Settings],
                    )
                    if (panelOpen) {
                        Column(Modifier.width(236.dp).fillMaxHeight().background(colors.sidebar).endBorder(colors.divider)) {
                            when (sidePanel) {
                                SidePanel.Files -> FilesPanel(
                                    newAction = actions[ActionId.NewDocument],
                                    tabs = tabs,
                                    selectedIndex = selectedIndex,
                                    onSelect = selectDocument,
                                    recentFiles = recentFiles,
                                    openPaths = documents.mapNotNull { it.file?.path }.toSet(),
                                    onOpenRecent = openRecent,
                                )
                                SidePanel.Search -> SearchPanel(
                                    searchField = searchField,
                                    content = content,
                                    matches = if (searchedQuery.isNotEmpty() && searchedQuery == searchQuery) searchMatches else emptyList(),
                                    current = shownMatch,
                                    onSelect = { selectMatch(searchMatches, it) },
                                )
                                SidePanel.Outline -> OutlinePanel(outline, selection.start, onSelect = revealOutline)
                                SidePanel.Export -> ExportPanel(
                                    listOfNotNull(
                                        actions[ActionId.ExportHtml] to ui.exportHtmlHint,
                                        if (pdfExportSupported) actions[ActionId.ExportPdf] to ui.exportPdfHint else null,
                                    ),
                                    listOf(actions[ActionId.Save], actions[ActionId.SaveAs])
                                )
                                null -> Unit
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (currentDocument != null) {
                            FormatBar(
                                actions = actions,
                                currentHeading = headingLevelAt(content, selection.start),
                                crumb = listOfNotNull(
                                    documentDirectory()?.trimEnd('/', '\\')?.substringAfterLast('/')?.substringAfterLast('\\')?.ifEmpty { null },
                                    currentDocument.name
                                ).joinToString("  /  "),
                            )
                        }
                        workspace()
                        StatusBar(
                            dirty = currentDocument != null && isDirty(selectedIndex),
                            hasFile = currentDocument?.file != null,
                            content = content,
                            caret = selection.start,
                            message = fileMessage,
                            syncScroll = syncScroll,
                            onToggleSync = { syncScroll = !syncScroll },
                            zoom = zoom,
                            onResetZoom = { zoom = 1f },
                        )
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                CompactTopBar(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onSelect = selectDocument,
                    onClose = closeDocumentAt,
                    onNew = newDocument,
                    actions = actions,
                    previewActive = workspaceMode == WorkspaceMode.Preview,
                    onPreviewToggle = {
                        workspaceMode = if (workspaceMode == WorkspaceMode.Preview) WorkspaceMode.Editor else WorkspaceMode.Preview
                    },
                    searchOpen = searchOpen,
                    onSearchToggle = { if (searchOpen) closeSearch() else { searchOpen = true; searchFocusRequests++ } },
                )
                if (searchOpen) {
                    Box(Modifier.fillMaxWidth().background(colors.chrome).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        searchField(Modifier.fillMaxWidth())
                    }
                }
                fileMessage?.let {
                    Text(it, Modifier.fillMaxWidth().background(colors.chrome).padding(horizontal = 16.dp, vertical = 6.dp), color = colors.secondary, fontSize = 12.sp)
                }
                workspace()
                if (currentDocument != null && workspaceMode != WorkspaceMode.Preview) CompactFormatBar(actions)
            }
        }

        if (paletteOpen) {
            val entries = buildList {
                actions.all.filter { it.enabled && it.id != ActionId.CommandPalette }.forEach { action ->
                    add(PaletteEntry(action.label, icon = action.icon, shortcut = action.shortcut?.label(), run = action.run))
                }
                documents.forEachIndexed { index, document ->
                    if (index != selectedIndex) add(PaletteEntry(document.name, ui.goTo, KomdIcons.File) { selectDocument(index) })
                }
                val openPaths = documents.mapNotNull { it.file?.path }.toSet()
                recentFiles.filter { it.path !in openPaths }.forEach { recent ->
                    add(PaletteEntry(recent.name, parentDirectory(recent.path), KomdIcons.Clock) { openRecent(recent) })
                }
            }
            CommandPalette(entries, ui.searchOrRun, ui.noMatches, onDismiss = { paletteOpen = false })
        }

        if (showImageOptions) {
            AlertDialog(
                onDismissRequest = { showImageOptions = false },
                title = { Text(ui.insertImage) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ui.imageIntro)
                        ImageAlignmentPicker(imageAlignment, ui, onChange = { imageAlignment = it })
                    }
                },
                confirmButton = { TextButton(onClick = { showImageOptions = false; imagePicker.launch() }) { Text(ui.chooseFile) } },
                dismissButton = { TextButton(onClick = { showImageOptions = false; showImageUrlDialog = true }) { Text(ui.internetUrl) } }
            )
        }

        if (showImageUrlDialog) {
            AlertDialog(
                onDismissRequest = { showImageUrlDialog = false },
                title = { Text(ui.imageFromUrl) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = imageAlt, onValueChange = { imageAlt = it }, label = { Text(ui.description) }, singleLine = true)
                        OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text(ui.imageUrl) }, placeholder = { Text("https://…") }, singleLine = true)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val url = imageUrl.trim()
                        if (url.startsWith("https://") || url.startsWith("http://")) {
                            insertImage(imageAlt.ifBlank { ui.image }, url)
                            imageUrl = ""
                            fileMessage = ui.imageLinkAdded
                            showImageUrlDialog = false
                        }
                    }) { Text(ui.insert) }
                },
                dismissButton = { TextButton(onClick = { showImageUrlDialog = false }) { Text(ui.cancel) } }
            )
        }

        if (showCustomLinkDialog) {
            AlertDialog(
                onDismissRequest = { showCustomLinkDialog = false },
                title = { Text(ui.customLink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customLinkText,
                            onValueChange = { customLinkText = it },
                            label = { Text(ui.linkTextOptional) },
                            placeholder = { Text(ui.documentationExample) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customLinkUrl,
                            onValueChange = { customLinkUrl = it },
                            label = { Text(ui.webAddress) },
                            placeholder = { Text("https://…") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showCustomLinkDialog = false; linkPicker.launch() }) { Text(ui.localFile) }
                        TextButton(
                            enabled = customLinkUrl.isNotBlank(),
                            onClick = {
                                val target = customLinkUrl.trim()
                                insertLink(customLinkText.ifBlank { target }, target)
                                customLinkText = ""
                                customLinkUrl = ""
                                showCustomLinkDialog = false
                                fileMessage = ui.linkAdded
                            }
                        ) { Text(ui.insertUrl) }
                    }
                },
                dismissButton = { TextButton(onClick = { showCustomLinkDialog = false }) { Text(ui.cancel) } }
            )
        }

        pendingCloseIndex?.let { index ->
            val name = documents.getOrNull(index)?.name ?: ""
            AlertDialog(
                onDismissRequest = { pendingCloseIndex = null },
                title = { Text(ui.unsavedTitle) },
                text = { Text(ui.unsavedMessage.replace("{name}", name)) },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { pendingCloseIndex = null; removeDocument(index) }) { Text(ui.dontSave) }
                        TextButton(onClick = {
                            pendingCloseIndex = null
                            // Save may go through "Save as": the document is closed only if the save succeeds.
                            saveDocument(true) { removeDocument(index) }
                        }) { Text(ui.save) }
                    }
                },
                dismissButton = { TextButton(onClick = { pendingCloseIndex = null }) { Text(ui.cancel) } }
            )
        }

        if (showExitConfirm) {
            val dirtyCount = documents.indices.count(isDirty)
            AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                title = { Text(ui.unsavedTitle) },
                text = { Text(ui.exitUnsavedMessage.replace("{count}", dirtyCount.toString())) },
                confirmButton = { TextButton(onClick = { showExitConfirm = false; onExit() }) { Text(ui.exitWithoutSaving) } },
                dismissButton = { TextButton(onClick = { showExitConfirm = false }) { Text(ui.cancel) } }
            )
        }

        missingRecent?.let { entry ->
            val dismiss = {
                updateRecentFiles { it.withoutRecent(entry.path) }
                missingRecent = null
            }
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text(ui.missingFileTitle) },
                text = { Text(ui.missingFileMessage.replace("{name}", entry.name).replace("{path}", entry.path)) },
                confirmButton = { TextButton(onClick = dismiss) { Text(ui.ok) } }
            )
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                containerColor = colors.background,
                titleContentColor = colors.foreground,
                textContentColor = colors.foreground,
                title = { Text(ui.settingsTitle) },
                text = {
                    Row(Modifier.fillMaxWidth().height(290.dp)) {
                        Column(
                            Modifier.width(126.dp).fillMaxHeight().background(colors.sidebar).padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            SettingsMenuRow(ui.languages, settingsSection == SettingsSection.Languages) { settingsSection = SettingsSection.Languages }
                            SettingsMenuRow(ui.theme, settingsSection == SettingsSection.Appearance) { settingsSection = SettingsSection.Appearance }
                            SettingsMenuRow(ui.text, settingsSection == SettingsSection.Text) { settingsSection = SettingsSection.Text }
                        }
                        Box(Modifier.fillMaxHeight().width(1.dp).background(colors.divider))
                        Column(
                            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 4.dp, top = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            when (settingsSection) {
                                SettingsSection.Languages -> {
                                    Text(ui.languageLabel, color = colors.secondary, fontSize = 11.sp)
                                    UiLanguage.entries.forEach { option ->
                                        SettingsOptionRow(option.nativeName, language == option) { changeLanguage(option) }
                                    }
                                }
                                SettingsSection.Appearance -> {
                                    Text(ui.theme, color = colors.secondary, fontSize = 11.sp)
                                    SettingsOptionRow(ui.light, !darkTheme) { onDarkThemeChange(false) }
                                    SettingsOptionRow(ui.dark, darkTheme) { onDarkThemeChange(true) }
                                }
                                SettingsSection.Text -> {
                                    Text(ui.textSize, color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(ui.textSizeHint, color = colors.secondary, fontSize = 11.sp, lineHeight = 16.sp)
                                    InterfaceScaleControl(interfaceScale, onInterfaceScaleChange)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text(ui.done, color = colors.accent) } }
            )
        }
    }
}

/** Heading level (1-6) of the line holding [offset], or 0 for body text. Drives the "H▾" button. */
internal fun headingLevelAt(text: String, offset: Int): Int {
    val safe = offset.coerceIn(0, text.length)
    val start = text.lastIndexOf('\n', safe - 1) + 1
    var hashes = 0
    while (start + hashes < text.length && text[start + hashes] == '#') hashes++
    return if (hashes in 1..6 && text.getOrNull(start + hashes) == ' ') hashes else 0
}

@Composable
private fun ImageAlignmentPicker(selected: ImageAlignment, ui: AppStrings, onChange: (ImageAlignment) -> Unit) {
    Column {
        Text(ui.imageAlignment, color = KomdTheme.colors.secondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            ImageAlignment.entries.forEach { alignment ->
                TextButton(onClick = { onChange(alignment) }) {
                    val label = imageAlignmentLabel(alignment, ui)
                    Text(if (alignment == selected) "✓ $label" else label, fontSize = 12.sp)
                }
            }
        }
    }
}
