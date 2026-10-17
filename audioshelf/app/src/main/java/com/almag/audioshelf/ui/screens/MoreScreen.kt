package com.almag.audioshelf.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.BuildConfig
import com.almag.audioshelf.core.Library
import com.almag.audioshelf.core.Seed
import com.almag.audioshelf.ui.ShelfViewModel
import com.almag.audioshelf.ui.components.ShelfCard
import com.almag.audioshelf.ui.theme.Playfair
import com.almag.audioshelf.ui.theme.Shelf
import java.time.LocalDate

@Composable
fun MoreScreen(lib: Library, vm: ShelfViewModel, contentPadding: PaddingValues) {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(context.contentResolver, uri)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCsv(context.contentResolver, uri)
    }

    LazyColumn(Modifier.fillMaxSize().testTag("page:more"), contentPadding = contentPadding) {
        item {
            Text("Настройки", fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Shelf.white, modifier = Modifier.padding(bottom = 16.dp))
        }
        item {
            ShelfCard(Modifier.padding(bottom = 10.dp)) {
                CardTitle("Цель на год")
                Text(
                    "Сколько книг хочешь прослушать за ${LocalDate.now().year}. Прогресс видно на главной.",
                    fontSize = 13.sp,
                    color = Shelf.sub,
                    lineHeight = 19.sp,
                )
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton("−") { vm.setYearlyGoal(lib.settings.yearlyGoal - if (lib.settings.yearlyGoal > 10) 5 else 1) }
                    Text(
                        if (lib.settings.yearlyGoal == 0) "нет" else "${lib.settings.yearlyGoal}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Shelf.amber,
                        modifier = Modifier.width(84.dp).testTag("goal"),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    RoundButton("+") { vm.setYearlyGoal(lib.settings.yearlyGoal + if (lib.settings.yearlyGoal >= 10) 5 else 1) }
                }
            }
        }
        item {
            ShelfCard(Modifier.padding(bottom = 10.dp)) {
                CardTitle("Данные")
                SettingRow("📤", "Экспорт CSV", "Открывается в Excel и Google Таблицах") {
                    exporter.launch("bookshelf-${LocalDate.now()}.csv")
                }
                SettingRow("📥", "Импорт CSV", "Файл из веб-версии полки или из Excel. Совпадающие книги обновятся") {
                    importer.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream"))
                }
                SettingRow("🗑", "Сбросить к исходным", "Вернуть ${Seed.size} книг из исходного списка", color = Shelf.red) {
                    confirmReset = true
                }
            }
        }
        item {
            ShelfCard(Modifier.padding(bottom = 10.dp)) {
                CardTitle("Подсказки")
                Text(
                    "• Свайп влево-вправо — переключение вкладок\n" +
                        "• Нажми на корешок — карточка книги, долгое нажатие — редактирование\n" +
                        "• Долгое нажатие на ＋ — добавить автора или серию\n" +
                        "• Долгое нажатие на серию или автора — переименовать\n" +
                        "• Данные хранятся только на телефоне, раз в день делается резервная копия",
                    fontSize = 13.sp,
                    color = Shelf.sub,
                    lineHeight = 21.sp,
                )
            }
        }
        item {
            Text(
                "Книжная полка ${BuildConfig.VERSION_NAME}",
                fontSize = 11.sp,
                color = Shelf.muted,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Shelf.bg2,
            title = { Text("Сбросить данные?", fontFamily = Playfair, color = Shelf.white) },
            text = { Text("Все твои изменения будут потеряны, вернутся исходные книги.", color = Shelf.sub) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; vm.resetToSeed() }) { Text("Сбросить", color = Shelf.red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = Shelf.sub) } },
        )
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color = Shelf.muted,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun SettingRow(icon: String, title: String, sub: String, color: Color = Shelf.text, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 11.sp, color = Shelf.muted, lineHeight = 15.sp)
        }
        Text("›", fontSize = 20.sp, color = Shelf.muted)
    }
}

@Composable
private fun RoundButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Shelf.bg3).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Shelf.text)
    }
}
