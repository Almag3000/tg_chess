package com.almag.audioshelf.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.core.Book
import com.almag.audioshelf.core.BookDraft
import com.almag.audioshelf.core.Library
import com.almag.audioshelf.core.Queries
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.core.Text as T
import com.almag.audioshelf.ui.Sheet
import com.almag.audioshelf.ui.ShelfViewModel
import com.almag.audioshelf.ui.components.SmallShape
import com.almag.audioshelf.ui.components.Stars
import com.almag.audioshelf.ui.components.shade
import com.almag.audioshelf.ui.components.spineColor
import com.almag.audioshelf.ui.theme.Playfair
import com.almag.audioshelf.ui.theme.Shelf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val statusOrder = listOf(Status.LISTENED, Status.LISTENING, Status.DROPPED, Status.WANT)
private val statusEmoji = mapOf(Status.LISTENED to "✅", Status.LISTENING to "🎧", Status.DROPPED to "❌", Status.WANT to "🔖")

/** Shows whatever [sheet] says is open. */
@Composable
fun SheetHost(lib: Library, sheet: Sheet, vm: ShelfViewModel) {
    when (sheet) {
        Sheet.None -> Unit
        is Sheet.Detail -> {
            val book = lib.book(sheet.bookId)
            if (book != null) BookDetailSheet(lib, book, vm) else LaunchedEffect(sheet) { vm.close() }
        }
        is Sheet.Editor -> BookEditorSheet(lib, sheet, vm)
        Sheet.AddAuthor -> NameDialog(
            title = "Новый автор",
            label = "Имя автора",
            placeholder = "Имя Фамилия или псевдоним",
            confirm = "Добавить",
            onDismiss = vm::close,
            onConfirm = { vm.addAuthor(it) },
        )
        is Sheet.AddSeries -> AddSeriesDialog(lib, sheet.presetAuthor, vm)
        is Sheet.RenameAuthor -> NameDialog(
            title = "Переименовать автора",
            label = "Имя автора",
            initial = sheet.name,
            confirm = "Сохранить",
            note = "Изменится у всех книг и серий автора",
            onDismiss = vm::close,
            onConfirm = { vm.renameAuthor(sheet.name, it) },
        )
        is Sheet.RenameSeries -> NameDialog(
            title = "Переименовать серию",
            label = "Название серии",
            initial = sheet.name,
            confirm = "Сохранить",
            note = "Изменится у всех книг серии",
            onDismiss = vm::close,
            onConfirm = { vm.renameSeries(sheet.name, it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfSheet(onDismiss: () -> Unit, tag: String, content: @Composable () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Shelf.bg2,
        contentColor = Shelf.text,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(40.dp, 4.dp).clip(CircleShape).background(Shelf.line))
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .testTag(tag),
        ) {
            content()
        }
    }
}

// ── detail ────────────────────────────────────────────────────────────────

@Composable
private fun BookDetailSheet(lib: Library, book: Book, vm: ShelfViewModel) {
    val zone = vm.zone()
    ShelfSheet(onDismiss = vm::close, tag = "sheet:detail") {
        val col = spineColor(book)
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(col, col.shade(-40)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                book.title,
                fontFamily = Playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(book.title, fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Shelf.white, lineHeight = 26.sp)
        val seriesBooks = remember(lib, book.series) {
            if (book.series.isEmpty()) emptyList() else Queries.inSeriesOrder(lib.books.filter { it.series == book.series })
        }
        val meta = buildString {
            append("✍️ ").append(book.author)
            if (book.series.isNotEmpty()) {
                append(" · ").append(book.series)
                val idx = seriesBooks.indexOfFirst { it.id == book.id }
                if (idx >= 0) append(" (").append(idx + 1).append(" из ").append(seriesBooks.size).append(")")
            }
        }
        Text(meta, fontSize = 12.sp, color = Shelf.sub, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

        Label("Статус")
        StatusPicker(book.status) { vm.setStatus(book.id, it) }

        if (book.status == Status.LISTENING) {
            Spacer(Modifier.height(14.dp))
            ProgressEditor(book) { vm.setProgress(book.id, it) }
        }

        Spacer(Modifier.height(14.dp))
        Label("Оценка")
        Stars(book.rating, onChange = { vm.setRating(book.id, it) })

        val dates = listOfNotNull(
            book.startedAt?.let { "Начата" to it },
            book.finishedAt?.let { "Прослушана" to it },
            book.addedAt.takeIf { it > 0 }?.let { "Добавлена" to it },
        )
        if (dates.isNotEmpty() || book.narrator.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier.fillMaxWidth().clip(SmallShape).background(Shelf.bg3).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (book.narrator.isNotEmpty()) InfoRow("Читает", book.narrator)
                for ((label, at) in dates) InfoRow(label, formatDate(at, zone))
            }
        }

        if (book.note.isNotEmpty()) {
            Text(
                book.note,
                fontSize = 14.sp,
                color = Shelf.sub,
                fontStyle = FontStyle.Italic,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        if (book.series.isNotEmpty()) {
            val next = seriesBooks.dropWhile { it.id != book.id }.drop(1).firstOrNull()
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkChip("Вся серия →") { vm.close(); vm.showOnShelf(book.series) }
                if (next != null) LinkChip("Следующая: ${next.title}") { vm.open(Sheet.Detail(next.id)) }
            }
        }

        Spacer(Modifier.height(18.dp))
        MainButton("✏️ Редактировать", Modifier.testTag("edit")) { vm.open(Sheet.Editor(book.id)) }
        GhostButton("Закрыть") { vm.close() }
    }
}

@Composable
private fun ProgressEditor(book: Book, onChange: (Int) -> Unit) {
    var value by remember(book.id, book.progress) { mutableFloatStateOf(book.progress.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label("Прогресс", Modifier.weight(1f))
        Text("${value.toInt()}%", color = Shelf.amber, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.testTag("progress"))
    }
    Slider(
        value = value,
        onValueChange = { value = it },
        onValueChangeFinished = { onChange(value.toInt()) },
        valueRange = 0f..100f,
        colors = SliderDefaults.colors(
            thumbColor = Shelf.amber,
            activeTrackColor = Shelf.amber,
            inactiveTrackColor = Shelf.bg4,
        ),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (step in listOf(-10, -5, 5, 10)) {
            SmallButton(if (step > 0) "+$step%" else "$step%", Modifier.weight(1f)) {
                onChange((value.toInt() + step).coerceIn(0, 100))
            }
        }
        SmallButton("Готово ✓", Modifier.weight(1.4f), color = Shelf.green) { onChange(100) }
    }
}

// ── editor ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookEditorSheet(lib: Library, sheet: Sheet.Editor, vm: ShelfViewModel) {
    val old = sheet.bookId?.let { lib.book(it) }
    var title by rememberSaveable(sheet) { mutableStateOf(old?.title ?: "") }
    var series by rememberSaveable(sheet) { mutableStateOf(old?.series ?: sheet.presetSeries) }
    var author by rememberSaveable(sheet) { mutableStateOf(old?.author ?: sheet.presetAuthor.ifEmpty { lib.seriesNamed(sheet.presetSeries)?.author.orEmpty() }) }
    var status by rememberSaveable(sheet) { mutableStateOf(old?.status ?: Status.WANT) }
    var note by rememberSaveable(sheet) { mutableStateOf(old?.note ?: "") }
    var narrator by rememberSaveable(sheet) { mutableStateOf(old?.narrator ?: "") }
    var volume by rememberSaveable(sheet) { mutableStateOf(old?.volume?.toString() ?: "") }
    var rating by rememberSaveable(sheet) { mutableIntStateOf(old?.rating ?: 0) }
    var seriesFocused by remember { mutableStateOf(false) }
    var authorFocused by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    LaunchedEffect(sheet) {
        if (old == null) runCatching { titleFocus.requestFocus() }
    }

    ShelfSheet(onDismiss = vm::close, tag = "sheet:editor") {
        Text(
            if (old == null) "Новая книга" else "Редактировать",
            fontFamily = Playfair,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = Shelf.white,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(old?.title ?: "Добавь на полку", fontSize = 13.sp, color = Shelf.sub, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

        Field("Название", title, { title = it }, "Название книги или тома…", tag = "field:title", focus = titleFocus)

        // Series with suggestions; picking one also fills its author.
        Field(
            "Серия",
            series,
            { series = it },
            "Без серии",
            onFocus = { seriesFocused = it },
            tag = "field:series",
        )
        val seriesMatches = remember(lib, series) {
            if (series.isBlank()) emptyList()
            else lib.series.filter { T.matches(series, it.name) && it.name != series }.take(6)
        }
        if (seriesFocused && seriesMatches.isNotEmpty()) {
            FlowRow(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in seriesMatches) Suggestion("${s.name} · ${s.author}") { series = s.name; author = s.author }
            }
        }
        val existingSeries = lib.seriesNamed(series.trim())
        if (series.isNotBlank() && existingSeries == null) {
            Hint("✦ Новая серия — привяжется к автору ниже")
        }
        if (existingSeries != null && old == null && title.isBlank()) {
            val count = lib.books.count { it.series == existingSeries.name }
            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Suggestion("Том ${count + 1}") { title = "Том ${count + 1}"; volume = "${count + 1}" }
            }
        }

        Field(
            "Автор",
            author,
            { author = it },
            if (existingSeries != null) existingSeries.author else "Имя автора",
            onFocus = { authorFocused = it },
            tag = "field:author",
        )
        val authorMatches = remember(lib, author) {
            if (author.isBlank()) emptyList() else lib.authors.filter { T.matches(author, it) && it != author }.take(6)
        }
        if (authorFocused && authorMatches.isNotEmpty()) {
            FlowRow(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (a in authorMatches) Suggestion(a) { author = a }
            }
        }
        if (author.isNotBlank() && lib.authors.none { it == author.trim() }) Hint("✦ Новый автор")

        Label("Статус")
        StatusPicker(status) { status = it }
        Spacer(Modifier.height(14.dp))

        Label("Оценка")
        Stars(rating, onChange = { rating = it })
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(
                "Том №",
                volume,
                { v -> volume = v.filter { it.isDigit() }.take(3) },
                T.guessVolume(title)?.toString() ?: "—",
                Modifier.width(96.dp),
                keyboard = KeyboardType.Number,
            )
            Field("Чтец", narrator, { narrator = it }, "Кто читает", Modifier.weight(1f))
        }
        Field("Заметка", note, { note = it }, "Впечатления, цитата…", singleLine = false, tag = "field:note")

        MainButton("Сохранить", Modifier.testTag("save")) {
            vm.saveBook(
                old?.id,
                BookDraft(
                    title = title,
                    series = series,
                    author = author,
                    status = status,
                    note = note,
                    rating = rating,
                    volume = volume.toIntOrNull(),
                    progress = old?.progress ?: 0,
                    narrator = narrator,
                ),
            )
        }
        if (old != null) {
            DangerButton("Удалить книгу", Modifier.testTag("delete")) { confirmDelete = true }
        }
        GhostButton("Отмена") { if (old != null) vm.open(Sheet.Detail(old.id)) else vm.close() }
    }

    if (confirmDelete && old != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Shelf.bg2,
            title = { Text("Удалить книгу?", fontFamily = Playfair, color = Shelf.white) },
            text = { Text("«${old.title}» будет удалена с полки.", color = Shelf.sub) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteBook(old.id) }, modifier = Modifier.testTag("confirm")) {
                    Text("Удалить", color = Shelf.red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена", color = Shelf.sub) } },
        )
    }
}

// ── small dialogs ─────────────────────────────────────────────────────────

@Composable
private fun NameDialog(
    title: String,
    label: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initial: String = "",
    placeholder: String = "",
    note: String? = null,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    ShelfSheet(onDismiss = onDismiss, tag = "sheet:name") {
        SheetTitle(title)
        Field(label, value, { value = it }, placeholder, tag = "field:name", focus = focus)
        if (note != null) Hint(note)
        MainButton(confirm, Modifier.testTag("confirm")) { onConfirm(value) }
        GhostButton("Отмена", onDismiss)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSeriesDialog(lib: Library, presetAuthor: String, vm: ShelfViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf(presetAuthor) }
    ShelfSheet(onDismiss = vm::close, tag = "sheet:series") {
        SheetTitle("Новая серия")
        Field("Название серии", name, { name = it }, "Название цикла или серии", tag = "field:name")
        Field("Автор", author, { author = it }, "Имя автора", tag = "field:author")
        val matches = lib.authors.filter { author.isNotBlank() && T.matches(author, it) && it != author }.take(4)
        if (matches.isNotEmpty()) {
            FlowRow(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (a in matches) Suggestion(a) { author = a }
            }
        }
        MainButton("Добавить", Modifier.testTag("confirm")) { vm.addSeries(name, author) }
        GhostButton("Отмена") { vm.close() }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        fontFamily = Playfair,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = Shelf.white,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
    )
}

// ── building blocks ───────────────────────────────────────────────────────

@Composable
private fun StatusPicker(selected: Status, onPick: (Status) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        statusOrder.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                for (s in pair) {
                    val sel = s == selected
                    val c = Shelf.color(s)
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(SmallShape)
                            .background(if (sel) Shelf.background(s) else Shelf.bg3)
                            .border(BorderStroke(1.5.dp, if (sel) c else Shelf.line), SmallShape)
                            .clickable { onPick(s) }
                            .padding(vertical = 11.dp, horizontal = 8.dp)
                            .testTag("status:${s.name}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${statusEmoji[s]} ${if (s == Status.LISTENING) "Слушаю сейчас" else s.label}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sel) c else Shelf.muted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
    onFocus: ((Boolean) -> Unit)? = null,
    tag: String? = null,
    focus: FocusRequester? = null,
) {
    Column(modifier.padding(bottom = 12.dp)) {
        Label(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = Shelf.muted, maxLines = 1) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            shape = SmallShape,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = keyboard),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Shelf.amber,
                unfocusedBorderColor = Shelf.line,
                focusedContainerColor = Shelf.bg3,
                unfocusedContainerColor = Shelf.bg3,
                cursorColor = Shelf.amber,
                focusedTextColor = Shelf.text,
                unfocusedTextColor = Shelf.text,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .let { m -> if (focus != null) m.focusRequester(focus) else m }
                .let { m -> if (onFocus != null) m.onFocusChanged { onFocus(it.isFocused) } else m }
                .let { m -> if (tag != null) m.testTag(tag) else m },
        )
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color = Shelf.muted,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 11.sp, color = Shelf.amber, modifier = Modifier.padding(top = 0.dp, bottom = 12.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(label, fontSize = 12.sp, color = Shelf.muted, modifier = Modifier.width(96.dp))
        Text(value, fontSize = 12.sp, color = Shelf.text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Suggestion(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Shelf.amber,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(CircleShape)
            .background(Shelf.amberBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun LinkChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Shelf.amber,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, Shelf.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun SmallButton(text: String, modifier: Modifier, color: Color = Shelf.sub, onClick: () -> Unit) {
    Box(
        modifier.clip(SmallShape).background(Shelf.bg3).clickable(onClick = onClick).padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

private val ButtonShape = RoundedCornerShape(14.dp)

@Composable
private fun MainButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(ButtonShape)
            .background(Shelf.amber)
            .clickable(onClick = onClick)
            .padding(15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.bg)
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(ButtonShape)
            .border(1.5.dp, Shelf.line, ButtonShape)
            .clickable(onClick = onClick)
            .padding(13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Shelf.muted)
    }
}

@Composable
private fun DangerButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(ButtonShape)
            .border(1.5.dp, Shelf.red.copy(alpha = 0.3f), ButtonShape)
            .clickable(onClick = onClick)
            .padding(13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Shelf.red)
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

private fun formatDate(ms: Long, zone: ZoneId): String = Instant.ofEpochMilli(ms).atZone(zone).format(dateFormat)
