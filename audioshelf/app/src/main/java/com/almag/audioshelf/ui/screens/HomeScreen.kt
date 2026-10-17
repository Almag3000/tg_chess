package com.almag.audioshelf.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.core.Filter
import com.almag.audioshelf.core.Library
import com.almag.audioshelf.core.NextUp
import com.almag.audioshelf.core.Queries
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.core.Text as T
import com.almag.audioshelf.ui.Sheet
import com.almag.audioshelf.ui.ShelfViewModel
import com.almag.audioshelf.ui.components.BookRow
import com.almag.audioshelf.ui.components.EmptyState
import com.almag.audioshelf.ui.components.MiniSpine
import com.almag.audioshelf.ui.components.ProgressLine
import com.almag.audioshelf.ui.components.SectionTitle
import com.almag.audioshelf.ui.components.ShelfCard
import com.almag.audioshelf.ui.theme.Playfair
import com.almag.audioshelf.ui.theme.Shelf
import java.time.Instant
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(lib: Library, vm: ShelfViewModel, contentPadding: PaddingValues) {
    val overview = remember(lib) { Queries.overview(lib) }
    val listening = remember(lib) { Queries.byStatus(lib, Status.LISTENING) }
    val want = remember(lib) { Queries.byStatus(lib, Status.WANT) }
    val nextUp = remember(lib) { Queries.nextUp(lib) }
    val year = remember { Instant.ofEpochMilli(vm.now()).atZone(vm.zone()).year }
    val months = remember(lib, year) { Queries.finishedByMonth(lib, year, vm.zone()) }

    LazyColumn(
        Modifier.fillMaxSize().testTag("page:home"),
        contentPadding = contentPadding,
    ) {
        item { Hero(overview.total, "${T.authors(overview.authors)} · ${T.series(overview.series)}", overview, vm) }
        item {
            ShelfCard(Modifier.padding(bottom = 12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Прогресс прослушивания", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Shelf.white, modifier = Modifier.weight(1f))
                    Text("${overview.percent}%", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.amber)
                }
                Spacer(Modifier.height(8.dp))
                ProgressLine(overview.percent / 100f, height = 8.dp)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("${overview.listened} прослушано", fontSize = 11.sp, color = Shelf.muted, modifier = Modifier.weight(1f))
                    Text("${overview.total - overview.listened} осталось", fontSize = 11.sp, color = Shelf.muted)
                }
            }
        }
        item { YearCard(year, months, lib.settings.yearlyGoal) }

        item { SectionTitle("Сейчас слушаю") }
        if (listening.isEmpty()) {
            item { EmptyState("🎧", "Сейчас ничего не слушаешь", "Отметь книгу как «Слушаю» — она появится здесь") }
        } else {
            items(listening, key = { "l" + it.id }) { b -> BookRow(b, onClick = { vm.open(Sheet.Detail(b.id)) }) }
        }

        if (nextUp.isNotEmpty()) {
            item { SectionTitle("Продолжить серию") }
            items(nextUp.take(5), key = { "n" + it.next.id }) { n -> NextUpCard(n, vm) }
        }

        item { SectionTitle("Хочу послушать", action = if (want.isNotEmpty()) "все →" else null, onAction = { vm.showFiltered(Filter.WANT) }) }
        if (want.isEmpty()) {
            item {
                Text(
                    "Список пуст — добавь книги со статусом «Хочу»",
                    fontSize = 13.sp,
                    color = Shelf.muted,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
                )
            }
        } else {
            items(want.take(4), key = { "w" + it.id }) { b -> BookRow(b, onClick = { vm.open(Sheet.Detail(b.id)) }) }
        }

        item { SectionTitle("Вся библиотека") }
        item { DonutCard(overview.total, overview, onClick = { vm.showFiltered(it) }) }
    }
}

