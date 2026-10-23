package com.example.data

/**
 * Dedicated data file for strong acid and base species used in pH / pOH calculation drills.
 */
enum class SpeciesType(val label: String) {
    ACID("Acid"),
    BASE("Base")
}

data class ChemicalSpecies(
    val id: String,
    val formula: String,
    val name: String,
    val type: SpeciesType,
    val ionsReleased: Int, // number of H+ or OH- ions released per formula unit
    val molarMass: Double // g/mol
) {
    val valence: Int get() = ionsReleased
    val isAcid: Boolean get() = type == SpeciesType.ACID
}

object PhSpeciesData {

    val SPECIES_LIST: List<ChemicalSpecies> = listOf(
        ChemicalSpecies(
            id = "hcl",
            formula = "HCl",
            name = "Hydrochloric acid",
            type = SpeciesType.ACID,
            ionsReleased = 1,
            molarMass = 36.5
        ),
        ChemicalSpecies(
            id = "hno3",
            formula = "HNO₃",
            name = "Nitric acid",
            type = SpeciesType.ACID,
            ionsReleased = 1,
            molarMass = 63.0
        ),
        ChemicalSpecies(
            id = "hbr",
            formula = "HBr",
            name = "Hydrobromic acid",
            type = SpeciesType.ACID,
            ionsReleased = 1,
            molarMass = 81.0
        ),
        ChemicalSpecies(
            id = "h2so4",
            formula = "H₂SO₄",
            name = "Sulfuric acid",
            type = SpeciesType.ACID,
            ionsReleased = 2,
            molarMass = 98.0
        ),
        ChemicalSpecies(
            id = "naoh",
            formula = "NaOH",
            name = "Sodium hydroxide",
            type = SpeciesType.BASE,
            ionsReleased = 1,
            molarMass = 40.0
        ),
        ChemicalSpecies(
            id = "koh",
            formula = "KOH",
            name = "Potassium hydroxide",
            type = SpeciesType.BASE,
            ionsReleased = 1,
            molarMass = 56.1
        ),
        ChemicalSpecies(
            id = "ca_oh_2",
            formula = "Ca(OH)₂",
            name = "Calcium hydroxide",
            type = SpeciesType.BASE,
            ionsReleased = 2,
            molarMass = 74.1
        ),
        ChemicalSpecies(
            id = "ba_oh_2",
            formula = "Ba(OH)₂",
            name = "Barium hydroxide",
            type = SpeciesType.BASE,
            ionsReleased = 2,
            molarMass = 171.3
        )
    )

    val ACIDS: List<ChemicalSpecies>
        get() = SPECIES_LIST.filter { it.type == SpeciesType.ACID }

    val BASES: List<ChemicalSpecies>
        get() = SPECIES_LIST.filter { it.type == SpeciesType.BASE }

    val allSpecies: List<ChemicalSpecies>
        get() = SPECIES_LIST

    val acids: List<ChemicalSpecies>
        get() = ACIDS

    val bases: List<ChemicalSpecies>
        get() = BASES
}
