package dev.mariinkys.openPillReminder.ui.home

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mariinkys.openPillReminder.model.PillLog
import dev.mariinkys.openPillReminder.model.SettingsState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val PAGE_SIZE = 28
private const val TOTAL_DAYS = 365

@Composable
fun HomeScreen(
    settings: SettingsState,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    notificationDate: String? = null,
) {
    val pillLogs by viewModel.pillLogs.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val firstDate = settings.firstPillDate
    val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1)

    val allDates = List(TOTAL_DAYS) { index -> firstDate.plusDays(index.toLong()) }
    val pages = allDates.chunked(PAGE_SIZE)
    val totalPages = pages.size

    val today = LocalDate.now()
    val todayIndex = allDates.indexOfFirst { it == today || it.isAfter(today) }.coerceAtLeast(0)
    val initialPage = (todayIndex / PAGE_SIZE).coerceIn(0, totalPages - 1)

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(notificationDate) {
        notificationDate?.let { selectedDate = LocalDate.parse(it) }
    }

    // determine grid layout based on orientation
    val daysPerColumn = if (isLandscape) 4 else 7
    val daysPerRow = if (isLandscape) 7 else 4

    Column(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val pageDates = pages[pageIndex]

            val firstVisibleDate = pageDates.first()
            val lastVisibleDate = pageDates.last()
            val monthRangeText = if (firstVisibleDate.month == lastVisibleDate.month) {
                firstVisibleDate.format(monthFormatter)
            } else {
                "${firstVisibleDate.format(monthFormatter)} - ${lastVisibleDate.format(monthFormatter)}"
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // calculate bubble size
                val maxWidth = maxWidth - 32.dp
                val maxHeight = maxHeight - 64.dp
                val bubbleSize = minOf(maxWidth / (daysPerRow + 1), maxHeight / (daysPerColumn + 1)).coerceIn(48.dp, 80.dp)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val chunkedByColumn = pageDates.chunked(daysPerColumn)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chunkedByColumn.forEachIndexed { colIdx, columnDates ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                columnDates.forEachIndexed { rowIdx, date ->
                                    val absoluteIndex = (pageIndex * PAGE_SIZE) + (colIdx * daysPerColumn) + rowIdx
                                    val positionInCycle = absoluteIndex % cycleLength
                                    val isBreakDay = positionInCycle >= settings.activePills
                                    val log = pillLogs[date]
                                    val isTaken = log?.taken == true
                                    val isFuture = date.isAfter(today)

                                    PillBubble(
                                        date = date,
                                        isBreakDay = isBreakDay,
                                        isTaken = isTaken,
                                        isFuture = isFuture,
                                        size = bubbleSize,
                                        onClick = { if (!isFuture) selectedDate = date }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = monthRangeText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Dialog handling
    selectedDate?.let { date ->
        val absoluteIndex = allDates.indexOf(date)
        val positionInCycle = if (cycleLength > 0) absoluteIndex % cycleLength else 0
        val isBreakDay = positionInCycle >= settings.activePills
        val existingLog = pillLogs[date] ?: PillLog(date = date)

        PillLogDialog(
            date = date,
            isBreakDay = isBreakDay,
            log = existingLog,
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                selectedDate = null },
            onSave = { updatedLog ->
                viewModel.saveLog(updatedLog)
                @Suppress("AssignedValueIsNeverRead")
                selectedDate = null
            }
        )
    }
}

@Composable
fun PillBubble(
    date: LocalDate,
    isBreakDay: Boolean,
    isTaken: Boolean,
    isFuture: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    val formatterDay = DateTimeFormatter.ofPattern("EEE")
    val formatterDate = DateTimeFormatter.ofPattern("d")
    val isToday = date == LocalDate.now()

    val colors = MaterialTheme.colorScheme
    val targetColor = when {
        isTaken -> colors.primary
        isBreakDay -> colors.secondaryContainer
        else -> colors.tertiaryContainer
    }

    val contentColor = when {
        isTaken -> colors.onPrimary
        isBreakDay -> colors.onSecondaryContainer
        else -> colors.onTertiaryContainer
    }

    val animatedColor by animateColorAsState(targetValue = targetColor, label = "color")
    val scale by animateFloatAsState(targetValue = if (isTaken) 1.05f else 1f, label = "scale")

    val fontSize = (size.value * 0.2f).sp

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isFuture) 0.3f else 1f
            }
            .clip(CircleShape)
            .background(animatedColor)
            .then(
                if (isToday) Modifier.border(3.dp, colors.primary, CircleShape) else Modifier
            )
            .then(
                if (!isFuture) Modifier.clickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.format(formatterDay).uppercase(),
                color = contentColor,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize * 0.8f)
            )
            Text(
                text = date.format(formatterDate),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            )
        }
    }
}

@Composable
fun PillLogDialog(
    date: LocalDate,
    isBreakDay: Boolean,
    log: PillLog,
    onDismiss: () -> Unit,
    onSave: (PillLog) -> Unit
) {
    var taken by remember { mutableStateOf(log.taken) }
    var note by remember { mutableStateOf(log.note) }
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(formatter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isBreakDay) "Placebo taken" else "Pill taken")
                    Switch(checked = taken, onCheckedChange = { taken = it })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(log.copy(taken = taken, note = note)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}