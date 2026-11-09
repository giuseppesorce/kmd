package komd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.launch

internal data class KomdPalette(
    val background: Color,
    val sidebar: Color,
    val toolbar: Color,
    val divider: Color,
    val foreground: Color,
    val secondary: Color,
    val accent: Color,
    val editorText: Color,
    val selectedDocument: Color,
    val button: Color,
)

private val darkPalette = KomdPalette(
    background = Color(0xFF071526), sidebar = Color(0xFF0A1B30), toolbar = Color(0xFF0B1B2E),
    divider = Color(0xFF1C3149), foreground = Color(0xFFE7EDF7), secondary = Color(0xFF91A4BC),
    accent = Color(0xFFB8A3FF), editorText = Color(0xFFDCE5F1), selectedDocument = Color(0xFF172E49),
    button = Color(0xFF142A42)
)
private val lightPalette = KomdPalette(
    background = Color(0xFFFDF9F3), sidebar = Color(0xFFF7F0E7), toolbar = Color(0xFFFFFCF7),
    divider = Color(0xFFE9DDD0), foreground = Color(0xFF382F2A), secondary = Color(0xFF8A7568),
    accent = Color(0xFFE06D44), editorText = Color(0xFF453A34), selectedDocument = Color(0xFFF5E2D5),
    button = Color(0xFFF3E9DE)
)

private fun paperPalette(accent: Color) = KomdPalette(
    background = Color.White,
    sidebar = Color.White,
    toolbar = Color.White,
    divider = Color(0xFFE6E6E6),
    foreground = Color(0xFF1C1C1C),
    secondary = Color(0xFF6B7280),
    accent = accent,
    editorText = Color(0xFF1C1C1C),
    selectedDocument = Color(0xFFF4F4F5),
    button = Color(0xFFF4F4F5)
)
private data class AccentOption(val name: String, val color: Color)

private fun lightAccentOptions(strings: AppStrings) = listOf(
    AccentOption(strings.colorMandarin, Color(0xFFE06D44)),
    AccentOption(strings.colorApricot, Color(0xFFF08A62)),
    AccentOption(strings.colorPeach, Color(0xFFD98B72)),
    AccentOption(strings.colorTerracotta, Color(0xFFB96B4E)),
    AccentOption(strings.colorRose, Color(0xFFC87578)),
    AccentOption(strings.colorHoney, Color(0xFFC48A3A)),
    AccentOption(strings.colorSage, Color(0xFF7D9270)),
)
internal val LocalKomdPalette = staticCompositionLocalOf { darkPalette }

private data class Document(val name: String, val content: String, val file: PlatformFile? = null)

private enum class UiLanguage(val nativeName: String) {
    Italian("Italiano"), English("English"), French("Français"), Spanish("Español"), German("Deutsch")
}
private enum class SettingsSection { Languages, Colors, Text }
internal enum class ImageAlignment { Left, Center, Right }
private enum class WorkspaceMode { Split, Editor, Preview }

internal data class AppStrings(
    val tagline: String,
    val newDocument: String,
    val openFile: String,
    val save: String,
    val documents: String,
    val quickElements: String,
    val table: String,
    val image: String,
    val link: String,
    val customLink: String,
    val code: String,
    val list: String,
    val quote: String,
    val settings: String,
    val commandSearch: String,
    val preview: String,
    val activeSession: String,
    val split: String,
    val editor: String,
    val line: String,
    val sourceMarkdown: String,
    val monoFont: String,
    val words: String,
    val characters: String,
    val localDraft: String,
    val commonMark: String,
    val livePreview: String,
    val syncedScroll: String,
    val gfmRender: String,
    val document: String,
    val updatedNow: String,
    val insertImage: String,
    val imageIntro: String,
    val chooseFile: String,
    val internetUrl: String,
    val imageFromUrl: String,
    val description: String,
    val imageUrl: String,
    val insert: String,
    val cancel: String,
    val linkTextOptional: String,
    val documentationExample: String,
    val webAddress: String,
    val localFile: String,
    val insertUrl: String,
    val exampleText: String,
    val settingsTitle: String,
    val languages: String,
    val colors: String,
    val text: String,
    val textSize: String,
    val textSizeHint: String,
    val copyCode: String,
    val copied: String,
    val languageLabel: String,
    val theme: String,
    val dark: String,
    val light: String,
    val accentColor: String,
    val accentDescription: String,
    val darkFixed: String,
    val done: String,
    val imageAlignment: String,
    val left: String,
    val center: String,
    val right: String,
    val openFailure: String,
    val unknownError: String,
    val opened: String,
    val readFailure: String,
    val saveFailure: String,
    val saved: String,
    val imageFailure: String,
    val imageAdded: String,
    val filePickerFailure: String,
    val fileLinkAdded: String,
    val imageLinkAdded: String,
    val linkAdded: String,
    val imageUnsupported: String,
    val column: String,
    val value: String,
    val listItem: String,
    val quoteExample: String,
    val hello: String,
    val welcomeTitle: String,
    val welcomeIntro: String,
    val welcomeSection: String,
    val welcomeDirections: String,
    val welcomeBullets: List<String>,
    val welcomeQuote: String,
    val notesName: String,
    val websiteName: String,
    val draftName: String,
    val websiteTitle: String,
    val websiteDraft: String,
    val ideasTitle: String,
    val ideasPrompt: String,
    val untitledName: String,
    val untitledTitle: String,
    val colorMandarin: String,
    val colorApricot: String,
    val colorPeach: String,
    val colorTerracotta: String,
    val colorRose: String,
    val colorHoney: String,
    val colorSage: String,
)

