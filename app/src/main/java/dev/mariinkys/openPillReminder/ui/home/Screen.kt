package dev.mariinkys.openPillReminder.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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

    if (!settings.active) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tracking is not active")
        }
        return
    }

    val firstDate = settings.firstPillDate
    val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1)

    // Generate 1 year of dates from firstDate
    val allDates = List(TOTAL_DAYS) { index -> firstDate.plusDays(index.toLong()) }
    val pages = allDates.chunked(PAGE_SIZE)
    val totalPages = pages.size

    // Start on the page that contains today
    val today = LocalDate.now()
    val todayIndex = allDates.indexOfFirst { it == today || it.isAfter(today) }.coerceAtLeast(0)
    val initialPage = (todayIndex / PAGE_SIZE).coerceIn(0, totalPages - 1)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // If we opened the app from a notification we grab the date from the notification
    LaunchedEffect(notificationDate) {
        notificationDate?.let {
            selectedDate = LocalDate.parse(it)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Page indicator dots
        //        if (totalPages > 1) {
        //            Row(
        //                modifier = Modifier
        //                    .fillMaxWidth()
        //                    .padding(vertical = 8.dp),
        //                horizontalArrangement = Arrangement.Center
        //            ) {
        //                repeat(totalPages) { index ->
        //                    Box(
        //                        modifier = Modifier
        //                            .padding(horizontal = 3.dp)
        //                            .size(if (pagerState.currentPage == index) 8.dp else 5.dp)
        //                            .clip(CircleShape)
        //                            .background(
        //                                if (pagerState.currentPage == index) Color(0xFFFF6F91)
        //                                else Color.LightGray
        //                            )
        //                    )
        //                }
        //            }
        //        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pageDates = pages[pageIndex]
            val startIndex = pageIndex * PAGE_SIZE

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val chunked = pageDates.chunked(7)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunked.forEachIndexed { colIdx, columnDates ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            columnDates.forEachIndexed { rowIdx, date ->
                                val absoluteIndex = startIndex + colIdx * 7 + rowIdx
                                // Determine position within cycle
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
                                    onClick = {
                                        if (!isFuture) selectedDate = date
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

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
fun PillLogDialog(
    date: LocalDate,
    isBreakDay: Boolean,
    log: PillLog,
    onDismiss: () -> Unit,
    onSave: (PillLog) -> Unit
) {
    var taken by remember { mutableStateOf(log.taken) }
    var note by remember { mutableStateOf(log.note) }
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(formatter)) },
        text = {
            Column {
                val label = if (isBreakDay) "Placebo taken" else "Pill taken"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label)
                    Switch(checked = taken, onCheckedChange = { taken = it })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(log.copy(taken = taken, note = note)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PillBubble(
    date: LocalDate,
    isBreakDay: Boolean,
    isTaken: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit
) {
    val formatterDay = DateTimeFormatter.ofPattern("EEE")
    val formatterDate = DateTimeFormatter.ofPattern("d")
    val today = LocalDate.now()
    val isToday = date == today

    val targetColor = when {
        isTaken -> Color(0xFF4CAF50)
        isBreakDay -> Color(0xFF838080)
        else -> Color(0xFFFF6F91)
    }

    val animatedColor by animateColorAsState(targetValue = targetColor, label = "")
    val scale by animateFloatAsState(targetValue = if (isTaken) 1.1f else 1f, label = "")

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isFuture) 0.4f else 1f
            }
            .clip(CircleShape)
            .background(animatedColor)
            .then(
                if (isToday) Modifier.border(2.dp, Color.Yellow, CircleShape) else Modifier
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
            Text(date.format(formatterDay))
            Text(date.format(formatterDate))
        }
    }
}