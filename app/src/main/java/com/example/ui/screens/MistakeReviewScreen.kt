package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MistakeCategoryGroup
import com.example.domain.MistakeItem
import com.example.domain.MistakeSelector
import com.example.domain.drills.TopicTagResolver
import com.example.ui.theme.ZenTheme

@Composable
fun MistakeReviewScreen(
    groups: List<MistakeCategoryGroup>,
    isAllTimeScope: Boolean,
    includeResolved: Boolean,
    onToggleIncludeResolved: (Boolean) -> Unit,
    onBack: () -> Unit,
    onStartPractice: (List<MistakeItem>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val allAvailableKeys = remember(groups) { MistakeSelector.allKeys(groups) }

    var selectedKeys by remember(groups) { mutableStateOf(allAvailableKeys) }
    var expandedCategories by remember(groups) {
        mutableStateOf(groups.map { it.category }.toSet())
    }
    var expandedDrills by remember(groups) {
        mutableStateOf(
            groups.flatMap { it.drillGroups }.map { it.drillMode }.toSet()
        )
    }
    var expandedQuestions by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(allAvailableKeys) {
        selectedKeys = allAvailableKeys
    }

    val replayQueue = remember(groups, selectedKeys) {
        MistakeSelector.buildReplayQueueFromKeys(groups, selectedKeys, cap = 30)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 20.dp)
            .testTag("mistake_review_screen")
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, CircleShape)
                        .testTag("mistake_review_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textMain,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isAllTimeScope) "WEAK SPOTS REVIEW" else "SESSION MISTAKES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = colors.textMain
                    )
                    Text(
                        text = "${allAvailableKeys.size} question${if (allAvailableKeys.size == 1) "" else "s"} across ${groups.sumOf { it.drillGroups.size }} drill${if (groups.sumOf { it.drillGroups.size } == 1) "" else "s"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Filter & Selection Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAllTimeScope) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChipButton(
                        label = "Unresolved",
                        selected = !includeResolved,
                        onClick = { onToggleIncludeResolved(false) },
                        modifier = Modifier.testTag("filter_unresolved_chip")
                    )
                    FilterChipButton(
                        label = "All Mistakes",
                        selected = includeResolved,
                        onClick = { onToggleIncludeResolved(true) },
                        modifier = Modifier.testTag("filter_all_mistakes_chip")
                    )
                }
            } else {
                Text(
                    text = "SELECT TO PRACTICE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = colors.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { selectedKeys = allAvailableKeys },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("select_all_mistakes_btn")
                ) {
                    Text(
                        text = "Select All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                TextButton(
                    onClick = { selectedKeys = emptySet() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("clear_all_mistakes_btn")
                ) {
                    Text(
                        text = "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }
            }
        }

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.correct,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No mistakes found in this view!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    if (isAllTimeScope && !includeResolved) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Switch to 'All Mistakes' to review previously resolved items.",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("mistake_review_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                groups.forEach { catGroup ->
                    item(key = "cat_${catGroup.category.name}") {
                        val catKeys = MistakeSelector.keysForCategory(catGroup)
                        val isCatAllSelected = catKeys.isNotEmpty() && catKeys.all { it in selectedKeys }
                        val isCatExpanded = catGroup.category in expandedCategories
                        val catShape = RoundedCornerShape(16.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(catShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, catShape)
                        ) {
                            // Category Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategories = if (isCatExpanded) {
                                            expandedCategories - catGroup.category
                                        } else {
                                            expandedCategories + catGroup.category
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isCatAllSelected,
                                        onCheckedChange = { checked ->
                                            selectedKeys = if (checked) {
                                                selectedKeys + catKeys
                                            } else {
                                                selectedKeys - catKeys
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colors.primary,
                                            uncheckedColor = colors.textSecondary
                                        ),
                                        modifier = Modifier.testTag("category_checkbox_${catGroup.category.name}")
                                    )
                                    Column {
                                        Text(
                                            text = catGroup.category.label.uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = "${catGroup.drillGroups.size} drill${if (catGroup.drillGroups.size == 1) "" else "s"} · ${catGroup.totalItems} missed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isCatExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = colors.textSecondary
                                )
                            }

                            AnimatedVisibility(visible = isCatExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    catGroup.drillGroups.forEach { drillGroup ->
                                        val drillKeys = MistakeSelector.keysForDrill(drillGroup)
                                        val isDrillSelected = drillKeys.isNotEmpty() && drillKeys.all { it in selectedKeys }
                                        val isDrillExpanded = drillGroup.drillMode in expandedDrills
                                        val drillShape = RoundedCornerShape(12.dp)

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(drillShape)
                                                .background(colors.surface.copy(alpha = 0.65f))
                                                .border(1.dp, colors.cardBorder, drillShape)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedDrills = if (isDrillExpanded) {
                                                            expandedDrills - drillGroup.drillMode
                                                        } else {
                                                            expandedDrills + drillGroup.drillMode
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = isDrillSelected,
                                                        onCheckedChange = { checked ->
                                                            selectedKeys = if (checked) {
                                                                selectedKeys + drillKeys
                                                            } else {
                                                                selectedKeys - drillKeys
                                                            }
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = colors.primary,
                                                            uncheckedColor = colors.textSecondary
                                                        ),
                                                        modifier = Modifier.testTag("drill_checkbox_${drillGroup.drillMode.id}")
                                                    )
                                                    Text(
                                                        text = drillGroup.displayName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textMain
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(colors.incorrect.copy(alpha = 0.12f))
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = "${drillGroup.items.size}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = colors.incorrect
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (isDrillExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            AnimatedVisibility(visible = isDrillExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    drillGroup.items.forEach { item ->
                                                        MistakeQuestionRow(
                                                            item = item,
                                                            isSelected = item.key in selectedKeys,
                                                            isExpanded = item.key in expandedQuestions,
                                                            onToggleSelect = { checked ->
                                                                selectedKeys = if (checked) {
                                                                    selectedKeys + item.key
                                                                } else {
                                                                    selectedKeys - item.key
                                                                }
                                                            },
                                                            onToggleExpand = {
                                                                expandedQuestions = if (item.key in expandedQuestions) {
                                                                    expandedQuestions - item.key
                                                                } else {
                                                                    expandedQuestions + item.key
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pinned Bottom CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp)
        ) {
            Button(
                onClick = {
                    val label = MistakeSelector.formatScopeLabel(
                        groups = groups,
                        selectedKeys = selectedKeys,
                        queueSize = replayQueue.size,
                        isSessionScope = !isAllTimeScope
                    )
                    onStartPractice(replayQueue, label)
                },
                enabled = replayQueue.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.cardBorder,
                    disabledContentColor = colors.textSecondary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_mistake_practice_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (replayQueue.isEmpty()) {
                        "SELECT MISTAKES TO PRACTICE"
                    } else {
                        "PRACTICE SELECTED MISTAKES (${replayQueue.size})"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primary else colors.card)
            .border(1.dp, if (selected) colors.primary else colors.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) colors.onPrimary else colors.textSecondary
        )
    }
}

@Composable
private fun MistakeQuestionRow(
    item: MistakeItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onToggleExpand: () -> Unit
) {
    val colors = ZenTheme.colors
    val rowShape = RoundedCornerShape(10.dp)
    val unitSuffix = item.unitText?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, rowShape)
            .clickable(onClick = onToggleExpand)
            .padding(10.dp)
            .testTag("mistake_item_${item.key}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onToggleSelect,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.textSecondary
                    ),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.questionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMain
                    )
                    Text(
                        text = TopicTagResolver.friendlyTopicLabel(item.topicTag),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "✗ ${item.latestWrongUserAnswer?.takeIf { it.isNotBlank() } ?: "—"}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.incorrect
                )
                Text(
                    text = "✓ ${item.correctAnswer}$unitSuffix",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.correct
                )
                if (item.errorCount > 1 || item.isResolved) {
                    Text(
                        text = if (item.isResolved) "Resolved · ${item.errorCount}x missed" else "${item.errorCount}x missed",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isResolved) colors.correct else colors.textSecondary
                    )
                }
            }
        }

        if (item.solutionSteps.isNotEmpty()) {
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SOLUTION STEPS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = colors.primary
                    )
                    item.solutionSteps.forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. $step",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textMain
                        )
                    }
                }
            }
        }
    }
}