private val italianStrings = AppStrings(
    tagline = "Editor Markdown", newDocument = "Nuovo documento", openFile = "Apri file", save = "Salva",
    documents = "DOCUMENTI", quickElements = "ELEMENTI RAPIDI", table = "Tabella", image = "Immagine", link = "Link",
    customLink = "Custom Link", code = "Codice", list = "Elenco", quote = "Citazione", settings = "Impostazioni",
    commandSearch = "Cerca o esegui comando…", preview = "Anteprima", activeSession = "SESSIONE ATTIVA", split = "Diviso",
    editor = "Editor", line = "Riga", sourceMarkdown = "SORGENTE MD", monoFont = "Geist Mono", words = "parole", characters = "caratteri",
    localDraft = "Bozza locale", commonMark = "CommonMark", livePreview = "ANTEPRIMA LIVE", syncedScroll = "Scroll sincronizzato",
    gfmRender = "GFM Render", document = "DOCUMENTO KMD", updatedNow = "Aggiornato ora", insertImage = "Inserisci immagine",
    imageIntro = "Scegli un file dal dispositivo oppure inserisci un URL internet.", chooseFile = "Scegli file…", internetUrl = "URL internet",
    imageFromUrl = "Immagine da URL", description = "Descrizione", imageUrl = "URL immagine", insert = "Inserisci", cancel = "Annulla",
    linkTextOptional = "Testo del link (facoltativo)", documentationExample = "es. Documentazione", webAddress = "Indirizzo web",
    localFile = "File locale", insertUrl = "Inserisci URL", exampleText = "testo", settingsTitle = "Impostazioni", languages = "Lingue", colors = "Colori",
    text = "Testo", textSize = "Dimensione del testo", textSizeHint = "Ingrandisce menu, barre e pulsanti. Editor e anteprima restano regolabili con il loro zoom.",
    copyCode = "Copia", copied = "Copiato",
    languageLabel = "Lingua dell’interfaccia", theme = "Tema", dark = "Scuro", light = "Chiaro", accentColor = "Colore di accento",
    accentDescription = "Personalizza link, cursore ed elementi evidenziati.", darkFixed = "Il tema scuro mantiene i colori del design Stitch.",
    done = "Fine", imageAlignment = "Allineamento", left = "Sinistra", center = "Centro", right = "Destra",
    openFailure = "Impossibile aprire il file: {error}", unknownError = "errore sconosciuto", opened = "Aperto {name}",
    readFailure = "Impossibile leggere il file: {error}", saveFailure = "Impossibile salvare il file: {error}", saved = "Salvato {name}",
    imageFailure = "Impossibile aprire l’immagine: {error}", imageAdded = "Inserito {name}", filePickerFailure = "Impossibile scegliere il file: {error}",
    fileLinkAdded = "Inserito link a {name}", imageLinkAdded = "Inserito link immagine", linkAdded = "Link inserito",
    imageUnsupported = "Immagine non supportata: {source}", column = "Colonna", value = "Valore", listItem = "Elemento", quoteExample = "Citazione",
    hello = "Ciao", welcomeTitle = "Benvenuto in KoMD", welcomeIntro = "Un editor Markdown semplice, pensato per scrivere con calma.",
    welcomeSection = "Scrivi e visualizza", welcomeDirections = "Modifica il testo qui a sinistra e osserva l’anteprima aggiornarsi in tempo reale.",
    welcomeBullets = listOf("Usa i pulsanti rapidi per inserire elementi", "Scrivi in Markdown", "Concentrati sul contenuto"),
    welcomeQuote = "KoMD — Markdown, senza distrazioni.", notesName = "Appunti.md", websiteName = "Sito_Web.md", draftName = "Bozza.md",
    websiteTitle = "Sito web", websiteDraft = "Bozza per il prossimo progetto.", ideasTitle = "Idee", ideasPrompt = "Aggiungi qui le tue note.",
    untitledName = "Senza titolo.md", untitledTitle = "Senza titolo", colorMandarin = "Mandarino", colorApricot = "Albicocca",
    colorPeach = "Pesca", colorTerracotta = "Terracotta", colorRose = "Rosa", colorHoney = "Miele", colorSage = "Salvia"
)

private val englishStrings = AppStrings(
    tagline = "Markdown editor", newDocument = "New document", openFile = "Open file", save = "Save",
    documents = "DOCUMENTS", quickElements = "QUICK INSERTS", table = "Table", image = "Image", link = "Link",
    customLink = "Custom link", code = "Code", list = "List", quote = "Quote", settings = "Settings",
    commandSearch = "Search or run a command…", preview = "Preview", activeSession = "ACTIVE SESSION", split = "Split",
    editor = "Editor", line = "Line", sourceMarkdown = "MARKDOWN SOURCE", monoFont = "Geist Mono", words = "words", characters = "characters",
    localDraft = "Local draft", commonMark = "CommonMark", livePreview = "LIVE PREVIEW", syncedScroll = "Synced scrolling",
    gfmRender = "GFM render", document = "KOMD DOCUMENT", updatedNow = "Updated now", insertImage = "Insert image",
    imageIntro = "Choose a file from your device or enter an internet URL.", chooseFile = "Choose file…", internetUrl = "Internet URL",
    imageFromUrl = "Image from URL", description = "Description", imageUrl = "Image URL", insert = "Insert", cancel = "Cancel",
    linkTextOptional = "Link text (optional)", documentationExample = "e.g. Documentation", webAddress = "Web address",
    localFile = "Local file", insertUrl = "Insert URL", exampleText = "text", settingsTitle = "Settings", languages = "Languages", colors = "Colors",
    text = "Text", textSize = "Interface text size", textSizeHint = "Enlarges menus, bars, and buttons. The editor and preview keep their own zoom.",
    copyCode = "Copy", copied = "Copied",
    languageLabel = "Interface language", theme = "Theme", dark = "Dark", light = "Light", accentColor = "Accent color",
    accentDescription = "Customize links, the cursor, and highlighted elements.", darkFixed = "The dark theme keeps the Stitch design colors.",
    done = "Done", imageAlignment = "Alignment", left = "Left", center = "Center", right = "Right",
    openFailure = "Could not open file: {error}", unknownError = "unknown error", opened = "Opened {name}",
    readFailure = "Could not read file: {error}", saveFailure = "Could not save file: {error}", saved = "Saved {name}",
    imageFailure = "Could not open image: {error}", imageAdded = "Inserted {name}", filePickerFailure = "Could not choose file: {error}",
    fileLinkAdded = "Inserted link to {name}", imageLinkAdded = "Inserted image link", linkAdded = "Link inserted",
    imageUnsupported = "Unsupported image: {source}", column = "Column", value = "Value", listItem = "Item", quoteExample = "Quote",
    hello = "Hello", welcomeTitle = "Welcome to KoMD", welcomeIntro = "A simple Markdown editor, made for calm writing.",
    welcomeSection = "Write and preview", welcomeDirections = "Edit the text on the left and watch the preview update in real time.",
    welcomeBullets = listOf("Use quick actions to insert elements", "Write in Markdown", "Focus on your content"),
    welcomeQuote = "KoMD — Markdown, without distractions.", notesName = "Notes.md", websiteName = "Website.md", draftName = "Draft.md",
    websiteTitle = "Website", websiteDraft = "A draft for the next project.", ideasTitle = "Ideas", ideasPrompt = "Add your notes here.",
    untitledName = "Untitled.md", untitledTitle = "Untitled", colorMandarin = "Mandarin", colorApricot = "Apricot",
    colorPeach = "Peach", colorTerracotta = "Terracotta", colorRose = "Rose", colorHoney = "Honey", colorSage = "Sage"
)

