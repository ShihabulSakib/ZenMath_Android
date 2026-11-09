package com.example.domain.drills

import com.example.domain.GameMode

object TopicTagResolver {

    private val COMPOUND_MODE_IDS = setOf(
        GameMode.SPEED_ROUND.id,
        GameMode.WEAK_SPOTS.id,
        GameMode.MISTAKE_REVIEW.id,
        ""
    )

    /**
     * Resolves the effective sub-drill [GameMode] for a question record.
     *
     * Priority:
     * 1. Explicit `drillMode` column if present and not a compound mode.
     * 2. Infer from `topicTag` if `mode` is compound (`speed-round`, `weak-spots`, `mistake-review`) or blank.
     * 3. Explicit `mode` column if it is a concrete drill.
     * 4. Fallback to tag inference.
     */
    fun resolveDrillMode(
        topicTag: String,
        modeId: String = "",
        drillModeId: String = ""
    ): GameMode {
        if (drillModeId.isNotBlank() && drillModeId !in COMPOUND_MODE_IDS) {
            GameMode.entries.find { it.id == drillModeId }?.let { return it }
        }
        if (modeId !in COMPOUND_MODE_IDS) {
            GameMode.entries.find { it.id == modeId }?.let { mode ->
                // If outer mode is MIXED, keep MIXED unless caller specifically asks for tag mode
                return mode
            }
        }
        return inferModeFromTag(topicTag, fallbackModeId = modeId)
    }

    /**
     * Maps legacy standalone modes (`SQUARE_ROOT`, `MATH_CONSTANTS`) to their merged active counterparts
     * (`SQUARE`, `CONSTANTS`) when grouping active practice drills, while leaving `REFERENCE_VALUES` as-is.
     */
    fun canonicalActiveMode(mode: GameMode): GameMode {
        return when (mode) {
            GameMode.SQUARE_ROOT -> GameMode.SQUARE
            GameMode.MATH_CONSTANTS -> GameMode.CONSTANTS
            else -> mode
        }
    }

    fun inferModeFromTag(rawTag: String, fallbackModeId: String = ""): GameMode {
        val tag = rawTag.trim().lowercase()
        return when {
            tag == "addition" || tag == "+" -> GameMode.ADDITION
            tag == "subtraction" || tag == "-" || tag == "−" -> GameMode.SUBTRACTION
            tag == "multiplication" || tag == "*" || tag == "×" -> GameMode.MULTIPLICATION
            tag == "division" || tag == "/" || tag == "÷" -> GameMode.DIVISION
            tag == "multiplication-table" -> GameMode.MULTIPLICATION_TABLE
            tag == "factor-finding" -> GameMode.FACTOR_FINDING
            tag == "square" || tag == "squares" || tag == "square-root" || tag == "sqrt-irrational" -> GameMode.SQUARE
            tag == "fraction" -> GameMode.FRACTION
            tag == "percentage" -> GameMode.PERCENTAGE
            tag == "approximation" -> GameMode.APPROXIMATION
            tag == "number-series" -> GameMode.NUMBER_SERIES
            tag == "ratio" -> GameMode.RATIO
            tag == "chain-calculation" -> GameMode.CHAIN_CALCULATION
            tag.startsWith("memory-") || tag.startsWith("memory_") -> GameMode.MEMORY_TABLES
            tag.startsWith("sci-") || tag.startsWith("scientific") -> GameMode.SCIENTIFIC_NOTATION
            tag.startsWith("ph-") || tag == "ph-poh" -> GameMode.PH_POH
            tag.startsWith("composite-") || tag == "composite-logs-roots" -> GameMode.COMPOSITE_LOGS_ROOTS
            tag == "cubes" || tag == "powers" || tag == "cube-roots-exact" || tag == "cubes-and-powers" -> GameMode.CUBES_AND_POWERS
            tag.startsWith("atomic-mass-") || tag == "atomic-masses" -> GameMode.ATOMIC_MASSES
            tag.startsWith("conversion-") || tag == "unit-conversions" -> GameMode.UNIT_CONVERSIONS
            tag.startsWith("math-constant-") || tag == "math-constants" -> GameMode.CONSTANTS
            tag.startsWith("reference-") || tag == "reference-values" -> GameMode.REFERENCE_VALUES
            tag.startsWith("constant-") || tag == "constants" -> GameMode.CONSTANTS
            tag.isNotEmpty() -> {
                // Check if tag matches a GameMode id directly
                GameMode.entries.find { it.id == tag }
                    // Any HSC constant ID (e.g. "speed_of_light_vacuum", "planck_constant", "molar_gas_constant")
                    // uses snake_case and comes from ConstantsGenerator
                    ?: if (tag.contains("_")) GameMode.CONSTANTS
                    else GameMode.entries.find { it.id == fallbackModeId } ?: GameMode.ADDITION
            }
            else -> GameMode.entries.find { it.id == fallbackModeId } ?: GameMode.ADDITION
        }
    }

