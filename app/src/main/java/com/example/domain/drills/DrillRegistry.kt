package com.example.domain.drills

import com.example.domain.GameMode

enum class DrillCategory(val id: String, val label: String, val description: String) {
    ARITHMETIC(
        id = "arithmetic",
        label = "Core Arithmetic",
        description = "Foundational mental addition, subtraction, multiplication, division, and mixed operations."
    ),
    NUMBER_FACTS(
        id = "number_facts",
        label = "Number Facts & Powers",
        description = "Instant recall of multiplication tables, factors, squares, square roots, cubes, cube roots, and integer powers."
    ),
    FRACTIONS_PERCENT(
        id = "fractions_percent",
        label = "Fractions, Percent & Ratios",
        description = "Bidirectional fraction-decimal conversions, percentage calculations, and ratio/proportion scaling."
    ),
    REASONING(
        id = "reasoning",
        label = "Estimation & Reasoning",
        description = "Rapid numerical estimation, pattern recognition in number sequences, and multi-step chained calculations."
    ),
    SCIENCE_MATH(
        id = "science_math",
        label = "Science & Exam Math",
        description = "Calculator-free chemistry and physics arithmetic: memorized logs/roots/reciprocals, scientific notation, pH/pOH, and composite logs/roots."
    ),
    CONSTANTS(
        id = "constants",
        label = "Constants, Masses & Conversions",
        description = "Physical, chemical, and mathematical constants, element atomic masses, and SI/non-SI unit conversion factors."
    ),
    MIXED_REVIEW(
        id = "mixed_review",
        label = "Speed & Targeted Review",
        description = "Compound timed sprints across multiple drills and targeted practice on weak spots or past mistakes."
    )
}

data class QuestionFormat(
    val subtypeName: String,
    val promptPattern: String,
    val examplePrompt: String,
    val exampleAnswer: String,
    val explanation: String = ""
)

data class DrillParam(
    val name: String,
    val optionsDescription: String,
    val defaultValue: String
)

data class DrillSpec(
    val mode: GameMode,
    val category: DrillCategory,
    val title: String,
    val summary: String,
    val details: String,
    val questionFormats: List<QuestionFormat>,
    val parameters: List<DrillParam>,
    val answerChecking: String,
    val timingAndScoring: String,
    val supportsMcq: Boolean,
    val isLegacy: Boolean = false,
    val isCompound: Boolean = false
)

object DrillRegistry {

    private val specsByMode: Map<GameMode, DrillSpec> = buildSpecs().associateBy { it.mode }

    val all: List<DrillSpec>
        get() = GameMode.entries.map { of(it) }

    val activeDrills: List<DrillSpec>
        get() = all.filter { !it.isLegacy }

    fun of(mode: GameMode): DrillSpec {
        return specsByMode[mode] ?: error("Missing DrillSpec in DrillRegistry for GameMode.$mode (${mode.id})")
    }

    fun ofId(modeId: String): DrillSpec {
        val mode = GameMode.entries.find { it.id == modeId } ?: GameMode.ADDITION
        return of(mode)
    }

    fun categoryOf(mode: GameMode): DrillCategory = of(mode).category