private val frenchStrings = AppStrings(
    tagline = "Éditeur Markdown", newDocument = "Nouveau document", openFile = "Ouvrir un fichier", save = "Enregistrer",
    documents = "DOCUMENTS", quickElements = "INSERTIONS RAPIDES", table = "Tableau", image = "Image", link = "Lien",
    customLink = "Lien personnalisé", code = "Code", list = "Liste", quote = "Citation", settings = "Paramètres",
    commandSearch = "Rechercher ou lancer une commande…", preview = "Aperçu", activeSession = "SESSION ACTIVE", split = "Divisé",
    editor = "Éditeur", line = "Ligne", sourceMarkdown = "SOURCE MARKDOWN", monoFont = "Geist Mono", words = "mots", characters = "caractères",
    localDraft = "Brouillon local", commonMark = "CommonMark", livePreview = "APERÇU EN DIRECT", syncedScroll = "Défilement synchronisé",
    gfmRender = "Rendu GFM", document = "DOCUMENT KMD", updatedNow = "Mis à jour", insertImage = "Insérer une image",
    imageIntro = "Choisissez un fichier sur votre appareil ou saisissez une URL Internet.", chooseFile = "Choisir un fichier…", internetUrl = "URL Internet",
    imageFromUrl = "Image depuis une URL", description = "Description", imageUrl = "URL de l’image", insert = "Insérer", cancel = "Annuler",
    linkTextOptional = "Texte du lien (facultatif)", documentationExample = "ex. Documentation", webAddress = "Adresse Web",
    localFile = "Fichier local", insertUrl = "Insérer l’URL", exampleText = "texte", settingsTitle = "Paramètres", languages = "Langues", colors = "Couleurs",
    text = "Texte", textSize = "Taille du texte", textSizeHint = "Agrandit les menus, les barres et les boutons. L’éditeur et l’aperçu gardent leur zoom.",
    copyCode = "Copier", copied = "Copié",
    languageLabel = "Langue de l’interface", theme = "Thème", dark = "Sombre", light = "Clair", accentColor = "Couleur d’accentuation",
    accentDescription = "Personnalisez les liens, le curseur et les éléments mis en évidence.", darkFixed = "Le thème sombre conserve les couleurs du design Stitch.",
    done = "Terminé", imageAlignment = "Alignement", left = "Gauche", center = "Centre", right = "Droite",
    openFailure = "Impossible d’ouvrir le fichier : {error}", unknownError = "erreur inconnue", opened = "Ouvert : {name}",
    readFailure = "Impossible de lire le fichier : {error}", saveFailure = "Impossible d’enregistrer le fichier : {error}", saved = "Enregistré : {name}",
    imageFailure = "Impossible d’ouvrir l’image : {error}", imageAdded = "Inséré : {name}", filePickerFailure = "Impossible de choisir le fichier : {error}",
    fileLinkAdded = "Lien vers {name} inséré", imageLinkAdded = "Lien d’image inséré", linkAdded = "Lien inséré",
    imageUnsupported = "Image non prise en charge : {source}", column = "Colonne", value = "Valeur", listItem = "Élément", quoteExample = "Citation",
    hello = "Bonjour", welcomeTitle = "Bienvenue dans KoMD", welcomeIntro = "Un éditeur Markdown simple, conçu pour écrire sereinement.",
    welcomeSection = "Écrire et prévisualiser", welcomeDirections = "Modifiez le texte à gauche et voyez l’aperçu se mettre à jour en temps réel.",
    welcomeBullets = listOf("Utilisez les raccourcis pour insérer des éléments", "Écrivez en Markdown", "Concentrez-vous sur votre contenu"),
    welcomeQuote = "KoMD — Markdown, sans distraction.", notesName = "Notes.md", websiteName = "Site_Web.md", draftName = "Brouillon.md",
    websiteTitle = "Site web", websiteDraft = "Brouillon pour le prochain projet.", ideasTitle = "Idées", ideasPrompt = "Ajoutez vos notes ici.",
    untitledName = "Sans_titre.md", untitledTitle = "Sans titre", colorMandarin = "Mandarine", colorApricot = "Abricot",
    colorPeach = "Pêche", colorTerracotta = "Terracotta", colorRose = "Rose", colorHoney = "Miel", colorSage = "Sauge"
)

private val spanishStrings = AppStrings(
    tagline = "Editor Markdown", newDocument = "Nuevo documento", openFile = "Abrir archivo", save = "Guardar",
    documents = "DOCUMENTOS", quickElements = "INSERCIONES RÁPIDAS", table = "Tabla", image = "Imagen", link = "Enlace",
    customLink = "Enlace personalizado", code = "Código", list = "Lista", quote = "Cita", settings = "Ajustes",
    commandSearch = "Buscar o ejecutar un comando…", preview = "Vista previa", activeSession = "SESIÓN ACTIVA", split = "Dividido",
    editor = "Editor", line = "Línea", sourceMarkdown = "FUENTE MARKDOWN", monoFont = "Geist Mono", words = "palabras", characters = "caracteres",
    localDraft = "Borrador local", commonMark = "CommonMark", livePreview = "VISTA PREVIA EN VIVO", syncedScroll = "Desplazamiento sincronizado",
    gfmRender = "Renderizado GFM", document = "DOCUMENTO KMD", updatedNow = "Actualizado ahora", insertImage = "Insertar imagen",
    imageIntro = "Elige un archivo del dispositivo o introduce una URL de Internet.", chooseFile = "Elegir archivo…", internetUrl = "URL de Internet",
    imageFromUrl = "Imagen desde URL", description = "Descripción", imageUrl = "URL de la imagen", insert = "Insertar", cancel = "Cancelar",
    linkTextOptional = "Texto del enlace (opcional)", documentationExample = "p. ej., Documentación", webAddress = "Dirección web",
    localFile = "Archivo local", insertUrl = "Insertar URL", exampleText = "texto", settingsTitle = "Ajustes", languages = "Idiomas", colors = "Colores",
    text = "Texto", textSize = "Tamaño del texto", textSizeHint = "Amplía menús, barras y botones. El editor y la vista previa conservan su zoom.",
    copyCode = "Copiar", copied = "Copiado",
    languageLabel = "Idioma de la interfaz", theme = "Tema", dark = "Oscuro", light = "Claro", accentColor = "Color de acento",
    accentDescription = "Personaliza enlaces, el cursor y los elementos resaltados.", darkFixed = "El tema oscuro conserva los colores del diseño Stitch.",
    done = "Listo", imageAlignment = "Alineación", left = "Izquierda", center = "Centro", right = "Derecha",
    openFailure = "No se pudo abrir el archivo: {error}", unknownError = "error desconocido", opened = "Abierto: {name}",
    readFailure = "No se pudo leer el archivo: {error}", saveFailure = "No se pudo guardar el archivo: {error}", saved = "Guardado: {name}",
    imageFailure = "No se pudo abrir la imagen: {error}", imageAdded = "Insertado: {name}", filePickerFailure = "No se pudo elegir el archivo: {error}",
    fileLinkAdded = "Enlace a {name} insertado", imageLinkAdded = "Enlace de imagen insertado", linkAdded = "Enlace insertado",
    imageUnsupported = "Imagen no compatible: {source}", column = "Columna", value = "Valor", listItem = "Elemento", quoteExample = "Cita",
    hello = "Hola", welcomeTitle = "Te damos la bienvenida a KoMD", welcomeIntro = "Un editor Markdown sencillo, pensado para escribir con calma.",
    welcomeSection = "Escribe y visualiza", welcomeDirections = "Edita el texto de la izquierda y observa cómo la vista previa se actualiza en tiempo real.",
    welcomeBullets = listOf("Usa los accesos rápidos para insertar elementos", "Escribe en Markdown", "Concéntrate en el contenido"),
    welcomeQuote = "KoMD — Markdown, sin distracciones.", notesName = "Notas.md", websiteName = "Sitio_Web.md", draftName = "Borrador.md",
    websiteTitle = "Sitio web", websiteDraft = "Borrador para el próximo proyecto.", ideasTitle = "Ideas", ideasPrompt = "Añade aquí tus notas.",
    untitledName = "Sin_título.md", untitledTitle = "Sin título", colorMandarin = "Mandarina", colorApricot = "Albaricoque",
    colorPeach = "Melocotón", colorTerracotta = "Terracota", colorRose = "Rosa", colorHoney = "Miel", colorSage = "Salvia"
)

