package com.example.data

import android.content.Context

/*
 * ============================================================================
 * HSC PHYSICS & CHEMISTRY CONSTANTS FACADE
 * ============================================================================
 * All constants, mathematical constants, condition-dependent reference values,
 * atomic masses, unit conversions, derived formulas, and syllabus mappings are
 * loaded at runtime from `app/src/main/assets/hsc_physics_chemistry_constants.json`
 * with optional per-ID overrides from `app/src/main/assets/constants_overrides.json`.
 *
 * The 3 starter sample entries from v1.1.0 have been removed per Section 1.
 * ============================================================================
 */
object ConstantsData {

    fun preload(context: Context? = null) {
        ConstantsLoader.preloadAsync(context)
    }

    val dataset: ConstantsDataset
        get() = ConstantsLoader.getDataset(failLoudly = false)

    val entries: List<ConstantEntry>
        get() = dataset.constants

    val ENTRIES: List<ConstantEntry>
        get() = dataset.constants

    val referenceValues: List<ConstantEntry>
        get() = dataset.referenceValues

    val mathConstants: List<MathConstantEntry>
        get() = dataset.mathConstants

    val atomicMasses: List<AtomicMassEntry>
        get() = dataset.atomicMasses

    val unitConversions: List<UnitConversionEntry>
        get() = dataset.unitConversions

    val derivedFormulas: List<DerivedFormulaEntry>
        get() = dataset.derivedFormulas

    val notFixedTables: List<NotFixedTableEntry>
        get() = dataset.notFixedTables

    val syllabusChapters: List<ChapterRef>
        get() = dataset.syllabusChapters
}
