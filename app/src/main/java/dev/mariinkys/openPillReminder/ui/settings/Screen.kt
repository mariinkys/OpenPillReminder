package dev.mariinkys.openPillReminder.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.mariinkys.openPillReminder.model.SettingsState
import dev.mariinkys.openPillReminder.model.ThemeMode
import dev.mariinkys.openPillReminder.model.toDisplayString
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsState,
    onSettingsChange: (SettingsState) -> Unit,
    backupState: SettingsViewModel.BackupUiState,
    onCreateBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var showColorPicker by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { onCreateBackup(it) } }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreConfirmDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PermissionWarnings()

        // PROFILE
        SettingsSection(title = "Profile") {
            OutlinedTextField(
                value = settings.userName,
                onValueChange = { onSettingsChange(settings.copy(userName = it)) },
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // PILL SCHEDULE
        SettingsSection(title = "Pill Schedule") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = settings.activePills.toString(),
                    onValueChange = { onSettingsChange(settings.copy(activePills = it.toIntOrNull() ?: 0)) },
                    label = { Text("Active Pills") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = settings.breakDays.toString(),
                    onValueChange = { onSettingsChange(settings.copy(breakDays = it.toIntOrNull() ?: 0)) },
                    label = { Text("Break Days") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            SettingsSwitchRow(
                label = "Placebo Pills on Break Days",
                checked = settings.placebo,
                onCheckedChange = { onSettingsChange(settings.copy(placebo = it)) },
                description = "Reminds you to take non-active pills so you don't lose the habit.",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("First Pill Date", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { showDatePicker = true }) {
                    Text(settings.firstPillDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                }
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reminder Time", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onSettingsChange(settings.copy(reminderTime = LocalTime.of(hour, minute)))
                        },
                        settings.reminderTime.hour,
                        settings.reminderTime.minute,
                        true
                    ).show()
                }) {
                    Text(settings.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }
        }

        // Pill Buying Reminder
        SettingsSection(title = "Pill Buying Reminder") {
            SettingsSwitchRow(
                label = "Enable Buying Reminder",
                checked = settings.buyingReminder,
                onCheckedChange = { onSettingsChange(settings.copy(buyingReminder = it)) }
            )

            if (settings.buyingReminder) {
                var showBuyingScheduleMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reminder Day", style = MaterialTheme.typography.bodyLarge)
                    Box {
                        TextButton(onClick = { showBuyingScheduleMenu = true }) {
                            Text(settings.buyingReminderSchedule.toDisplayString())
                        }
                        DropdownMenu(
                            expanded = showBuyingScheduleMenu,
                            onDismissRequest = { showBuyingScheduleMenu = false }
                        ) {
                            dev.mariinkys.openPillReminder.model.BuyingReminderSchedule.entries.forEach { schedule ->
                                DropdownMenuItem(
                                    text = { Text(schedule.toDisplayString()) },
                                    onClick = {
                                        onSettingsChange(settings.copy(buyingReminderSchedule = schedule))
                                        showBuyingScheduleMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Time Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reminder Time", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onSettingsChange(settings.copy(buyingReminderTime = LocalTime.of(hour, minute)))
                            },
                            settings.buyingReminderTime.hour,
                            settings.buyingReminderTime.minute,
                            true
                        ).show()
                    }) {
                        Text(settings.buyingReminderTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
            }
        }


        // APPEARANCE
        SettingsSection(title = "Appearance") {
            // Theme Selection
            Text("Theme Mode", style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onSettingsChange(settings.copy(themeMode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                    ) {
                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsSwitchRow(
                    label = "Dynamic Color (Material You)",
                    checked = settings.useDynamicColor,
                    onCheckedChange = { onSettingsChange(settings.copy(useDynamicColor = it)) }
                )
            }

            if (!settings.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Text("Accent Color", style = MaterialTheme.typography.bodyMedium)
                val colorOptions = listOf(0xFF6750A4, 0xFF006A60, 0xFF984061, 0xFF3D662F, 0xFF005FAF)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { colorLong ->
                        val colorInt = colorLong.toInt()
                        ColorDot(
                            color = Color(colorLong),
                            isSelected = settings.seedColor == colorInt,
                            onClick = { onSettingsChange(settings.copy(seedColor = colorInt)) }
                        )
                    }
                    AddColorDot(onClick = { showColorPicker = true })
                }
            }
        }

        // BACKUP
        SettingsSection(title = "Backup") {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val timestamp = java.time.LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                            createBackupLauncher.launch("openpillreminder_$timestamp.json")
                        }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Create Backup", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Export all settings and pill history to a file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { restoreBackupLauncher.launch(arrayOf("application/json")) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Restore Backup", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "This will overwrite your current data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (backupState is SettingsViewModel.BackupUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        // ABOUT
        val uriHandler = LocalUriHandler.current
        SettingsSection(title = "About") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://github.com/mariinkys") }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Author",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "mariinkys",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Repository Link
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/mariinkys/OpenPillReminder") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Text("View Repository", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Issues Link
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/mariinkys/OpenPillReminder/issues") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Text("Report an Issue", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // License Link
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/mariinkys/OpenPillReminder/blob/main/LICENSE") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Text("License Info (GPL-3.0)", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Donation Link
            Button(
                onClick = { uriHandler.openUri("https://www.buymeacoffee.com/mariinkys") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text("Support the Project ❤️")
            }
        }

        // Version Name
        val versionName = remember {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = {
                uriHandler.openUri("https://github.com/mariinkys/OpenPillReminder/releases")
            }) {
                Text(
                    text = "v$versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                @Suppress("AssignedValueIsNeverRead")
                showRestoreConfirmDialog = false
                @Suppress("AssignedValueIsNeverRead")
                pendingRestoreUri = null
            },
            title = { Text("Restore Backup?") },
            text = { Text("This will replace all your current settings and pill history. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri?.let { onRestoreBackup(it) }
                        @Suppress("AssignedValueIsNeverRead")
                        showRestoreConfirmDialog = false
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showRestoreConfirmDialog = false
                    @Suppress("AssignedValueIsNeverRead")
                    pendingRestoreUri = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = Color(settings.seedColor),
            onColorSelected = { color ->
                onSettingsChange(settings.copy(seedColor = color.toArgb()))
                @Suppress("AssignedValueIsNeverRead")
                showColorPicker = false
            },
            onDismiss = {
                @Suppress("AssignedValueIsNeverRead")
                showColorPicker = false
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                @Suppress("AssignedValueIsNeverRead")
                showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onSettingsChange(settings.copy(firstPillDate = date))
                    }
                    @Suppress("AssignedValueIsNeverRead")
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    showDatePicker = false }
                ) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, description: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ColorDot(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color)
        }
        if (isSelected) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = Color.White)
            }
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor.toArgb(), initialHsv)

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val selectedColor by remember(hue, saturation, value) {
        derivedStateOf {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Pick a Color", style = MaterialTheme.typography.titleLarge)

                SaturationValueBox(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSaturationValueChange = { s, v ->
                        saturation = s
                        value = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                )

                // Hue slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Hue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HueSlider(
                        hue = hue,
                        onHueChange = { hue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                }

                // preview + hex label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                    )
                    val hexString = remember(selectedColor) {
                        "#%06X".format(selectedColor.toArgb() and 0xFFFFFF)
                    }
                    Text(
                        hexString,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(selectedColor) }) { Text("Select") }
                }
            }
        }
    }
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

    BoxWithConstraints(modifier = modifier) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onSaturationValueChange(
                            (offset.x / boxW).coerceIn(0f, 1f),
                            (1f - offset.y / boxH).coerceIn(0f, 1f)
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onSaturationValueChange(
                            (change.position.x / boxW).coerceIn(0f, 1f),
                            (1f - change.position.y / boxH).coerceIn(0f, 1f)
                        )
                    }
                }
        ) {
            // white-to-hue horizontal gradient
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
            // transparent-to-black vertical gradient (darkness)
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

            // Thumb
            val thumbX = saturation * size.width
            val thumbY = (1f - value) * size.height
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(thumbX, thumbY))
            drawCircle(
                color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))),
                radius = 7.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
            )
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val sliderW = constraints.maxWidth.toFloat()
        val sliderH = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onHueChange((offset.x / sliderW * 360f).coerceIn(0f, 360f))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onHueChange((change.position.x / sliderW * 360f).coerceIn(0f, 360f))
                    }
                }
        ) {
            drawRect(brush = hueGradient)

            val thumbX = hue / 360f * size.width
            val thumbY = size.height / 2f
            drawCircle(color = Color.White, radius = sliderH / 2f, center = Offset(thumbX, thumbY))
            drawCircle(
                color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                radius = sliderH / 2f - 3.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
        }
    }
}

@Composable
private fun AddColorDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Custom color",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PermissionWarnings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember { mutableStateOf(true) }
    var hasExactAlarmPermission by remember { mutableStateOf(true) }

    // re-check permissions every time the user returns to this screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!hasNotificationPermission) {
            WarningBanner(
                text = "Notifications are disabled. The app won't be able to remind you.",
                buttonText = "Enable",
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )
        }

        if (!hasExactAlarmPermission) {
            WarningBanner(
                text = "Exact alarms are disabled. Reminders might not fire on time.",
                buttonText = "Fix",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }
    }
}

@Composable
private fun WarningBanner(text: String, buttonText: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(buttonText)
            }
        }
    }
}