private val germanStrings = AppStrings(
    tagline = "Markdown-Editor", newDocument = "Neues Dokument", openFile = "Datei öffnen", save = "Speichern",
    documents = "DOKUMENTE", quickElements = "SCHNELLELEMENTE", table = "Tabelle", image = "Bild", link = "Link",
    customLink = "Eigener Link", code = "Code", list = "Liste", quote = "Zitat", settings = "Einstellungen",
    commandSearch = "Suchen oder Befehl ausführen…", preview = "Vorschau", activeSession = "AKTIVE SITZUNG", split = "Geteilt",
    editor = "Editor", line = "Zeile", sourceMarkdown = "MARKDOWN-QUELLE", monoFont = "Geist Mono", words = "Wörter", characters = "Zeichen",
    localDraft = "Lokaler Entwurf", commonMark = "CommonMark", livePreview = "LIVE-VORSCHAU", syncedScroll = "Synchrones Scrollen",
    gfmRender = "GFM-Darstellung", document = "KOMD-DOKUMENT", updatedNow = "Gerade aktualisiert", insertImage = "Bild einfügen",
    imageIntro = "Wähle eine Datei auf deinem Gerät oder gib eine Internet-URL ein.", chooseFile = "Datei auswählen…", internetUrl = "Internet-URL",
    imageFromUrl = "Bild über URL", description = "Beschreibung", imageUrl = "Bild-URL", insert = "Einfügen", cancel = "Abbrechen",
    linkTextOptional = "Linktext (optional)", documentationExample = "z. B. Dokumentation", webAddress = "Webadresse",
    localFile = "Lokale Datei", insertUrl = "URL einfügen", exampleText = "Text", settingsTitle = "Einstellungen", languages = "Sprachen", colors = "Farben",
    text = "Text", textSize = "Textgröße", textSizeHint = "Vergrößert Menüs, Leisten und Schaltflächen. Editor und Vorschau behalten ihren Zoom.",
    copyCode = "Kopieren", copied = "Kopiert",
    languageLabel = "Anzeigesprache", theme = "Design", dark = "Dunkel", light = "Hell", accentColor = "Akzentfarbe",
    accentDescription = "Passe Links, Cursor und hervorgehobene Elemente an.", darkFixed = "Das dunkle Design behält die Farben des Stitch-Entwurfs bei.",
    done = "Fertig", imageAlignment = "Ausrichtung", left = "Links", center = "Mitte", right = "Rechts",
    openFailure = "Datei konnte nicht geöffnet werden: {error}", unknownError = "unbekannter Fehler", opened = "Geöffnet: {name}",
    readFailure = "Datei konnte nicht gelesen werden: {error}", saveFailure = "Datei konnte nicht gespeichert werden: {error}", saved = "Gespeichert: {name}",
    imageFailure = "Bild konnte nicht geöffnet werden: {error}", imageAdded = "Eingefügt: {name}", filePickerFailure = "Datei konnte nicht ausgewählt werden: {error}",
    fileLinkAdded = "Link zu {name} eingefügt", imageLinkAdded = "Bildlink eingefügt", linkAdded = "Link eingefügt",
    imageUnsupported = "Nicht unterstütztes Bild: {source}", column = "Spalte", value = "Wert", listItem = "Eintrag", quoteExample = "Zitat",
    hello = "Hallo", welcomeTitle = "Willkommen bei KoMD", welcomeIntro = "Ein einfacher Markdown-Editor für entspanntes Schreiben.",
    welcomeSection = "Schreiben und Vorschau", welcomeDirections = "Bearbeite den Text links und sieh zu, wie die Vorschau live aktualisiert wird.",
    welcomeBullets = listOf("Füge Elemente über die Schnellaktionen ein", "Schreibe in Markdown", "Konzentriere dich auf den Inhalt"),
    welcomeQuote = "KoMD — Markdown ohne Ablenkung.", notesName = "Notizen.md", websiteName = "Website.md", draftName = "Entwurf.md",
    websiteTitle = "Website", websiteDraft = "Entwurf für das nächste Projekt.", ideasTitle = "Ideen", ideasPrompt = "Füge hier deine Notizen hinzu.",
    untitledName = "Unbenannt.md", untitledTitle = "Unbenannt", colorMandarin = "Mandarine", colorApricot = "Aprikose",
    colorPeach = "Pfirsich", colorTerracotta = "Terrakotta", colorRose = "Rosa", colorHoney = "Honig", colorSage = "Salbei"
)

private fun stringsFor(language: UiLanguage): AppStrings = when (language) {
    UiLanguage.Italian -> italianStrings
    UiLanguage.English -> englishStrings
    UiLanguage.French -> frenchStrings
    UiLanguage.Spanish -> spanishStrings
    UiLanguage.German -> germanStrings
}

internal val LocalUiStrings = staticCompositionLocalOf { italianStrings }
private val LocalInterfaceScale = staticCompositionLocalOf { 1f }

internal data class PreviewImage(val alt: String, val source: String, val alignment: ImageAlignment)

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
    Document(strings.notesName, welcomeText(strings)),
    Document(strings.websiteName, "# ${strings.websiteTitle}\n\n${strings.websiteDraft}\n"),
    Document(strings.draftName, "# ${strings.ideasTitle}\n\n${strings.ideasPrompt}\n")
)

private fun imageAlignmentLabel(alignment: ImageAlignment, strings: AppStrings): String = when (alignment) {
    ImageAlignment.Left -> strings.left
    ImageAlignment.Center -> strings.center
    ImageAlignment.Right -> strings.right
}

private fun workspaceModeLabel(mode: WorkspaceMode, strings: AppStrings): String = when (mode) {
    WorkspaceMode.Split -> strings.split
    WorkspaceMode.Editor -> strings.editor
    WorkspaceMode.Preview -> strings.preview
}

@Composable
fun App() {
    var darkTheme by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(UiLanguage.Italian) }
    var lightAccent by remember { mutableStateOf(lightPalette.accent) }
    var interfaceScale by remember { mutableFloatStateOf(1f) }
    val palette = if (darkTheme) darkPalette else lightPalette.copy(accent = lightAccent)
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalKomdPalette provides palette,
        LocalUiStrings provides stringsFor(language),
        LocalInterfaceScale provides interfaceScale,
        LocalDensity provides Density(density.density, density.fontScale * interfaceScale)
    ) {
        AppContent(
            darkTheme = darkTheme,
            onToggleTheme = { darkTheme = !darkTheme },
            lightAccent = lightAccent,
            onLightAccentChange = { lightAccent = it },
            language = language,
            onLanguageChange = { language = it },
            interfaceScale = interfaceScale,
            onInterfaceScaleChange = { interfaceScale = it }
        )
    }
}