@Composable
private fun Hero(total: Int, sub: String, overview: com.almag.audioshelf.core.Overview, vm: ShelfViewModel) {
    ShelfCard(Modifier.padding(bottom = 12.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 20.dp), shape = RoundedCornerShape(20.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Text("🎧", fontSize = 90.sp, modifier = Modifier.align(Alignment.BottomEnd).alpha(0.07f))
            Column {
                Text("ВСЕГО КНИГ", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp, color = Shelf.muted)
                Text(
                    "$total",
                    fontSize = 48.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Shelf.white,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp).testTag("total"),
                )
                Text(sub, fontSize = 13.sp, color = Shelf.sub)
                Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(Shelf.line))
                Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (s in listOf(Status.LISTENED, Status.LISTENING, Status.DROPPED, Status.WANT)) {
                        val filter = Filter.entries.first { it.status == s }
                        Column(
                            Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.showFiltered(filter) }.padding(4.dp),
                        ) {
                            Text("${overview.count(s)}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.color(s))
                            Text(s.shortLabel.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = Shelf.muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearCard(year: Int, months: List<com.almag.audioshelf.core.MonthCount>, goal: Int) {
    val total = months.sumOf { it.count }
    if (total == 0 && goal == 0) return
    ShelfCard(Modifier.padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("$year ГОД", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp, color = Shelf.muted)
                Text(
                    "Прослушано ${T.books(total)}",
                    fontFamily = Playfair,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    color = Shelf.white,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (goal > 0) {
                Text("цель $goal", fontSize = 12.sp, color = Shelf.amber, fontWeight = FontWeight.Bold)
            }
        }
        if (goal > 0) {
            Spacer(Modifier.height(10.dp))
            ProgressLine(total.toFloat() / goal, color = Shelf.green, height = 6.dp)
        }
        val max = (months.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
        Row(
            Modifier.fillMaxWidth().height(86.dp).padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (m in months) {
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (m.count > 0) Text("${m.count}", fontSize = 9.sp, color = Shelf.sub, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((4 + 44f * m.count / max).dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (m.count > 0) Shelf.amber else Shelf.bg3),
                    )
                    Text(
                        java.time.Month.of(m.month).getDisplayName(TextStyle.NARROW_STANDALONE, Locale("ru")),
                        fontSize = 9.sp,
                        color = Shelf.muted,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NextUpCard(n: NextUp, vm: ShelfViewModel) {
    ShelfCard(
        Modifier.padding(bottom = 6.dp),
        onClick = { vm.open(Sheet.Detail(n.next.id)) },
        padding = PaddingValues(horizontal = 13.dp, vertical = 12.dp),
        shape = RoundedCornerShape(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniSpine(n.next)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(n.series, color = Shelf.white, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Дальше: ${n.next.title}", color = Shelf.sub, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${n.listened} из ${n.total} прослушано", color = Shelf.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                "Слушать",
                color = Shelf.bg,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Shelf.amber)
                    .clickable { vm.startListening(n.next.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("start:${n.next.id}"),
            )
        }
    }
}

@Composable
private fun DonutCard(total: Int, o: com.almag.audioshelf.core.Overview, onClick: (Filter) -> Unit) {
    val segments = listOf(Status.LISTENED, Status.LISTENING, Status.WANT, Status.DROPPED).map { it to o.count(it) }
    ShelfCard(padding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)) {
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(170.dp)) {
                val strokeW = 22.dp.toPx()
                val inset = strokeW / 2
                val arcSize = Size(size.width - strokeW, size.height - strokeW)
                drawArc(Color(0xFF252320), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(strokeW))
                if (total > 0) {
                    var start = -90f
                    for ((s, c) in segments) {
                        if (c == 0) continue
                        val sweep = 360f * c / total
                        drawArc(Shelf.color(s), start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(strokeW))
                        start += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$total", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.white)
                Text(T.plural(total, "книга", "книги", "книг"), fontSize = 11.sp, color = Shelf.muted, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            segments.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    for ((s, c) in pair) {
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onClick(Filter.entries.first { it.status == s }) }.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Shelf.color(s)))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$c", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Shelf.color(s))
                                    Text(
                                        " ${if (total == 0) 0 else Math.round(c * 100f / total)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Shelf.muted,
                                        modifier = Modifier.padding(bottom = 1.dp),
                                    )
                                }
                                Text(s.shortLabel, fontSize = 11.sp, color = Shelf.sub)
                            }
                        }
                    }
                }
            }
        }
    }
}

