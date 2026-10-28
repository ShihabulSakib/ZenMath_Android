package com.example.data

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Legacy subject enum preserved for compatibility alongside HSC Paper & Chapter mapping.
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
 * HSC Paper grouping for syllabus filtering (Section 4 & 5).
 */
enum class HscPaper(val id: String, val displayName: String, val subjectName: String, val paperNumber: String) {
    PHYSICS_1ST("physics_1st", "Physics 1st", "Physics", "1st"),
    PHYSICS_2ND("physics_2nd", "Physics 2nd", "Physics", "2nd"),
    CHEMISTRY_1ST("chemistry_1st", "Chemistry 1st", "Chemistry", "1st"),
    CHEMISTRY_2ND("chemistry_2nd", "Chemistry 2nd", "Chemistry", "2nd"),
    GENERAL_PHYSICS("general_physics", "General physics", "Physics", "General");

    companion object {
        fun fromSubjectAndPaper(subject: String, paper: String): HscPaper {
            return when {
                subject.equals("Physics", ignoreCase = true) && paper.startsWith("1") -> PHYSICS_1ST
                subject.equals("Physics", ignoreCase = true) && paper.startsWith("2") -> PHYSICS_2ND
                subject.equals("Chemistry", ignoreCase = true) && paper.startsWith("1") -> CHEMISTRY_1ST
                subject.equals("Chemistry", ignoreCase = true) && paper.startsWith("2") -> CHEMISTRY_2ND
                else -> GENERAL_PHYSICS
            }
        }
    }
}

/**
 * Three-tier reliability classification for constants (Section 4).
 */
enum class ReliabilityClass(
    val id: String,
    val displayName: String,
    val shortLabel: String
) {
    FUNDAMENTAL(
        id = "fundamental",
        displayName = "Defined or measured fundamental values",
        shortLabel = "Fundamental"
    ),
    CONVENTIONS(
        id = "conventions",
        displayName = "Conventions",
        shortLabel = "Conventions"
    ),
    REFERENCE_AND_APPROXIMATE(
        id = "reference_and_approximate",
        displayName = "Reference and approximate values",
        shortLabel = "Reference & Approx"
    );

    companion object {
        private val FUNDAMENTAL_TYPES = setOf(
            "defined_exact",
            "defined_exact_from_charge",
            "defined_unit",
            "measured",
            "measured_dimensionless",
            "measured_in_post_2019_SI",
            "measured_derived",
            "derived",
            "derived_from_exact_constants",
            "derived_exact_from_defined_constants",
            "derived_measured"
        )

        private val CONVENTION_TYPES = setOf(
            "conventional",
            "defined_conventional",
            "condition_specific_derived"
        )

        private val REFERENCE_TYPES = setOf(
            "condition_dependent_approximation",
            "condition_dependent_reference",
            "condition_specific_approximation",
            "approximate",
            "measured_reference",
            "derived_reference",
            "derived_measured_reference",
            "empirical_approximation"
        )

        fun fromConstantType(constantType: String): ReliabilityClass {
            val cleaned = constantType.trim()
            return when {
                cleaned in FUNDAMENTAL_TYPES -> FUNDAMENTAL
                cleaned in CONVENTION_TYPES -> CONVENTIONS
                cleaned in REFERENCE_TYPES -> REFERENCE_AND_APPROXIMATE
                cleaned.contains("exact", ignoreCase = true) || cleaned.contains("measured", ignoreCase = true) -> FUNDAMENTAL
                cleaned.contains("convention", ignoreCase = true) -> CONVENTIONS
                else -> REFERENCE_AND_APPROXIMATE
            }
        }
    }
}

/**
 * Syllabus chapter metadata with English and Bangla chapter names.
 */
data class ChapterRef(
    val paper: HscPaper,
    val chapterNumber: Int,
    val nameEn: String,
    val nameBn: String = "",
    val notes: String = ""
) {
    val key: String get() = "${paper.id}_ch$chapterNumber"

    /** English subject chip label required by Section 6: "Physics 2nd, Static Electricity". */
    val englishChipLabel: String
        get() = if (paper == HscPaper.GENERAL_PHYSICS || nameEn.isBlank()) {
            paper.displayName
        } else {
            "${paper.displayName}, $nameEn"
        }

    /** Revision display label combining English and Bangla chapter names. */
    val bilingualLabel: String
        get() = when {
            paper == HscPaper.GENERAL_PHYSICS -> paper.displayName
            nameBn.isNotBlank() -> "${paper.displayName} Ch $chapterNumber: $nameEn ($nameBn)"
            else -> "${paper.displayName} Ch $chapterNumber: $nameEn"
        }
}

/**
 * Structured representation of a constant's symbol (e.g., k_e -> base="k", subscript="e").
 */