@Composable
private fun AppContent(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    lightAccent: Color,
    onLightAccentChange: (Color) -> Unit,
    language: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit,
    interfaceScale: Float,
    onInterfaceScaleChange: (Float) -> Unit
) {
    val colors = LocalKomdPalette.current
    val ui = LocalUiStrings.current
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var documents by remember { mutableStateOf(initialDocuments(ui)) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var content by remember { mutableStateOf(welcomeText(ui)) }
    var editorZoom by remember { mutableFloatStateOf(1f) }
    var previewZoom by remember { mutableFloatStateOf(1f) }
    var workspaceMode by remember { mutableStateOf(WorkspaceMode.Split) }
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
                    content = if (untouchedSample) newSamples[index].content else document.content
                )
            }.toMutableList()
            if (selectedSampleIsUntouched) content = newSamples[selectedIndex].content
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
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("md", "markdown")),
        onError = { fileMessage = ui.openFailure.replace("{error}", it.message ?: ui.unknownError) },
        onResult = { file ->
            if (file != null) scope.launch {
                runCatching { file.readString() }
                    .onSuccess { loaded ->
                        documents = documents.toMutableList().also { list ->
                            if (selectedIndex in list.indices) list[selectedIndex] = list[selectedIndex].copy(content = content)
                            list += Document(file.name, loaded, file)
                        }
                        selectedIndex = documents.lastIndex
                        content = loaded
                        fileMessage = ui.opened.replace("{name}", file.name)
                    }
                    .onFailure { fileMessage = ui.readFailure.replace("{error}", it.message ?: ui.unknownError) }
            }
        }
    )
    val saver = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onError = { fileMessage = ui.saveFailure.replace("{error}", it.message ?: ui.unknownError) },
        onResult = { file ->
            if (file != null) scope.launch {
                runCatching { file.writeString(latestContent) }
                    .onSuccess {
                        documents = documents.toMutableList().also { list ->
                            if (selectedIndex in list.indices) list[selectedIndex] = list[selectedIndex].copy(name = file.name, content = latestContent, file = file)
                        }
                        fileMessage = ui.saved.replace("{name}", file.name)
                    }
                    .onFailure { fileMessage = ui.saveFailure.replace("{error}", it.message ?: ui.unknownError) }
            }
        }
    )
    val insertImage: (String, String) -> Unit = { alt, source ->
        content += "\n${alignedImageHtml(alt, source, imageAlignment)}\n"
        documents = documents.toMutableList().also { list ->
            if (selectedIndex in list.indices) list[selectedIndex] = list[selectedIndex].copy(content = content)
        }
    }
    val insertLink: (String, String) -> Unit = { label, target ->
        content += "\n${markdownLink(label, target)}\n"
        documents = documents.toMutableList().also { list ->
            if (selectedIndex in list.indices) list[selectedIndex] = list[selectedIndex].copy(content = content)
        }
    }
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onError = { fileMessage = ui.imageFailure.replace("{error}", it.message ?: ui.unknownError) },
        onResult = { file ->
            if (file != null) {
                insertImage(ui.image, file.path)
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
    val saveFile: (Boolean) -> Unit = { overwrite ->
        val index = selectedIndex
        val existing = documents.getOrNull(index)?.file
        if (overwrite && existing != null) {
            fileMessage = null
            scope.launch {
                runCatching { existing.writeString(latestContent) }
                    .onSuccess {
                        documents = documents.toMutableList().also { list ->
                            if (index in list.indices) list[index] = list[index].copy(content = latestContent)
                        }
                        fileMessage = ui.saved.replace("{name}", existing.name)
                    }
                    .onFailure { fileMessage = ui.saveFailure.replace("{error}", it.message ?: ui.unknownError) }
            }
        } else {
            fileMessage = null
            saver.launch(
                suggestedName = documents.getOrNull(index)?.name?.substringBeforeLast('.') ?: ui.untitledTitle,
                defaultExtension = "md",
                allowedExtensions = setOf("md", "markdown")
            )
        }
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(colors.background).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
            val shortcut = if (shortcutUsesCommand) event.isMetaPressed else event.isCtrlPressed
            if (!shortcut || event.isAltPressed || event.isShiftPressed) return@onPreviewKeyEvent false
            when (event.key) {
                Key.O -> {
                    openFile()
                    true
                }
                Key.S -> {
                    saveFile(true)
                    true
                }
                else -> false
            }
        }
    ) {
        val wide = maxWidth >= 940.dp
        val splitWidthPx = with(LocalDensity.current) { (maxWidth - 240.dp).toPx() }
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                Sidebar(
                    documents = documents,
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        documents = documents.toMutableList().also { it[selectedIndex] = it[selectedIndex].copy(content = content) }
                        selectedIndex = index
                        content = documents[index].content
                    },
                    onNew = {
                        documents = documents + Document(ui.untitledName, "# ${ui.untitledTitle}\n\n")
                        selectedIndex = documents.lastIndex
                        content = documents.last().content
                    },
                    onOpen = { fileMessage = null; picker.launch() },
                    onSave = {
                        fileMessage = null
                        saver.launch(
                            suggestedName = documents.getOrNull(selectedIndex)?.name?.substringBeforeLast('.') ?: ui.untitledTitle,
                            defaultExtension = "md",
                            allowedExtensions = setOf("md", "markdown")
                        )
                    },
                    fileMessage = fileMessage,
                    onInsert = { content += it },
                    onImage = { showImageOptions = true },
                    onCustomLink = { showCustomLinkDialog = true },
                    onSettings = { showSettings = true },
                    modifier = Modifier.width(240.dp).fillMaxHeight()
                )
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                AppTopBar(
                    documentName = documents.getOrNull(selectedIndex)?.name ?: "KoMD",
                    ui = ui,
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    onSettings = { showSettings = true },
                    onInsert = { content += it },
                    onOpen = { fileMessage = null; picker.launch() },
                    onSave = {
                        fileMessage = null
                        saver.launch(
                            suggestedName = documents.getOrNull(selectedIndex)?.name?.substringBeforeLast('.') ?: ui.untitledTitle,
                            defaultExtension = "md",
                            allowedExtensions = setOf("md", "markdown")
                        )
                    },
                    wide = wide,
                    onPreviewToggle = {
                        workspaceMode = if (workspaceMode == WorkspaceMode.Preview) WorkspaceMode.Split else WorkspaceMode.Preview
                    }
                )
                if (wide) {
                    WorkspaceToolbar(
                        documentName = documents.getOrNull(selectedIndex)?.name ?: "KoMD",
                        ui = ui,
                        mode = workspaceMode,
                        onModeChange = { workspaceMode = it },
                        onSave = {
                            fileMessage = null
                            saver.launch(
                                suggestedName = documents.getOrNull(selectedIndex)?.name?.substringBeforeLast('.') ?: ui.untitledTitle,
                                defaultExtension = "md",
                                allowedExtensions = setOf("md", "markdown")
                            )
                        }
                    )
                }
                DividerLine(colors)

                if (!wide) fileMessage?.let { Text(it, Modifier.fillMaxWidth().background(colors.sidebar).padding(horizontal = 18.dp, vertical = 7.dp), color = colors.secondary, fontSize = 12.sp) }

                val editorPane: @Composable (Modifier) -> Unit = { paneModifier ->
                    EditorPane(content, { newValue ->
                        content = newValue
                        documents = documents.toMutableList().also { if (selectedIndex in it.indices) it[selectedIndex] = it[selectedIndex].copy(content = newValue) }
                    }, ui, editorZoom, { editorZoom = it }, paneModifier)
                }
                when {
                    wide && workspaceMode == WorkspaceMode.Split -> Row(Modifier.weight(1f).fillMaxWidth()) {
                        editorPane(Modifier.weight(splitRatio).fillMaxHeight())
                        Splitter(
                            colors = colors,
                            availableWidthPx = splitWidthPx,
                            onDrag = { delta -> splitRatio = (splitRatio + delta).coerceIn(0.2f, 0.8f) }
                        )
                        PreviewPane(content, ui, previewZoom, { previewZoom = it }, Modifier.weight(1f - splitRatio).fillMaxHeight())
                    }
                    workspaceMode == WorkspaceMode.Preview -> PreviewPane(content, ui, previewZoom, { previewZoom = it }, Modifier.weight(1f).fillMaxWidth())
                    else -> editorPane(Modifier.weight(1f).fillMaxWidth())
                }
                if (wide) WorkspaceStatusBar(content, fileMessage, ui)
            }
        }

        if (!wide && workspaceMode != WorkspaceMode.Preview) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(colors.toolbar).horizontalScroll(rememberScrollState()).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                    listOf(ui.table, ui.image, ui.link, ui.customLink, ui.code, ui.list, ui.quote).forEach { label ->
                        SmallButton(label) {
                            if (label == ui.image) showImageOptions = true
                            else if (label == ui.customLink) showCustomLinkDialog = true
                            else content += when (label) {
                                ui.table -> "\n| ${ui.column} 1 | ${ui.column} 2 |\n| --- | --- |\n| ${ui.value} | ${ui.value} |\n"
                                ui.link -> "[${ui.link}](https://)"
                                ui.code -> "\n```\n ${ui.code}\n```\n"
                                ui.list -> "\n- ${ui.listItem}\n- ${ui.listItem}\n"
                                else -> "\n> ${ui.quoteExample}\n"
                            }
                        }
                    }
                }
            }
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

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text(ui.settingsTitle) },
                text = {
                    Row(Modifier.fillMaxWidth().height(290.dp)) {
                        Column(
                            Modifier.width(126.dp).fillMaxHeight().background(colors.sidebar).padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            SettingsMenuRow(ui.languages, settingsSection == SettingsSection.Languages) {
                                settingsSection = SettingsSection.Languages
                            }
                            SettingsMenuRow(ui.colors, settingsSection == SettingsSection.Colors) {
                                settingsSection = SettingsSection.Colors
                            }
                            SettingsMenuRow(ui.text, settingsSection == SettingsSection.Text) {
                                settingsSection = SettingsSection.Text
                            }
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
                                SettingsSection.Colors -> {
                                    Text("${ui.theme}: ${if (darkTheme) ui.dark else ui.light}", color = colors.secondary, fontSize = 11.sp)
                                    if (darkTheme) {
                                        Text(ui.darkFixed, color = colors.foreground, fontSize = 13.sp, lineHeight = 19.sp)
                                    } else {
                                        Text(ui.accentColor, color = colors.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text(ui.accentDescription, color = colors.secondary, fontSize = 11.sp, lineHeight = 16.sp)
                                        lightAccentOptions(ui).chunked(2).forEach { options ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                options.forEach { option ->
                                                    val selected = lightAccent == option.color
                                                    AccentChoiceRow(option, selected, Modifier.weight(1f)) {
                                                        onLightAccentChange(option.color)
                                                    }
                                                }
                                                if (options.size < 2) Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
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
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text(ui.done) } }
            )
        }
    }

@Composable
private fun AppTopBar(
    documentName: String,
    ui: AppStrings,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSettings: () -> Unit,
    onInsert: (String) -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onPreviewToggle: () -> Unit,
    wide: Boolean
) {
    val colors = LocalKomdPalette.current
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(colors.toolbar).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!wide) {
                Text("KoMD", color = colors.foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(14.dp))
                SmallButton("↓") { onOpen() }
                SmallButton("↥") { onSave() }
            } else {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color(0xFF41C9A0)))
                Spacer(Modifier.width(9.dp))
                Text(documentName, color = colors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.weight(1f))
        if (wide) {
            Row(
                Modifier.widthIn(max = 310.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(colors.background).padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌕", color = colors.secondary, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(ui.commandSearch, color = colors.secondary, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("⌘ K", color = colors.secondary, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            SmallButton("B") { onInsert(" **${ui.exampleText}** ") }
            SmallButton("I") { onInsert(" *${ui.exampleText}* ") }
            SmallButton("↗") { onInsert(" [${ui.exampleText}](https://) ") }
            SmallButton("〈〉") { onInsert(" `codice` ") }
            SmallButton(ui.preview) { onPreviewToggle() }
        }
        SmallButton(if (darkTheme) "☼" else "☾") { onToggleTheme() }
        SmallButton("⚙") { onSettings() }
    }
}

@Composable
private fun WorkspaceToolbar(
    documentName: String,
    ui: AppStrings,
    mode: WorkspaceMode,
    onModeChange: (WorkspaceMode) -> Unit,
    onSave: () -> Unit
) {
    val colors = LocalKomdPalette.current
    Row(
        Modifier.fillMaxWidth().height(44.dp).background(colors.sidebar).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF41C9A0)))
        Spacer(Modifier.width(8.dp))
        Text(ui.activeSession, color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
        Text("   ${ui.documents.lowercase()}   /   $documentName", color = colors.secondary, fontSize = 10.sp, maxLines = 1)
        Spacer(Modifier.weight(1f))
        WorkspaceMode.entries.forEach { option ->
            ModeButton(workspaceModeLabel(option, ui), mode == option, onClick = { onModeChange(option) })
        }
        Spacer(Modifier.width(8.dp))
        SmallButton(ui.save) { onSave() }
    }
}