    fun friendlyTopicLabel(rawTag: String): String {
        val tag = rawTag.trim()
        if (tag.isEmpty()) return "General"
        val lower = tag.lowercase()
        return when {
            lower == "addition" -> "Addition"
            lower == "subtraction" -> "Subtraction"
            lower == "multiplication" -> "Multiplication"
            lower == "division" -> "Division"
            lower == "mixed" -> "Mixed Operations"
            lower == "multiplication-table" -> "Multiplication Tables"
            lower == "factor-finding" -> "Factor Finding"
            lower == "square" || lower == "squares" -> "Squares (n²)"
            lower == "square-root" -> "Perfect Square Roots (√n²)"
            lower == "sqrt-irrational" -> "Square Roots (√2–√10)"
            lower == "fraction" -> "Fractions"
            lower == "percentage" -> "Percentages"
            lower == "approximation" -> "Estimation"
            lower == "number-series" -> "Number Series"
            lower == "ratio" -> "Ratios"
            lower == "chain-calculation" -> "Chain Calculation"
            lower == "cubes" -> "Cubes (n³)"
            lower == "powers" -> "Small Powers (aᵇ)"
            lower == "cube-roots-exact" -> "Cube Roots (∛n)"
            lower == "sci-normalize" -> "Sci. Notation · Normalize"
            lower == "sci-shift" -> "Sci. Notation · Shift Rule"
            lower == "sci-mul-div" -> "Sci. Notation · Multiply/Divide"
            lower == "sci-root-pow10" -> "Sci. Notation · Roots of 10ⁿ"
            lower == "ph-conc-to-ph" -> "pH · [H⁺] → pH"
            lower == "ph-sci-to-ph" -> "pH · a×10⁻ⁿ M → pH"
            lower == "ph-to-conc" -> "pH · pH → [H⁺]"
            lower == "ph-poh-conv" -> "pH · pH ↔ pOH"
            lower == "ph-percent-wv" -> "pH · % w/v → pH"
            lower == "composite-log-sci" -> "Logs & Roots · log₁₀(k×10ⁿ)"
            lower == "composite-log-prod" -> "Logs & Roots · Composite log₁₀(a×b)"
            lower == "composite-root-sci" -> "Logs & Roots · √(a×10ⁿ)"
            lower.startsWith("memory-") -> {
                val isRev = lower.endsWith("-reverse")
                val core = lower.removePrefix("memory-").removeSuffix("-reverse")
                val table = when (core) {
                    "sqrt_table" -> "√2–√10"
                    "cbrt_table" -> "∛2–∛10"
                    "log_table" -> "log₁₀(1–10)"
                    "reciprocal_table" -> "Reciprocals 1/11–1/15"
                    else -> core.replace("_", " ")
                }
                if (isRev) "Memory · $table (Reverse)" else "Memory · $table"
            }
            lower.startsWith("math-constant-") -> {
                val id = lower.removePrefix("math-constant-")
                "Math Constant · ${id.replace("_", " ").replaceFirstChar { it.uppercase() }}"
            }
            lower.startsWith("atomic-mass-") -> {
                val sym = tag.removePrefix("atomic-mass-").replaceFirstChar { it.uppercase() }
                "Atomic Mass · $sym"
            }
            lower.startsWith("conversion-") -> {
                val grp = lower.removePrefix("conversion-").replace("_", " ")
                "Conversion · ${grp.replaceFirstChar { it.uppercase() }}"
            }
            lower.startsWith("reference-") -> {
                val id = lower.removePrefix("reference-").replace("_", " ")
                "Reference · ${id.replaceFirstChar { it.uppercase() }}"
            }
            lower.startsWith("constant-") -> {
                val id = lower.removePrefix("constant-").replace("_", " ")
                "Constant · ${id.replaceFirstChar { it.uppercase() }}"
            }
            else -> tag.replace("_", " ").replace("-", " ").replaceFirstChar { it.uppercase() }
        }
    }
}