data class StructuredSymbol(
    val base: String,
    val subscript: String? = null,
    val superscript: String? = null,
    val rawFullSymbol: String? = null,
    val shortDisplaySymbol: String? = null
) {
    val shortSymbol: String
        get() = shortDisplaySymbol ?: toPlainString()

    val fullSymbol: String
        get() = rawFullSymbol ?: toPlainString()

    fun toPlainString(): String {
        if (!shortDisplaySymbol.isNullOrBlank()) return shortDisplaySymbol
        val sub = if (!subscript.isNullOrEmpty()) "_$subscript" else ""
        val sup = if (!superscript.isNullOrEmpty()) "^$superscript" else ""
        return "$base$sub$sup"
    }

    companion object {
        fun fromRawSymbol(rawSymbol: String, overrideDisplaySymbol: String? = null): StructuredSymbol {
            val full = rawSymbol.trim()
            val shortRaw = (overrideDisplaySymbol?.trim()?.takeIf { it.isNotEmpty() } ?: extractShortSymbol(full))
            val (base, sub, sup) = splitSimpleSymbolParts(shortRaw)
            return StructuredSymbol(
                base = base,
                subscript = sub,
                superscript = sup,
                rawFullSymbol = full,
                shortDisplaySymbol = shortRaw
            )
        }

        /**
         * Section 3: For list rows and drill questions, use the short symbol before the first `=` or `(`
         * (so `R = N_A k_B` displays as `R`, `E°(H⁺/H₂)` as `E°`, `e (math)` as `e`, `u (or m_u)` as `u`).
         */
        fun extractShortSymbol(rawSymbol: String): String {
            val trimmed = rawSymbol.trim()
            val eqIdx = trimmed.indexOf('=')
            val parenIdx = trimmed.indexOf('(')
            val cutIdx = when {
                eqIdx >= 0 && parenIdx >= 0 -> minOf(eqIdx, parenIdx)
                eqIdx >= 0 -> eqIdx
                parenIdx >= 0 -> parenIdx
                else -> -1
            }
            val candidate = if (cutIdx > 0) trimmed.substring(0, cutIdx).trim() else trimmed
            return candidate.ifEmpty { trimmed }
        }

        private fun splitSimpleSymbolParts(shortSym: String): Triple<String, String?, String?> {
            // If it contains spaces or slashes (like "m_e c²", "e/m_e", "ln 2", "0 K"), keep full shortSym in base
            // so RichTextHelper can render all inline subscripts/superscripts cleanly.
            if (shortSym.contains(' ') || shortSym.contains('/')) {
                return Triple(shortSym, null, null)
            }
            val underIdx = shortSym.indexOf('_')
            if (underIdx > 0 && underIdx < shortSym.length - 1) {
                val basePart = shortSym.substring(0, underIdx)
                val rest = shortSym.substring(underIdx + 1)
                if (rest.endsWith("°")) {
                    return Triple(basePart, rest.removeSuffix("°"), "°")
                }
                return Triple(basePart, rest, null)
            }
            return Triple(shortSym, null, null)
        }
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
    val factors: List<UnitFactor>,
    val rawUnitString: String? = null
) {
    val isDimensionless: Boolean
        get() {
            val raw = rawUnitString?.trim()
            if (raw != null) {
                return raw.isEmpty() || raw.equals("dimensionless", ignoreCase = true) || raw == "1" || raw.equals("no unit", ignoreCase = true)
            }
            return factors.isEmpty()
        }

    fun toPlainString(): String {
        val raw = rawUnitString?.trim()
        if (raw != null) {
            if (raw.isEmpty() || raw.equals("dimensionless", ignoreCase = true) || raw == "1" || raw.equals("no unit", ignoreCase = true)) {
                return "no unit"
            }
            return raw
        }
        if (factors.isEmpty()) return "no unit"
        return factors.joinToString("·") { f ->
            if (f.exponent == 1) f.symbol else "${f.symbol}^${f.exponent}"
        }
    }

    companion object {
        val DIMENSIONLESS = StructuredUnit(emptyList(), "dimensionless")

        fun fromRawUnit(rawUnit: String): StructuredUnit {
            val trimmed = rawUnit.trim()
            if (trimmed.isEmpty() || trimmed.equals("dimensionless", ignoreCase = true) || trimmed == "1" || trimmed.equals("no unit", ignoreCase = true)) {
                return StructuredUnit(emptyList(), trimmed.ifEmpty { "dimensionless" })
            }
            val factors = parseFactors(trimmed)
            return StructuredUnit(factors = factors, rawUnitString = trimmed)
        }

        private fun parseFactors(raw: String): List<UnitFactor> {
            // Simple space-separated factor parser for standard SI/CGS units (e.g. "m^3 kg^-1 s^-2")
            if (raw.contains('/') || raw.contains('(')) {
                return listOf(UnitFactor(raw, 1))
            }
            val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
            return tokens.map { tok ->
                val caretIdx = tok.indexOf('^')
                if (caretIdx > 0 && caretIdx < tok.length - 1) {
                    val sym = tok.substring(0, caretIdx)
                    val expStr = tok.substring(caretIdx + 1).replace("−", "-")
                    val exp = expStr.toIntOrNull() ?: 1
                    UnitFactor(sym, exp)
                } else {
                    UnitFactor(tok, 1)
                }
            }
        }
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
    val label: String? = null,
    val preciseValue: Double = mantissa * 10.0.pow(exponent.toDouble()),
    val courseValue: Double = preciseValue,
    val system: String = label ?: "alternate",
    val formatted: String? = null
)

/**
 * Per-ID override entry loaded from `constants_overrides.json`.
 */
data class ConstantOverride(
    val id: String,
    val courseMantissa: Double? = null,
    val courseExponent: Int? = null,
    val tolerance: Double? = null,
    val enabled: Boolean? = null,
    val displaySymbol: String? = null,
    val extraConfusableIds: List<String> = emptyList()
) {
    val courseNumericValue: Double?
        get() = if (courseMantissa != null) {
            val exp = courseExponent ?: 0
            courseMantissa * 10.0.pow(exp.toDouble())
        } else {
            null
        }
}

/**
 * Full schema for a single scientific constant entry (or condition-dependent reference value).
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
    val isSample: Boolean = false,
    // Extended HSC Dataset fields
    val preciseValue: Double = mantissa * 10.0.pow(exponent.toDouble()),
    val courseMantissa: Double = mantissa,
    val courseExponent: Int = exponent,
    val courseValue: Double = courseMantissa * 10.0.pow(courseExponent.toDouble()),
    val commonHscApproximationRaw: String? = null,
    val primarySystem: String = "SI",
    val primaryFormatted: String? = null,
    val allAlternateValues: List<AlternateConstantValue> = listOfNotNull(alternateValue),
    val category: String = subject.id,
    val constantType: String = "defined_exact",
    val reliabilityClass: ReliabilityClass = ReliabilityClass.fromConstantType(constantType),
    val definitionOrMeaning: String? = null,
    val formulaUses: List<String> = emptyList(),
    val notesList: List<String> = listOfNotNull(notes),
    val chapters: List<ChapterRef> = emptyList(),
    val sources: List<String> = emptyList(),
    val isReferenceValue: Boolean = false
) {
    constructor(
        id: String,
        name: String,
        symbol: StructuredSymbol,
        subject: String,
        mantissa: Double,
        exponent: Int,
        unit: StructuredUnit,
        notes: String? = null,
        alternateValue: AlternateConstantValue? = null,
        confusableIds: List<String> = emptyList(),
        toleranceOverride: Double? = null,
        enabled: Boolean = true,
        isSample: Boolean = false
    ) : this(
        id = id,
        name = name,
        symbol = symbol,
        subject = ConstantSubject.entries.find {
            it.displayName.equals(subject, ignoreCase = true) || it.id.equals(subject, ignoreCase = true)
        } ?: ConstantSubject.MECHANICS,
        mantissa = mantissa,
        exponent = exponent,
        unit = unit,
        notes = notes,
        alternateValue = alternateValue,
        confusableIds = confusableIds,
        toleranceOverride = toleranceOverride,
        enabled = enabled,
        isSample = isSample
    )

    val numericValue: Double
        get() = preciseValue

    val alternateValues: List<AlternateConstantValue>
        get() = allAlternateValues

    val relativeToleranceOverride: Double?
        get() = toleranceOverride

    val confusableWith: List<String>
        get() = confusableIds

    val isZeroValued: Boolean
        get() = abs(preciseValue) <= 1e-15 && abs(courseValue) <= 1e-15

    /**
     * Primary English subject chip for drill questions (e.g. "Physics 2nd, Static Electricity").
     */
    val subjectLabel: String
        get() = chapters.firstOrNull()?.englishChipLabel ?: subject.displayName

    val papers: Set<HscPaper>
        get() = if (chapters.isEmpty()) setOf(HscPaper.GENERAL_PHYSICS) else chapters.map { it.paper }.toSet()
}

/**
 * Mathematical constant entry (`pi`, `euler_number`, `natural_log_2`, `natural_log_10`).
 */
data class MathConstantEntry(
    val id: String,
    val name: String,
    val symbol: StructuredSymbol,
    val value: Double,
    val unit: StructuredUnit = StructuredUnit.DIMENSIONLESS,
    val commonApproximations: List<String> = emptyList(),
    val relevance: List<String> = emptyList(),
    val chapters: List<ChapterRef> = emptyList(),
    val confusableIds: List<String> = emptyList()
) {
    /** Numeric approximations parsed from [commonApproximations], including fractions like `22/7`. */
    val acceptedNumericApproximations: List<Double>
        get() = commonApproximations.mapNotNull { raw ->
            val trimmed = raw.trim()
            if (trimmed.contains('/')) {
                val parts = trimmed.split('/')
                val num = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
                val den = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
                if (num != null && den != null && den != 0.0) num / den else null
            } else {
                trimmed.toDoubleOrNull()
            }
        }
}

/**
 * Common HSC atomic mass entry (39 elements).
 */
data class AtomicMassEntry(
    val symbol: String,
    val element: String,
    val relativeAtomicMassSchool: Double,
    val molarMassSchoolGMol: Double,
    val modernCiaawValue: String,
    val notes: String? = null,
    val classification: String = "",
    val source: String = ""
)

/**
 * Single conversion factor or formula in a [UnitConversionEntry].
 */
data class ConversionValueItem(
    val fromUnit: StructuredUnit? = null,
    val toUnit: StructuredUnit? = null,
    val factor: Double? = null,
    val formula: String? = null
) {
    val isNumericFactor: Boolean
        get() = fromUnit != null && toUnit != null && factor != null && !factor.isNaN()
}

/**
 * Unit conversion group (14 entries; `temperature` is formula-only).
 */
data class UnitConversionEntry(
    val id: String,
    val quantity: String,
    val conversionSummary: String,
    val values: List<ConversionValueItem>
) {
    val isFormulaOnly: Boolean
        get() = values.none { it.isNumericFactor }
}

/**
 * Read-only derived formula reference (14 entries).
 */
data class DerivedFormulaEntry(
    val id: String,
    val formula: String,
    val description: String,
    val chapters: List<String>
)

/**
 * Entry in `not_fixed_constants_or_condition_dependent_tables` (8 entries).
 */
data class NotFixedTableEntry(
    val id: String,
    val items: List<String>,
    val whyNotOneFixedValue: String,
    val formulaNotes: List<String> = emptyList(),
    val chapters: List<String> = emptyList()
)

/**
 * Bibliographic source entry in `sources`.
 */
data class ConstantSourceEntry(
    val id: String,
    val organization: String,
    val title: String,
    val url: String,
    val usedFor: String
)

/**
 * Dataset metadata (`version`, `generated_date`, `symbol_notes`, `important_limitations`, etc.).
 */
data class ConstantsMetadata(
    val title: String = "",
    val version: String = "1.0.0",
    val generatedDate: String = "",
    val target: String = "",
    val scope: String = "",
    val valuePrecisionPolicy: String = "",
    val importantLimitations: List<String> = emptyList(),
    val symbolNotes: List<String> = emptyList()
)

/**
 * Complete parsed HSC constants dataset cached in memory.
 */
data class ConstantsDataset(
    val metadata: ConstantsMetadata = ConstantsMetadata(),
    val sources: List<ConstantSourceEntry> = emptyList(),
    val syllabusChapters: List<ChapterRef> = emptyList(),
    val constants: List<ConstantEntry> = emptyList(),
    val mathConstants: List<MathConstantEntry> = emptyList(),
    val referenceValues: List<ConstantEntry> = emptyList(),
    val atomicMasses: List<AtomicMassEntry> = emptyList(),
    val unitConversions: List<UnitConversionEntry> = emptyList(),
    val derivedFormulas: List<DerivedFormulaEntry> = emptyList(),
    val notFixedTables: List<NotFixedTableEntry> = emptyList(),
    val chapterAliases: Map<String, String> = emptyMap(),
    val rawChapterConstantIds: List<Pair<ChapterRef, List<String>>> = emptyList(),
    val overrides: Map<String, ConstantOverride> = emptyMap()
) {
    /** All constants + reference values combined for unified lookup. */
    val allConstantAndReferenceEntries: List<ConstantEntry>
        get() = constants + referenceValues
}

object ConstantsLoader {

    const val MAIN_ASSET_NAME = "hsc_physics_chemistry_constants.json"
    const val OVERRIDES_ASSET_NAME = "constants_overrides.json"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    @Volatile
    private var cachedDataset: ConstantsDataset? = null

    /**
     * Triggers non-blocking background loading and caching of the constants dataset.
     */
    fun preloadAsync(context: Context? = null) {
        if (cachedDataset != null) return
        backgroundScope.launch {
            getDataset(context)
        }
    }

    /**
     * Returns the cached [ConstantsDataset], or loads and caches it synchronously if not yet loaded.
     */
    fun getDataset(
        context: Context? = null,
        isDebug: Boolean = BuildConfig.DEBUG,
        failLoudly: Boolean? = null
    ): ConstantsDataset {
        cachedDataset?.let { return it }
        synchronized(lock) {
            cachedDataset?.let { return it }
            val mainJson = readAssetText(MAIN_ASSET_NAME, context)
            val overridesJson = readAssetTextOrNull(OVERRIDES_ASSET_NAME, context)
            val parsed = parseDatasetFromJson(
                mainJsonStr = mainJson,
                overridesJsonStr = overridesJson,
                isDebug = isDebug,
                failLoudly = failLoudly
            )
            cachedDataset = parsed
            return parsed
        }
    }

    /**
     * Clears the cached dataset (useful in unit tests testing custom overrides).
     */
    fun clearCache() {
        synchronized(lock) {
            cachedDataset = null
        }
    }

    /**
     * Parses a [ConstantsDataset] from raw JSON strings (used both at runtime and in unit tests
     * testing custom override strings).
     */
    fun parseDatasetFromJson(
        mainJsonStr: String,
        overridesJsonStr: String? = null,
        isDebug: Boolean = BuildConfig.DEBUG,
        failLoudly: Boolean? = null
    ): ConstantsDataset {
        val shouldFailLoudly = failLoudly ?: isDebug
        val overrides = parseOverridesJson(overridesJsonStr)
        val root = SimpleJsonObject.parse(mainJsonStr)

        // 1. Metadata
        val metaObj = root.optJSONObject("metadata") ?: SimpleJsonObject()
        val metadata = ConstantsMetadata(
            title = metaObj.optString("title", ""),
            version = metaObj.optString("version", "1.0.0"),
            generatedDate = metaObj.optString("generated_date", ""),
            target = metaObj.optString("target", ""),
            scope = metaObj.optString("scope", ""),
            valuePrecisionPolicy = metaObj.optString("value_precision_policy", ""),
            importantLimitations = jsonArrayToStringList(metaObj.optJSONArray("important_limitations")),
            symbolNotes = jsonArrayToStringList(metaObj.optJSONArray("symbol_notes"))
        )

        // 2. Sources
        val sourcesArr = root.optJSONArray("sources") ?: SimpleJsonArray()
        val sources = mutableListOf<ConstantSourceEntry>()
        for (i in 0 until sourcesArr.length()) {
            val sObj = sourcesArr.optJSONObject(i) ?: continue
            sources.add(
                ConstantSourceEntry(
                    id = sObj.optString("id", ""),
                    organization = sObj.optString("organization", ""),
                    title = sObj.optString("title", ""),
                    url = sObj.optString("url", ""),
                    usedFor = sObj.optString("used_for", "")
                )
            )
        }

        // 3. Syllabus Map (Physics 1st/2nd, Chemistry 1st/2nd with English & Bangla chapter names)
        val syllabusObj = root.optJSONObject("syllabus_map") ?: SimpleJsonObject()
        val syllabusChapters = mutableListOf<ChapterRef>()
        parseSyllabusSubject(syllabusObj.optJSONObject("physics"), "Physics", syllabusChapters)
        parseSyllabusSubject(syllabusObj.optJSONObject("chemistry"), "Chemistry", syllabusChapters)

        val chapterByPaperAndNum = syllabusChapters.associateBy { Pair(it.paper, it.chapterNumber) }
        val chapterByPaperAndNameEn = syllabusChapters.associateBy {
            Pair(it.paper, it.nameEn.lowercase().trim())
        }

        // 4. Chapter Constant Map & Aliases
        val aliasesObj = root.optJSONObject("chapter_map_aliases") ?: SimpleJsonObject()
        val chapterAliases = mutableMapOf<String, String>()
        val aliasKeys = aliasesObj.keys()
        while (aliasKeys.hasNext()) {
            val key = aliasKeys.next()
            val target = aliasesObj.optJSONObject(key)?.optString("points_to", "") ?: ""
            chapterAliases[key] = target
        }

        val chapterMapArr = root.optJSONArray("chapter_constant_map") ?: SimpleJsonArray()
        val invertedChapterMap = mutableMapOf<String, MutableList<ChapterRef>>()
        val rawChapterConstantIds = mutableListOf<Pair<ChapterRef, List<String>>>()

        for (i in 0 until chapterMapArr.length()) {
            val chObj = chapterMapArr.optJSONObject(i) ?: continue
            val subj = chObj.optString("subject", "Physics")
            val paperStr = chObj.optString("paper", "1st")
            val chNum = chObj.optInt("chapter", 1)
            val nameEn = chObj.optString("name", "")
            val notes = chObj.optString("notes", "")
            val hscPaper = HscPaper.fromSubjectAndPaper(subj, paperStr)
            val sylMatch = chapterByPaperAndNum[Pair(hscPaper, chNum)]
            val chapterRef = ChapterRef(
                paper = hscPaper,
                chapterNumber = chNum,
                nameEn = sylMatch?.nameEn?.ifBlank { nameEn } ?: nameEn,
                nameBn = sylMatch?.nameBn ?: "",
                notes = notes
            )
            val cids = jsonArrayToStringList(chObj.optJSONArray("constant_ids"))
            rawChapterConstantIds.add(Pair(chapterRef, cids))
            for (cid in cids) {
                invertedChapterMap.getOrPut(cid) { mutableListOf() }.add(chapterRef)
                if (cid == "mathematical_constants") {
                    for (mathId in listOf("pi", "euler_number", "natural_log_2", "natural_log_10")) {
                        invertedChapterMap.getOrPut(mathId) { mutableListOf() }.add(chapterRef)
                    }
                }
            }
        }

        val generalPhysicsChapter = ChapterRef(
            paper = HscPaper.GENERAL_PHYSICS,
            chapterNumber = 0,
            nameEn = "General physics",
            nameBn = "সাধারণ পদার্থবিজ্ঞান"
        )

        // Track all IDs across constants, math constants, reference values, and unit conversions
        val globalSeenIds = mutableSetOf<String>()

        // 5. Parse 61 Constants
        val constantsArr = root.optJSONArray("constants") ?: SimpleJsonArray()
        val rawConstants = mutableListOf<ConstantEntry>()
        for (i in 0 until constantsArr.length()) {
            val cObj = constantsArr.optJSONObject(i) ?: continue
            val parsedEntry = parseConstantObject(
                cObj = cObj,
                overrides = overrides,
                invertedChapterMap = invertedChapterMap,
                generalPhysicsChapter = generalPhysicsChapter,
                seenIds = globalSeenIds,
                shouldFailLoudly = shouldFailLoudly
            )
            if (parsedEntry != null) {
                rawConstants.add(parsedEntry)
            }
        }

        // 6. Parse 4 Mathematical Constants
        val mathArr = root.optJSONArray("mathematical_constants_used_in_physics_and_chemistry") ?: SimpleJsonArray()
        val rawMathConstants = mutableListOf<MathConstantEntry>()
        for (i in 0 until mathArr.length()) {
            val mObj = mathArr.optJSONObject(i) ?: continue
            val id = mObj.optString("id", "").trim()
            val name = mObj.optString("name", "").trim()
            val symbolRaw = mObj.optString("symbol", "").trim()
            val valNum = mObj.optDouble("value", Double.NaN)

            val err = when {
                id.isBlank() -> "Math constant has blank id"
                name.isBlank() -> "Math constant '$id' has blank name"
                symbolRaw.isBlank() -> "Math constant '$id' has blank symbol"
                valNum.isNaN() || valNum.isInfinite() -> "Math constant '$id' has non-numeric value"
                !globalSeenIds.add(id) -> "Duplicate id '$id' in mathematical_constants"
                else -> null
            }
            if (err != null) {
                if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: $err")
                else continue
            }

            val approxArr = mObj.optJSONArray("common_HSC_approximations") ?: SimpleJsonArray()
            val approxStrings = mutableListOf<String>()
            for (j in 0 until approxArr.length()) {
                val item = approxArr.opt(j)
                if (item != null) {
                    approxStrings.add(item.toString())
                }
            }
            val chapters = invertedChapterMap[id]?.distinctBy { it.key } ?: listOf(generalPhysicsChapter)
            val override = overrides[id]
            rawMathConstants.add(
                MathConstantEntry(
                    id = id,
                    name = name,
                    symbol = StructuredSymbol.fromRawSymbol(symbolRaw, override?.displaySymbol),
                    value = valNum,
                    unit = StructuredUnit.fromRawUnit(mObj.optString("unit", "dimensionless")),
                    commonApproximations = approxStrings,
                    relevance = jsonArrayToStringList(mObj.optJSONArray("relevance")),
                    chapters = chapters
                )
            )
        }

        // 7. Parse 12 Condition-Dependent Reference Values
        val refArr = root.optJSONArray("condition_dependent_reference_values") ?: SimpleJsonArray()
        val rawReferenceValues = mutableListOf<ConstantEntry>()
        for (i in 0 until refArr.length()) {
            val rObj = refArr.optJSONObject(i) ?: continue
            val parsedRef = parseReferenceValueObject(
                rObj = rObj,
                overrides = overrides,
                chapterByPaperAndNameEn = chapterByPaperAndNameEn,
                generalPhysicsChapter = generalPhysicsChapter,
                seenIds = globalSeenIds,
                shouldFailLoudly = shouldFailLoudly
            )
            if (parsedRef != null) {
                rawReferenceValues.add(parsedRef)
            }
        }

        // 8. Compute automatic confusable IDs across constants, reference values, and math constants
        val allSymbolItems = mutableListOf<Triple<String, String, List<String>>>()
        rawConstants.forEach { c ->
            allSymbolItems.add(Triple(c.id, c.symbol.fullSymbol, overrides[c.id]?.extraConfusableIds.orEmpty()))
        }
        rawReferenceValues.forEach { r ->
            allSymbolItems.add(Triple(r.id, r.symbol.fullSymbol, overrides[r.id]?.extraConfusableIds.orEmpty()))
        }
        rawMathConstants.forEach { m ->
            allSymbolItems.add(Triple(m.id, m.symbol.fullSymbol, overrides[m.id]?.extraConfusableIds.orEmpty()))
        }

        val confusableMap = buildAutomaticConfusablesMap(allSymbolItems)

        val finalConstants = rawConstants.map { c ->
            c.copy(confusableIds = confusableMap[c.id].orEmpty())
        }
        val finalReferenceValues = rawReferenceValues.map { r ->
            r.copy(confusableIds = confusableMap[r.id].orEmpty())
        }
        val finalMathConstants = rawMathConstants.map { m ->
            m.copy(confusableIds = confusableMap[m.id].orEmpty())
        }

        // 9. Parse 39 Atomic Masses
        val atomicArr = root.optJSONArray("common_hsc_atomic_masses") ?: SimpleJsonArray()
        val atomicMasses = mutableListOf<AtomicMassEntry>()
        val seenElementSymbols = mutableSetOf<String>()
        for (i in 0 until atomicArr.length()) {
            val aObj = atomicArr.optJSONObject(i) ?: continue
            val sym = aObj.optString("symbol", "").trim()
            val elem = aObj.optString("element", "").trim()
            val relMass = aObj.optDouble("relative_atomic_mass_school", Double.NaN)
            val molMass = aObj.optDouble("molar_mass_school_g_mol", relMass)
            val err = when {
                sym.isBlank() -> "Atomic mass entry has blank symbol"
                elem.isBlank() -> "Atomic mass '$sym' has blank element name"
                relMass.isNaN() || relMass.isInfinite() -> "Atomic mass '$sym' has non-numeric school mass"
                !seenElementSymbols.add(sym) -> "Duplicate atomic mass symbol '$sym'"
                else -> null
            }
            if (err != null) {
                if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: $err")
                else continue
            }
            atomicMasses.add(
                AtomicMassEntry(
                    symbol = sym,
                    element = elem,
                    relativeAtomicMassSchool = relMass,
                    molarMassSchoolGMol = if (molMass.isNaN()) relMass else molMass,
                    modernCiaawValue = aObj.optString("modern_CIAAW_value", ""),
                    notes = aObj.optString("notes", "").takeIf { it.isNotBlank() },
                    classification = aObj.optString("classification", ""),
                    source = aObj.optString("source", "")
                )
            )
        }

        // 10. Parse 14 Unit Conversions
        val convArr = root.optJSONArray("unit_conversions") ?: SimpleJsonArray()
        val unitConversions = mutableListOf<UnitConversionEntry>()
        for (i in 0 until convArr.length()) {
            val uObj = convArr.optJSONObject(i) ?: continue
            val id = uObj.optString("id", "").trim()
            val quantity = uObj.optString("quantity", "").trim()
            val convText = uObj.optString("conversion", "").trim()
            if (id.isBlank() || !globalSeenIds.add(id)) {
                if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: invalid/duplicate conversion id '$id'")
                else continue
            }
            val valsArr = uObj.optJSONArray("values") ?: SimpleJsonArray()
            val valItems = mutableListOf<ConversionValueItem>()
            for (j in 0 until valsArr.length()) {
                val vObj = valsArr.optJSONObject(j) ?: continue
                if (vObj.has("formula")) {
                    valItems.add(ConversionValueItem(formula = vObj.optString("formula", "")))
                } else {
                    val fromStr = vObj.optString("from", "")
                    val toStr = vObj.optString("to", "")
                    val factor = vObj.optDouble("factor", Double.NaN)
                    if (fromStr.isNotBlank() && toStr.isNotBlank() && !factor.isNaN()) {
                        valItems.add(
                            ConversionValueItem(
                                fromUnit = StructuredUnit.fromRawUnit(fromStr),
                                toUnit = StructuredUnit.fromRawUnit(toStr),
                                factor = factor
                            )
                        )
                    }
                }
            }
            unitConversions.add(
                UnitConversionEntry(
                    id = id,
                    quantity = quantity,
                    conversionSummary = convText,
                    values = valItems
                )
            )
        }

        // 11. Parse 14 Derived Formulas
        val derivedArr = root.optJSONArray("derived_formula_reference") ?: SimpleJsonArray()
        val derivedFormulas = mutableListOf<DerivedFormulaEntry>()
        val seenDerivedIds = mutableSetOf<String>()
        for (i in 0 until derivedArr.length()) {
            val dObj = derivedArr.optJSONObject(i) ?: continue
            val id = dObj.optString("id", "").trim()
            if (id.isBlank() || !seenDerivedIds.add(id)) {
                if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: duplicate derived formula '$id'")
                else continue
            }
            derivedFormulas.add(
                DerivedFormulaEntry(
                    id = id,
                    formula = dObj.optString("formula", ""),
                    description = dObj.optString("description", ""),
                    chapters = jsonArrayToStringList(dObj.optJSONArray("chapters"))
                )
            )
        }

        // 12. Parse 8 Not-Fixed Tables
        val notFixedArr = root.optJSONArray("not_fixed_constants_or_condition_dependent_tables") ?: SimpleJsonArray()
        val notFixedTables = mutableListOf<NotFixedTableEntry>()
        for (i in 0 until notFixedArr.length()) {
            val nObj = notFixedArr.optJSONObject(i) ?: continue
            notFixedTables.add(
                NotFixedTableEntry(
                    id = nObj.optString("id", ""),
                    items = jsonArrayToStringList(nObj.optJSONArray("items")),
                    whyNotOneFixedValue = nObj.optString("why_not_one_fixed_value", ""),
                    formulaNotes = jsonArrayToStringList(nObj.optJSONArray("formula_notes")),
                    chapters = jsonArrayToStringList(nObj.optJSONArray("chapters"))
                )
            )
        }

        return ConstantsDataset(
            metadata = metadata,
            sources = sources,
            syllabusChapters = syllabusChapters,
            constants = finalConstants,
            mathConstants = finalMathConstants,
            referenceValues = finalReferenceValues,
            atomicMasses = atomicMasses,
            unitConversions = unitConversions,
            derivedFormulas = derivedFormulas,
            notFixedTables = notFixedTables,
            chapterAliases = chapterAliases,
            rawChapterConstantIds = rawChapterConstantIds,
            overrides = overrides
        )
    }

    /**
     * Validates a list of [ConstantEntry] instances.
     * Note: Zero-valued entries (`standard_hydrogen_electrode_potential = 0 V` and
     * `water_absolute_zero = 0 K`) are valid in the real HSC dataset! Only `NaN` or `Infinite`
     * values are rejected as non-numeric.
     */
    fun validateAndLoad(
        entries: List<ConstantEntry> = ConstantsData.ENTRIES,
        isDebug: Boolean = BuildConfig.DEBUG,
        failLoudly: Boolean? = null
    ): List<ConstantEntry> {
        val shouldFailLoudly = failLoudly ?: isDebug
        val seenIds = mutableSetOf<String>()
        val validEntries = mutableListOf<ConstantEntry>()

        for (entry in entries) {
            val errorReason = when {
                entry.id.isBlank() -> "Constant entry has blank id"
                entry.name.isBlank() -> "Constant '${entry.id}' has blank descriptive name"
                entry.symbol.base.isBlank() -> "Constant '${entry.id}' has blank symbol base"
                entry.mantissa.isNaN() || entry.mantissa.isInfinite() ->
                    "Constant '${entry.id}' has non-numeric mantissa: ${entry.mantissa}"
                entry.preciseValue.isNaN() || entry.preciseValue.isInfinite() ->
                    "Constant '${entry.id}' has non-numeric value: ${entry.preciseValue}"
                !seenIds.add(entry.id) -> "Duplicate constant id: '${entry.id}'"
                else -> null
            }

            if (errorReason != null) {
                if (shouldFailLoudly) {
                    throw IllegalStateException("ConstantsLoader validation failed: $errorReason")
                } else {
                    continue
                }
            }

            validEntries.add(entry)
        }

        return validEntries
    }

    fun subjects(entries: List<ConstantEntry> = validateAndLoad(failLoudly = false)): List<String> {
        return entries.filter { it.enabled }
            .flatMap { e ->
                if (e.chapters.isEmpty()) listOf(HscPaper.GENERAL_PHYSICS.displayName)
                else e.chapters.map { it.paper.displayName }
            }
            .distinct()
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
        entries: List<ConstantEntry> = getDataset(failLoudly = false).allConstantAndReferenceEntries,
        isDebug: Boolean = BuildConfig.DEBUG
    ): ConstantEntry? {
        return validateAndLoad(entries, isDebug).find { it.id == id }
    }

    // =========================================================================
    // Internal Parsing & Normalization Helpers
    // =========================================================================

    private val NON_SI_FIRST_PRIMARY_IDS = setOf(
        "fine_structure_constant",
        "molar_volume_stp_100kpa",
        "molar_volume_stp_1atm",
        "water_ion_product_25c",
        "standard_hydrogen_electrode_potential",
        "refractive_index_vacuum"
    )

    private fun parseConstantObject(
        cObj: SimpleJsonObject,
        overrides: Map<String, ConstantOverride>,
        invertedChapterMap: Map<String, List<ChapterRef>>,
        generalPhysicsChapter: ChapterRef,
        seenIds: MutableSet<String>,
        shouldFailLoudly: Boolean
    ): ConstantEntry? {
        val id = cObj.optString("id", "").trim()
        val name = cObj.optString("name", "").trim()
        val rawSymbol = cObj.optString("symbol", "").trim()
        val valuesArr = cObj.optJSONArray("values")

        val baseError = when {
            id.isBlank() -> "Constant entry has blank id"
            name.isBlank() -> "Constant '$id' has blank name"
            rawSymbol.isBlank() -> "Constant '$id' has blank symbol"
            valuesArr == null || valuesArr.length() == 0 -> "Constant '$id' has empty values array"
            !seenIds.add(id) -> "Duplicate constant id '$id'"
            else -> null
        }
        if (baseError != null) {
            if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: $baseError")
            else return null
        }

        val override = overrides[id]
        val structuredSymbol = StructuredSymbol.fromRawSymbol(rawSymbol, override?.displaySymbol)
        val shortSym = structuredSymbol.shortSymbol

        data class RawValItem(
            val value: Double,
            val unitStr: String,
            val system: String,
            val formatted: String
        )

        val rawVariants = mutableListOf<RawValItem>()
        for (j in 0 until valuesArr!!.length()) {
            val vObj = valuesArr.optJSONObject(j) ?: continue
            if (!vObj.has("value") || vObj.isNull("value")) {
                if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: Constant '$id' variant missing numeric value")
                else continue
            }
            val vNum = vObj.optDouble("value", Double.NaN)
            if (vNum.isNaN() || vNum.isInfinite()) {
                if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: Constant '$id' has non-numeric value")
                else continue
            }
            rawVariants.add(
                RawValItem(
                    value = vNum,
                    unitStr = vObj.optString("unit", "dimensionless").trim(),
                    system = vObj.optString("system", "SI").trim(),
                    formatted = vObj.optString("formatted", "").trim()
                )
            )
        }

        if (rawVariants.isEmpty()) {
            if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: Constant '$id' has no valid variants")
            else return null
        }

        // Section 3 Primary Variant Rule:
        // "the first values entry whose system starts with SI, otherwise the first entry.
        // Six entries have a non-SI first entry (fine structure constant, two molar volumes,
        // water ion product, standard hydrogen electrode, vacuum refractive index); treat their first entry as primary."
        val primaryIdx = if (id in NON_SI_FIRST_PRIMARY_IDS) {
            0
        } else {
            val siIdx = rawVariants.indexOfFirst { it.system.startsWith("SI", ignoreCase = true) }
            if (siIdx >= 0) siIdx else 0
        }
        val primaryRaw = rawVariants[primaryIdx]
        val preciseVal = primaryRaw.value
        val (preciseMantissa, preciseExponent) = decomposeToMantissaAndExponent(preciseVal)

        val approxRaw = if (cObj.isNull("common_hsc_approximation")) null
        else cObj.optString("common_hsc_approximation", "").takeIf { it.isNotBlank() }

        // Course value precedence (Section 3):
        // 1. constants_overrides.json
        // 2. Numeric approximation extracted from common_hsc_approximation (matching primary scale)
        // 3. Fallback: precise value rounded to 4 significant figures
        val (courseMant, courseExp, courseVal) = when {
            override?.courseMantissa != null -> {
                val cm = override.courseMantissa
                val ce = override.courseExponent ?: 0
                Triple(cm, ce, cm * 10.0.pow(ce.toDouble()))
            }
            else -> {
                val extracted = extractCourseApproximationForValue(approxRaw, preciseVal)
                val targetVal = extracted ?: roundToSignificantFigures(preciseVal, 4)
                val (m, e) = decomposeToMantissaAndExponent(targetVal)
                Triple(m, e, targetVal)
            }
        }

        // Alternate variants (Section 3):
        // Skip trivial self-definitions where value == 1 and unit == symbol (1 u, 1 eV, 1 au)
        // and skip the reciprocal 1/α entry on fine_structure_constant.
        val alternateVariants = mutableListOf<AlternateConstantValue>()
        rawVariants.forEachIndexed { idx, rv ->
            if (idx == primaryIdx) return@forEachIndexed
            if (isSkippedTrivialAlternate(shortSym, rv.value, rv.unitStr)) return@forEachIndexed
            // Also skip duplicate unit+system school approximation on vacuum_permeability if identical unit to primary
            if (id == "vacuum_permeability" && rv.unitStr == primaryRaw.unitStr) return@forEachIndexed

            val (altM, altE) = decomposeToMantissaAndExponent(rv.value)
            val altCourseVal = extractCourseApproximationForValue(approxRaw, rv.value)
                ?: roundToSignificantFigures(rv.value, 4)
            alternateVariants.add(
                AlternateConstantValue(
                    mantissa = altM,
                    exponent = altE,
                    unit = StructuredUnit.fromRawUnit(rv.unitStr),
                    label = rv.system,
                    preciseValue = rv.value,
                    courseValue = altCourseVal,
                    system = rv.system,
                    formatted = rv.formatted.ifEmpty { null }
                )
            )
        }

        val category = cObj.optString("category", "mechanics")
        val constantType = cObj.optString("constant_type", "defined_exact")
        val definition = cObj.optString("definition_or_meaning", "").takeIf { it.isNotBlank() }
        val formulaUses = jsonArrayToStringList(cObj.optJSONArray("formula_uses"))
        val notesList = jsonArrayToStringList(cObj.optJSONArray("notes"))
        val notesJoined = notesList.joinToString(" ").takeIf { it.isNotBlank() }
        val chapters = invertedChapterMap[id]?.distinctBy { it.key } ?: listOf(generalPhysicsChapter)
        val mappedSubject = mapCategoryAndChaptersToLegacySubject(category, chapters)

        return ConstantEntry(
            id = id,
            name = name,
            symbol = structuredSymbol,
            subject = mappedSubject,
            mantissa = preciseMantissa,
            exponent = preciseExponent,
            unit = StructuredUnit.fromRawUnit(primaryRaw.unitStr),
            notes = notesJoined,
            alternateValue = alternateVariants.firstOrNull(),
            confusableIds = emptyList(), // populated in pass 2
            toleranceOverride = override?.tolerance,
            enabled = override?.enabled ?: true,
            isSample = false,
            preciseValue = preciseVal,
            courseMantissa = courseMant,
            courseExponent = courseExp,
            courseValue = courseVal,
            commonHscApproximationRaw = approxRaw,
            primarySystem = primaryRaw.system,
            primaryFormatted = primaryRaw.formatted.ifEmpty { null },
            allAlternateValues = alternateVariants,
            category = category,
            constantType = constantType,
            reliabilityClass = ReliabilityClass.fromConstantType(constantType),
            definitionOrMeaning = definition,
            formulaUses = formulaUses,
            notesList = notesList,
            chapters = chapters,
            sources = jsonArrayToStringList(cObj.optJSONArray("sources")),
            isReferenceValue = false
        )
    }

    private fun parseReferenceValueObject(
        rObj: SimpleJsonObject,
        overrides: Map<String, ConstantOverride>,
        chapterByPaperAndNameEn: Map<Pair<HscPaper, String>, ChapterRef>,
        generalPhysicsChapter: ChapterRef,
        seenIds: MutableSet<String>,
        shouldFailLoudly: Boolean
    ): ConstantEntry? {
        val id = rObj.optString("id", "").trim()
        val name = rObj.optString("name", "").trim()
        val rawSymbol = rObj.optString("symbol", "").trim()
        if (!rObj.has("value") || rObj.isNull("value")) {
            if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: Reference value '$id' missing value")
            else return null
        }
        val preciseVal = rObj.optDouble("value", Double.NaN)
        val err = when {
            id.isBlank() -> "Reference value has blank id"
            name.isBlank() -> "Reference value '$id' has blank name"
            rawSymbol.isBlank() -> "Reference value '$id' has blank symbol"
            preciseVal.isNaN() || preciseVal.isInfinite() -> "Reference value '$id' has non-numeric value"
            !seenIds.add(id) -> "Duplicate id '$id' in condition_dependent_reference_values"
            else -> null
        }
        if (err != null) {
            if (shouldFailLoudly) throw IllegalStateException("ConstantsLoader validation failed: $err")
            else return null
        }

        val override = overrides[id]
        val structuredSymbol = StructuredSymbol.fromRawSymbol(rawSymbol, override?.displaySymbol)
        val unitStr = rObj.optString("unit", "dimensionless").trim()
        val schoolForm = rObj.optString("common_school_form", "").takeIf { it.isNotBlank() }
        val conditions = rObj.optString("conditions", "").takeIf { it.isNotBlank() }
        val noteStr = rObj.optString("notes", "").takeIf { it.isNotBlank() }

        val (preciseMantissa, preciseExponent) = decomposeToMantissaAndExponent(preciseVal)
        val (courseMant, courseExp, courseVal) = when {
            override?.courseMantissa != null -> {
                val cm = override.courseMantissa
                val ce = override.courseExponent ?: 0
                Triple(cm, ce, cm * 10.0.pow(ce.toDouble()))
            }
            else -> {
                val extracted = extractCourseApproximationForValue(schoolForm, preciseVal)
                val targetVal = extracted ?: roundToSignificantFigures(preciseVal, 4)
                val (m, e) = decomposeToMantissaAndExponent(targetVal)
                Triple(m, e, targetVal)
            }
        }

        val chapterStrings = jsonArrayToStringList(rObj.optJSONArray("chapters"))
        val resolvedChapters = resolveChapterStrings(chapterStrings, chapterByPaperAndNameEn)
            .ifEmpty { listOf(generalPhysicsChapter) }

        val notesList = listOfNotNull(conditions?.let { "Conditions: $it" }, noteStr)
        val mappedSubject = mapCategoryAndChaptersToLegacySubject("condition_dependent_reference", resolvedChapters)

        return ConstantEntry(
            id = id,
            name = name,
            symbol = structuredSymbol,
            subject = mappedSubject,
            mantissa = preciseMantissa,
            exponent = preciseExponent,
            unit = StructuredUnit.fromRawUnit(unitStr),
            notes = notesList.joinToString(" ").takeIf { it.isNotBlank() },
            alternateValue = null,
            confusableIds = emptyList(),
            toleranceOverride = override?.tolerance,
            enabled = override?.enabled ?: true,
            isSample = false,
            preciseValue = preciseVal,
            courseMantissa = courseMant,
            courseExponent = courseExp,
            courseValue = courseVal,
            commonHscApproximationRaw = schoolForm,
            primarySystem = "SI",
            primaryFormatted = schoolForm,
            allAlternateValues = emptyList(),
            category = "condition_dependent_reference",
            constantType = "condition_dependent_reference",
            reliabilityClass = ReliabilityClass.REFERENCE_AND_APPROXIMATE,
            definitionOrMeaning = conditions,
            formulaUses = chapterStrings,
            notesList = notesList,
            chapters = resolvedChapters,
            sources = emptyList(),
            isReferenceValue = true
        )
    }

    /**
     * Checks whether an alternate variant should be skipped per Section 3:
     * - Trivial self-definitions where `value == 1` and `unit == symbol` (`1 u`, `1 eV`, `1 au`)
     * - Reciprocal `1/α` entry on `fine_structure_constant`
     */
    internal fun isSkippedTrivialAlternate(shortSymbol: String, value: Double, unitStr: String): Boolean {
        val u = unitStr.trim()
        if (u == "1/α" || u.equals("1/alpha", ignoreCase = true)) return true
        if (abs(value - 1.0) <= 1e-12 && u.equals(shortSymbol.trim(), ignoreCase = true)) return true
        return false
    }

    /**
     * Extracts a numeric course approximation from `common_hsc_approximation` (or `common_school_form`)
     * if and only if it represents the same unit/scale (within 2% relative error, or `< 0.01` for zero).
     */
    internal fun extractCourseApproximationForValue(approxStr: String?, preciseVal: Double): Double? {
        if (approxStr.isNullOrBlank()) return null
        if (approxStr.contains("4π") || approxStr.contains("4\\pi")) return null
        if (approxStr.contains("1/137")) {
            val v = 1.0 / 137.0
            if (preciseVal != 0.0 && abs(v - preciseVal) / abs(preciseVal) <= 0.01) return v
        }

        val sciRegex = Regex("""([+-]?\d+(?:\.\d+)?)\s*[×x*]\s*10\^([+-−]?\d+)""")
        val numRegex = Regex("""([+-]?\d+(?:\.\d+)?)""")

        for (clause in approxStr.split(';')) {
            var cleaned = clause
            if (cleaned.contains('=')) cleaned = cleaned.substringAfterLast('=')
            if (cleaned.contains('≈')) cleaned = cleaned.substringAfterLast('≈')

            val sciMatch = sciRegex.find(cleaned)
            if (sciMatch != null) {
                val mant = sciMatch.groupValues[1].toDoubleOrNull()
                val exp = sciMatch.groupValues[2].replace("−", "-").toIntOrNull()
                if (mant != null && exp != null) {
                    val cand = mant * 10.0.pow(exp.toDouble())
                    if (isAcceptableCourseApproximation(cand, preciseVal)) return cand
                }
            }

            for (numMatch in numRegex.findAll(cleaned)) {
                val cand = numMatch.groupValues[1].toDoubleOrNull() ?: continue
                if (isAcceptableCourseApproximation(cand, preciseVal)) return cand
            }
        }
        return null
    }

    private fun isAcceptableCourseApproximation(candidate: Double, preciseVal: Double): Boolean {
        if (candidate.isNaN() || candidate.isInfinite()) return false
        if (abs(preciseVal) <= 1e-15) {
            return abs(candidate) <= 0.01
        }
        val relDiff = abs(candidate - preciseVal) / abs(preciseVal)
        return relDiff <= 0.025 // within 2.5% (all 61 constants are within 0.88%, reference values within 2.1%)
    }

    internal fun roundToSignificantFigures(value: Double, sigFigs: Int = 4): Double {
        if (value == 0.0 || value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value)
            .round(MathContext(sigFigs, RoundingMode.HALF_UP))
            .toDouble()
    }

    internal fun decomposeToMantissaAndExponent(value: Double): Pair<Double, Int> {
        if (value == 0.0 || value.isNaN() || value.isInfinite()) return Pair(0.0, 0)
        val absVal = abs(value)
        // Keep plain numbers in [0.001, 100000) with exponent 0 if they don't need scientific notation,
        // or normalize into scientific [1, 10) × 10^exp for very large / very small numbers.
        if (absVal >= 1e-3 && absVal < 1e6) {
            return Pair(value, 0)
        }
        val exp = floor(log10(absVal)).toInt()
        val mant = value / 10.0.pow(exp.toDouble())
        val roundedMant = BigDecimal.valueOf(mant)
            .round(MathContext(10, RoundingMode.HALF_UP))
            .toDouble()
        return if (abs(roundedMant) >= 10.0) {
            Pair(roundedMant / 10.0, exp + 1)
        } else {
            Pair(roundedMant, exp)
        }
    }

    private fun parseSyllabusSubject(
        subjObj: SimpleJsonObject?,
        subjectName: String,
        out: MutableList<ChapterRef>
    ) {
        if (subjObj == null) return
        val firstArr = subjObj.optJSONArray("first_paper") ?: SimpleJsonArray()
        val secondArr = subjObj.optJSONArray("second_paper") ?: SimpleJsonArray()
        val paper1 = HscPaper.fromSubjectAndPaper(subjectName, "1st")
        val paper2 = HscPaper.fromSubjectAndPaper(subjectName, "2nd")

        for (i in 0 until firstArr.length()) {
            val ch = firstArr.optJSONObject(i) ?: continue
            out.add(
                ChapterRef(
                    paper = paper1,
                    chapterNumber = ch.optInt("chapter", i + 1),
                    nameEn = ch.optString("name_en", ""),
                    nameBn = ch.optString("name_bn", "")
                )
            )
        }
        for (i in 0 until secondArr.length()) {
            val ch = secondArr.optJSONObject(i) ?: continue
            out.add(
                ChapterRef(
                    paper = paper2,
                    chapterNumber = ch.optInt("chapter", i + 1),
                    nameEn = ch.optString("name_en", ""),
                    nameBn = ch.optString("name_bn", "")
                )
            )
        }
    }

    private fun resolveChapterStrings(
        rawStrings: List<String>,
        chapterByPaperAndNameEn: Map<Pair<HscPaper, String>, ChapterRef>
    ): List<ChapterRef> {
        val result = mutableListOf<ChapterRef>()
        for (raw in rawStrings) {
            val parts = raw.split(':')
            if (parts.size < 2) continue
            val paperPrefix = parts[0].trim()
            val hscPaper = when {
                paperPrefix.contains("Physics 1st", ignoreCase = true) -> HscPaper.PHYSICS_1ST
                paperPrefix.contains("Physics 2nd", ignoreCase = true) -> HscPaper.PHYSICS_2ND
                paperPrefix.contains("Chemistry 1st", ignoreCase = true) -> HscPaper.CHEMISTRY_1ST
                paperPrefix.contains("Chemistry 2nd", ignoreCase = true) -> HscPaper.CHEMISTRY_2ND
                else -> HscPaper.GENERAL_PHYSICS
            }
            val names = parts[1].split(';')
            for (n in names) {
                val cleanName = n.trim()
                val exactMatch = chapterByPaperAndNameEn[Pair(hscPaper, cleanName.lowercase())]
                if (exactMatch != null) {
                    result.add(exactMatch)
                } else {
                    val partialMatch = chapterByPaperAndNameEn.entries.firstOrNull { (k, _) ->
                        k.first == hscPaper && (k.second.contains(cleanName.lowercase()) || cleanName.lowercase().contains(k.second))
                    }?.value
                    if (partialMatch != null) {
                        result.add(partialMatch)
                    } else if (cleanName.isNotEmpty()) {
                        result.add(ChapterRef(paper = hscPaper, chapterNumber = 0, nameEn = cleanName))
                    }
                }
            }
        }
        return result.distinctBy { "${it.paper.id}_${it.nameEn}" }
    }

    private fun mapCategoryAndChaptersToLegacySubject(
        category: String,
        chapters: List<ChapterRef>
    ): ConstantSubject {
        val cat = category.lowercase()
        val firstCh = chapters.firstOrNull()
        if (firstCh != null && (firstCh.paper == HscPaper.CHEMISTRY_1ST || firstCh.paper == HscPaper.CHEMISTRY_2ND)) {
            return ConstantSubject.CHEMISTRY
        }
        return when {
            cat.contains("chemistry") || cat.contains("electrochemistry") || cat.contains("gas") -> ConstantSubject.CHEMISTRY
            cat.contains("thermodynamics") || cat.contains("thermal") -> ConstantSubject.THERMODYNAMICS
            cat.contains("electro") -> ConstantSubject.ELECTROSTATICS
            cat.contains("magnet") -> ConstantSubject.MAGNETISM
            cat.contains("optic") || cat.contains("wave") -> ConstantSubject.WAVES_AND_OPTICS
            cat.contains("quantum") || cat.contains("modern") || cat.contains("atomic") || cat.contains("nuclear") -> ConstantSubject.MODERN_PHYSICS
            else -> ConstantSubject.MECHANICS
        }
    }

    private fun buildAutomaticConfusablesMap(
        items: List<Triple<String, String, List<String>>>
    ): Map<String, List<String>> {
        val byRoot = mutableMapOf<String, MutableList<String>>()
        for ((id, fullSymbol, _) in items) {
            val rootKey = confusableRootKey(fullSymbol)
            byRoot.getOrPut(rootKey) { mutableListOf() }.add(id)
        }

        val result = mutableMapOf<String, List<String>>()
        for ((id, fullSymbol, extraIds) in items) {
            val rootKey = confusableRootKey(fullSymbol)
            val siblings = byRoot[rootKey].orEmpty().filter { it != id }
            val merged = (siblings + extraIds.filter { it != id }).distinct()
            result[id] = merged
        }
        return result
    }

    internal fun confusableRootKey(fullSymbol: String): String {
        val short = StructuredSymbol.extractShortSymbol(fullSymbol)
        if (short == "2.303RT/F" || short == "RT/F") return "RT/F"
        if (short == "e/m_e") return "e"
        if (short == "eV") return "eV"
        if (short in setOf("au", "ly", "pc", "atm", "0 K")) return short
        if (short.startsWith("ln ")) return "ln"
        if (short.endsWith(" c²")) return "mc2"

        val rootBuilder = StringBuilder()
        for (ch in short) {
            if (ch.isLetter() || ch in "αβγδεζηθικλμνξοπρστυφχψωħ") {
                rootBuilder.append(ch)
            } else {
                break
            }
        }
        val root = rootBuilder.toString().ifEmpty { short }
        return when (root) {
            "G", "g" -> "g_G"
            "h", "ħ" -> "h_hbar"
            "R", "r" -> "R_r"
            else -> root
        }
    }

    private fun parseOverridesJson(jsonStr: String?): Map<String, ConstantOverride> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        val root = try {
            SimpleJsonObject.parse(jsonStr)
        } catch (_: Throwable) {
            return emptyMap()
        }
        val overridesObj = root.optJSONObject("overrides") ?: root
        val result = mutableMapOf<String, ConstantOverride>()
        val keys = overridesObj.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            if (id.startsWith("_")) continue
            val itemObj = overridesObj.optJSONObject(id) ?: continue
            val courseMant = if (itemObj.has("courseMantissa") && !itemObj.isNull("courseMantissa")) {
                itemObj.optDouble("courseMantissa")
            } else null
            val courseExp = if (itemObj.has("courseExponent") && !itemObj.isNull("courseExponent")) {
                itemObj.optInt("courseExponent", 0)
            } else null
            val tol = if (itemObj.has("tolerance") && !itemObj.isNull("tolerance")) {
                itemObj.optDouble("tolerance")
            } else null
            val enabled = if (itemObj.has("enabled") && !itemObj.isNull("enabled")) {
                itemObj.optBoolean("enabled", true)
            } else null
            val dispSym = if (itemObj.has("displaySymbol") && !itemObj.isNull("displaySymbol")) {
                itemObj.optString("displaySymbol").takeIf { it.isNotBlank() }
            } else null
            val extraConf = jsonArrayToStringList(itemObj.optJSONArray("extraConfusableIds"))

            result[id] = ConstantOverride(
                id = id,
                courseMantissa = courseMant,
                courseExponent = courseExp,
                tolerance = tol,
                enabled = enabled,
                displaySymbol = dispSym,
                extraConfusableIds = extraConf
            )
        }
        return result
    }

    private fun jsonArrayToStringList(arr: SimpleJsonArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "").trim()
            if (s.isNotEmpty()) list.add(s)
        }
        return list
    }

    private fun readAssetText(assetName: String, context: Context?): String {
        return readAssetTextOrNull(assetName, context)
            ?: throw IllegalStateException("Required asset '$assetName' could not be found in assets, classpath, or project directory.")
    }

    private fun readAssetTextOrNull(assetName: String, context: Context?): String? {
        // 1. Try Android Context assets if provided
        if (context != null) {
            try {
                return context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Throwable) {
                // Fall through
            }
        }
        // 2. Try JVM ClassLoader resource (works in unit tests with assets.srcDirs configured)
        val classLoader = ConstantsLoader::class.java.classLoader
        if (classLoader != null) {
            for (path in listOf(assetName, "assets/$assetName")) {
                val stream = classLoader.getResourceAsStream(path)
                if (stream != null) {
                    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            }
        }
        // 3. Try local file system paths (for pure JVM test runners regardless of working directory)
        val candidateFiles = listOf(
            File("src/main/assets/$assetName"),
            File("app/src/main/assets/$assetName"),
            File("../app/src/main/assets/$assetName"),
            File("docs/.agents/$assetName")
        )
        for (f in candidateFiles) {
            if (f.exists() && f.isFile) {
                return f.readText(Charsets.UTF_8)
            }
        }
        return null
    }
}
