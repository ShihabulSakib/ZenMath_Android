package com.example.domain

import com.example.data.QuestionRecordEntity
import com.example.domain.drills.DrillCategory
import com.example.domain.drills.DrillRegistry
import com.example.domain.drills.TopicTagResolver

data class MistakeItem(
    val key: String,
    val drillMode: GameMode,
    val category: DrillCategory,
    val topicTag: String,
    val questionText: String,
    val latestWrongUserAnswer: String?,
    val correctAnswer: String,
    val unitText: String?,
    val errorCount: Int,
    val attemptCount: Int,
    val latestRecordId: Long,
    val isResolved: Boolean,
    val solutionSteps: List<String>,
    val problem: MathProblem
)

data class MistakeDrillGroup(
    val drillMode: GameMode,
    val displayName: String,
    val items: List<MistakeItem>
)

data class MistakeCategoryGroup(
    val category: DrillCategory,
    val drillGroups: List<MistakeDrillGroup>
) {
    val totalItems: Int get() = drillGroups.sumOf { it.items.size }
}

object MistakeSelector {

    private val DISPLAY_CATEGORIES: List<DrillCategory> = DrillCategory.entries

    fun fromSessionResults(
        results: List<QuestionResult>,
        fallbackMode: GameMode
    ): List<MistakeCategoryGroup> {
        val wrongResults = results.mapIndexedNotNull { idx, r ->
            if (!r.isCorrect) idx to r else null
        }
        if (wrongResults.isEmpty()) return emptyList()

        val grouped = wrongResults.groupBy { (_, r) ->
            val resolved = TopicTagResolver.resolveDrillMode(r.topicTag, fallbackMode.id, r.drillModeId)
            val canonical = TopicTagResolver.canonicalActiveMode(resolved)
            canonical to r.questionText.trim()
        }

        val items = grouped.entries.mapNotNull { (pair, attempts) ->
            val (drillMode, qText) = pair
            val (lastIdx, latest) = attempts.last()
            val problem: MathProblem = latest.problemJson?.let { ProblemCodec.decode(it) } ?: run {
                val tempRecord = QuestionRecordEntity(
                    sessionId = "session",
                    num1 = latest.num1,
                    num2 = latest.num2,
                    operation = latest.operation,
                    correctAnswer = latest.correctAnswer,
                    userAnswer = latest.userAnswer,
                    timeMs = latest.timeTakenMs,
                    isCorrect = false,
                    mode = fallbackMode.id,
                    difficulty = "medium",
                    questionText = latest.questionText,
                    topicTag = latest.topicTag,
                    hintUsed = latest.hintUsed,
                    drillMode = latest.drillModeId,
                    problemJson = latest.problemJson.orEmpty()
                )
                ProblemCodec.fromRecordOrFallback(tempRecord)
            } ?: return@mapNotNull null

            val category = DrillRegistry.categoryOf(drillMode)
            MistakeItem(
                key = "${drillMode.id}:$qText",
                drillMode = drillMode,
                category = category,
                topicTag = latest.topicTag.ifBlank { problem.topicTag.ifBlank { drillMode.id } },
                questionText = qText,
                latestWrongUserAnswer = latest.userAnswer,
                correctAnswer = latest.correctAnswer.ifBlank { problem.answer },
                unitText = problem.unitText,
                errorCount = attempts.size,
                attemptCount = results.count { it.questionText.trim() == qText },
                latestRecordId = lastIdx.toLong(),
                isResolved = false,
                solutionSteps = problem.solutionSteps,
                problem = problem
            )
        }

        return groupIntoCategories(items)
    }

    fun fromAllRecords(
        allQuestions: List<QuestionRecordEntity>,
        includeResolved: Boolean = false
    ): List<MistakeCategoryGroup> {
        if (allQuestions.isEmpty()) return emptyList()

        val grouped = allQuestions.groupBy { q ->
            val resolved = TopicTagResolver.resolveDrillMode(q.topicTag, q.mode, q.drillMode)
            val canonical = TopicTagResolver.canonicalActiveMode(resolved)
            canonical to q.questionText.trim()
        }

        val items = mutableListOf<MistakeItem>()
        for ((pair, attempts) in grouped) {
            val (drillMode, qText) = pair
            if (qText.isBlank()) continue
            val wrongAttempts = attempts.filter { !it.isCorrect }
            if (wrongAttempts.isEmpty()) continue

            val latestAttempt = attempts.maxByOrNull { it.id } ?: continue
            val isResolved = latestAttempt.isCorrect && !latestAttempt.hintUsed
            if (!includeResolved && isResolved) continue

            val latestWrong = wrongAttempts.maxByOrNull { it.id } ?: continue
            val problem = ProblemCodec.fromRecordOrFallback(latestWrong) ?: continue
            val category = DrillRegistry.categoryOf(drillMode)

            items.add(
                MistakeItem(
                    key = "${drillMode.id}:$qText",
                    drillMode = drillMode,
                    category = category,
                    topicTag = latestWrong.topicTag.ifBlank { problem.topicTag.ifBlank { drillMode.id } },
                    questionText = qText,
                    latestWrongUserAnswer = latestWrong.userAnswer,
                    correctAnswer = latestWrong.correctAnswer.ifBlank { problem.answer },
                    unitText = problem.unitText,
                    errorCount = wrongAttempts.size,
                    attemptCount = attempts.size,
                    latestRecordId = latestWrong.id,
                    isResolved = isResolved,
                    solutionSteps = problem.solutionSteps,
                    problem = problem
                )
            )
        }

        return groupIntoCategories(items)
    }

