package com.almag.audioshelf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.core.AuthorSummary
import com.almag.audioshelf.core.Library
import com.almag.audioshelf.core.Queries
import com.almag.audioshelf.core.SeriesSummary
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.core.Text as T
import com.almag.audioshelf.ui.Sheet
import com.almag.audioshelf.ui.ShelfViewModel
import com.almag.audioshelf.ui.components.Avatar
import com.almag.audioshelf.ui.components.EmptyState
import com.almag.audioshelf.ui.components.ProgressLine
import com.almag.audioshelf.ui.components.SearchField
import com.almag.audioshelf.ui.components.ShelfCard
import com.almag.audioshelf.ui.components.spineColor
import com.almag.audioshelf.ui.theme.Shelf

@Composable
fun SeriesScreen(lib: Library, vm: ShelfViewModel, contentPadding: PaddingValues) {
    val query by vm.seriesQuery.collectAsState()
    val list = remember(lib, query) { Queries.series(lib, query) }
    LazyColumn(Modifier.fillMaxSize().testTag("page:series"), contentPadding = contentPadding) {
        item { SearchField(query, vm::setSeriesQuery, "Поиск серии…", Modifier.padding(bottom = 14.dp)) }
        item {
            Text(
                "Нажми на серию — покажу её книги на полке. Долгое нажатие — переименовать.",
                fontSize = 11.sp,
                color = Shelf.muted,
                modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
            )
        }
        if (list.isEmpty()) item { EmptyState("🗂", "Серий нет", "Серия появится, когда добавишь книгу с серией") }
        items(list, key = { it.name }) { s -> SeriesCard(s, vm) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesCard(s: SeriesSummary, vm: ShelfViewModel) {
    val col = spineColor(s.name)
    ShelfCard(
        Modifier.padding(bottom = 8.dp).testTag("series:${s.name}"),
        onClick = { vm.showOnShelf(s.name) },
        onLongClick = { vm.open(Sheet.RenameSeries(s.name)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.white, modifier = Modifier.weight(1f))
            Text("${s.percent}%", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.amber, modifier = Modifier.padding(start = 10.dp))
        }
        Text("✍️ ${s.author} · ${T.books(s.total)}", fontSize = 11.sp, color = Shelf.sub, modifier = Modifier.padding(bottom = 8.dp))
        ProgressLine(s.percent / 100f, color = col)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)) {
            Text("${s.listened} из ${s.total}", fontSize = 10.sp, color = Shelf.muted, modifier = Modifier.weight(1f))
            if (s.completed) {
                Text("✅ Завершена", fontSize = 10.sp, color = Shelf.green)
            } else {
                Text("${s.remaining} осталось", fontSize = 10.sp, color = Shelf.muted)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (b in s.books) {
                val c = when (b.status) {
                    Status.LISTENED -> col
                    Status.LISTENING -> Shelf.amber
                    Status.DROPPED -> Shelf.red
                    Status.WANT -> Shelf.line
                }
                Box(Modifier.size(26.dp, 8.dp).clip(RoundedCornerShape(3.dp)).background(c))
            }
        }
    }
}

@Composable
fun AuthorsScreen(lib: Library, vm: ShelfViewModel, contentPadding: PaddingValues) {
    val list = remember(lib) { Queries.authors(lib) }
    LazyColumn(Modifier.fillMaxSize().testTag("page:authors"), contentPadding = contentPadding) {
        if (list.isEmpty()) item { EmptyState("✍️", "Авторов нет", "Автор появится вместе с первой книгой") }
        items(list, key = { it.name }) { a -> AuthorCard(a, vm) }
    }
}

@Composable
private fun AuthorCard(a: AuthorSummary, vm: ShelfViewModel) {
    val col = spineColor(a.name)
    ShelfCard(
        Modifier.padding(bottom = 8.dp).testTag("author:${a.name}"),
        onClick = { vm.showOnShelf(a.name) },
        onLongClick = { vm.open(Sheet.RenameAuthor(a.name)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Avatar(a.name, T.initials(a.name))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(a.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.white)
                Text("${T.series(a.seriesCount)} · ${T.books(a.total)}", fontSize = 11.sp, color = Shelf.sub, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat("${a.listened}", "Прослушано", Shelf.green)
            if (a.listening > 0) Stat("${a.listening}", "Слушаю", Shelf.amber)
            Stat("${a.percent}%", "Готово", Shelf.amber)
        }
        Column(Modifier.padding(top = 10.dp)) {
            ProgressLine(a.percent / 100f, color = col)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("${a.listened} из ${a.total} прослушано", fontSize = 10.sp, color = Shelf.muted, modifier = Modifier.weight(1f))
                Text("${a.total - a.listened} осталось", fontSize = 10.sp, color = Shelf.muted)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Shelf.muted)
    }
}
