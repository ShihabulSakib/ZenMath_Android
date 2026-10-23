package com.example.data

import com.example.BuildConfig

/**
 * Schema and loader/validator for the modular Constants feature (Section 5.1 & 5.2).
 */
enum class ConstantSubject(val id: String, val displayName: String) {
    MECHANICS("mechanics", "Mechanics"),
    THERMODYNAMICS("thermodynamics", "Thermodynamics"),
    ELECTROSTATICS("electrostatics", "Electrostatics"),
    CURRENT_ELECTRICITY("current_electricity", "Current electricity"),
    MAGNETISM("magnetism", "Magnetism"),
    WAVES_AND_OPTICS("waves_and_optics", "Waves and optics"),
    MODERN_PHYSICS("modern_physics", "Modern physics"),
    CHEMISTRY("chemistry", "Chemistry")
}

/**
 * Structured representation of a constant's symbol (e.g., k_e -> base="k", subscript="e").
 */
data class StructuredSymbol(
    val base: String,
    val subscript: String? = null,
    val superscript: String? = null
) {
    fun toPlainString(): String {
        val sub = if (!subscript.isNullOrEmpty()) "_$subscript" else ""
        val sup = if (!superscript.isNullOrEmpty()) "^$superscript" else ""
        return "$base$sub$sup"
    }
}

/**
 * Single factor in a structured physical unit, e.g., UnitFactor("m", 1), UnitFactor("s", -2).
 */
data class UnitFactor(
    val symbol: String,
    val exponent: Int = 1
)

/**
 * Structured representation of a physical unit for rich rendering with true superscripts.
 */
data class StructuredUnit(
    val factors: List<UnitFactor>
) {
    fun toPlainString(): String {
        if (factors.isEmpty()) return ""
        return factors.joinToString("·") { f ->
            if (f.exponent == 1) f.symbol else "${f.symbol}^${f.exponent}"
        }
    }

    companion object {
        val DIMENSIONLESS = StructuredUnit(emptyList())
    }
}

/**
 * Optional alternate value and unit for constants taught in multiple unit systems
 * (for example, the gas constant R in J·mol⁻¹·K⁻¹ vs L·atm·mol⁻¹·K⁻¹).
 */
data class AlternateConstantValue(
    val mantissa: Double,
    val exponent: Int,
    val unit: StructuredUnit,
    val label: String? = null
)

/**
 * Full schema for a single scientific constant entry.
 */
data class ConstantEntry(
    val id: String,
    val name: String,
    val symbol: StructuredSymbol,
    val subject: ConstantSubject,
    val mantissa: Double,
    val exponent: Int,
    val unit: StructuredUnit,
    val notes: String? = null,
    val alternateValue: AlternateConstantValue? = null,
    val confusableIds: List<String> = emptyList(),
    val toleranceOverride: Double? = null,
    val enabled: Boolean = true,
    val isSample: Boolean = false
)

object ConstantsLoader {

    /**
     * Validates and returns the list of valid constant entries.
     * In debug builds ([isDebug] = true), throws [IllegalStateException] on duplicate IDs
     * or missing required fields.
     * In release builds ([isDebug] = false), skips invalid or duplicate entries and carries on.
     */
    fun validateAndLoad(
        entries: List<ConstantEntry> = ConstantsData.ENTRIES,
        isDebug: Boolean = BuildConfig.DEBUG
    ): List<ConstantEntry> {
        val seenIds = mutableSetOf<String>()
        val validEntries = mutableListOf<ConstantEntry>()

        for (entry in entries) {
            val errorReason = when {
                entry.id.isBlank() -> "Constant entry has blank id"
                entry.name.isBlank() -> "Constant '${entry.id}' has blank descriptive name"
                entry.symbol.base.isBlank() -> "Constant '${entry.id}' has blank symbol base"
                entry.mantissa.isNaN() || entry.mantissa.isInfinite() || entry.mantissa == 0.0 ->
                    "Constant '${entry.id}' has invalid mantissa: ${entry.mantissa}"
                !seenIds.add(entry.id) -> "Duplicate constant id: '${entry.id}'"
                else -> null
            }

            if (errorReason != null) {
                if (isDebug) {
                    throw IllegalStateException("ConstantsLoader validation failed: $errorReason")
                } else {
                    continue
                }
            }

            validEntries.add(entry)
        }

        return validEntries
    }

    fun getEnabledEntries(
        subjects: Set<ConstantSubject>? = null,
        entries: List<ConstantEntry> = ConstantsData.ENTRIES,
        isDebug: Boolean = BuildConfig.DEBUG
    ): List<ConstantEntry> {
        val loaded = validateAndLoad(entries, isDebug).filter { it.enabled }
        return if (subjects.isNullOrEmpty()) {
            loaded
        } else {
            loaded.filter { it.subject in subjects }
        }
    }

    fun findById(
        id: String,
        entries: List<ConstantEntry> = ConstantsData.ENTRIES,
        isDebug: Boolean = BuildConfig.DEBUG
    ): ConstantEntry? {
        return validateAndLoad(entries, isDebug).find { it.id == id }
    }
}
