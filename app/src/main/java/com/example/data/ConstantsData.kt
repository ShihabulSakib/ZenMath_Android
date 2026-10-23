package com.example.data

/*
 * ============================================================================
 * HOW TO ADD NEW CONSTANTS TO THIS FILE
 * ============================================================================
 * 1. Append a new `ConstantEntry(...)` inside `ConstantsData.ENTRIES` below.
 * 2. Required fields:
 *    - `id`: Unique snake_case identifier (e.g., "planck_h"). Duplicate IDs fail debug builds.
 *    - `name`: Full descriptive name (e.g., "Coulomb's constant", never just a symbol).
 *    - `symbol`: `StructuredSymbol(base = "k", subscript = "e")` (rendered with rich spans).
 *    - `subject`: One of `ConstantSubject.MECHANICS`, `THERMODYNAMICS`, `ELECTROSTATICS`,
 *      `CURRENT_ELECTRICITY`, `MAGNETISM`, `WAVES_AND_OPTICS`, `MODERN_PHYSICS`, `CHEMISTRY`.
 *    - `mantissa` & `exponent`: Value in scientific notation `mantissa × 10^exponent`
 *      (for plain numbers like 9.8, use `mantissa = 9.8, exponent = 0`).
 *    - `unit`: `StructuredUnit(listOf(UnitFactor("N", 1), UnitFactor("m", 2), UnitFactor("C", -2)))`.
 * 3. Optional fields:
 *    - `notes`: Extra memory tips or exam context shown in the Revision tab.
 *    - `alternateValue`: `AlternateConstantValue(mantissa, exponent, unit, label)` for constants
 *      used in a second unit system (such as gas constant R in L·atm·mol⁻¹·K⁻¹).
 *    - `confusableIds`: List of other `ConstantEntry.id` strings commonly confused with this one.
 *    - `toleranceOverride`: Relative tolerance fraction (e.g., `0.005` for 0.5%) if different from preset.
 *    - `enabled`: Set `true` to include in drills and revision.
 *    - `isSample`: Set `false` for real production constants (only the 3 starter entries below are `true`).
 * ============================================================================
 */
object ConstantsData {

    val ENTRIES: List<ConstantEntry> = listOf(
        ConstantEntry(
            id = "sample_coulomb_k",
            name = "Coulomb's constant [SAMPLE]",
            symbol = StructuredSymbol(base = "k", subscript = "e"),
            subject = ConstantSubject.ELECTROSTATICS,
            mantissa = 9.0,
            exponent = 9,
            unit = StructuredUnit(
                listOf(
                    UnitFactor("N", 1),
                    UnitFactor("m", 2),
                    UnitFactor("C", -2)
                )
            ),
            notes = "Sample entry: 1 / (4πε₀) ≈ 9 × 10⁹ N·m²·C⁻². Do not confuse with spring constant k.",
            confusableIds = listOf("sample_spring_k_ref"),
            enabled = true,
            isSample = true
        ),
        ConstantEntry(
            id = "sample_gas_constant_r",
            name = "Molar gas constant [SAMPLE]",
            symbol = StructuredSymbol(base = "R"),
            subject = ConstantSubject.THERMODYNAMICS,
            mantissa = 8.314,
            exponent = 0,
            unit = StructuredUnit(
                listOf(
                    UnitFactor("J", 1),
                    UnitFactor("mol", -1),
                    UnitFactor("K", -1)
                )
            ),
            notes = "Sample entry: Use 8.314 J·mol⁻¹·K⁻¹ in SI energy problems, or 0.0821 L·atm·mol⁻¹·K⁻¹ in gas law (PV = nRT) problems.",
            alternateValue = AlternateConstantValue(
                mantissa = 8.21,
                exponent = -2,
                unit = StructuredUnit(
                    listOf(
                        UnitFactor("L", 1),
                        UnitFactor("atm", 1),
                        UnitFactor("mol", -1),
                        UnitFactor("K", -1)
                    )
                ),
                label = "0.0821 L·atm·mol⁻¹·K⁻¹"
            ),
            enabled = true,
            isSample = true
        ),
        ConstantEntry(
            id = "sample_spring_k_ref",
            name = "Standard gravity [SAMPLE]",
            symbol = StructuredSymbol(base = "g"),
            subject = ConstantSubject.MECHANICS,
            mantissa = 9.8,
            exponent = 0,
            unit = StructuredUnit(
                listOf(
                    UnitFactor("m", 1),
                    UnitFactor("s", -2)
                )
            ),
            notes = "Sample entry: Often approximated as 9.8 m·s⁻² (or 10 m·s⁻² in quick kinematics).",
            confusableIds = listOf("sample_coulomb_k"),
            enabled = true,
            isSample = true
        )
    )
}