@Composable
private fun WorkspaceStatusBar(content: String, message: String?, ui: AppStrings) {
    val colors = LocalKomdPalette.current
    val words = content.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    Row(
        Modifier.fillMaxWidth().height(32.dp).background(colors.sidebar).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF41C9A0)))
        Spacer(Modifier.width(7.dp))
        Text(message ?: ui.localDraft, color = colors.secondary, fontSize = 9.sp, maxLines = 1)
        Spacer(Modifier.width(16.dp))
        Text("${content.length} ${ui.characters}", color = colors.secondary, fontSize = 9.sp)
        Spacer(Modifier.width(10.dp))
        Text("$words ${ui.words}", color = colors.secondary, fontSize = 9.sp)
        Spacer(Modifier.weight(1f))
        Text("${ui.commonMark}   ·   UTF-8", color = colors.secondary, fontSize = 9.sp)
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Text(
        label,
        Modifier.clip(RoundedCornerShape(6.dp)).background(if (selected) colors.selectedDocument else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 6.dp),
        color = if (selected) colors.foreground else colors.secondary,
        fontSize = 10.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1
    )
}

@Composable
private fun SettingsMenuRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Text(
        label,
        Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
            .background(if (selected) colors.selectedDocument else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 10.dp),
        color = if (selected) colors.foreground else colors.secondary,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1
    )
}