    private fun buildSpecs(): List<DrillSpec> = listOf(
        DrillSpec(
            mode = GameMode.ADDITION,
            category = DrillCategory.ARITHMETIC,
            title = "Addition",
            summary = "Mental addition of multi-digit integers with difficulty-weighted carry frequency.",
            details = "Generates two-operand integer addition problems (a + b). Number magnitude is governed by the selected digit count (1 to 4 digits, corresponding to 1–9 up to 1000–9999). Difficulty controls operand distribution: Easy biases toward small leading digits and fewer carries, Medium samples uniformly across the digit range, and Hard biases toward upper-range digits (e.g., 7, 8, 9) that force cascading carries across place values.",
            questionFormats = listOf(
                QuestionFormat("1-Digit Addition", "a + b", "8 + 7", "15", "Single-digit sums up to 18."),
                QuestionFormat("Multi-Digit Addition", "A + B", "487 + 396", "883", "Multi-column mental addition with carries.")
            ),
            parameters = listOf(
                DrillParam("Digits", "1 (1–9), 2 (10–99), 3 (100–999), 4 (1000–9999)", "1 digit"),
                DrillParam("Difficulty", "Easy (low digits), Medium (uniform), Hard (high-carry 7–9 bias)", "Medium")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact). Typed via numeric keypad.",
            timingAndScoring = "Per-question countdown timer (default 20s). Timeout counts as incorrect.",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.SUBTRACTION,
            category = DrillCategory.ARITHMETIC,
            title = "Subtraction",
            summary = "Mental subtraction of multi-digit integers, optionally allowing negative differences.",
            details = "Generates two-operand integer subtraction problems (a − b). By default, operands are ordered so a ≥ b to produce non-negative results. When 'Allow Negative Results' is enabled, operand order is unrestricted and negative answers require toggling the minus sign on the keypad. Difficulty adjusts borrow frequency by biasing digits.",
            questionFormats = listOf(
                QuestionFormat("Non-Negative Difference", "a − b (a ≥ b)", "83 − 47", "36", "Standard mental borrowing."),
                QuestionFormat("Signed Difference", "a − b (any order)", "42 − 79", "-37", "Requires negative sign entry when b > a.")
            ),
            parameters = listOf(
                DrillParam("Digits", "1 to 4 digits", "1 digit"),
                DrillParam("Difficulty", "Easy, Medium, Hard (borrow-heavy bias)", "Medium"),
                DrillParam("Allow Negative Results", "Boolean toggle; enables a < b", "Off")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.MULTIPLICATION,
            category = DrillCategory.ARITHMETIC,
            title = "Multiplication",
            summary = "Mental multiplication across 1 to 4 digit operands with difficulty scaling.",
            details = "Generates integer products (a × b). On Easy difficulty, the second operand is constrained to a single digit (2–9) even when higher digit counts are selected, training N-digit × 1-digit distributive multiplication before full N-digit × N-digit cross-multiplication on Medium and Hard.",
            questionFormats = listOf(
                QuestionFormat("N-Digit × 1-Digit (Easy)", "A × b", "46 × 7", "322", "Left-to-right distributive multiplication."),
                QuestionFormat("Multi-Digit Product", "A × B", "38 × 64", "2432", "Full multi-digit mental multiplication.")
            ),
            parameters = listOf(
                DrillParam("Digits", "1 to 4 digits", "1 digit"),
                DrillParam("Difficulty", "Easy (2nd operand 1-digit), Medium (uniform), Hard (upper-bound digits)", "Medium")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.DIVISION,
            category = DrillCategory.ARITHMETIC,
            title = "Division",
            summary = "Exact integer division or integer quotient division with remainders.",
            details = "Generates division problems (dividend ÷ divisor). When 'Allow Remainder' is off (default), problems are constructed in reverse by multiplying divisor × quotient so division is guaranteed exact. When 'Allow Remainder' is on, the user enters the integer quotient floor(a / b).",
            questionFormats = listOf(
                QuestionFormat("Exact Division", "(b × q) ÷ b", "144 ÷ 12", "12", "Constructed to have zero remainder."),
                QuestionFormat("Integer Quotient", "a ÷ b", "85 ÷ 9", "9", "When remainders are enabled, expects integer quotient.")
            ),
            parameters = listOf(
                DrillParam("Digits", "1 to 4 digits", "1 digit"),
                DrillParam("Difficulty", "Easy, Medium, Hard", "Medium"),
                DrillParam("Allow Remainder", "Boolean toggle; whether non-exact divisions are generated", "Off")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.MIXED,
            category = DrillCategory.ARITHMETIC,
            title = "Mixed Operations",
            summary = "Randomized combination of addition, subtraction, multiplication, and division.",
            details = "Samples uniformly from whichever of the four core arithmetic operations (+, −, ×, ÷) are toggled active in setup, applying the shared digit count, difficulty tier, negative-result setting, and remainder setting. Each question is tagged with its specific underlying operation ('addition', 'subtraction', 'multiplication', 'division') for granular analytics.",
            questionFormats = listOf(
                QuestionFormat("Random Core Operation", "a [+|−|×|÷] b", "72 ÷ 8", "9", "Switches operation per question.")
            ),
            parameters = listOf(
                DrillParam("Active Operations", "Subset of {+, −, ×, ÷}", "All four enabled"),
                DrillParam("Digits & Difficulty", "1–4 digits; Easy / Medium / Hard", "1 digit, Medium"),
                DrillParam("Negative / Remainder", "Shared toggles for subtraction and division", "Off")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.MULTIPLICATION_TABLE,
            category = DrillCategory.NUMBER_FACTS,
            title = "Multiplication Tables",
            summary = "Focused practice on times tables within a chosen multiplier range (e.g., 1–10, 11–20).",
            details = "Trains rapid recall of multiplication tables. The first operand is sampled from the selected table range (such as 1–10, 11–20, or custom range) and multiplied by an integer from 1 to 10.",
            questionFormats = listOf(
                QuestionFormat("Table Product", "table × m (m ∈ 1..10)", "17 × 8", "136", "Direct times-table recall.")
            ),
            parameters = listOf(
                DrillParam("Table Range", "Preset or custom min–max table numbers", "1 to 10")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.FACTOR_FINDING,
            category = DrillCategory.NUMBER_FACTS,
            title = "Factor Finding",
            summary = "Given a composite product and one factor, find the missing cofactor.",
            details = "Presents an equation of the form 'a × ? = product' where 'a' is drawn from the selected range and the missing factor is in 1..10. Reinforces multiplicative inverse fluency and rapid factoring.",
            questionFormats = listOf(
                QuestionFormat("Missing Factor", "a × ? = c", "14 × ? = 112", "8", "Solve c ÷ a mentally.")
            ),
            parameters = listOf(
                DrillParam("Factor Range", "Min–max range for the known factor", "1 to 10")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.SQUARE,
            category = DrillCategory.NUMBER_FACTS,
            title = "Squares & Square Roots",
            summary = "Perfect squares (n²), perfect square roots (√n²), and memorized irrational roots (√2–√10) with optional MCQ.",
            details = "Unified drill covering both squaring and square-root extraction, structured like the Cubes & Powers drill. Users can practice: (1) Squares (n²) for n in 1–25 or a custom range, (2) Perfect Square Roots (√n² = n) over the same range, (3) Irrational Square Roots (√2 through √10) using HSC memorized course values (e.g., √2 ≈ 1.41, √3 ≈ 1.73, √5 ≈ 2.24), or (4) All Subtypes mixed. Supports both typed keypad entry and 2×2 Multiple Choice with algebraically realistic distractors (adjacent squares (n±1)², off-by-10 squares, or neighboring irrational roots).",
            questionFormats = listOf(
                QuestionFormat("Squares (n²)", "n²", "19²", "361", "Uses (a+b)² = a² + 2ab + b² mental decomposition."),
                QuestionFormat("Perfect Square Roots (√n²)", "√(n²)", "√484", "22", "Exact integer root of a perfect square."),
                QuestionFormat("Irrational Roots (√2–√10)", "√n = ?", "√7 = ?", "2.65", "Memorized HSC decimal root (±0.05 tolerance).")
            ),
            parameters = listOf(
                DrillParam("Subtype", "All Subtypes, Squares (n²), Perfect Square Roots (√n²), Square Roots (√2–√10)", "All Subtypes"),
                DrillParam("Range", "Fixed 1–25 or Custom min–max (applies to Squares and Perfect Roots)", "1 to 25"),
                DrillParam("Answer Format", "Typed keypad or 2×2 Multiple Choice (MCQ)", "Typed")
            ),
            answerChecking = "Exact integer match for Squares and Perfect Roots; AbsoluteTolerance(±0.05) accepting both HSC course value and true value for √2–√10; Choice match when MCQ is enabled.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.CUBES_AND_POWERS,
            category = DrillCategory.NUMBER_FACTS,
            title = "Cubes & Powers",
            summary = "Perfect cubes (n³), exact cube roots (∛n³), and small integer powers (2ⁿ, 3ⁿ, 5ⁿ).",
            details = "Covers three subtypes: (1) Cubes (n³) for n in 1–15 (or custom range), (2) Small Powers (2¹–2¹⁰, 3¹–3⁶, 5¹–5⁴), and (3) Perfect Cube Roots (∛n³ = n). Supports both free keypad entry and 2×2 Multiple Choice.",
            questionFormats = listOf(
                QuestionFormat("Cubes (n³)", "n³", "12³", "1728", "Exact cube of integer n."),
                QuestionFormat("Small Powers (aᵇ)", "aᵇ", "2⁸", "256", "Powers of base 2, 3, or 5."),
                QuestionFormat("Perfect Cube Roots (∛n)", "∛(n³)", "∛2197", "13", "Exact integer cube root.")
            ),
            parameters = listOf(
                DrillParam("Subtype", "All Subtypes, Cubes (n³), Small Powers (aᵇ), Cube Roots (∛n)", "All Subtypes"),
                DrillParam("Cube Range", "Custom min–max base integer", "1 to 15"),
                DrillParam("Answer Format", "Typed or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact) or Choice in MCQ mode.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.FRACTION,
            category = DrillCategory.FRACTIONS_PERCENT,
            title = "Fractions",
            summary = "Bidirectional conversion between fractions (n/d) and decimals, with HSC course-value support.",
            details = "Alternates between Fraction → Decimal (e.g., '3/8 = ?') and Decimal → Fraction (e.g., '0.375 = ?'). Users configure numerator and denominator ranges. When 'Course Values' is enabled, standard 2-decimal HSC approximations for reciprocals (such as 1/13 ≈ 0.07 or 0.08) are accepted alongside standard rounded decimals.",
            questionFormats = listOf(
                QuestionFormat("Fraction to Decimal", "n/d", "5/8", "0.625", "Convert fraction to decimal representation."),
                QuestionFormat("Decimal to Fraction", "0.xyz", "0.375", "3/8", "Enter reduced fraction using '/' key.")
            ),
            parameters = listOf(
                DrillParam("Numerator Range", "Min–max numerator", "1 to 9"),
                DrillParam("Denominator Range", "Min–max denominator", "2 to 10"),
                DrillParam("Course Values", "Accept 2-decimal HSC reciprocal course values", "Off")
            ),
            answerChecking = "FractionSpec: checks decimal within tolerance for Fraction→Decimal, or reduced/equivalent 'n/d' ratio for Decimal→Fraction.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.PERCENTAGE,
            category = DrillCategory.FRACTIONS_PERCENT,
            title = "Percentages",
            summary = "Mental percentage calculations (P% of N) scaled by difficulty.",
            details = "Presents percentage problems where Easy uses benchmark percentages (10%, 20%, 25%, 50%, 75%) on clean multiples, Medium introduces 5% increments and two-digit bases, and Hard uses arbitrary percentages or reverse/compound percentage relationships that still resolve to clean answers.",
            questionFormats = listOf(
                QuestionFormat("Benchmark Percent", "P% of X", "25% of 160", "40", "Convert 25% → 1/4 and divide."),
                QuestionFormat("Compound/General Percent", "P% of X", "15% of 240", "36", "Decompose 10% (24) + 5% (12) = 36.")
            ),
            parameters = listOf(
                DrillParam("Difficulty", "Easy (benchmarks), Medium (5% steps), Hard (general)", "Medium")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.RATIO,
            category = DrillCategory.FRACTIONS_PERCENT,
            title = "Ratios & Proportions",
            summary = "Solve missing terms in equivalent ratios, part-to-whole splits, and proportional scaling.",
            details = "Generates ratio problems such as finding x in 'a : b = c : x', simplifying ratios, or dividing a quantity into a given ratio. Difficulty scales the multiplier and whether intermediate simplification is needed.",
            questionFormats = listOf(
                QuestionFormat("Equivalent Ratio", "a : b = c : ?", "3 : 5 = 18 : ?", "30", "Scale by factor 18 ÷ 3 = 6."),
                QuestionFormat("Ratio Share", "Share N in a:b", "Split 90 in 4:5 (larger part)", "50", "90 ÷ (4+5) × 5 = 50.")
            ),
            parameters = listOf(
                DrillParam("Difficulty", "Easy (integer scale), Medium, Hard (simplify first)", "Medium")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.APPROXIMATION,
            category = DrillCategory.REASONING,
            title = "Estimation",
            summary = "Rapid order-of-magnitude and front-end estimation of ugly arithmetic expressions.",
            details = "Presents arithmetic expressions with non-round numbers (e.g., 498 × 21, 1987 ÷ 49, √2450) where exact calculation is slow but rounding operands yields a fast, accurate estimate. Checked via relative tolerance governed by the user's Tolerance Preset (Strict 0.5%, Normal 1.0%, Lenient 2.0% or expression-specific override). Supports 2×2 Multiple Choice.",
            questionFormats = listOf(
                QuestionFormat("Product/Quotient Estimate", "A × B ≈ ?", "498 × 21 ≈ ?", "10458", "Round to 500 × 21 = 10500 (accepted within tolerance).")
            ),
            parameters = listOf(
                DrillParam("Difficulty", "Easy, Medium, Hard", "Medium"),
                DrillParam("Answer Format", "Typed (within tolerance) or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "RelativeTolerance (typically 2%–5% depending on expression, or TolerancePreset) or Choice in MCQ mode.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.NUMBER_SERIES,
            category = DrillCategory.REASONING,
            title = "Number Series",
            summary = "Identify the pattern in a numeric sequence and compute the next term.",
            details = "Generates sequences governed by arithmetic progressions, geometric ratios, second-order differences, alternating steps, squares/cubes offsets, or Fibonacci-style recurrences. Solution steps explain the underlying rule on review.",
            questionFormats = listOf(
                QuestionFormat("Next Term in Sequence", "t₁, t₂, t₃, t₄, ?", "3, 8, 15, 24, ?", "35", "Differences are +5, +7, +9, +11 (n² − 1).")
            ),
            parameters = listOf(
                DrillParam("Difficulty", "Easy (arithmetic/geometric), Medium (2nd-order/squares), Hard (hybrid/alternating)", "Medium")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.CHAIN_CALCULATION,
            category = DrillCategory.REASONING,
            title = "Chain Calculation",
            summary = "Multi-step sequential mental arithmetic evaluated left-to-right.",
            details = "Presents a chain of 3 to 5 operations (e.g., '((12 + 8) × 3) − 15') that must be tracked in working memory step-by-step. Intermediate results are guaranteed to remain integers.",
            questionFormats = listOf(
                QuestionFormat("Sequential Chain", "((a op₁ b) op₂ c) op₃ d", "((15 × 4) − 12) ÷ 6", "8", "15×4=60 → 60−12=48 → 48÷6=8.")
            ),
            parameters = listOf(
                DrillParam("Difficulty", "Easy (3 steps, small), Medium (4 steps), Hard (4–5 steps, larger intermediates)", "Medium")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = false
        ),
        DrillSpec(
            mode = GameMode.MEMORY_TABLES,
            category = DrillCategory.SCIENCE_MATH,
            title = "Memory Tables",
            summary = "Memorized HSC lookup tables: √2–√10, ∛2–∛10, log₁₀(1)–log₁₀(10), and reciprocals 1/11–1/15.",
            details = "Trains instant bidirectional recall of the four core lookup tables needed for calculator-free physics and chemistry exams: Square Roots (√2–√10), Cube Roots (∛2–∛10), Base-10 Logarithms (log 1–log 10), and Reciprocals (1/11–1/15). Supports Forward (symbol → decimal value), Reverse (decimal value → integer argument n), and Mixed directions, plus 2×2 MCQ.",
            questionFormats = listOf(
                QuestionFormat("Forward Lookup", "log₁₀(n) = ? / √n = ?", "log₁₀(7) = ?", "0.85", "Accepts HSC course value 0.85 or 0.845 (±0.05)."),
                QuestionFormat("Reverse Lookup", "If log₁₀(n) ≈ 0.48, n = ?", "If √n ≈ 2.24, n = ?", "5", "Exact integer argument n.")
            ),
            parameters = listOf(
                DrillParam("Direction", "Forward, Reverse, Mixed", "Mixed"),
                DrillParam("Included Tables", "Multi-select from Square Roots, Cube Roots, Base-10 Logs, Reciprocals", "All 4 tables"),
                DrillParam("Answer Format", "Typed or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "Forward uses AbsoluteTolerance (±0.05 for roots/logs, ±0.005 for reciprocals) accepting both HSC course value and true value; Reverse uses Exact integer match.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.SCIENTIFIC_NOTATION,
            category = DrillCategory.SCIENCE_MATH,
            title = "Scientific Notation",
            summary = "Normalize decimals, apply exponent shift rules, multiply/divide, and take roots of powers of 10.",
            details = "Covers four algebraic manipulations of scientific notation (m × 10ⁿ): (1) Normalize Decimal (convert 0.0045 or 450 × 10³ into standard 1 ≤ |m| < 10 form), (2) Shift Direction Rule (find the new exponent when shifting the decimal point), (3) Multiply & Divide ((a×10ᵐ) ×/÷ (b×10ⁿ)), and (4) Roots of 10ⁿ (√10²ⁿ or make exponent even before rooting). Entered via the dedicated '×10ⁿ' keypad key or 2×2 MCQ.",
            questionFormats = listOf(
                QuestionFormat("Normalize Decimal", "x → m×10ⁿ", "Normalize 0.00072", "7.2×10^-4", "Requires normalized mantissa 1 ≤ |m| < 10."),
                QuestionFormat("Multiply & Divide", "(a×10ᵐ) × (b×10ⁿ)", "(4×10³) × (5×10⁻⁶)", "2×10^-2", "Multiply mantissas, add exponents, normalize."),
                QuestionFormat("Roots of 10ⁿ", "√(10²ᵏ)", "√(10⁻⁸)", "1×10^-4", "Halve the even exponent.")
            ),
            parameters = listOf(
                DrillParam("Subtype", "All Subtypes, Normalize Decimal, Shift Direction Rule, Multiply & Divide, Roots of 10ⁿ", "All Subtypes"),
                DrillParam("Answer Format", "Typed (with ×10ⁿ key) or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "ScientificValue (relative tolerance on mantissa + exponent check, enforcing 1 ≤ |m| < 10 on normalization drills) or Exact for exponent-only shifts.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.PH_POH,
            category = DrillCategory.SCIENCE_MATH,
            title = "pH & pOH Calculations",
            summary = "Chemistry acid-base calculations: [H⁺] → pH, a×10⁻ⁿ M → pH, pH → [H⁺], pH ↔ pOH, and % w/v strong base/acid.",
            details = "Trains the mental logarithm identities used in HSC Chemistry acid-base equilibria: pH = n − log₁₀(a) for [H⁺] = a × 10⁻ⁿ M, pH + pOH = 14 at 25 °C, and converting % (w/v) solutions of strong acids/bases (NaOH, HCl, H₂SO₄, KOH) to molarity and pH.",
            questionFormats = listOf(
                QuestionFormat("a×10⁻ⁿ M → pH", "[H⁺] = a×10⁻ⁿ M → pH", "[H⁺] = 2×10⁻⁴ M, pH = ?", "3.7", "pH = 4 − log₁₀(2) = 4 − 0.30 = 3.70."),
                QuestionFormat("pH ↔ pOH", "pH + pOH = 14", "If pOH = 4.3, pH = ?", "9.7", "14 − 4.3 = 9.7."),
                QuestionFormat("pH → [H⁺]", "pH → 10⁻ᵖᴴ M", "pH = 3 → [H⁺] (M)", "1×10^-3", "Direct power-of-10 conversion.")
            ),
            parameters = listOf(
                DrillParam("Subtype", "All Subtypes, Concentration → pH, a×10⁻ⁿ M → pH, pH → [H⁺], pH ↔ pOH, % w/v Base → pH", "All Subtypes"),
                DrillParam("Answer Format", "Typed or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "AbsoluteTolerance(±0.1) for pH/pOH values; ScientificValue for concentration outputs.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.COMPOSITE_LOGS_ROOTS,
            category = DrillCategory.SCIENCE_MATH,
            title = "Composite Logs & Roots",
            summary = "Evaluate log₁₀(k×10ⁿ), composite log₁₀(a×b) via prime factoring, and √(a×10ⁿ) using memorized tables.",
            details = "Bridges memorized single-digit logs/roots to real exam numbers: (1) log₁₀(k × 10ⁿ) = n + log₁₀(k), (2) log₁₀(composite) by factoring into 2, 3, 5, 7 (e.g., log 12 = 2 log 2 + log 3), and (3) √(a × 10ⁿ) by adjusting 10ⁿ to an even exponent before taking √a × 10^(n/2).",
            questionFormats = listOf(
                QuestionFormat("log₁₀(k × 10ⁿ)", "log₁₀(k × 10ⁿ)", "log₁₀(3 × 10⁻⁵)", "-4.52", "-5 + log₁₀(3) = -5 + 0.48 = -4.52."),
                QuestionFormat("Composite Log", "log₁₀(a × b)", "log₁₀(15)", "1.18", "log 3 + log 5 = 0.48 + 0.70 = 1.18."),
                QuestionFormat("Scientific Square Root", "√(a × 10ⁿ)", "√(5 × 10⁻⁶)", "2.24×10^-3", "√5 × 10⁻³ ≈ 2.24 × 10⁻³.")
            ),
            parameters = listOf(
                DrillParam("Subtype", "All Subtypes, log₁₀(k × 10ⁿ), log₁₀(a × b) Composite, √(a × 10ⁿ)", "All Subtypes"),
                DrillParam("Difficulty", "Easy, Medium, Hard", "Medium"),
                DrillParam("Answer Format", "Typed or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "AbsoluteTolerance(±0.08) for logs; ScientificValue (relative tolerance) for scientific roots.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.CONSTANTS,
            category = DrillCategory.CONSTANTS,
            title = "Constants (Physics, Chemistry & Math)",
            summary = "61 HSC Physics & Chemistry constants plus 4 Mathematical constants (π, e, ln 2, ln 10), filterable by paper, chapter, and reliability.",
            details = "Tests recall of physical, chemical, and mathematical constants from the HSC curriculum dataset. Users can filter by category/paper (Physics 1st Paper, Physics 2nd Paper, Chemistry 1st Paper, Chemistry 2nd Paper, and Mathematics), syllabus chapter (bilingual English/Bangla), reliability class (Fundamental, Conventions, Reference & Approx), and whether to include alternate non-SI unit systems (e.g., R in L·atm·mol⁻¹·K⁻¹ or h in eV·s). Weighted selection prioritizes previously missed constants.",
            questionFormats = listOf(
                QuestionFormat("Physical/Chemical Constant", "Name (Symbol) [Unit]", "Planck constant (h) [J·s]", "6.63×10^-34", "Accepts HSC course value (6.63×10⁻³⁴) and CODATA precise value (6.626×10⁻³⁴)."),
                QuestionFormat("Mathematical Constant", "Name (Symbol)", "Euler's number (e)", "2.718", "Accepts decimal or fraction approximations (e.g. 22/7 for π).")
            ),
            parameters = listOf(
                DrillParam("Categories", "Physics 1st, Physics 2nd, Chemistry 1st, Chemistry 2nd, Mathematics", "All categories"),
                DrillParam("Reliability Class", "Fundamental, Conventions, Reference & Approx", "All 3 classes"),
                DrillParam("Unit Systems", "Standard SI only vs Include Alternate Unit Systems", "SI only"),
                DrillParam("Chapters", "Filter by specific HSC syllabus chapters", "All chapters"),
                DrillParam("Answer Format", "Typed (scientific/fraction keypad) or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "Dual-key ScientificValue / RelativeTolerance checking: accepts both HSC textbook course value and precise scientific value (plus fraction 22/7 for π).",
            timingAndScoring = "Per-question countdown timer (default 20s); reveals post-answer solution steps and confusable constant warnings.",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.ATOMIC_MASSES,
            category = DrillCategory.CONSTANTS,
            title = "Atomic Masses",
            summary = "Relative atomic masses (Aᵣ) of 39 curriculum-critical chemical elements.",
            details = "Tests recall of standard HSC atomic masses (e.g., H = 1, C = 12, N = 14, O = 16, Na = 23, Cl = 35.5, Cu = 63.5, Ag = 108) used in stoichiometry and molarity problems.",
            questionFormats = listOf(
                QuestionFormat("Element to Atomic Mass", "Relative atomic mass of Element (Symbol)", "Relative atomic mass of Chlorine (Cl)", "35.5", "Accepts standard course mass within ±0.05.")
            ),
            parameters = listOf(
                DrillParam("Answer Format", "Typed or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "AbsoluteTolerance(±0.05) accepting both course mass and IUPAC conventional value.",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.UNIT_CONVERSIONS,
            category = DrillCategory.CONSTANTS,
            title = "Unit Conversions",
            summary = "Conversion factors across pressure, energy, length, volume, charge, mass, and time.",
            details = "Tests forward and reverse conversion factors between SI and common non-SI scientific units (e.g., atm ↔ Pa, L·atm ↔ J, cal ↔ J, eV ↔ J, Å ↔ m, L ↔ m³, bar ↔ Pa).",
            questionFormats = listOf(
                QuestionFormat("Unit Conversion Factor", "1 [from_unit] = ? [to_unit]", "1 atm = ? Pa", "1.013×10^5", "Accepts course factor and exact conversion within 1% relative tolerance.")
            ),
            parameters = listOf(
                DrillParam("Answer Format", "Typed (with ×10ⁿ key) or 2×2 Multiple Choice", "Typed")
            ),
            answerChecking = "ScientificValue / RelativeTolerance (1% tolerance).",
            timingAndScoring = "Per-question countdown timer (default 20s).",
            supportsMcq = true
        ),
        DrillSpec(
            mode = GameMode.SPEED_ROUND,
            category = DrillCategory.MIXED_REVIEW,
            title = "Speed Round",
            summary = "Continuous timed sprint (1, 2, 3, or 5 minutes) sampling from a customizable pool of drills, in typed or MCQ mode.",
            details = "A compound high-tempo mode with a single session countdown clock (1, 2, 3, or 5 minutes) instead of per-question timers. Questions are drawn randomly from the user's selected topic pool and Advance immediately (200ms transition) without pausing on wrong answers. Each question records its underlying sub-drill in `drillMode` and `topicTag` so post-session breakdowns and exports attribute performance accurately. Supports both free keypad entry and 2×2 Multiple Choice.",
            questionFormats = listOf(
                QuestionFormat("Rapid Multi-Topic Sprint", "Any question from active topic pool", "log₁₀(5) = ?", "0.7", "Measured by total accuracy and Questions Per Minute (QPM).")
            ),
            parameters = listOf(
                DrillParam("Duration", "1, 2, 3, or 5 minutes", "1 minute"),
                DrillParam("Topic Pool", "Multi-select from any active drill mode", "Core arithmetic + Science Math"),
                DrillParam("Answer Format", "Typed or 2×2 Multiple Choice (MCQ)", "Typed")
            ),
            answerChecking = "Delegates to each sub-drill's native AnswerSpec (or AnswerSpec.Choice when MCQ is enabled).",
            timingAndScoring = "Session-wide countdown timer; tracks Questions Per Minute (QPM), per-topic accuracy, and average response time.",
            supportsMcq = true,
            isCompound = true
        ),
        DrillSpec(
            mode = GameMode.WEAK_SPOTS,
            category = DrillCategory.MIXED_REVIEW,
            title = "Weak Spots Auto-Drill",
            summary = "Adaptive session that automatically generates new questions in your highest-error topics.",
            details = "Analyzes historical question records to find topics with the highest error rates and slowest response times, then generates fresh questions targeting those drills.",
            questionFormats = listOf(
                QuestionFormat("Adaptive Topic Question", "Generated from top weak topicTags", "√(3 × 10⁻⁴)", "1.73×10^-2", "Dynamically targets weak areas.")
            ),
            parameters = listOf(
                DrillParam("Question Selection", "Automatic based on historical error rate and latency", "Auto")
            ),
            answerChecking = "Delegates to the underlying drill's AnswerSpec.",
            timingAndScoring = "Logged with sessionType = 'weak_spots'; each question records its true sub-drill in `drillMode`.",
            supportsMcq = false,
            isCompound = true
        ),
        DrillSpec(
            mode = GameMode.MISTAKE_REVIEW,
            category = DrillCategory.MIXED_REVIEW,
            title = "Mistake Review",
            summary = "Targeted replay of the exact questions missed in a previous session or across selected drills.",
            details = "Replays the exact problem payloads (`MathProblem` serialized via `ProblemCodec`) that the user previously answered incorrectly or used a hint on, scoped to user-selected categories and drills (either from the post-session results screen or the all-time Mistake Review explorer). Questions are deduplicated and ordered by highest error count, then most recent.",
            questionFormats = listOf(
                QuestionFormat("Exact Mistake Replay", "Original missed question & answer spec", "[H⁺] = 3.2×10⁻⁴ M → pH", "3.49", "Replays exact prompt, unit, and checking tolerance.")
            ),
            parameters = listOf(
                DrillParam("Scope", "Single session mistakes or All-time mistakes filtered by category/drill", "Unresolved mistakes"),
                DrillParam("Session Cap", "Up to 30 unique questions per practice run, single pass", "Max 30 questions")
            ),
            answerChecking = "Restores original AnswerSpec from ProblemCodec (or numeric tolerance fallback for legacy records).",
            timingAndScoring = "Tracked as a full session with sessionType = 'mistake_review' and scopeLabel; updates daily goal without altering adaptive difficulty.",
            supportsMcq = false,
            isCompound = true
        ),
        // Legacy modes preserved for historical session & export compatibility
        DrillSpec(
            mode = GameMode.SQUARE_ROOT,
            category = DrillCategory.NUMBER_FACTS,
            title = "Roots (Legacy — merged into Squares)",
            summary = "Historical standalone perfect square-root drill, now merged into Squares & Square Roots.",
            details = "Legacy standalone mode for perfect square roots (√n² = n). In current versions, all square root subtypes (perfect roots and irrational roots √2–√10) are practiced inside the unified Squares & Square Roots drill.",
            questionFormats = listOf(
                QuestionFormat("Perfect Square Root", "√(n²)", "√225", "15", "Now part of Squares drill.")
            ),
            parameters = listOf(
                DrillParam("Range", "By Difficulty (Easy 1–10, Medium 11–25, Hard 26–35) or Custom", "By Difficulty")
            ),
            answerChecking = "Exact integer match (AnswerSpec.Exact).",
            timingAndScoring = "Legacy mode; historical sessions remain included in Number Facts statistics.",
            supportsMcq = true,
            isLegacy = true
        ),
        DrillSpec(
            mode = GameMode.MATH_CONSTANTS,
            category = DrillCategory.CONSTANTS,
            title = "Math Constants (Legacy — merged into Constants)",
            summary = "Historical standalone drill for π, e, ln 2, and ln 10, now a category inside Constants.",
            details = "Legacy standalone mode for the 4 mathematical constants (π, e, ln 2, ln 10). Now integrated directly into the Constants drill under the Mathematics category.",
            questionFormats = listOf(
                QuestionFormat("Math Constant", "Name (Symbol)", "Pi (π)", "3.1416", "Accepts 3.14, 3.1416, or 22/7.")
            ),
            parameters = emptyList(),
            answerChecking = "RelativeTolerance(1%) plus accepted approximations and fractions.",
            timingAndScoring = "Legacy mode; historical sessions remain included in Constants statistics.",
            supportsMcq = true,
            isLegacy = true
        ),
        DrillSpec(
            mode = GameMode.REFERENCE_VALUES,
            category = DrillCategory.CONSTANTS,
            title = "Reference Values (Legacy — removed)",
            summary = "Historical drill for condition-dependent reference values, removed from active drills.",
            details = "Legacy mode that tested 12 condition-dependent reference values. Removed from the active app; historical sessions are preserved for longitudinal record integrity.",
            questionFormats = listOf(
                QuestionFormat("Reference Value", "Name (Symbol)", "Speed of sound in air at 0 °C", "331", "Legacy reference entry.")
            ),
            parameters = emptyList(),
            answerChecking = "ScientificValue (1.5% relative tolerance).",
            timingAndScoring = "Legacy mode; preserved only if historical records exist.",
            supportsMcq = true,
            isLegacy = true
        )
    )
}
