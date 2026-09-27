package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SessionEntity
import com.example.domain.DayBucket
import com.example.domain.RecentPerformanceDay
import com.example.domain.StatsData
import com.example.ui.components.ActivityCalendar
import com.example.ui.components.HorizonWaveChart
import com.example.ui.components.SevenDayQuestionsChart
import com.example.ui.theme.ZenTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    statsData: StatsData,
    sessions: List<SessionEntity>,
    recentPerformance: List<RecentPerformanceDay>,
    horizonWaveBuckets: List<DayBucket>,
    dailyGoal: Int,
    onClearAllData: () -> Unit,
    onExportBackup: suspend () -> String,
    onImportBackup: suspend (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }

    // Storage Access Framework Launcher for downloading/saving JSON file
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = onExportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Backup JSON file downloaded and saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Storage Access Framework Launcher for importing ONLY JSON files from device
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    var fileName = ""
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    if (fileName.isNotEmpty() && !fileName.endsWith(".json", ignoreCase = true)) {
                        Toast.makeText(context, "Only .json backup files can be imported!", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val jsonText = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    }
                    if (!jsonText.isNullOrEmpty()) {
                        onImportBackup(jsonText)
                        Toast.makeText(context, "Backup restored from file successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Selected file was empty", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .testTag("stats_screen")
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
                text = "Statistics",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2x2 Top Summary Metric Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "SESSIONS",
                            value = statsData.totalSessions.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "QUESTIONS",
                            value = statsData.totalQuestions.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "ACCURACY",
                            value = "${statsData.avgAccuracy.toInt()}%",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "STREAK",
                            value = "${statsData.streakDays}d",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. HorizonWave Session Flow Chart
            item {
                HorizonWaveChart(buckets = horizonWaveBuckets)
            }

            // 2. Last 7 Days Activity & Question Count Bar Chart
            item {
                SevenDayQuestionsChart(performanceDays = recentPerformance)
            }

            // 3. Monthly Activity Calendar
            if (sessions.isNotEmpty()) {
                item {
                    ActivityCalendar(sessions = sessions, goal = dailyGoal)
                }
            }

            // Mode Performance List
            if (statsData.modes.isNotEmpty()) {
                item {
                    val cardShape = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "MODE PERFORMANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = colors.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            statsData.modes.entries.sortedByDescending { it.value.sessions }.forEach { (modeKey, modeStat) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = modeKey.replace("-", " ").replaceFirstChar { it.uppercase() },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = "${modeStat.sessions} session${if (modeStat.sessions > 1) "s" else ""}",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${modeStat.accuracy.toInt()}%",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (modeStat.accuracy >= 80) colors.correct else if (modeStat.accuracy >= 50) colors.primary else colors.incorrect
                                        )
                                        Text(
                                            text = String.format("%.1fs avg", modeStat.avgTimeMs / 1000.0),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Best Mode Card
            val bestMode = statsData.modes.maxByOrNull { it.value.accuracy }
            if (bestMode != null && bestMode.value.sessions > 0) {
                item {
                    val cardShape = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "BEST MODE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = colors.textSecondary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = bestMode.key.replace("-", " ").replaceFirstChar { it.uppercase() },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "${bestMode.value.accuracy.toInt()}% accuracy across ${bestMode.value.sessions} sessions",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Backup & Data Management Card (File Download & Device File Import)
            item {
                val cardShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, cardShape)
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BACKUP & PROTECTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = colors.textSecondary.copy(alpha = 0.6f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.correct)
                                )
                                Text(
                                    text = "Hard Device Storage (SQLite)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.correct
                                )
                            }
                        }

                        Text(
                            text = "Sessions and stats are saved as hard persistent data on your device's internal SQLite database (not temporary cache memory), protecting your statistics from automatic cache clears. You can download individual .json backup files directly to your device or import .json backup files.",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Download Backup as File
                            Button(
                                onClick = {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    createDocumentLauncher.launch("zenmath-backup-$dateStr.json")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary.copy(alpha = 0.1f),
                                    contentColor = colors.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Import Backup from File (Only JSON files allowed)
                            Button(
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("application/json"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary.copy(alpha = 0.1f),
                                    contentColor = colors.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Additional Copy to Clipboard option for convenience
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    scope.launch {
                                        val backupJson = onExportBackup()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("ZenMath Backup", backupJson))
                                        Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy JSON to Clipboard", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Clear All Data Button
            item {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.incorrect.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.incorrect
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Data", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }

    // Confirmation Dialog for Clear Data
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?", fontWeight = FontWeight.Bold, color = colors.textMain) },
            text = { Text("This will permanently delete all session history, questions, and statistics. This cannot be undone.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllData()
                        showClearDialog = false
                        Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = colors.incorrect, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.card
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = colors.textSecondary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = colors.textMain
            )
        }
    }
}
