package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConstantsData
import com.example.data.ConstantsLoader
import com.example.data.HscPaper
import com.example.data.MemoryTableCategory
import com.example.data.ReliabilityClass
import com.example.domain.CompositeLogRootSubtype
import com.example.domain.CubesPowersSubtype
import com.example.domain.Difficulty
import com.example.domain.GameMode
import com.example.domain.MemoryDirection
import com.example.domain.PhSubType
import com.example.domain.SciNotationSubtype
import com.example.ui.components.ZenSlider
import com.example.ui.components.ZenToggle
import com.example.ui.theme.ZenTheme

@Composable
fun GameSetupScreen(
    mode: GameMode,
    digits: Int,
    difficulty: Difficulty,
    allowRemainder: Boolean,
    allowNegativeResults: Boolean,
    mixedOps: List<Boolean>,
    squareRangeType: String,
    customSquareRange: Pair<Int, Int>,
    sqrtRangeType: String,
    customSqrtRange: Pair<Int, Int>,
    fractionDenRange: Pair<Int, Int>,
    fractionNumRange: Pair<Int, Int>,
    fractionUseCourseValues: Boolean = false,
    memoryDirection: MemoryDirection = MemoryDirection.MIXED,
    memoryCategories: Set<MemoryTableCategory> = MemoryTableCategory.entries.toSet(),
    sciNotationSubtype: SciNotationSubtype = SciNotationSubtype.ALL,
    phSubType: PhSubType = PhSubType.ALL,
    compositeLogRootSubtype: CompositeLogRootSubtype = CompositeLogRootSubtype.ALL,
    cubesPowersSubtype: CubesPowersSubtype = CubesPowersSubtype.ALL,
    cubeRange: Pair<Int, Int> = Pair(1, 15),
    mcqEnabled: Boolean = false,
    speedRoundDurationMin: Int = 1,
    speedRoundTopics: Set<GameMode> = setOf(GameMode.ADDITION, GameMode.SUBTRACTION, GameMode.MULTIPLICATION, GameMode.DIVISION),
    constantSubjects: Set<String> = emptySet(),
    onDigitsChange: (Int) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onAllowRemainderChange: (Boolean) -> Unit,
    onAllowNegativeResultsChange: (Boolean) -> Unit,
    onMixedOpsChange: (List<Boolean>) -> Unit,
    onSquareRangeTypeChange: (String) -> Unit,
    onCustomSquareRangeChange: (Pair<Int, Int>) -> Unit,
    onSqrtRangeTypeChange: (String) -> Unit,
    onCustomSqrtRangeChange: (Pair<Int, Int>) -> Unit,
    onFractionDenRangeChange: (Pair<Int, Int>) -> Unit,
    onFractionNumRangeChange: (Pair<Int, Int>) -> Unit,
    onFractionUseCourseValuesChange: (Boolean) -> Unit = {},
    onMemoryDirectionChange: (MemoryDirection) -> Unit = {},
    onMemoryCategoriesChange: (Set<MemoryTableCategory>) -> Unit = {},
    onSciNotationSubtypeChange: (SciNotationSubtype) -> Unit = {},
    onPhSubTypeChange: (PhSubType) -> Unit = {},
    onCompositeLogRootSubtypeChange: (CompositeLogRootSubtype) -> Unit = {},
    onCubesPowersSubtypeChange: (CubesPowersSubtype) -> Unit = {},
    onCubeRangeChange: (Pair<Int, Int>) -> Unit = {},
    onMcqEnabledChange: (Boolean) -> Unit = {},
    onSpeedRoundDurationMinChange: (Int) -> Unit = {},
    onSpeedRoundTopicsChange: (Set<GameMode>) -> Unit = {},
    onConstantSubjectsChange: (Set<String>) -> Unit = {},
    constantsIncludeAlternateUnits: Boolean = false,
    constantsSelectedPapers: Set<HscPaper> = emptySet(),
    constantsSelectedChapters: Set<String> = emptySet(),
    constantsSelectedReliability: Set<ReliabilityClass> = ReliabilityClass.entries.toSet(),
    onConstantsIncludeAlternateUnitsChange: (Boolean) -> Unit = {},
    onConstantsSelectedPapersChange: (Set<HscPaper>) -> Unit = {},
    onConstantsSelectedChaptersChange: (Set<String>) -> Unit = {},
    onConstantsSelectedReliabilityChange: (Set<ReliabilityClass>) -> Unit = {},
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .testTag("game_setup_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textMain
                    )
                }

                Text(
                    text = mode.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Scrollable settings
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mixed Operations Toggles
                if (mode == GameMode.MIXED) {
                    item {
                        SectionHeader(title = "ARITHMETIC OPERATIONS")
                        val opList = listOf(
                            Triple("Addition", Icons.Default.Add, 0),
                            Triple("Subtraction", Icons.Default.Remove, 1),
                            Triple("Multiplication", Icons.Default.Close, 2),
                            Triple("Division", Icons.Default.Percent, 3)
                        )

                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(vertical = 4.dp)
                        ) {
                            Column {
                                opList.forEach { (label, icon, idx) ->
                                    val checked = mixedOps.getOrElse(idx) { true }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.primary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = label,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textMain,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier.size(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ZenToggle(
                                                checked = checked,
                                                onCheckedChange = { newVal ->
                                                    val updated = mixedOps.toMutableList()
                                                    updated[idx] = newVal
                                                    if (updated.any { it }) {
                                                        onMixedOpsChange(updated)
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

                // Digits selector
                if (mode in listOf(GameMode.ADDITION, GameMode.SUBTRACTION, GameMode.MULTIPLICATION, GameMode.DIVISION, GameMode.MIXED)) {
                    item {
                        SectionHeader(title = "NUMBER OF DIGITS")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(1, 2, 3, 4, 5).forEach { d ->
                                val isSelected = digits == d
                                val shape = RoundedCornerShape(16.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(shape)
                                        .background(if (isSelected) colors.primary else colors.card)
                                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, shape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                                            onClick = { onDigitsChange(d) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = d.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Difficulty selector (only shown for modes that use difficulty and when custom range isn't overriding it)
                val showDifficulty = mode.usesDifficulty && !(mode == GameMode.SQUARE_ROOT && sqrtRangeType == "custom")
                if (showDifficulty) {
                    item {
                        SectionHeader(title = "DIFFICULTY")
                        val cardShape = RoundedCornerShape(24.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Difficulty.entries.forEach { diff ->
                                        val isSelected = difficulty == diff
                                        Text(
                                            text = diff.label.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.clickable { onDifficultyChange(diff) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val diffIndex = Difficulty.entries.indexOf(difficulty).toFloat()
                                ZenSlider(
                                    value = diffIndex,
                                    onValueChange = { floatVal ->
                                        val idx = floatVal.toInt().coerceIn(0, 2)
                                        onDifficultyChange(Difficulty.entries[idx])
                                    },
                                    valueRange = 0f..2f,
                                    steps = 1
                                )
                            }
                        }
                    }
                }

                // Subtraction or Mixed: Negative Results toggle
                if (mode == GameMode.SUBTRACTION || mode == GameMode.MIXED) {
                    item {
                        SectionHeader(title = "SUBTRACTION OPTIONS")
                        ToggleCardRow(
                            title = "Negative Results",
                            subtitle = if (allowNegativeResults) "Negative answers allowed" else "Positive results only",
                            icon = Icons.Default.Remove,
                            checked = allowNegativeResults,
                            onCheckedChange = onAllowNegativeResultsChange
                        )
                    }
                }

                // Division or Mixed: Allow Remainders toggle
                if (mode == GameMode.DIVISION || mode == GameMode.MIXED) {
                    item {
                        SectionHeader(title = "DIVISION OPTIONS")
                        ToggleCardRow(
                            title = "Allow Remainders",
                            subtitle = if (allowRemainder) "Include decimal solutions" else "Exact division only",
                            icon = Icons.Default.Calculate,
                            checked = allowRemainder,
                            onCheckedChange = onAllowRemainderChange
                        )
                    }
                }

                // Fraction: Denominator & Numerator Ranges + Course Values
                if (mode == GameMode.FRACTION) {
                    item {
                        SectionHeader(title = "MAXIMUM DENOMINATOR")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(2, 4, 6, 8, 10, 12, 15).forEach { den ->
                                val isSelected = fractionDenRange.second == den
                                val shape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(shape)
                                        .background(if (isSelected) colors.primary else colors.card)
                                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, shape)
                                        .clickable { onFractionDenRangeChange(Pair(2, den)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = den.toString(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "MAXIMUM NUMERATOR")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4, 6, 8, 10).forEach { num ->
                                val isSelected = fractionNumRange.second == num
                                val shape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(shape)
                                        .background(if (isSelected) colors.primary else colors.card)
                                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, shape)
                                        .clickable { onFractionNumRangeChange(Pair(1, num)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = num.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "DECIMAL CONVENTION")
                        ToggleCardRow(
                            title = "Course Values",
                            subtitle = if (fractionUseCourseValues) "Accepts 2-decimal course values (e.g. 1/13 = 0.07)" else "Standard rounded decimal values",
                            icon = Icons.Default.Calculate,
                            checked = fractionUseCourseValues,
                            onCheckedChange = onFractionUseCourseValuesChange
                        )
                    }
                }

                // Square Numbers: Range selector
                if (mode == GameMode.SQUARE) {
                    item {
                        SectionHeader(title = "SQUARE RANGE")
                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val fixedActive = squareRangeType == "fixed"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (fixedActive) colors.primary else colors.surface)
                                            .clickable { onSquareRangeTypeChange("fixed") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "1 to 25",
                                            fontWeight = FontWeight.Bold,
                                            color = if (fixedActive) colors.onPrimary else colors.textSecondary
                                        )
                                    }

                                    val customActive = squareRangeType == "custom"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (customActive) colors.primary else colors.surface)
                                            .clickable { onSquareRangeTypeChange("custom") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Custom",
                                            fontWeight = FontWeight.Bold,
                                            color = if (customActive) colors.onPrimary else colors.textSecondary
                                        )
                                    }
                                }

                                if (squareRangeType == "custom") {
                                    CustomRangeEditor(
                                        range = customSquareRange,
                                        onRangeChange = onCustomSquareRangeChange,
                                        testTagPrefix = "square_custom"
                                    )
                                }
                            }
                        }
                    }
                }

                // Square Root Range Selector
                if (mode == GameMode.SQUARE_ROOT) {
                    item {
                        SectionHeader(title = "SQUARE ROOT RANGE")
                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val fixedActive = sqrtRangeType == "fixed"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (fixedActive) colors.primary else colors.surface)
                                            .clickable { onSqrtRangeTypeChange("fixed") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "By Difficulty",
                                            fontWeight = FontWeight.Bold,
                                            color = if (fixedActive) colors.onPrimary else colors.textSecondary
                                        )
                                    }

                                    val customActive = sqrtRangeType == "custom"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (customActive) colors.primary else colors.surface)
                                            .clickable { onSqrtRangeTypeChange("custom") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Custom",
                                            fontWeight = FontWeight.Bold,
                                            color = if (customActive) colors.onPrimary else colors.textSecondary
                                        )
                                    }
                                }

                                if (sqrtRangeType == "custom") {
                                    CustomRangeEditor(
                                        range = customSqrtRange,
                                        onRangeChange = onCustomSqrtRangeChange,
                                        testTagPrefix = "sqrt_custom"
                                    )
                                }
                            }
                        }
                    }
                }

                // Memory Tables Setup
                if (mode == GameMode.MEMORY_TABLES) {
                    item {
                        SectionHeader(title = "DRILL DIRECTION")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MemoryDirection.entries.forEach { dir ->
                                val selected = memoryDirection == dir
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) colors.primary else colors.card)
                                        .border(1.dp, if (selected) colors.primary else colors.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable { onMemoryDirectionChange(dir) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dir.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "INCLUDED TABLES")
                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(vertical = 4.dp)
                        ) {
                            Column {
                                MemoryTableCategory.entries.forEach { cat ->
                                    val checked = cat in memoryCategories
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMain,
                                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                                        )
                                        Box(
                                            modifier = Modifier.size(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ZenToggle(
                                                checked = checked,
                                                onCheckedChange = { newVal ->
                                                    val updated = if (newVal) memoryCategories + cat else memoryCategories - cat
                                                    if (updated.isNotEmpty()) {
                                                        onMemoryCategoriesChange(updated)
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

                // Scientific Notation Setup
                if (mode == GameMode.SCIENTIFIC_NOTATION) {
                    item {
                        SectionHeader(title = "QUESTION TYPE")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SciNotationSubtype.entries.forEach { sub ->
                                val selected = sciNotationSubtype == sub
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) colors.primary else colors.card)
                                        .border(1.dp, if (selected) colors.primary else colors.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable { onSciNotationSubtypeChange(sub) }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = sub.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) colors.onPrimary else colors.textMain
                                    )
                                }
                            }
                        }
                    }
                }

                // pH & pOH Setup
                if (mode == GameMode.PH_POH) {
                    item {
                        SectionHeader(title = "QUESTION TYPE")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhSubType.entries.forEach { sub ->
                                val selected = phSubType == sub
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) colors.primary else colors.card)
                                        .border(1.dp, if (selected) colors.primary else colors.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable { onPhSubTypeChange(sub) }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = sub.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) colors.onPrimary else colors.textMain
                                    )
                                }
                            }
                        }
                    }
                }

                // Composite Logs & Roots Setup
                if (mode == GameMode.COMPOSITE_LOGS_ROOTS) {
                    item {
                        SectionHeader(title = "QUESTION TYPE")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompositeLogRootSubtype.entries.forEach { sub ->
                                val selected = compositeLogRootSubtype == sub
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) colors.primary else colors.card)
                                        .border(1.dp, if (selected) colors.primary else colors.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable { onCompositeLogRootSubtypeChange(sub) }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = sub.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) colors.onPrimary else colors.textMain
                                    )
                                }
                            }
                        }
                    }
                }

                // Cubes & Powers Setup
                if (mode == GameMode.CUBES_AND_POWERS) {
                    item {
                        SectionHeader(title = "QUESTION TYPE")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CubesPowersSubtype.entries.forEach { sub ->
                                val selected = cubesPowersSubtype == sub
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) colors.primary else colors.card)
                                        .border(1.dp, if (selected) colors.primary else colors.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable { onCubesPowersSubtypeChange(sub) }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = sub.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) colors.onPrimary else colors.textMain
                                    )
                                }
                            }
                        }

                        if (cubesPowersSubtype != CubesPowersSubtype.POWERS) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(title = "CUBE RANGE (DEFAULT 1–15)")
                            val cardShape = RoundedCornerShape(20.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(colors.card)
                                    .border(1.dp, colors.cardBorder, cardShape)
                                    .padding(16.dp)
                            ) {
                                CustomRangeEditor(
                                    range = cubeRange,
                                    onRangeChange = onCubeRangeChange,
                                    testTagPrefix = "cube_custom"
                                )
                            }
                        }
                    }
                }

                // 5.4 Constants Setup: Paper, Chapter, Reliability, Alternate Units
                if (mode == GameMode.CONSTANTS) {
                    item {
                        val dataset = remember { ConstantsLoader.getDataset(failLoudly = false) }
                        val allChapters = dataset.syllabusChapters

                        // 1. Paper Filter
                        SectionHeader(title = "CURRICULUM PAPERS")
                        val papers = listOf(
                            HscPaper.PHYSICS_1ST,
                            HscPaper.PHYSICS_2ND,
                            HscPaper.CHEMISTRY_1ST,
                            HscPaper.CHEMISTRY_2ND
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            papers.forEach { paper ->
                                val isSelected = constantsSelectedPapers.isEmpty() || paper in constantsSelectedPapers
                                val cardShape = RoundedCornerShape(12.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(cardShape)
                                        .background(if (isSelected && constantsSelectedPapers.isNotEmpty()) colors.primary else colors.card)
                                        .border(
                                            1.dp,
                                            if (isSelected && constantsSelectedPapers.isNotEmpty()) colors.primary else colors.cardBorder,
                                            cardShape
                                        )
                                        .clickable {
                                            val updated = if (constantsSelectedPapers.isEmpty()) {
                                                setOf(paper)
                                            } else if (paper in constantsSelectedPapers) {
                                                val next = constantsSelectedPapers - paper
                                                if (next.isEmpty()) emptySet() else next
                                            } else {
                                                val next = constantsSelectedPapers + paper
                                                if (next.size == papers.size) emptySet() else next
                                            }
                                            onConstantsSelectedPapersChange(updated)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = paper.displayName.replace(" ", "\n"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected && constantsSelectedPapers.isNotEmpty()) colors.onPrimary else colors.textMain,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Reliability Class Filter
                        SectionHeader(title = "RELIABILITY CLASS")
                        val relShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(relShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, relShape)
                                .padding(vertical = 4.dp)
                        ) {
                            Column {
                                ReliabilityClass.entries.forEach { rel ->
                                    val checked = rel in constantsSelectedReliability
                                    val (title, subtitle) = when (rel) {
                                        ReliabilityClass.FUNDAMENTAL -> "Fundamental" to "Defined or measured fundamental values"
                                        ReliabilityClass.CONVENTIONS -> "Conventions" to "Conventional, defined & condition-specific"
                                        ReliabilityClass.REFERENCE_AND_APPROXIMATE -> "Reference & Approx" to "Empirical & reference approximations"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = subtitle,
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.size(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ZenToggle(
                                                checked = checked,
                                                onCheckedChange = { newVal ->
                                                    val updated = if (newVal) {
                                                        constantsSelectedReliability + rel
                                                    } else {
                                                        if (constantsSelectedReliability.size > 1) {
                                                            constantsSelectedReliability - rel
                                                        } else {
                                                            constantsSelectedReliability
                                                        }
                                                    }
                                                    onConstantsSelectedReliabilityChange(updated)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Unit Systems
                        SectionHeader(title = "UNIT SYSTEMS")
                        ToggleCardRow(
                            title = "Include Alternate Unit Systems",
                            subtitle = if (constantsIncludeAlternateUnits) "Includes non-SI variants (e.g. R in L·atm, h in eV·s)" else "Standard SI units only",
                            icon = Icons.Default.Calculate,
                            checked = constantsIncludeAlternateUnits,
                            onCheckedChange = onConstantsIncludeAlternateUnitsChange
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Chapters Filter (Bilingual English + Bangla)
                        var chaptersExpanded by remember { mutableStateOf(false) }
                        val filteredChapters = remember(constantsSelectedPapers) {
                            if (constantsSelectedPapers.isEmpty()) allChapters
                            else allChapters.filter { it.paper in constantsSelectedPapers }
                        }
                        val selectedCount = if (constantsSelectedChapters.isEmpty()) {
                            filteredChapters.size
                        } else {
                            filteredChapters.count { it.key in constantsSelectedChapters }
                        }

                        SectionHeader(title = "CHAPTER FILTER ($selectedCount/${filteredChapters.size})")
                        val chapCardShape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(chapCardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, chapCardShape)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { chaptersExpanded = !chaptersExpanded }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (constantsSelectedChapters.isEmpty()) "All Chapters Active" else "$selectedCount Chapters Selected",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = if (chaptersExpanded) "Hide ▲" else "Edit ▼",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                }

                                if (chaptersExpanded) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        filteredChapters.forEach { chap ->
                                            val isChecked = constantsSelectedChapters.isEmpty() || chap.key in constantsSelectedChapters
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val activeSet = if (constantsSelectedChapters.isEmpty()) {
                                                            filteredChapters.map { it.key }.toSet()
                                                        } else {
                                                            constantsSelectedChapters
                                                        }
                                                        val updated = if (chap.key in activeSet) {
                                                            val next = activeSet - chap.key
                                                            if (next.isEmpty() || next.size == filteredChapters.size) emptySet() else next
                                                        } else {
                                                            val next = activeSet + chap.key
                                                            if (next.size == filteredChapters.size) emptySet() else next
                                                        }
                                                        onConstantsSelectedChaptersChange(updated)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Text(
                                                        text = "${chap.paper.displayName} • Ch ${chap.chapterNumber}: ${chap.nameEn}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.textMain
                                                    )
                                                    Text(
                                                        text = chap.nameBn,
                                                        fontSize = 11.sp,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier.size(48.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    ZenToggle(
                                                        checked = isChecked,
                                                        onCheckedChange = { newVal ->
                                                            val activeSet = if (constantsSelectedChapters.isEmpty()) {
                                                                filteredChapters.map { it.key }.toSet()
                                                            } else {
                                                                constantsSelectedChapters
                                                            }
                                                            val updated = if (newVal) {
                                                                val next = activeSet + chap.key
                                                                if (next.size == filteredChapters.size) emptySet() else next
                                                            } else {
                                                                val next = activeSet - chap.key
                                                                if (next.isEmpty() || next.size == filteredChapters.size) emptySet() else next
                                                            }
                                                            onConstantsSelectedChaptersChange(updated)
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

                // 4.3 Speed Round Setup (Duration 1, 2, 3, or 5 minutes + Topic Pool)
                if (mode == GameMode.SPEED_ROUND) {
                    item {
                        SectionHeader(title = "DURATION (MINUTES)")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(1, 2, 3, 5).forEach { mins ->
                                val isSelected = speedRoundDurationMin == mins
                                val shape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(shape)
                                        .background(if (isSelected) colors.primary else colors.card)
                                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, shape)
                                        .clickable { onSpeedRoundDurationMinChange(mins) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "TOPIC POOL")
                        val selectableModes = remember {
                            listOf(
                                GameMode.ADDITION,
                                GameMode.SUBTRACTION,
                                GameMode.MULTIPLICATION,
                                GameMode.DIVISION,
                                GameMode.SQUARE,
                                GameMode.CUBES_AND_POWERS,
                                GameMode.FRACTION,
                                GameMode.PERCENTAGE,
                                GameMode.SQUARE_ROOT,
                                GameMode.APPROXIMATION,
                                GameMode.MEMORY_TABLES,
                                GameMode.SCIENTIFIC_NOTATION,
                                GameMode.PH_POH,
                                GameMode.COMPOSITE_LOGS_ROOTS,
                                GameMode.CONSTANTS
                            )
                        }
                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(vertical = 4.dp)
                        ) {
                            Column {
                                selectableModes.forEach { topicMode ->
                                    val checked = topicMode in speedRoundTopics
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = topicMode.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMain,
                                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                                        )
                                        Box(
                                            modifier = Modifier.size(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ZenToggle(
                                                checked = checked,
                                                onCheckedChange = { newVal ->
                                                    val updated = if (newVal) speedRoundTopics + topicMode else speedRoundTopics - topicMode
                                                    if (updated.isNotEmpty()) {
                                                        onSpeedRoundTopicsChange(updated)
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

                // 3.6 Multiple Choice (2×2) per-session setup toggle
                if (mode.supportsMcq) {
                    item {
                        SectionHeader(title = "ANSWER FORMAT")
                        ToggleCardRow(
                            title = "Multiple Choice (2×2)",
                            subtitle = if (mcqEnabled) "4 options spaced ≥ 3× tolerance apart" else "Free numeric / scientific keypad entry",
                            icon = Icons.Default.Calculate,
                            checked = mcqEnabled,
                            onCheckedChange = onMcqEnabledChange
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Fixed CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_practice_btn")
                ) {
                    Text(
                        text = "Start Practice",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = ZenTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = colors.textSecondary.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun ToggleCardRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
            }

            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                ZenToggle(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
    }
}

@Composable
private fun CustomRangeEditor(
    range: Pair<Int, Int>,
    onRangeChange: (Pair<Int, Int>) -> Unit,
    testTagPrefix: String
) {
    val colors = ZenTheme.colors
    var minText by remember(range.first) { mutableStateOf(range.first.toString()) }
    var maxText by remember(range.second) { mutableStateOf(range.second.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = minText,
            onValueChange = { raw ->
                minText = raw.filter { it.isDigit() }.take(3)
                val parsedMin = minText.toIntOrNull()
                if (parsedMin != null) {
                    val safeMin = parsedMin.coerceIn(1, 199)
                    val safeMax = range.second.coerceIn(safeMin + 1, 200)
                    onRangeChange(Pair(safeMin, safeMax))
                }
            },
            label = { Text("Min (≥1)", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.cardBorder,
                focusedTextColor = colors.textMain,
                unfocusedTextColor = colors.textMain
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_min")
        )

        OutlinedTextField(
            value = maxText,
            onValueChange = { raw ->
                maxText = raw.filter { it.isDigit() }.take(3)
                val parsedMax = maxText.toIntOrNull()
                if (parsedMax != null) {
                    val safeMin = range.first.coerceIn(1, 199)
                    val safeMax = parsedMax.coerceIn(safeMin + 1, 200)
                    onRangeChange(Pair(safeMin, safeMax))
                }
            },
            label = { Text("Max (>Min)", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.cardBorder,
                focusedTextColor = colors.textMain,
                unfocusedTextColor = colors.textMain
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_max")
        )
    }
}