@Composable
private fun SettingsOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
            .background(if (selected) colors.selectedDocument else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.foreground, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        if (selected) Text("✓", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccentChoiceRow(option: AccentOption, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Row(
        modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.selectedDocument else colors.toolbar)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.size(17.dp).clip(RoundedCornerShape(50)).background(option.color))
        Text(option.name, color = colors.foreground, fontSize = 11.sp, maxLines = 1)
        if (selected) Text("✓", color = colors.accent, fontSize = 11.sp)
    }
}

@Composable
private fun Sidebar(
    documents: List<Document>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    fileMessage: String?,
    onInsert: (String) -> Unit,
    onImage: () -> Unit,
    onCustomLink: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKomdPalette.current
    val ui = LocalUiStrings.current
    Column(modifier.background(colors.sidebar)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 15.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(colors.accent), contentAlignment = Alignment.Center) {
                    Text("K", color = colors.background, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("KoMD", color = colors.foreground, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(ui.tagline, color = colors.secondary, fontSize = 10.sp)
                }
            }
            SidebarPrimaryButton("＋  ${ui.newDocument}", onNew)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SidebarUtilityButton(ui.openFile, Modifier.weight(1f), onOpen)
                SidebarUtilityButton(ui.save, Modifier.weight(1f), onSave)
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ui.documents, color = colors.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                Spacer(Modifier.weight(1f))
                Text("${documents.size}", color = colors.secondary, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            documents.forEachIndexed { index, document ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (selectedIndex == index) colors.selectedDocument else Color.Transparent)
                        .clickable { onSelect(index) }.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▤", color = if (selectedIndex == index) colors.accent else colors.secondary, fontSize = 13.sp)
                    Spacer(Modifier.width(9.dp))
                    Text(document.name, color = colors.foreground, fontSize = 12.sp, maxLines = 1)
                    if (selectedIndex == index) {
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF41C9A0)))
                    }
                }
            }
            fileMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(colors.button).padding(9.dp), color = colors.secondary, fontSize = 10.sp)
            }
            Spacer(Modifier.height(20.dp))
            DividerLine(colors)
            Text(ui.quickElements, Modifier.padding(top = 17.dp, bottom = 8.dp), color = colors.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            listOf(
                "▦   ${ui.table}" to "\n| ${ui.column} 1 | ${ui.column} 2 |\n| --- | --- |\n| ${ui.value} | ${ui.value} |\n",
                "↗   ${ui.link}" to "[${ui.link}](https://)",
                "〈〉   ${ui.code}" to "\n```\n ${ui.code}\n```\n",
                "☷   ${ui.list}" to "\n- ${ui.listItem}\n- ${ui.listItem}\n",
                "❞   ${ui.quote}" to "\n> ${ui.quoteExample}\n"
            ).forEach { (label, snippet) ->
                SidebarButton(label) { onInsert(snippet) }
            }
            SidebarButton("▧   ${ui.image}") { onImage() }
            SidebarButton("↗   ${ui.customLink}") { onCustomLink() }
        }
        DividerLine(colors)
        Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 8.dp)) {
            SidebarButton("⚙   ${ui.settings}", onSettings)
        }
    }
}