    private fun groupIntoCategories(items: List<MistakeItem>): List<MistakeCategoryGroup> {
        if (items.isEmpty()) return emptyList()
        val byCategory = items.groupBy { it.category }

        return DISPLAY_CATEGORIES.mapNotNull { cat ->
            val catItems = byCategory[cat].orEmpty()
            if (catItems.isEmpty()) return@mapNotNull null

            val drillGroups = catItems
                .groupBy { it.drillMode }
                .entries
                .sortedBy { it.key.ordinal }
                .map { entry ->
                    val mode = entry.key
                    val modeItems = entry.value
                    val sortedItems = modeItems.sortedWith(
                        compareByDescending<MistakeItem> { it.errorCount }
                            .thenByDescending { it.latestRecordId }
                    )
                    MistakeDrillGroup(
                        drillMode = mode,
                        displayName = DrillRegistry.of(mode).title,
                        items = sortedItems
                    )
                }

            MistakeCategoryGroup(
                category = cat,
                drillGroups = drillGroups
            )
        }
    }

    fun allKeys(groups: List<MistakeCategoryGroup>): Set<String> =
        groups.asSequence()
            .flatMap { it.drillGroups.asSequence() }
            .flatMap { it.items.asSequence() }
            .map { it.key }
            .toSet()

    fun keysForCategory(group: MistakeCategoryGroup): Set<String> =
        group.drillGroups.asSequence()
            .flatMap { it.items.asSequence() }
            .map { it.key }
            .toSet()

    fun keysForDrill(drillGroup: MistakeDrillGroup): Set<String> =
        drillGroup.items.asSequence().map { it.key }.toSet()

    fun buildReplayQueue(
        groups: List<MistakeCategoryGroup>,
        selectedDrills: Set<GameMode>,
        cap: Int = 30
    ): List<MistakeItem> {
        val matchingItems = groups.asSequence()
            .flatMap { it.drillGroups.asSequence() }
            .filter { it.drillMode in selectedDrills }
            .flatMap { it.items.asSequence() }
            .toList()
        return deduplicateAndRank(matchingItems, cap)
    }

    fun buildReplayQueueFromKeys(
        groups: List<MistakeCategoryGroup>,
        selectedKeys: Set<String>,
        cap: Int = 30
    ): List<MistakeItem> {
        val matchingItems = groups.asSequence()
            .flatMap { it.drillGroups.asSequence() }
            .flatMap { it.items.asSequence() }
            .filter { it.key in selectedKeys }
            .toList()
        return deduplicateAndRank(matchingItems, cap)
    }

    private fun deduplicateAndRank(items: List<MistakeItem>, cap: Int): List<MistakeItem> {
        return items
            .distinctBy { it.drillMode to it.questionText.trim() }
            .sortedWith(
                compareByDescending<MistakeItem> { it.errorCount }
                    .thenByDescending { it.latestRecordId }
            )
            .take(cap.coerceAtLeast(1))
    }

    fun formatScopeLabel(
        groups: List<MistakeCategoryGroup>,
        selectedKeys: Set<String>,
        queueSize: Int,
        isSessionScope: Boolean
    ): String {
        val selectedDrills = groups.asSequence()
            .flatMap { it.drillGroups.asSequence() }
            .filter { dg -> dg.items.any { it.key in selectedKeys } }
            .map { it.displayName }
            .distinct()
            .toList()

        val prefix = if (isSessionScope) "Session Mistakes" else "Weak Spots"
        return when {
            selectedDrills.isEmpty() -> "$prefix (${queueSize}Q)"
            selectedDrills.size <= 2 -> "$prefix: ${selectedDrills.joinToString(", ")} (${queueSize}Q)"
            else -> "$prefix: ${selectedDrills.take(2).joinToString(", ")} +${selectedDrills.size - 2} (${queueSize}Q)"
        }
    }
}
