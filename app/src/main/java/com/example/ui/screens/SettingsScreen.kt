package com.example.ui.screens

/**
 * ZenMath Settings & Preferences
 *
 * Inspired by Shihabul Sakib's open-source ZenMath:
 * Repository: https://github.com/ShihabulSakib/zenmath
 * Original Creator: Shihabul Sakib
 */

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.GameSettings
import com.example.notifications.NotificationScheduler
import com.example.ui.components.ZenSlider
import com.example.ui.components.ZenToggle
import com.example.ui.theme.ZenTheme

private val TIME_SLOTS = listOf(
    Pair("08:00", "8:00 AM"),
    Pair("12:00", "12:00 PM"),
    Pair("15:00", "3:00 PM"),
    Pair("18:00", "6:00 PM"),
    Pair("20:00", "8:00 PM")
)

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSaveSettings: (GameSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSaveSettings(settings.copy(notificationsEnabled = true))
        } else {
            Toast.makeText(context, "Notification permission was denied", Toast.LENGTH_SHORT).show()
            onSaveSettings(settings.copy(notificationsEnabled = false))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .testTag("settings_screen")
    ) {
        // Sticky Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                letterSpacing = (-0.5).sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Appearance Theme Card
            item {
                SectionHeader(title = "APPEARANCE")
                val cardShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, cardShape)
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (settings.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Theme",
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = if (settings.isDarkTheme) "Zen Obsidian" else "PaperZen",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = if (settings.isDarkTheme) "Deep minimalist dark theme" else "Crisp paper-style light theme",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        ZenToggle(
                            checked = settings.isDarkTheme,
                            onCheckedChange = { onSaveSettings(settings.copy(isDarkTheme = it)) }
                        )
                    }
                }
            }

            // Gameplay Configuration
            item {
                SectionHeader(title = "GAMEPLAY")
                val cardShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, cardShape)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        // Total questions slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Questions per session",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "${settings.totalQuestions}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.primary
                                )
                            }
                            ZenSlider(
                                value = settings.totalQuestions.toFloat(),
                                onValueChange = { onSaveSettings(settings.copy(totalQuestions = it.toInt())) },
                                valueRange = 1f..50f
                            )
                        }

                        // Time limit slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Time limit per question",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "${settings.timeLimit}s",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.primary
                                )
                            }
                            ZenSlider(
                                value = settings.timeLimit.toFloat(),
                                onValueChange = { onSaveSettings(settings.copy(timeLimit = it.toInt())) },
                                valueRange = 5f..60f
                            )
                        }

                        // Daily Goal Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily Goal (sessions)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textMain
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { onSaveSettings(settings.copy(dailyGoal = maxOf(1, settings.dailyGoal - 1))) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                        .border(1.dp, colors.cardBorder, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = colors.textMain, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = settings.dailyGoal.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMain,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = { onSaveSettings(settings.copy(dailyGoal = minOf(100, settings.dailyGoal + 1))) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                        .border(1.dp, colors.cardBorder, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = colors.textMain, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Adaptive Difficulty & Streak
            item {
                SectionHeader(title = "ASSISTANCE & MOTIVATION")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val cardShape = RoundedCornerShape(20.dp)

                    // Adaptive Difficulty toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
                                    Text("Adaptive Difficulty", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                    Text("Auto-escalate digits when accuracy ≥ 85%", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                            ZenToggle(
                                checked = settings.adaptiveDifficulty,
                                onCheckedChange = { onSaveSettings(settings.copy(adaptiveDifficulty = it)) }
                            )
                        }
                    }

                    // Show Streak toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
                                    Text("Show Streak Flame", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                    Text("Display active streak during gameplay", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                            ZenToggle(
                                checked = settings.showStreak,
                                onCheckedChange = { onSaveSettings(settings.copy(showStreak = it)) }
                            )
                        }
                    }

                    // Haptic Keystrokes toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Vibration, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
                                    Text("Haptic Keystrokes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                    Text("Vibrate on keypad number and button presses", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                            ZenToggle(
                                checked = settings.hapticFeedback,
                                onCheckedChange = { onSaveSettings(settings.copy(hapticFeedback = it)) }
                            )
                        }
                    }
                }
            }

            // Daily Reminders
            item {
                SectionHeader(title = "DAILY PRACTICE REMINDERS")
                val cardShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, cardShape)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (settings.notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text("Daily Reminder", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                    Text("Native on-device habit reminders", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }

                            ZenToggle(
                                checked = settings.notificationsEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            onSaveSettings(settings.copy(notificationsEnabled = true))
                                        }
                                    } else {
                                        onSaveSettings(settings.copy(notificationsEnabled = false))
                                    }
                                }
                            )
                        }

                        if (settings.notificationsEnabled) {
                            Text(
                                text = "REMINDER TIMES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = colors.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            // Time slot chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TIME_SLOTS.forEach { (timeVal, timeLabel) ->
                                    val isSelected = settings.notificationTimes.contains(timeVal)
                                    val chipShape = RoundedCornerShape(10.dp)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(chipShape)
                                            .background(if (isSelected) colors.primary else colors.surface)
                                            .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, chipShape)
                                            .clickable {
                                                val updated = settings.notificationTimes.toMutableSet()
                                                if (isSelected) updated.remove(timeVal) else updated.add(timeVal)
                                                onSaveSettings(settings.copy(notificationTimes = updated))
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = timeLabel.replace(":00", "").replace(" ", ""),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colors.onPrimary else colors.textSecondary
                                        )
                                    }
                                }
                            }

                            // Test notification button
                            Button(
                                onClick = {
                                    NotificationScheduler.sendTestNotification(context)
                                    Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary.copy(alpha = 0.08f),
                                    contentColor = colors.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Send Test Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Background Optimization / Battery Exemption Prompt
                            val powerManager = remember(context) {
                                context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                            }
                            var isIgnoringBatteryOptimizations by remember(context, settings.notificationsEnabled) {
                                mutableStateOf(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
                                    } else {
                                        true
                                    }
                                )
                            }

                            // Re-check whitelist state whenever returning to Settings screen
                            LifecycleResumeEffect(Unit) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    isIgnoringBatteryOptimizations =
                                        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
                                }
                                onPauseOrDispose { }
                            }

                            if (!isIgnoringBatteryOptimizations) {
                                val bannerShape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(bannerShape)
                                        .background(colors.primary.copy(alpha = 0.06f))
                                        .border(1.dp, colors.primary.copy(alpha = 0.2f), bannerShape)
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Allow background activity for timely reminders",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                        }
                                        Text(
                                            text = "Deep sleep mode and OEM battery savers may suppress notifications when the app is idle. Whitelisting ZenMath ensures daily alarms fire promptly.",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            lineHeight = 15.sp
                                        )
                                        Button(
                                            onClick = {
                                                var launched = false
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    try {
                                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                            data = Uri.parse("package:${context.packageName}")
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(intent)
                                                        launched = true
                                                    } catch (_: Exception) {
                                                        // OEM ROMs (or devices without support) might reject direct whitelist prompt
                                                    }
                                                }

                                                if (!launched) {
                                                    // Fallback 1: App Info settings (where battery/background permission can be toggled)
                                                    try {
                                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                            data = Uri.parse("package:${context.packageName}")
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(intent)
                                                        Toast.makeText(context, "Tap Battery > Allow background activity", Toast.LENGTH_LONG).show()
                                                        launched = true
                                                    } catch (_: Exception) {
                                                        // Fallback 2: General battery optimization settings
                                                        try {
                                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                            launched = true
                                                        } catch (_: Exception) {
                                                            Toast.makeText(context, "Please allow background activity in system settings", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.primary,
                                                contentColor = colors.onPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                        ) {
                                            Text("Allow Background Activity", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // App About & Attribution Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "ZenMath App Icon",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ZenMath",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        letterSpacing = (-0.3).sp
                    )

                    Text(
                        text = "v1.0.0 • Native Android Edition",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contribution & Inspiration badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Contribution",
                            tint = colors.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Inspired by ShihabulSakib/zenmath",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain
                        )
                    }

                    Text(
                        text = "Original Creator: Shihabul Sakib",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = ZenTheme.colors
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = colors.textSecondary.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