@Composable
private fun SidebarPrimaryButton(label: String, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Text(
        label,
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(colors.accent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        color = if (colors.background == darkPalette.background) colors.background else Color(0xFF39251C),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}

@Composable
private fun SidebarUtilityButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Text(
        label,
        modifier.clip(RoundedCornerShape(8.dp)).background(colors.button).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        color = colors.foreground,
        fontSize = 10.sp,
        maxLines = 1
    )
}

@Composable
private fun EditorPane(
    content: String,
    onChange: (String) -> Unit,
    ui: AppStrings,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var editorHasFocus by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalKomdPalette provides paperPalette(LocalKomdPalette.current.accent)) {
    val colors = LocalKomdPalette.current
    val lineCount = content.lines().size
    val wordCount = content.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    Column(modifier.background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(colors.toolbar).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ui.sourceMarkdown, color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
            Spacer(Modifier.width(8.dp))
            Text(ui.monoFont, color = colors.secondary, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("${ui.line} $lineCount   ·   $wordCount ${ui.words}", color = colors.secondary, fontSize = 9.sp)
            Spacer(Modifier.width(9.dp))
            ZoomControl(zoom, onZoomChange)
        }
        DividerLine(colors)
        val documentScale = LocalInterfaceScale.current.coerceAtLeast(0.5f)
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale / documentScale * zoom)) {
            Row(
                Modifier.fillMaxWidth().weight(1f).padding(start = 14.dp, top = 13.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    content.lines().indices.joinToString("\n") { (it + 1).toString().padStart(2, '0') },
                    Modifier.width(31.dp).padding(top = 3.dp),
                    color = colors.secondary.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    lineHeight = 23.sp,
                    fontFamily = FontFamily.Monospace
                )
                BasicTextField(
                    value = content,
                    onValueChange = onChange,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .onFocusChanged { editorHasFocus = it.isFocused }
                        .pointerInput(zoom, editorHasFocus) {
                            awaitPointerEventScope {
                                while (true) {
                                    // Intercept modified wheel and wheel-button events before
                                    // BasicTextField's selection/context-menu handling sees them.
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (editorHasFocus && event.keyboardModifiers.isMetaPressed) {
                                        when (event.type) {
                                            PointerEventType.Scroll -> {
                                                val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                if (scrollY != 0f) {
                                                    val step = if (scrollY < 0f) 1 else -1
                                                    onZoomChange(((zoom * 10).roundToInt() + step).coerceIn(7, 20) / 10f)
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                            PointerEventType.Press, PointerEventType.Release -> {
                                                // Pressing the wheel sends a separate click event; without
                                                // consuming it, the text field can open its copy/paste menu.
                                                event.changes.forEach { it.consume() }
                                            }
                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                        .padding(start = 2.dp, end = 8.dp),
                    textStyle = TextStyle(color = colors.editorText, fontSize = 13.sp, lineHeight = 23.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent)
                )
            }
        }
    }
    }
}

@Composable
private fun ImageAlignmentPicker(selected: ImageAlignment, ui: AppStrings, onChange: (ImageAlignment) -> Unit) {
    Column {
        Text(ui.imageAlignment, color = LocalKomdPalette.current.secondary, fontSize = 12.sp)
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

private fun alignedImageHtml(alt: String, source: String, alignment: ImageAlignment): String {
    val imageSource = if (source.startsWith("/")) fileUri(source) else source
    val margins = when (alignment) {
        ImageAlignment.Left -> "margin-left:0;margin-right:auto;"
        ImageAlignment.Center -> "margin-left:auto;margin-right:auto;"
        ImageAlignment.Right -> "margin-left:auto;margin-right:0;"
    }
    return "<img src=\"${escapeHtmlAttribute(imageSource)}\" alt=\"${escapeHtmlAttribute(alt)}\" style=\"display:block;max-width:100%;$margins\" />"
}

internal fun fileUri(path: String): String {
    val safe = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/-._~!$&'()*+,;=:@"
    val hex = "0123456789ABCDEF"
    val encodedPath = buildString {
        path.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and 0xFF
            val character = value.toChar()
            if (value < 128 && character in safe) append(character)
            else {
                append('%')
                append(hex[value shr 4])
                append(hex[value and 0x0F])
            }
        }
    }
    return "file://$encodedPath"
}

private fun escapeHtmlAttribute(value: String): String = value
    .replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun markdownLink(label: String, target: String): String {
    val safeLabel = label.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
    val safeTarget = target.replace("\n", "").replace(" ", "%20").replace(">", "%3E").replace("<", "%3C")
    return "[$safeLabel](<$safeTarget>)"
}

@Composable
private fun PreviewPane(
    markdown: String,
    ui: AppStrings,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalKomdPalette provides paperPalette(LocalKomdPalette.current.accent)) {
    val colors = LocalKomdPalette.current
    Column(modifier.background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(colors.toolbar).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ui.livePreview, color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
            Spacer(Modifier.width(12.dp))
            Text("●  ${ui.syncedScroll}", color = Color(0xFF51C7A4), fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text(ui.gfmRender, color = colors.secondary, fontSize = 9.sp)
            Spacer(Modifier.width(10.dp))
            ZoomControl(zoom, onZoomChange)
            Spacer(Modifier.width(8.dp))
            Text("↗  ·  ⎙", color = colors.secondary, fontSize = 12.sp)
        }
        DividerLine(colors)
        val documentScale = LocalInterfaceScale.current.coerceAtLeast(0.5f)
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale / documentScale * zoom)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.widthIn(max = 780.dp).fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ui.document, Modifier.clip(RoundedCornerShape(5.dp)).background(colors.selectedDocument).padding(horizontal = 8.dp, vertical = 5.dp), color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(ui.updatedNow, color = colors.secondary, fontSize = 9.sp)
                    }
                    Spacer(Modifier.height(22.dp))
                    MarkdownDocument(markdown, ui.imageUnsupported)
                }
            }
        }
    }
    }
}

private val markdownImagePattern = Regex("""^!\[(.*?)\]\((?:<([^>]+)>|([^\s)]+))\)$""")
private val htmlImagePattern = Regex("""<img\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
private val htmlAttributePattern = Regex("""([\w:-]+)\s*=\s*["']([^"']*)["']""")

internal fun parsePreviewImage(line: String): PreviewImage? {
    val trimmed = line.trim()
    val htmlMatch = htmlImagePattern.matchEntire(trimmed)
    if (htmlMatch != null) {
        val attributes = htmlAttributePattern.findAll(htmlMatch.groupValues[1])
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        val source = attributes["src"] ?: return null
        val style = attributes["style"].orEmpty().lowercase().replace(" ", "")
        val alignment = when {
            "margin-left:auto" in style && "margin-right:auto" in style -> ImageAlignment.Center
            "margin-left:auto" in style -> ImageAlignment.Right
            else -> ImageAlignment.Left
        }
        return PreviewImage(attributes["alt"].orEmpty(), unescapeHtmlAttribute(source), alignment)
    }

    val markdownMatch = markdownImagePattern.matchEntire(trimmed) ?: return null
    val source = markdownMatch.groups[2]?.value ?: markdownMatch.groups[3]?.value ?: return null
    return PreviewImage(markdownMatch.groups[1]?.value.orEmpty(), source, ImageAlignment.Left)
}

private fun unescapeHtmlAttribute(value: String): String = value
    .replace("&quot;", "\"")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")

@Composable
private fun InterfaceScaleControl(scale: Float, onChange: (Float) -> Unit) {
    val colors = LocalKomdPalette.current
    Row(
        Modifier.clip(RoundedCornerShape(7.dp)).background(colors.button),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "−",
            Modifier.clickable { onChange(((scale * 10).roundToInt() - 1).coerceAtLeast(9) / 10f) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = colors.foreground,
            fontSize = 16.sp
        )
        Text(
            "${(scale * 100).roundToInt()}%",
            Modifier.clickable { onChange(1f) }.padding(horizontal = 8.dp, vertical = 8.dp),
            color = colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "+",
            Modifier.clickable { onChange(((scale * 10).roundToInt() + 1).coerceAtMost(18) / 10f) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = colors.foreground,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ZoomControl(zoom: Float, onZoomChange: (Float) -> Unit) {
    val colors = LocalKomdPalette.current
    Row(
        Modifier.clip(RoundedCornerShape(7.dp)).background(colors.button),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "−",
            Modifier.clickable { onZoomChange(((zoom * 10).roundToInt() - 1).coerceAtLeast(7) / 10f) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = colors.foreground,
            fontSize = 13.sp
        )
        Text(
            "${(zoom * 100).roundToInt()}%",
            Modifier.clickable { onZoomChange(1f) }.padding(horizontal = 2.dp, vertical = 5.dp),
            color = colors.accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            "+",
            Modifier.clickable { onZoomChange(((zoom * 10).roundToInt() + 1).coerceAtMost(20) / 10f) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = colors.foreground,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SidebarButton(label: String, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Text(label, Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp), color = colors.foreground, fontSize = 13.sp)
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    val colors = LocalKomdPalette.current
    Text(label, Modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(6.dp)).background(colors.button).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), color = colors.foreground, fontSize = 12.sp, maxLines = 1)
}

@Composable
private fun DividerLine(colors: KomdPalette) = Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

@Composable
private fun Splitter(colors: KomdPalette, availableWidthPx: Float, onDrag: (Float) -> Unit) {
    Box(
        Modifier.width(12.dp).fillMaxHeight().background(Color.White)
            .pointerHoverIcon(columnResizePointerIcon)
            .pointerInput(availableWidthPx) {
            detectDragGestures { _, dragAmount ->
                if (availableWidthPx > 0f) onDrag(dragAmount.x / availableWidthPx)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.divider))
    }
}
