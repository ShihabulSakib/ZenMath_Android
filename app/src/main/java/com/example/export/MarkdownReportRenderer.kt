package com.example.export

import com.example.domain.drills.DrillCategory
import com.example.domain.drills.DrillRegistry
import java.util.Locale

object MarkdownReportRenderer {

    fun render(snapshot: StatsSnapshot): String {
        val sb = StringBuilder(32_768)
        val ov = snapshot.overview

        // YAML Front Matter
        sb.appendLine("---")
        sb.appendLine("schema_version: \"1.0\"")
        sb.appendLine("report_type: \"zen_math_ai_coaching_report\"")
        sb.appendLine("app_name: \"ZenMath\"")
        sb.appendLine("app_version: \"${escapeYaml(snapshot.appVersion)}\"")
        sb.appendLine("generated_at: \"${escapeYaml(snapshot.generatedAtIso)}\"")
        sb.appendLine("generated_local_date: \"${escapeYaml(snapshot.generatedLocalDate)}\"")
        sb.appendLine("timezone: \"${escapeYaml(snapshot.timezoneId)}\"")
        sb.appendLine("total_sessions: ${ov.totalSessions}")
        sb.appendLine("total_questions: ${ov.totalQuestions}")
        sb.appendLine("overall_accuracy_pct: ${fmt1(ov.overallAccuracyPct)}")
        sb.appendLine("wilson_95_lower_pct: ${fmt1(ov.wilson95LowerPct)}")
        sb.appendLine("daily_goal_sessions: ${snapshot.settings.dailyGoal}")
        sb.appendLine("time_limit_seconds: ${snapshot.settings.timeLimit}")
        sb.appendLine("questions_per_session: ${snapshot.settings.totalQuestions}")
        sb.appendLine("tolerance_preset: \"${snapshot.settings.tolerancePreset.name.lowercase()}\"")
        sb.appendLine("adaptive_difficulty: ${snapshot.settings.adaptiveDifficulty}")
        sb.appendLine("---")
        sb.appendLine()

        // Title & Section 1: AI Instructions
        sb.appendLine("# ZenMath — Comprehensive Performance & AI Coaching Report")
        sb.appendLine()
        sb.appendLine("## 1. Instructions for AI Agents & LLMs")
        sb.appendLine()
        sb.appendLine("> **System Context for AI Assistants:** This document is a self-contained export from **ZenMath**, an Android mental-mathematics and calculator-free science exam training application. It contains full documentation of every drill in the app alongside the user's complete statistical history, latency distributions, error classifications, and item-level mistake log.")
        sb.appendLine()
        sb.appendLine("When analyzing this report or coaching the user:")
        sb.appendLine("1. **Prioritize Wilson 95% Lower Bound (`Wilson 95% LB`) over raw accuracy** when sample sizes are small. A drill with `100% (2/2)` has a Wilson 95% LB of `34.2%`, whereas `92% (46/50)` has a Wilson 95% LB of `81.2%`.")
        sb.appendLine("2. **Evaluate both Accuracy and Latency (`Avg`, `Median`, `P90`).** A drill with high accuracy (>90%) but a high P90 latency (>8s) indicates working-memory bottleneck rather than conceptual failure—recommend Speed Round or mental decomposition shortcuts.")
        sb.appendLine("3. **Compare `Typed` vs `MCQ` accuracy.** If MCQ accuracy is significantly higher than Typed accuracy on a drill, the user recognizes the ballpark answer or elimination cues but struggles with exact recall or normalization.")
        sb.appendLine("4. **Use the Item-Level Mistake Log (Section 8) and Mistake Patterns (Section 7.3)** to diagnose whether errors stem from sign inversions, power-of-10 exponent shifts, rounding drift, or memorized constant/table gaps.")
        sb.appendLine("5. **Reference specific ZenMath drills and parameters from Section 3** when prescribing a practice routine (e.g., *\"Run 2 sessions of Composite Logs & Roots → Subtype: √(a × 10ⁿ) before attempting Speed Round\"*).")
        sb.appendLine()

        // Section 2: Glossary & Scoring Rules
        sb.appendLine("## 2. App Overview, Glossary & Scoring Mechanics")
        sb.appendLine()
        sb.appendLine("### 2.1 Purpose of ZenMath")
        sb.appendLine("ZenMath trains rapid, calculator-free arithmetic and scientific computation for competitive and Higher Secondary Certificate (HSC) Physics, Chemistry, and Mathematics examinations. Users practice fundamental operations, powers/roots, fractions/percentages, memorized logarithms/roots/reciprocals, scientific notation, acid-base pH/pOH calculations, and physical/chemical/mathematical constants.")
        sb.appendLine()
        sb.appendLine("### 2.2 Session Types")
        sb.appendLine("| Session Type ID | Name | Behavior & Scoring |")
        sb.appendLine("| :--- | :--- | :--- |")
        sb.appendLine("| `normal` | Standard Drill Session | Fixed question count (user setting: ${snapshot.settings.totalQuestions} questions) with a per-question countdown timer (${snapshot.settings.timeLimit}s). Wrong answers pause on the solution steps until the user advances. |")
        sb.appendLine("| `speed_round` | Speed Round | Continuous timed sprint (1, 2, 3, or 5 minutes) sampling randomly from a user-selected pool of drills. No per-question pause; tracks Questions Per Minute (QPM). Each question is attributed to its underlying sub-drill via `drillMode`. |")
        sb.appendLine("| `weak_spots` | Weak Spots Auto-Drill | Automatically generates new questions targeting the user's highest-error topics based on historical accuracy and latency. |")
        sb.appendLine("| `mistake_review` | Mistake Review | Replays the exact questions the user previously missed (from a just-completed session or the all-time Weak Spots explorer), grouped and selected by category/drill. Does not alter adaptive difficulty. |")
        sb.appendLine()
        sb.appendLine("### 2.3 Metric Definitions")
        sb.appendLine("- **Wilson 95% LB:** Lower bound of the 95% Wilson score confidence interval for binomial accuracy (z = 1.96). Penalizes small sample sizes to prevent overestimating mastery.")
        sb.appendLine("- **Avg / Median / P90 Time:** Mean, 50th-percentile, and 90th-percentile per-question response latency in seconds.")
        sb.appendLine("- **Hint Rate:** Percentage of attempts where the user revealed the step-by-step hint before answering.")
        sb.appendLine("- **Timeout Rate:** Percentage of attempts where the per-question timer expired without an answer.")
        sb.appendLine("- **Trend Δ Acc / Δ Speed:** Difference between the second (most recent) half and first (earlier) half of attempts for drills with >= 6 recorded questions. Positive Δ Acc and negative Δ Speed indicate improvement.")
        sb.appendLine("- **Status (`Unresolved` vs `Resolved`):** A mistake is marked `Resolved` once its most recent attempt was answered correctly without using a hint.")
        sb.appendLine()

        // Section 3: Complete Drill Handbook
        sb.appendLine("## 3. Complete ZenMath Drill Handbook")
        sb.appendLine()
        sb.appendLine("Below is the authoritative specification of every drill mode in ZenMath, organized by category.")
        sb.appendLine()
        for (category in DrillCategory.entries) {
            val specs = DrillRegistry.all.filter { it.category == category && (!it.isLegacy || snapshot.drills.any { d -> d.mode == it.mode }) }
            if (specs.isEmpty()) continue
            sb.appendLine("### 3.${category.ordinal + 1} Category: ${category.label} (`${category.id}`)")
            sb.appendLine("${category.description}")
            sb.appendLine()
            for (spec in specs) {
                val legacyBadge = if (spec.isLegacy) " *(Legacy Mode)*" else ""
                sb.appendLine("#### ${spec.title} (`${spec.mode.id}`)$legacyBadge")
                sb.appendLine("- **Summary:** ${spec.summary}")
                sb.appendLine("- **Detailed Mechanics:** ${spec.details}")
                sb.appendLine("- **Answer Checking:** ${spec.answerChecking}")
                sb.appendLine("- **Timing & Scoring:** ${spec.timingAndScoring}")
                sb.appendLine("- **Supports 2×2 Multiple Choice (MCQ):** ${if (spec.supportsMcq) "Yes" else "No"}")
                if (spec.parameters.isNotEmpty()) {
                    sb.appendLine("- **Configurable Parameters:**")
                    for (param in spec.parameters) {
                        sb.appendLine("  - `${param.name}`: ${param.optionsDescription} *(Default: ${param.defaultValue})*")
                    }
                }
                if (spec.questionFormats.isNotEmpty()) {
                    sb.appendLine("- **Question Subtypes & Examples:**")
                    sb.appendLine("  | Subtype | Pattern | Example Prompt | Example Answer | Notes |")
                    sb.appendLine("  | :--- | :--- | :--- | :--- | :--- |")
                    for (qf in spec.questionFormats) {
                        sb.appendLine("  | ${escapeMd(qf.subtypeName)} | `${escapeMd(qf.promptPattern)}` | `${escapeMd(qf.examplePrompt)}` | `${escapeMd(qf.exampleAnswer)}` | ${escapeMd(qf.explanation)} |")
                    }
                }
                sb.appendLine()
            }
        }

        // Section 4: Overall Performance Summary
        sb.appendLine("## 4. Executive Performance Summary")
        sb.appendLine()
        sb.appendLine("| Metric | Value |")
        sb.appendLine("| :--- | :--- |")
        sb.appendLine("| **Total Sessions Completed** | ${ov.totalSessions} (Standard: ${ov.normalSessions}, Speed Round: ${ov.speedRoundSessions}, Weak Spots: ${ov.weakSpotsSessions}, Mistake Review: ${ov.mistakeReviewSessions}) |")
        sb.appendLine("| **Total Questions Answered** | ${ov.totalQuestions} (${ov.totalCorrect} correct, ${ov.totalQuestions - ov.totalCorrect} incorrect) |")
        sb.appendLine("| **Overall Accuracy** | ${fmt1(ov.overallAccuracyPct)}% (Wilson 95% LB: ${fmt1(ov.wilson95LowerPct)}%) |")
        sb.appendLine("| **Total Active Practice Time** | ${formatDurationHuman(ov.totalPracticeSeconds)} (${fmt1(ov.totalPracticeSeconds)}s) |")
        sb.appendLine("| **Response Time (Avg / Median / P90)** | ${fmtSec(ov.avgTimeMs)} / ${fmtSec(ov.medianTimeMs)} / ${fmtSec(ov.p90TimeMs)} |")
        sb.appendLine("| **Current Streak / Best Streak** | ${ov.currentStreakDays} days / ${ov.bestStreakDays} days |")
        sb.appendLine("| **Active Practice Days** | ${ov.activeDaysCount} days (Daily Goal Hit Rate: ${fmt1(ov.dailyGoalHitRatePct)}%) |")
        sb.appendLine()

        // Section 5: Category, Drill & Sub-topic Breakdown
        sb.appendLine("## 5. Performance Breakdown by Category, Drill & Sub-Topic")
        sb.appendLine()
        sb.appendLine("### 5.1 Category Summary")
        if (snapshot.categories.isEmpty()) {
            sb.appendLine("*No category data recorded yet.*")
        } else {
            sb.appendLine("| Category | Sessions | Attempts | Correct | Accuracy | Wilson 95% LB | Avg Time | Median Time | P90 Time | Practice Share |")
            sb.appendLine("| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
            for (c in snapshot.categories) {
                sb.appendLine("| **${escapeMd(c.displayName)}** | ${c.sessions} | ${c.attempts} | ${c.correct} | ${fmt1(c.accuracyPct)}% | ${fmt1(c.wilson95LowerPct)}% | ${fmtSec(c.avgTimeMs)} | ${fmtSec(c.medianTimeMs)} | ${fmtSec(c.p90TimeMs)} | ${fmt1(c.practiceSharePct)}% |")
            }
        }
        sb.appendLine()

        sb.appendLine("### 5.2 Per-Drill Performance")
        if (snapshot.drills.isEmpty()) {
            sb.appendLine("*No drill performance data recorded yet.*")
        } else {
            sb.appendLine("| Drill | Category | Sessions | Attempts | Accuracy | Wilson 95% LB | Avg Time | Median | P90 | Typed Acc (N) | MCQ Acc (N) | Hint % | Timeout % | Trend Δ Acc | Trend Δ Speed | Last Practiced |")
            sb.appendLine("| :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | :--- |")
            for (d in snapshot.drills) {
                val typedStr = if (d.typedAttempts > 0) "${fmt1(d.typedAccuracyPct)}% (${d.typedAttempts})" else "—"
                val mcqStr = if (d.mcqAttempts > 0) "${fmt1(d.mcqAccuracyPct)}% (${d.mcqAttempts})" else "—"
                val trendAccStr = d.trendDeltaAccuracyPct?.let { (if (it >= 0) "+" else "") + "${fmt1(it)}%" } ?: "—"
                val trendSpdStr = d.trendDeltaSpeedMs?.let { (if (it >= 0) "+" else "") + fmtSec(it) } ?: "—"
                sb.appendLine("| **${escapeMd(d.displayName)}** (`${d.mode.id}`) | ${escapeMd(d.category.label)} | ${d.sessions} | ${d.attempts} | ${fmt1(d.accuracyPct)}% | ${fmt1(d.wilson95LowerPct)}% | ${fmtSec(d.avgTimeMs)} | ${fmtSec(d.medianTimeMs)} | ${fmtSec(d.p90TimeMs)} | $typedStr | $mcqStr | ${fmt1(d.hintRatePct)}% | ${fmt1(d.timeoutRatePct)}% | $trendAccStr | $trendSpdStr | ${d.lastPracticedDate ?: "—"} |")
            }
        }
        sb.appendLine()

        sb.appendLine("### 5.3 Granular Sub-Topic (`topicTag`) Breakdown")
        if (snapshot.subtopics.isEmpty()) {
            sb.appendLine("*No granular question records available yet.*")
        } else {
            sb.appendLine("| Sub-Topic Label | Tag ID | Parent Drill | Attempts | Correct | Accuracy | Wilson 95% LB | Avg Time | Hint % |")
            sb.appendLine("| :--- | :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |")
            for (st in snapshot.subtopics) {
                sb.appendLine("| ${escapeMd(st.friendlyLabel)} | `${escapeMd(st.topicTag)}` | ${escapeMd(st.drillDisplayName)} | ${st.attempts} | ${st.correct} | ${fmt1(st.accuracyPct)}% | ${fmt1(st.wilson95LowerPct)}% | ${fmtSec(st.avgTimeMs)} | ${fmt1(st.hintRatePct)}% |")
            }
        }
        sb.appendLine()

        // Section 6: Temporal Trends & Practice Habits
        sb.appendLine("## 6. Temporal Trends & Practice Habits")
        sb.appendLine()
        sb.appendLine("### 6.1 Weekly Progression (Last 12 Weeks)")
        val activeWeeks = snapshot.weeklyTrends.filter { it.questions > 0 || it.sessions > 0 }
        if (activeWeeks.isEmpty()) {
            sb.appendLine("*No weekly trend data available yet.*")
        } else {
            sb.appendLine("| Week Range | Sessions | Questions | Accuracy | Avg Time | Effective QPM |")
            sb.appendLine("| :--- | ---: | ---: | ---: | ---: | ---: |")
            for (w in activeWeeks) {
                sb.appendLine("| `${w.weekLabel}` | ${w.sessions} | ${w.questions} | ${fmt1(w.accuracyPct)}% | ${fmtSec(w.avgTimeMs)} | ${fmt1(w.questionsPerMin)} |")
            }
        }
        sb.appendLine()

        sb.appendLine("### 6.2 Recent 30-Day Daily Activity")
        val last30Active = snapshot.dailyActivity.takeLast(30).filter { it.sessions > 0 || it.questions > 0 }
        if (last30Active.isEmpty()) {
            sb.appendLine("*No daily activity in the last 30 days.*")
        } else {
            sb.appendLine("| Local Date | Sessions | Questions | Correct | Accuracy | Avg Time | Duration | Hit Daily Goal |")
            sb.appendLine("| :--- | ---: | ---: | ---: | ---: | ---: | ---: | :--- |")
            for (day in last30Active) {
                sb.appendLine("| `${day.localDate}` | ${day.sessions} | ${day.questions} | ${day.correct} | ${fmt1(day.accuracyPct)}% | ${fmtSec(day.avgTimeMs)} | ${fmt1(day.durationSec)}s | ${if (day.hitDailyGoal) "Yes" else "No"} |")
            }
        }
        sb.appendLine()

        sb.appendLine("### 6.3 Practice Time-of-Day & Day-of-Week Distribution")
        if (snapshot.punchCard.isEmpty()) {
            sb.appendLine("*No time-of-day distribution data available yet.*")
        } else {
            val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
            sb.appendLine("| Day of Week | Hour (24h) | Attempts | Accuracy |")
            sb.appendLine("| :--- | :--- | ---: | ---: |")
            for (cell in snapshot.punchCard) {
                val dName = dayNames[cell.dayOfWeek] ?: "Day ${cell.dayOfWeek}"
                val hLabel = String.format(Locale.US, "%02d:00–%02d:59", cell.hourOfDay, cell.hourOfDay)
                sb.appendLine("| $dName | `$hLabel` | ${cell.attempts} | ${fmt1(cell.accuracyPct)}% |")
            }
        }
        sb.appendLine()

        // Section 7: Strengths, Weaknesses & Mistake Patterns
        sb.appendLine("## 7. Diagnostic Findings: Strengths, Weaknesses & Error Patterns")
        sb.appendLine()
        sb.appendLine("### 7.1 Top Strengths")
        if (snapshot.strengths.isEmpty()) {
            sb.appendLine("*Complete more sessions to unlock automated strength diagnostics.*")
        } else {
            for (s in snapshot.strengths) {
                sb.appendLine("- **${s.title}** (${s.categoryName}): ${s.evidenceText} *Recommendation:* ${s.recommendationText}")
            }
        }
        sb.appendLine()

        sb.appendLine("### 7.2 Priority Weaknesses & Focus Areas")
        if (snapshot.weaknesses.isEmpty()) {
            sb.appendLine("*Complete more sessions to unlock automated weakness diagnostics.*")
        } else {
            for (w in snapshot.weaknesses) {
                sb.appendLine("- **${w.title}** (${w.categoryName}): ${w.evidenceText} *Action:* ${w.recommendationText}")
            }
        }
        sb.appendLine()

        sb.appendLine("### 7.3 Mistake Pattern Classification")
        if (snapshot.mistakePatterns.isEmpty()) {
            sb.appendLine("*No mistakes recorded yet.*")
        } else {
            sb.appendLine("| Error Pattern | Count | Share of Errors | Description | Representative Examples |")
            sb.appendLine("| :--- | ---: | ---: | :--- | :--- |")
            for (mp in snapshot.mistakePatterns) {
                val exStr = mp.examples.joinToString("; ") { "`${escapeMd(it)}`" }
                sb.appendLine("| **${escapeMd(mp.patternType)}** | ${mp.count} | ${fmt1(mp.sharePct)}% | ${escapeMd(mp.description)} | $exStr |")
            }
        }
        sb.appendLine()

        // Section 8: Detailed Mistake Log
        sb.appendLine("## 8. Item-Level Mistake & Weak-Spot Log")
        sb.appendLine()
        if (snapshot.mistakes.isEmpty()) {
            sb.appendLine("*Zero mistakes recorded across all sessions.*")
        } else {
            if (snapshot.totalDistinctMistakes > snapshot.mistakes.size) {
                sb.appendLine("> **Note:** Showing the top ${snapshot.mistakes.size} most critical mistakes out of ${snapshot.totalDistinctMistakes} total distinct missed questions (ordered by unresolved status, error frequency, and response latency).")
                sb.appendLine()
            }
            sb.appendLine("| # | Status | Drill | Sub-Topic | Question Prompt | Expected Answer | Latest Wrong Answer | Missed / Attempts | Error Rate | Avg Time | Last Seen |")
            sb.appendLine("| ---: | :--- | :--- | :--- | :--- | :--- | :--- | ---: | ---: | ---: | :--- |")
            snapshot.mistakes.forEachIndexed { idx, m ->
                val statusLabel = if (m.isResolved) "Resolved" else "Unresolved"
                sb.appendLine("| ${idx + 1} | **$statusLabel** | ${escapeMd(m.drillDisplayName)} | ${escapeMd(m.topicLabel)} | `${escapeMd(m.questionText)}` | `${escapeMd(m.correctAnswer)}` | `${escapeMd(m.latestUserAnswer)}` | ${m.errors}/${m.attempts} | ${fmt1(m.errorRatePct)}% | ${fmtSec(m.avgTimeMs)} | `${m.lastSeenDate}` |")
            }
        }
        sb.appendLine()

        // Section 9: Recent Session History
        sb.appendLine("## 9. Recent Session History")
        sb.appendLine()
        if (snapshot.recentSessions.isEmpty()) {
            sb.appendLine("*No sessions recorded yet.*")
        } else {
            sb.appendLine("| Local Date | UTC Timestamp | Mode / Drill | Session Type | Scope / Notes | Difficulty | Score | Accuracy | Avg Time | Duration |")
            sb.appendLine("| :--- | :--- | :--- | :--- | :--- | :--- | ---: | ---: | ---: | ---: |")
            for (s in snapshot.recentSessions) {
                val scopeStr = s.scopeLabel?.let { escapeMd(it) } ?: "—"
                sb.appendLine("| `${s.localDate}` | `${s.dateIso}` | **${escapeMd(s.modeDisplayName)}** (`${s.modeId}`) | `${s.sessionType}` | $scopeStr | ${s.difficulty} | ${s.correct}/${s.totalQuestions} | ${fmt1(s.accuracyPct)}% | ${fmtSec(s.avgTimeMs)} | ${fmt1(s.durationSec)}s |")
            }
        }

        return sb.toString()
    }

    private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)

    private fun fmtSec(ms: Double): String = String.format(Locale.US, "%.2fs", ms / 1000.0)

    private fun formatDurationHuman(seconds: Double): String {
        val totalSec = seconds.toLong().coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return when {
            hours > 0 -> "${hours}h ${mins}m ${secs}s"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    private fun escapeYaml(raw: String): String =
        raw.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun escapeMd(raw: String): String =
        raw.replace("|", "\\|").replace("\n", " ").trim()
}
