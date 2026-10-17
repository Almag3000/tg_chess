package com.almag.audioshelf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.core.Text as T
import com.almag.audioshelf.ui.screens.AuthorsScreen
import com.almag.audioshelf.ui.screens.HomeScreen
import com.almag.audioshelf.ui.screens.MoreScreen
import com.almag.audioshelf.ui.screens.SeriesScreen
import com.almag.audioshelf.ui.screens.ShelfScreen
import com.almag.audioshelf.ui.sheets.SheetHost
import com.almag.audioshelf.ui.theme.Playfair
import com.almag.audioshelf.ui.theme.Shelf
import kotlinx.coroutines.launch

private val pageIcons: Map<Page, ImageVector> = mapOf(
    Page.HOME to Icons.Rounded.Home,
    Page.SHELF to Icons.AutoMirrored.Rounded.MenuBook,
    Page.SERIES to Icons.Rounded.LibraryBooks,
    Page.AUTHORS to Icons.Rounded.Person,
    Page.MORE to Icons.Rounded.MoreHoriz,
)

@Composable
fun ShelfAppUi(vm: ShelfViewModel) {
    val lib by vm.library.collectAsState()
    val sheet by vm.sheet.collectAsState()
    val query by vm.query.collectAsState()
    val pager = rememberPagerState { Page.entries.size }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val current = Page.entries[pager.currentPage]

    LaunchedEffect(Unit) {
        vm.pages.collect { pager.animateScrollToPage(it.ordinal) }
    }
    LaunchedEffect(Unit) {
        vm.messages.collect { text ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(text) }
        }
    }
    BackHandler(enabled = pager.currentPage != 0 && sheet == Sheet.None) {
        scope.launch { pager.animateScrollToPage(0) }
    }

    Scaffold(
        containerColor = Shelf.bg,
        topBar = {
            Column(Modifier.background(Shelf.bg).statusBarsPadding()) {
                Header(
                    subtitle = lib?.let { "${T.books(it.books.size)} · ${T.authors(it.authors.size)}" } ?: "",
                    onSearch = vm::openSearch,
                    onSettings = { vm.go(Page.MORE) },
                )
                NavRow(current) { p -> scope.launch { pager.animateScrollToPage(p.ordinal) } }
            }
        },
        floatingActionButton = {
            if (lib != null && current != Page.MORE) {
                AddButton(
                    onAdd = {
                        val presetSeries = lib?.seriesNamed(query.trim())?.name.orEmpty()
                        vm.open(Sheet.Editor(null, presetSeries = if (current == Page.SHELF) presetSeries else ""))
                    },
                    onAddAuthor = { vm.open(Sheet.AddAuthor) },
                    onAddSeries = { vm.open(Sheet.AddSeries()) },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    data,
                    containerColor = Shelf.bg4,
                    contentColor = Shelf.text,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
    ) { padding ->
        val library = lib
        if (library == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Shelf.amber)
            }
        } else {
            val content = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = padding.calculateBottomPadding() + 96.dp)
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                beyondViewportPageCount = 1,
                key = { it },
            ) { page ->
                when (Page.entries[page]) {
                    Page.HOME -> HomeScreen(library, vm, content)
                    Page.SHELF -> ShelfScreen(library, vm, content)
                    Page.SERIES -> SeriesScreen(library, vm, content)
                    Page.AUTHORS -> AuthorsScreen(library, vm, content)
                    Page.MORE -> MoreScreen(library, vm, content)
                }
            }
            SheetHost(library, sheet, vm)
        }
    }
}

@Composable
private fun Header(subtitle: String, onSearch: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Shelf.amber),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎧", fontSize = 16.sp)
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Книжная полка", fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Shelf.white, lineHeight = 22.sp)
            Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Shelf.muted, modifier = Modifier.testTag("subtitle"))
        }
        HeaderButton(Icons.Rounded.Search, "Поиск", onSearch)
        HeaderButton(Icons.Rounded.Settings, "Настройки", onSettings)
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 8.dp).size(38.dp).clip(CircleShape).background(Shelf.bg3).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Shelf.sub, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun NavRow(current: Page, onPick: (Page) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            for (p in Page.entries) {
                val sel = p == current
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(p) }
                        .padding(top = 6.dp)
                        .testTag("tab:${p.name}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(pageIcons.getValue(p), null, tint = if (sel) Shelf.amber else Shelf.muted, modifier = Modifier.size(20.dp))
                    Text(
                        p.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sel) Shelf.amber else Shelf.muted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 7.dp),
                        maxLines = 1,
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (sel) Shelf.amber else Shelf.bg),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Shelf.line))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddButton(onAdd: () -> Unit, onAddAuthor: () -> Unit, onAddSeries: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box {
        Box(
            Modifier
                .shadow(16.dp, CircleShape, ambientColor = Shelf.amber, spotColor = Shelf.amber)
                .size(58.dp)
                .clip(CircleShape)
                .background(Shelf.amber)
                .combinedClickable(
                    onClick = onAdd,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    },
                )
                .semantics { contentDescription = "Добавить книгу" }
                .testTag("fab"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, null, tint = Shelf.bg, modifier = Modifier.size(30.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Shelf.bg3) {
            DropdownMenuItem(text = { Text("📚 Книгу") }, onClick = { menu = false; onAdd() })
            DropdownMenuItem(text = { Text("✍️ Автора") }, onClick = { menu = false; onAddAuthor() })
            DropdownMenuItem(text = { Text("🗂 Серию") }, onClick = { menu = false; onAddSeries() })
        }
    }
}

