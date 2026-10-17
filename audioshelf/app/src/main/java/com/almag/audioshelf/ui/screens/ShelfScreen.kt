package com.almag.audioshelf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.core.Filter
import com.almag.audioshelf.core.Library
import com.almag.audioshelf.core.Queries
import com.almag.audioshelf.core.ShelfGroup
import com.almag.audioshelf.core.ShelfView
import com.almag.audioshelf.ui.Sheet
import com.almag.audioshelf.ui.ShelfViewModel
import com.almag.audioshelf.ui.components.BookRow
import com.almag.audioshelf.ui.components.EmptyState
import com.almag.audioshelf.ui.components.FilterChip
import com.almag.audioshelf.ui.components.SearchField
import com.almag.audioshelf.ui.components.ShelfBoard
import com.almag.audioshelf.ui.components.SmallShape
import com.almag.audioshelf.ui.components.Spine
import com.almag.audioshelf.ui.theme.Playfair
import com.almag.audioshelf.ui.theme.Shelf

@Composable
fun ShelfScreen(lib: Library, vm: ShelfViewModel, contentPadding: PaddingValues) {
    val filter by vm.filter.collectAsState()
    val query by vm.query.collectAsState()
    val groups = remember(lib, filter, query) { Queries.shelf(lib, filter, query) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        vm.focusSearch.collect { runCatching { focus.requestFocus() } }
    }
    val view = lib.settings.shelfView

    LazyColumn(Modifier.fillMaxSize().testTag("page:shelf"), contentPadding = contentPadding) {
        item {
            SearchField(query, vm::setQuery, "Поиск по названию, автору, серии…", Modifier.padding(bottom = 14.dp), focus)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                items(Filter.entries) { f ->
                    FilterChip(
                        f.label,
                        selected = f == filter,
                        onClick = { vm.setFilter(f) },
                        color = f.status?.let { Shelf.color(it) } ?: Shelf.amber,
                        modifier = Modifier.testTag("filter:${f.name}"),
                    )
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(SmallShape)
                    .background(Shelf.bg2)
                    .border(1.dp, Shelf.line, SmallShape)
                    .padding(3.dp),
            ) {
                ViewButton("📚 Полка", view == ShelfView.SHELF, Modifier.weight(1f)) { vm.setShelfView(ShelfView.SHELF) }
                ViewButton("☰ Список", view == ShelfView.LIST, Modifier.weight(1f)) { vm.setShelfView(ShelfView.LIST) }
            }
        }
        if (groups.isEmpty()) {
            item { EmptyState("🔍", "Ничего не найдено", "Попробуй другой запрос или фильтр") }
        } else if (view == ShelfView.SHELF) {
            items(groups, key = { "g:" + it.series }) { g -> ShelfBlock(g, vm) }
        } else {
            groups.forEach { g ->
                item(key = "h:" + g.series) {
                    Row(Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 14.dp, bottom = 8.dp)) {
                        Text(g.title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Shelf.muted, modifier = Modifier.weight(1f))
                        Text("${g.books.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Shelf.amber)
                    }
                }
                items(g.books, key = { "b:" + it.id }) { b ->
                    BookRow(b, onClick = { vm.open(Sheet.Detail(b.id)) }, showSeries = g.series.isEmpty(), onLongClick = { vm.open(Sheet.Editor(b.id)) })
                }
            }
        }
    }
}

@Composable
private fun ViewButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) Shelf.bg4 else Shelf.bg2)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Shelf.white else Shelf.muted)
    }
}

@Composable
private fun ShelfBlock(g: ShelfGroup, vm: ShelfViewModel) {
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp).testTag("shelf:${g.series}")) {
        Column(
            Modifier
                .padding(horizontal = 2.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = g.series.isNotEmpty()) { vm.showOnShelf(g.series) },
        ) {
            Text(g.title, fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Shelf.white, lineHeight = 20.sp)
            Text("${g.author} · ${g.listened}/${g.total}", fontSize = 11.sp, color = Shelf.muted, modifier = Modifier.padding(top = 2.dp))
        }
        LazyRow(
            Modifier.fillMaxWidth().heightIn(min = 130.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(g.books, key = { it.id }) { b ->
                Spine(b, onClick = { vm.open(Sheet.Detail(b.id)) }, onLongClick = { vm.open(Sheet.Editor(b.id)) })
            }
        }
        ShelfBoard()
    }
}
