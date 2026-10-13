package org.shawnz.dailytracker.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.data.ReminderMode
import org.shawnz.dailytracker.ui.PageMargin
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val PRIVACY_POLICY_URL = "https://shawnz.org/daily-tracker/privacy-policy.html"

private val LEAD_TIME_CHOICES = listOf(1L, 2L, 3L, 4L, 6L, 8L, 12L).map(Duration::ofHours)

/** The space between the edge of an icon button and its icon. */
private val ButtonInset = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.reminder.collectAsStateWithLifecycle()
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()
    val versionName =
        remember {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }

    // Whether notifications are permitted. Requested when reminders are switched on, and checked
    // again on resume because it can be changed in the system settings.
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    val askForPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> allowed = granted }
    LifecycleResumeEffect(Unit) {
        allowed = notificationsAllowed(context)
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val reminder = settings ?: return@Scaffold
        val enabled = reminder.enabled
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageMargin, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_reminders))
                    Text(
                        stringResource(R.string.settings_reminders_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        viewModel.setEnabled(it)
                        if (it && !allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            askForPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }

            HorizontalDivider()
            val fixedTime = reminder.mode == ReminderMode.FIXED_TIME
            var pickingTime by remember { mutableStateOf(false) }
            if (pickingTime) {
                ReminderTimeDialog(
                    time = reminder.time,
                    onPick = {
                        viewModel.pickTime(it)
                        pickingTime = false
                    },
                    onDismiss = { pickingTime = false },
                )
            }
            val hours = reminder.leadTime.toHours().toInt()
            val shorter = LEAD_TIME_CHOICES.lastOrNull { it < reminder.leadTime }
            val longer = LEAD_TIME_CHOICES.firstOrNull { it > reminder.leadTime }
            Column(Modifier.selectableGroup()) {
                ModeRow(
                    title =
                        stringResource(R.string.settings_reminder_fixed, timeText(reminder.time)),
                    body = stringResource(R.string.settings_reminder_fixed_body),
                    selected = fixedTime,
                    enabled = enabled,
                    onClick = { viewModel.setMode(ReminderMode.FIXED_TIME) },
                ) {
                    TextButton(onClick = { pickingTime = true }, enabled = enabled) {
                        Text(stringResource(R.string.action_edit))
                    }
                }
                ModeRow(
                    title = pluralStringResource(R.plurals.settings_reminder_before_reset, hours, hours),
                    body = stringResource(R.string.settings_reminder_before_reset_body),
                    selected = !fixedTime,
                    enabled = enabled,
                    onClick = { viewModel.setMode(ReminderMode.BEFORE_RESET) },
                ) {
                    IconButton(
                        onClick = { shorter?.let(viewModel::pickLeadTime) },
                        enabled = enabled && shorter != null,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_remove),
                            stringResource(R.string.settings_reminder_lead_time_less),
                        )
                    }
                    IconButton(
                        onClick = { longer?.let(viewModel::pickLeadTime) },
                        enabled = enabled && longer != null,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_add),
                            stringResource(R.string.settings_reminder_lead_time_more),
                        )
                    }
                }
            }
            HorizontalDivider()

            // Warn the user when reminders are on but notifications are not permitted.
            if (enabled && !allowed) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { openNotificationSettings(context) }
                            .padding(horizontal = PageMargin, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_notification_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                HorizontalDivider()
            }

            val appVersion =
                stringResource(
                    R.string.settings_app_version,
                    stringResource(R.string.app_name),
                    versionName,
                )
            val privacyPolicy = stringResource(R.string.settings_privacy_policy)
            val linkStyles =
                TextLinkStyles(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                )
            Text(
                buildAnnotatedString {
                    append(appVersion)
                    append(" - ")
                    withLink(
                        LinkAnnotation.Clickable("privacy-policy", linkStyles) {
                            CustomTabLauncher.launch(context, PRIVACY_POLICY_URL, toolbarColor)
                        },
                    ) { append(privacyPolicy) }
                },
                modifier = Modifier.padding(horizontal = PageMargin, vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A reminder mode, with [controls] for its value at the end of the row. The mode is selected
 * by a tap on the row, and [controls] are separate targets.
 */
@Composable
private fun ModeRow(
    title: String,
    body: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    controls: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).padding(start = PageMargin, end = PageMargin - ButtonInset, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(title)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        controls()
    }
}

/**
 * A dialog for choosing a time of day. It opens at [time], and has a dial or typed entry.
 *
 * The structure is from `TimePickerSwitchableSample` in the Material 3 samples.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    time: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
    var isPickerMode by rememberSaveable { mutableStateOf(true) }
    val displayMode =
        if (isPickerMode) {
            TimePickerDisplayMode.Picker
        } else {
            TimePickerDisplayMode.Input
        }
    val windowHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp()
        }
    val roomForDial = windowHeight > TimePickerDialogDefaults.MinHeightForTimePicker

    TimePickerDialog(
        title = { TimePickerDialogDefaults.Title(displayMode = displayMode) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        modeToggleButton = {
            if (roomForDial) {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = { isPickerMode = !isPickerMode },
                    displayMode = displayMode,
                )
            }
        },
    ) {
        if (displayMode == TimePickerDisplayMode.Picker && roomForDial) {
            TimePicker(state = state)
        } else {
            TimeInput(state = state)
        }
    }
}

/** Formats [time] for the device's locale and the device's 12-hour or 24-hour clock setting. */
@Composable
private fun timeText(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return time.format(DateTimeFormatter.ofPattern(pattern, locale))
}

/** POST_NOTIFICATIONS does not exist below Android 13, so notifications are always allowed. */
private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

/** Opens this app's notification settings, where a refused permission can be granted. */
private fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}
