package kmd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val background = Color(0xFF17191D)
private val sidebar = Color(0xFF20252D)
private val toolbar = Color(0xFF24272C)
private val divider = Color(0xFF343941)
private val foreground = Color(0xFFE4E7EB)
private val secondary = Color(0xFF9299A4)
private val accent = Color(0xFF83B8FF)

private data class Document(val name: String, val content: String)

private val welcome = """# Benvenuto in KMD

Un editor Markdown semplice, pensato per scrivere con calma.

## Scrivi e visualizza

Modifica il testo qui a sinistra e osserva l'anteprima aggiornarsi in tempo reale.

- Usa i pulsanti rapidi per inserire elementi
- Scrivi in Markdown
- Concentrati sul contenuto

> KMD — Markdown, senza distrazioni.

```kotlin
fun hello() = "Ciao, KMD!"
```
"""

@Composable
fun App() {
    var documents by remember { mutableStateOf(listOf(Document("Appunti.md", welcome), Document("Sito_Web.md", "# Sito web\n\nBozza per il prossimo progetto.\n"), Document("Draft.md", "# Idee\n\nAggiungi qui le tue note.\n"))) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var content by remember { mutableStateOf(welcome) }
    var showPreview by remember { mutableStateOf(true) }

    BoxWithConstraints(Modifier.fillMaxSize().background(background)) {
        val wide = maxWidth >= 940.dp
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
                        documents = documents + Document("Senza titolo.md", "# Senza titolo\n\n")
                        selectedIndex = documents.lastIndex
                        content = documents.last().content
                    },
                    onInsert = { content += it },
                    modifier = Modifier.width(228.dp).fillMaxHeight()
                )
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    Modifier.fillMaxWidth().height(58.dp).background(toolbar).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!wide) Text("KMD", color = foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    else Text(documents.getOrNull(selectedIndex)?.name ?: "KMD", color = foreground, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    if (wide) {
                        listOf("B" to "**testo**", "I" to "*testo*", "↗" to "[testo](url)", "〈〉" to "`codice`").forEach { (label, snippet) ->
                            SmallButton(label) { content += " $snippet " }
                        }
                    }
                    SmallButton(if (showPreview) "Anteprima ✓" else "Anteprima") { showPreview = !showPreview }
                }
                DividerLine()

                if (wide && showPreview) {
                    Row(Modifier.fillMaxSize()) {
                        EditorPane(content, { newValue ->
                            content = newValue
                            documents = documents.toMutableList().also { if (selectedIndex in it.indices) it[selectedIndex] = it[selectedIndex].copy(content = newValue) }
                        }, Modifier.weight(1f).fillMaxHeight())
                        DividerVertical()
                        PreviewPane(content, Modifier.weight(1f).fillMaxHeight())
                    }
                } else if (showPreview) {
                    PreviewPane(content, Modifier.fillMaxSize())
                } else {
                    EditorPane(content, { newValue ->
                        content = newValue
                        documents = documents.toMutableList().also { if (selectedIndex in it.indices) it[selectedIndex] = it[selectedIndex].copy(content = newValue) }
                    }, Modifier.fillMaxSize())
                }
            }
        }

        if (!wide) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(toolbar).horizontalScroll(rememberScrollState()).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Tabella" to "\n| Colonna 1 | Colonna 2 |\n| --- | --- |\n| Valore | Valore |\n", "Immagine" to "![Descrizione](url_immagine)", "Link" to "[testo del link](https://)", "Codice" to "\n```\n codice\n```\n", "Elenco" to "\n- Elemento\n- Elemento\n", "Citazione" to "\n> Citazione\n").forEach { (label, snippet) ->
                    SmallButton(label) { content += snippet }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    documents: List<Document>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onNew: () -> Unit,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.background(sidebar)) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("KMD", Modifier.padding(top = 24.dp, bottom = 28.dp), color = foreground, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("DOCUMENTI", color = secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            documents.forEachIndexed { index, document ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (selectedIndex == index) Color(0xFF343B46) else Color.Transparent)
                        .clickable { onSelect(index) }.padding(horizontal = 11.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▤", color = secondary, fontSize = 14.sp); Spacer(Modifier.width(9.dp))
                    Text(document.name, color = foreground, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            SidebarButton("＋   Nuovo documento", onNew)
        }
        DividerLine()
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("ELEMENTI RAPIDI", color = secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(9.dp))
            listOf("▦   Tabella" to "\n| Colonna 1 | Colonna 2 |\n| --- | --- |\n| Valore | Valore |\n", "▧   Immagine" to "![Descrizione](url_immagine)", "↗   Link" to "[testo del link](https://)", "〈〉   Codice" to "\n```\n codice\n```\n", "☷   Elenco" to "\n- Elemento\n- Elemento\n", "❞   Citazione" to "\n> Citazione\n").forEach { (label, snippet) ->
                SidebarButton(label) { onInsert(snippet) }
            }
        }
    }
}

@Composable
private fun EditorPane(content: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(background)) {
        Text("EDITOR", Modifier.padding(start = 22.dp, top = 14.dp, bottom = 10.dp), color = secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value = content,
            onValueChange = onChange,
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            textStyle = TextStyle(color = Color(0xFFD9DEE7), fontSize = 14.sp, lineHeight = 23.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent)
        )
    }
}

@Composable
private fun PreviewPane(markdown: String, modifier: Modifier = Modifier) {
    var inCode = false
    Column(modifier.background(background).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 22.dp)) {
        markdown.lines().forEachIndexed { index, line ->
            when {
                line.trimStart().startsWith("```") -> inCode = !inCode
                inCode -> Text(line.ifEmpty { " " }, Modifier.fillMaxWidth().background(toolbar).padding(horizontal = 12.dp, vertical = 5.dp), color = Color(0xFFD9DEE7), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                line.startsWith("### ") -> Text(line.removePrefix("### "), Modifier.padding(top = 12.dp, bottom = 4.dp), color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                line.startsWith("## ") -> Text(line.removePrefix("## "), Modifier.padding(top = 16.dp, bottom = 6.dp), color = foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                line.startsWith("# ") -> Text(line.removePrefix("# "), Modifier.padding(bottom = 10.dp), color = foreground, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
                line.startsWith("> ") -> Text(line.removePrefix("> "), Modifier.fillMaxWidth().padding(vertical = 4.dp).padding(start = 12.dp), color = secondary, fontSize = 14.sp)
                line.startsWith("- ") || line.startsWith("* ") -> Text("•  ${line.drop(2)}", Modifier.padding(vertical = 2.dp), color = foreground, fontSize = 14.sp)
                line.startsWith("|") -> Text(line, Modifier.padding(vertical = 1.dp), color = accent, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                line.isBlank() -> Spacer(Modifier.height(if (index == 0) 4.dp else 9.dp))
                else -> Text(line, Modifier.fillMaxWidth(), color = foreground, fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun SidebarButton(label: String, onClick: () -> Unit) {
    Text(label, Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp), color = foreground, fontSize = 13.sp)
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit) {
    Text(label, Modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF30343A)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), color = foreground, fontSize = 12.sp, maxLines = 1)
}

@Composable
private fun DividerLine() = Box(Modifier.fillMaxWidth().height(1.dp).background(divider))

@Composable
private fun DividerVertical() = Box(Modifier.width(1.dp).fillMaxHeight().background(divider))
