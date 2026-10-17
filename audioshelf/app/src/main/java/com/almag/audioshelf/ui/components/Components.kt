package com.almag.audioshelf.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almag.audioshelf.core.Book
import com.almag.audioshelf.core.Spines
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.ui.theme.Inter
import com.almag.audioshelf.ui.theme.Shelf

val CardShape = RoundedCornerShape(14.dp)
val SmallShape = RoundedCornerShape(9.dp)

fun spineColor(book: Book) = Color(Spines.colorFor(book))
fun spineColor(key: String) = Color(Spines.color(key))

/** Lightens (+) or darkens (-) a colour by [delta] per channel, like the web version's shade(). */
fun Color.shade(delta: Int): Color {
    fun ch(v: Float) = ((v * 255 + delta).coerceIn(0f, 255f)) / 255f
    return Color(ch(red), ch(green), ch(blue), alpha)
}

/** Lays a child out rotated by 90°, so text reads along a book spine. */
fun Modifier.vertical() = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = 0, maxWidth = constraints.maxHeight,
            minHeight = 0, maxHeight = constraints.maxWidth,
        ),
    )
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width - placeable.height) / 2,
            y = -(placeable.height - placeable.width) / 2,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    shape: RoundedCornerShape = CardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var m = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(Shelf.bg2)
        .border(1.dp, Shelf.line, shape)
    if (onClick != null || onLongClick != null) {
        m = m.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick?.let { { haptics.performHapticFeedback(HapticFeedbackType.LongPress); it() } },
        )
    }
    Column(m.padding(padding), content = content)
}

/** Small caps heading between sections, with an optional link on the right. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 10.sp),
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Text(
                action,
                color = Shelf.amber,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(SmallShape).clickable(onClick = onAction).padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun StatusBadge(status: Status, modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        status.label,
        color = Shelf.color(status),
        fontSize = if (large) 13.sp else 10.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(Shelf.background(status))
            .padding(horizontal = if (large) 14.dp else 9.dp, vertical = if (large) 5.dp else 3.dp),
    )
}

@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier, color: Color = Shelf.amber, height: Dp = 5.dp) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Shelf.bg3),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Little coloured book used in list rows, with the title written along it. */
@Composable
fun MiniSpine(book: Book, modifier: Modifier = Modifier) {
    val color = spineColor(book)
    Box(
        modifier
            .size(38.dp, 52.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            book.title,
            color = Shelf.bg,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.vertical().rotate(90f).padding(horizontal = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookRow(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSeries: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(SmallShape)
            .background(Shelf.bg2)
            .border(1.dp, Shelf.line, SmallShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let { { haptics.performHapticFeedback(HapticFeedbackType.LongPress); it() } },
            )
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("book:${book.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniSpine(book)
        Column(Modifier.weight(1f)) {
            Text(book.title, color = Shelf.white, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
            val meta = buildString {
                append(book.author)
                if (showSeries && book.series.isNotEmpty()) append(" · ").append(book.series)
            }
            Text(meta, color = Shelf.sub, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.note.isNotEmpty()) {
                Text(
                    book.note,
                    color = Shelf.amber,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (book.status == Status.LISTENING && book.progress > 0) {
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressLine(book.progress / 100f, Modifier.weight(1f), height = 4.dp)
                    Text("${book.progress}%", color = Shelf.amber, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(book.status)
            if (book.rating > 0) {
                Row(Modifier.padding(top = 6.dp)) {
                    repeat(book.rating) {
                        Icon(Icons.Rounded.Star, null, tint = Shelf.amber, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
    }
}

/** A book standing on the shelf. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Spine(book: Book, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val base = spineColor(book)
    val dim = book.status == Status.WANT || book.status == Status.DROPPED
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .width(30.dp)
            .height(Spines.height(book.title).dp)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .background(Brush.verticalGradient(listOf(base, base.shade(-18))))
            .background(
                Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.12f),
                    0.12f to Color.Transparent,
                    0.8f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f),
                ),
            )
            .let { if (dim) it.background(Shelf.bg.copy(alpha = 0.5f)) else it }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() },
            )
            .semantics { contentDescription = "${book.title}, ${book.status.label}" }
            .testTag("spine:${book.id}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            book.title,
            color = Color.White.copy(alpha = if (dim) 0.6f else 0.92f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 1f), 2f)),
            modifier = Modifier.vertical().rotate(90f).padding(horizontal = 10.dp),
        )
        if (book.status != Status.LISTENED) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Shelf.color(book.status)),
            )
        }
    }
}

@Composable
fun ShelfBoard(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF4A3728), Color(0xFF2E2118)))),
    )
}

@Composable
fun EmptyState(emoji: String, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Shelf.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, fontSize = 14.sp, color = Shelf.sub, textAlign = TextAlign.Center, lineHeight = 21.sp)
    }
}

@Composable
fun Avatar(name: String, initials: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(spineColor(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Shelf.bg, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.4f).sp)
    }
}

@Composable
fun Stars(rating: Int, onChange: ((Int) -> Unit)?, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            val filled = i <= rating
            Icon(
                if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = "$i из 5",
                tint = if (filled) Shelf.amber else Shelf.muted,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .let { m -> if (onChange != null) m.clickable { onChange(if (rating == i) 0 else i) } else m }
                    .padding(2.dp),
            )
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Shelf.amber) {
    val shape = CircleShape
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) color else Shelf.sub,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.12f) else Shelf.bg3)
            .border(BorderStroke(1.5.dp, if (selected) color else Shelf.line), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(SmallShape)
            .background(Shelf.bg2)
            .border(1.5.dp, Shelf.line, SmallShape)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Shelf.muted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp)) {
            if (value.isEmpty()) Text(placeholder, color = Shelf.muted, fontSize = 15.sp, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Shelf.text, fontSize = 15.sp, fontFamily = Inter),
                cursorBrush = SolidColor(Shelf.amber),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                    .testTag("search"),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onChange("") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Close, "Очистить", tint = Shelf.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
