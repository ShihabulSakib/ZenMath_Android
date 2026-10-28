# ZenMath — Developer Notes (`feature/next-release`)

## Step 0 — Recon Findings

### Architecture & Codebase Structure
- **Module & Package**: Single-module Android application (`:app`), namespace `com.example`, `applicationId = "com.shihabulsakib.zenmath"`, `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`.
- **Architecture Pattern**: MVVM (`ZenMathViewModel` -> `ZenMathRepository` -> `ZenMathDao` / `PreferencesManager`) with domain generators in `com.example.domain` (`QuestionGenerator`, `FractionsPool`, `GameModels`).
- **UI Toolkit**: Jetpack Compose with Material 3 (`composeBom = "2024.09.00"`), custom `ZenMathTheme` (`DarkZenColors` "Zen Obsidian" and `LightZenColors` "PaperZen"), and state-driven navigation via `ZenScreen` and `MainTab` enums inside `ZenMathApp.kt`.
- **Current Versioning**: `versionCode = 3`, `versionName = "1.0.5"`.
- **Room Database**:
  - `ZenMathDatabase` is currently at `version = 1` with `exportSchema = false` and `.fallbackToDestructiveMigration()`.
  - Entities: `SessionEntity` (`sessions` table) and `QuestionRecordEntity` (`questions` table).
  - Note: `QuestionRecordEntity` already has `timeMs: Long`, though `ZenMathViewModel` currently truncates elapsed time to whole seconds (`timeTaken: Int`) and multiplies by `1000L` when saving.
- **Existing Tests**:
  - `ExampleUnitTest.kt`: 8 JUnit4 unit tests covering `QuestionGenerator` and `FractionsPool`.
  - `ExampleRobolectricTest.kt`: 4 Robolectric tests covering context strings, `NotificationScheduler`, and `ReminderReceiver` (Note: line 98 on `main` called protected `shadowOf(notificationManager).cancelAll()`; fixed to `notificationManager.cancelAll()`).
  - `ExampleInstrumentedTest.kt`: 1 template androidTest.

---

## Build Order & Phase Plan

We will follow the specification sections 1 → 2 → 3 → 4 → 5 → 6 in order, committing after each verified working phase:

1. **Phase 1 — Foundations (Section 1)**
   - **1.1 Problem Model**: Extend `MathProblem` (preserving all existing fields) with `topicTag`, `answerSpec`, `unitText`, `subjectLabel`, `solutionSteps`, `mcqOptions`, `confusableIds`, and `hintText`.
   - **1.2 Central Answer Checker**: Implement `AnswerSpec` (`Exact`, `AbsoluteTolerance`, `RelativeTolerance`, `ScientificValue`, `Choice`, `FractionOrDecimal`), `TolerancePreset` (`STRICT`, `NORMAL`, `LENIENT`), and `AnswerChecker` supporting per-item tolerance overrides and course-value acceptance.
   - **1.3 Input (`×10ⁿ` & Keypad)**: Extend `Keypad` with `×10ⁿ` exponent entry key, support negative exponents and negative mantissas, parse scientific input cleanly into `Double` without crashing on malformed input, and raise input cap to 20 chars.
   - **1.4 Text Rendering**: Create `RichTextHelper` using Compose `AnnotatedString` (`SpanStyle` with `BaselineShift.Superscript` / `Subscript`) for scientific notation, chemical formulas, sub/superscripts, and structured units. Bundle a clean font with full Greek & math glyph coverage or configure typography cleanly.
   - **1.5 & 1.6 Database & Dates**: Enable Room schema export (`app/schemas`), write `MIGRATION_1_2` (no destructive fallback), add `questionText`, `topicTag`, `hintUsed` to `questions` and `localDate`, `sessionType` to `sessions`. Implement `DateBucketUtils` to convert UTC timestamps to device local calendar dates (`yyyy-MM-dd`), migrate existing rows, and use `DateBucketUtils` across streaks, charts, and calendar. Update JSON backup export/import (backward-compatible with v1 backups).
   - **1.7 Timing**: Use a monotonic clock (`SystemClock.elapsedRealtime()` with fallback for pure JVM tests) to measure question elapsed time in milliseconds (`timeMs: Long`), keeping UI display at `0.1s` and storing exact milliseconds in results and DB.

2. **Phase 2 — Fixes to Existing Behavior (Section 2)**
   - Hide difficulty slider in `GameSetupScreen` for modes that do not use it (`SQUARE`, `SQUARE_ROOT` when custom range is selected, `FRACTION`, `MULTIPLICATION_TABLE`, `FACTOR_FINDING`, and new table/constant drills where difficulty isn't used).
   - Add real number inputs / steppers with validation for custom Square and Square Root ranges in `GameSetupScreen`.
   - Differentiate Medium vs Hard in `PERCENTAGE` (Hard: fractional percentages like `12.5%`, `37.5%`, `62.5%`, `87.5%`, `33.33%`, larger bases), `NUMBER_SERIES` (Hard: 7 terms, second-order differences, alternating patterns; Fibonacci from random start pair and varied hidden index), and `CHAIN_CALCULATION` (Hard: 4 operations and brackets). Differentiate Easy vs Medium in `RATIO` (Medium: three-term `A:B:C` ratios).
   - Reset setup toggles (`allowRemainder`, `allowNegativeResults`) on mode switch, and expose them on the `MIXED` setup screen when Division/Subtraction are enabled.
   - Adaptive difficulty: step down difficulty (or digits if on Easy with digits > 1) when session accuracy < 50%; keep upward rule at >= 85%.
   - Fractions: extend denominators up to 15 (2, 4, 6, 8, 10, 12, 15) and add a "Course values" toggle accepting rounded course decimals (e.g., `1/3 -> 0.3`, `1/6 -> 0.15`, `1/11..1/15` course table values) within tolerance.

3. **Phase 3 — New Content: Revision Tables and Drills (Section 3)**
   - **3.1 Memory Tables**: Create `CourseValuesData.kt` holding square roots (2..10), cube roots (2..10), log10 (1..10), and reciprocals (1/11..1/15), plus course fraction decimals. Add Revision tab and drill mode (Forward, Reverse, Mixed directions).
   - **3.2 Scientific Notation Drills**: Normalize decimal, decimal-shift direction rule, multiply/divide with exponents, and square roots of powers of 10 (including odd exponents rewritten to even exponents), with step-by-step solutions.
   - **3.3 pH and pOH Drills**: Create `PhSpeciesData.kt` (strong monoprotic/diprotic acids and bases with molar masses). Implement drills: concentration (M/mM) to pH, scientific-notation concentration to pH via course rounding method, reverse pH to concentration (from constructible log values), pH <-> pOH (`pH + pOH = 14`), and `% w/v` of a base to pH (`c = 10 * % / M`). Add Revision tab.
   - **3.4 Composite Logs and Roots**: `log10(k × 10^n)`, `log10(a × b)` using log table, and `√(a × 10^n)`.
   - **3.5 Cubes and Powers**: `n³` (default 1..15, configurable), small `a^b`, cube roots of perfect cubes. Add `(10 + b)²` expansion hint/steps for Squares 11..25.
   - **3.6 Approximation MCQ**: Add per-session setup toggle to run Approximation and new modes in 4-option 2×2 MCQ mode, with options spaced >= 3× tolerance apart and realistic distractors (power-of-10 off, wrong sign, neighboring table value).

4. **Phase 4 — Learning Aids (Section 4)**
   - **4.1 Hints and Steps**: Ensure every generator populates `solutionSteps`. Add a `Hint` button on `GameScreen` (marks answer as `hintUsed = true`). On wrong answer or timeout, display a solution steps card that stays until the user taps `Next` (configurable in Settings: step card with manual Next vs short auto-advance).
   - **4.2 Weak-Spot Review**: Read saved `QuestionRecordEntity` rows from Room, aggregate by `topicTag` and `questionText` (error rate, avg time, last seen), display a "Weak Spots" section in `StatsScreen`, and support launching a "Practice Weak Spots" session.
   - **4.3 Speed Round**: Add Speed Round mode with setup for duration (1, 2, 3, or 5 minutes) and multi-select topic pool. Single total countdown, no per-question timeout, instant next question. Results screen shows score, accuracy, questions/minute, and per-topic breakdown; saved with `sessionType = "speed_round"`.

5. **Phase 5 — Constants Module (Section 5)**
   - **5.1 & 5.5 Data File & Schema**: Create `ConstantsSchema.kt` (data structures + validator/loader) and `ConstantsData.kt` (dedicated data file with how-to-add comment block at top and only 3 clearly flagged `isSample = true` sample entries: Coulomb's constant, Universal gas constant `R` with alternate unit, and Avogadro's constant).
   - **5.2 Validation**: Fail loudly (`IllegalStateException`) in `BuildConfig.DEBUG` on duplicate IDs or blank required fields; log/skip invalid entries in release builds.
   - **5.3 Revision Tab**: Constants tab in `RevisionScreen` grouped by subject, with subject filter chips and search box, large rich-rendered symbol, name, value, and unit, plus expandable notes and confusable constants on tap.
   - **5.4 Constants Drill**: Dynamic drill mode showing subject chip + descriptive name + symbol together, `×10ⁿ` input, feedback showing unit and confusable constants, subject filtering in setup, and weighting toward previously missed constants.

6. **Phase 6 — Testing, Polish, and Release Bump (Section 6)**
   - Write comprehensive unit & Robolectric tests for: `AnswerChecker` (all types & presets), `×10ⁿ` input parser, all generators (verifying answers match step recomputations), MCQ option spacing, `DateBucketUtils` across multiple timezones, Room migration v1 -> v2 with sample data, and v1/v2 backup JSON import/export.
   - Update `CHANGELOG.md` and `DEV_NOTES.md`, bump `versionName` to `"1.1.0"` and `versionCode` to `4`, run full test suite and assemble build, commit and tag `v1.1.0`.

---

## Decisions on Ambiguities & Edge Cases

1. **Reciprocals 1/13 and 1/14 Course Value Collision (`0.07`)**:
   - Plan 3.1 specifies reciprocals `1/11 to 1/15` as `0.0909, 0.08, 0.07, 0.07, 0.06`.
   - In forward drills (`1/13 = ?` or `1/14 = ?`), `0.07` (or true values `0.0769` / `0.0714` within tolerance / course value rule) is accepted.
   - In reverse drills (`1/? ≈ 0.07`), both `13` and `14` are accepted as valid exact answers so the user is never penalized for the identical rounded course value.
2. **Memorized Table Tolerance**:
   - Plan 1.2 states: "Memorized root, cube-root and log tables: accept the stored course value exactly, or anything within 0.05 absolute."
   - We implement `AbsoluteTolerance(expected = trueOrCourseValue, tolerance = 0.05, alsoAcceptExact = listOf(courseValue, trueValue))` so both the memorized rounded value and the true mathematical value within `0.05` are accepted.
3. **Sample Constants (Section 5.5)**:
   - Plan 5.1 mentions `"Coulomb's constant" versus "Spring constant"` and `"R in two unit systems"`, and 5.5 requires shipping with *only three clearly flagged sample entries*.
   - We include:
     1. `coulomb_k`: Coulomb's constant (`k_e`, Electrostatics, `9.0 × 10^9 N·m²·C⁻²`, `isSample = true`)
     2. `gas_constant_r`: Molar gas constant (`R`, Thermodynamics, `8.314 × 10^0 J·mol⁻¹·K⁻¹`, alternate `0.0821 × 10^0 L·atm·mol⁻¹·K⁻¹`, `isSample = true`)
     3. `standard_gravity_g`: Standard acceleration due to gravity (`g`, Mechanics, `9.8 × 10^0 m·s⁻²`, confusable with `coulomb_k` / `G` noted, `isSample = true`).
4. **Database `timeMs` Column**:
   - `QuestionRecordEntity` in v1 already has `val timeMs: Long`. In v2 (`MIGRATION_1_2`), we keep `timeMs` (now populated with true millisecond precision instead of `seconds * 1000`) and add `questionText`, `topicTag`, and `hintUsed` to `questions`, plus `localDate` and `sessionType` to `sessions`.
5. **Untracked Files in Repo Root**:
   - `ZENMATH_FEATURE_PLAN.md`, `FIX_DAILY_REMINDERS_PROMPT.md`, and `app-icons.zip` were untracked on `main`. We will leave `FIX_DAILY_REMINDERS_PROMPT.md` and `app-icons.zip` untracked and track `DEV_NOTES.md` and `CHANGELOG.md`.

---

## Progress Log
- **Step 0 (Recon)**: Completed exploration of codebase, verified architecture, Room v1 schema, and build/test setup. Created `feature/next-release` branch and `DEV_NOTES.md`.
- **Phase 1 (Foundations — Section 1)**:
  - Extended `MathProblem` with `topicTag`, `answerSpec`, `unitText`, `subjectLabel`, `solutionSteps`, `mcqOptions`, `confusableIds`, `constantSymbol`, and `structuredUnit`.
  - Created `AnswerChecker`, `AnswerSpec`, `TolerancePreset` (`STRICT` 0.5%, `NORMAL` 1.0%, `LENIENT` 2.0%), and `ScientificInputParser`.
  - Extended `Keypad` with `×10ⁿ` exponent-entry key, raised input length cap to 20, and created `RichTextHelper` using Compose `AnnotatedString` (`SpanStyle` with `BaselineShift.Superscript` / `Subscript`) for scientific notation, units, and symbols.
  - Enabled Room schema export (`app/schemas`), bumped `ZenMathDatabase` to v2 with non-destructive `MIGRATION_1_2`, created `DateBucketUtils` for local-date bucketing across timezones, updated backup JSON export/import (v1-compatible), and switched timing to monotonic milliseconds (`MonotonicClock`).
  - Added dedicated data files `CourseValuesData.kt`, `PhSpeciesData.kt`, `ConstantsSchema.kt`, and `ConstantsData.kt`.
- **Phase 2 (Fixes to Existing Behavior — Section 2)**:
  - Hid the Difficulty slider on `GameSetupScreen` for modes that do not use it (`SQUARE`, `FRACTION`, `SQUARE_ROOT` when custom range is selected, etc.).
  - Added validated Min / Max number fields (`CustomRangeEditor`) for custom Square and Square Root ranges.
  - Differentiated Medium vs Hard in `PERCENTAGE` (Hard: fractional percentages `12.5%`, `37.5%`, `62.5%`, `87.5%` and larger bases), `NUMBER_SERIES` (Hard: 7 terms, second-order differences, alternating patterns; Fibonacci from random start pair and varied hidden index), and `CHAIN_CALCULATION` (Hard: 4 operations and bracketed expressions `(a ± b) × c ± (d ± e)`).
  - Differentiated Easy vs Medium in `RATIO` (Medium & Hard: three-term `A:B:C` ratios).
  - Reset `allowRemainder` and `allowNegativeResults` on mode selection and exposed both toggles on `MIXED` setup.
  - Updated adaptive difficulty to step down (`HARD -> MEDIUM -> EASY -> digits - 1`) when session accuracy < 50%.
  - Extended Fraction denominators up to 15 (`2, 4, 6, 8, 10, 12, 15`) and added the "Course Values" toggle.
- **Phase 3 (New Content: Revision Tables and Drills — Section 3)**:
  - Implemented `generateMemoryTableQuestion` (Forward, Reverse, Mixed across Square Roots, Cube Roots, Base-10 Logs, and Reciprocals `1/11..1/15`, accepting both 13 and 14 for `0.07` and both 8 and 9 for `∛? ≈ 2.0`).
  - Implemented `generateScientificNotationQuestion` (`NORMALIZE`, `DECIMAL_SHIFT`, `MULTIPLY_DIVIDE`, and `ROOT_POWER_TEN` including odd exponents rewritten to even exponents, with full steps).
  - Implemented `generatePhQuestion` (`CONC_TO_PH` in M/mM for monoprotic/diprotic strong acids & bases, `SCI_CONC_TO_PH` via course integer-rounding method, `PH_TO_CONC` from constructible log values, `PH_POH_CONV`, and `PERCENT_WV_TO_PH` via `c = 10 × % / M`, tolerance `0.1`).
  - Implemented `generateCompositeLogRootQuestion` (`log10(k × 10^n)`, `log10(a × b)`, `√(a × 10^n)`).
  - Implemented `generateCubesPowersQuestion` (`n^3` configurable 1..15, `2^1..10`, `3^1..6`, `5^1..4`, and `∛(n^3)`).
  - Implemented `attachMcqOptions` and 2×2 Multiple Choice UI (`GameScreen`) with options spaced `>= 3 × tolerance` apart.
  - Extended `RevisionScreen` with tabs for Powers (Squares + `(10+b)^2` trick, Cubes, Small Powers), Course Values (Memory Tables), pH & Logs (Formulas, Worked Examples, Species Table), and Constants.
- **Phase 4 (Learning Aids — Section 4)**:
  - Added `Hint` button (`GameScreen`) revealing `solutionSteps` during a question and recording `hintUsed = true`.
  - Added solution steps card on wrong answer / timeout with manual `Next Question` button when `requireManualNextOnWrong` is enabled in Settings.
  - Wired `repository.getAllQuestionsFlow()` to compute `WeakSpotsSummary` (aggregating error rate, avg time, and last seen by `topicTag` and `questionText`), rendered the **Weak Spots Review** card and **Practice Weak Spots** button in `StatsScreen`, and added expandable per-session question history in `HistoryScreen`.
  - Implemented **Speed Round** mode (`GameMode.SPEED_ROUND`) with duration selector (1, 2, 3, or 5 minutes) and multi-select topic pool, single total countdown timer, instant next question, and results screen breakdown (score, accuracy, questions/minute, per-topic breakdown) saved with `sessionType = "speed_round"`.
- **Phase 5 (Constants Module — Section 5)**:
  - Completed `ConstantsSchema.kt` (loader/validator failing loudly in debug on duplicate IDs or blank required fields, skipping/logging in release) and `ConstantsData.kt` (how-to-add header comment + 3 clearly flagged `isSample = true` sample entries: `sample_coulomb_k`, `sample_gas_constant_r` with alternate `L·atm·mol⁻¹·K⁻¹` unit, and `sample_spring_k_ref`).
  - Wired Constants Revision tab (grouped by subject, filter chips, search box, rich symbol/unit rendering, expandable notes/alternates/confusables) and Constants Drill (`GameMode.CONSTANTS`) showing subject chip + descriptive name + symbol together, unit & confusable feedback after answering, subject filtering in setup, and weighting toward previously missed constants.
- **Phase 6 (Testing, Versioning, and Tag — Section 6)**:
  - Added comprehensive JUnit4 and Robolectric unit tests (`ExampleUnitTest.kt`, `ExampleRobolectricTest.kt`) covering:
    - Central `AnswerChecker` across all `AnswerSpec` variants and `TolerancePreset` presets (`STRICT`, `NORMAL`, `LENIENT`).
    - `×10ⁿ` input parser (`ScientificInputParser`) on valid and malformed inputs.
    - All existing and new generators (`MemoryTables`, `ScientificNotation`, `PhPoh`, `CompositeLogsRoots`, `CubesAndPowers`, `Constants`, `Percentage`, `NumberSeries`, `Ratio`, `ChainCalculation`, `FractionsPool`) with independent recomputation checks.
    - 2×2 Multiple Choice option spacing (`>= 3 × tolerance` between all option pairs).
    - `DateBucketUtils` across `UTC`, `Asia/Dhaka` (`+06:00`), and `America/Los_Angeles` (`-07:00`).
    - Non-destructive Room migration `MIGRATION_1_2` on a sample v1 SQLite database and v1/v2 JSON backup import/export.
  - Bumped `versionCode` to `4` and `versionName` to `"1.1.0"`, updated `CHANGELOG.md`, and tagged final commit `v1.1.0`.

## Deferred Items
- **Real Constants Dataset**: As specified in the prompt and Section 5.5 of `ZENMATH_FEATURE_PLAN.md`, the full constants list will be provided in a follow-up prompt. The app ships with the complete modular framework and the 3 clearly flagged sample entries (`isSample = true`) in `ConstantsData.kt`.

---

# Constants Import (HSC Physics & Chemistry Dataset)

## Step 0 — Recon Findings
- **Existing Framework (`feature/next-release`)**:
  - `ConstantsSchema.kt` and `ConstantsData.kt` currently hold the v1.1.0 modular schema (`ConstantEntry`, `ConstantSubject`, `StructuredUnit`, `ConstantVariant`, `ConstantsValidator`) and 3 `isSample = true` sample entries.
  - `RichTextHelper.kt` provides `formatSymbol`, `formatStructuredUnit`, `formatScientific`, and `formatFormula` using Compose `AnnotatedString` with `BaselineShift.Superscript` / `Subscript`, but currently only handles single-character subscripts after `_` and pre-structured `StructuredUnit` tokens rather than arbitrary raw unit strings from JSON.
  - `RevisionScreen.kt` has a `Constants` tab with basic subject filter chips (`ALL`, `PHYSICS`, `CHEMISTRY`, `GENERAL`), search bar, and expandable cards.
  - `QuestionGenerator.kt` (`generateConstantsQuestion`) and `GameScreen.kt` / `GameSetupScreen.kt` support basic `GameMode.CONSTANTS` drills with subject filter chips, `AnswerSpec.Scientific` / `ApproximateDouble`, and post-answer unit/confusable feedback.
- **Dataset Inspection (`docs/.agents/hsc_physics_chemistry_constants.json`)**:
  - `metadata` (version `1.0.0`, generated `2026-10-26`, `symbol_notes`, `limitations`) and `sources` (5 entries).
  - `syllabus_map`: `physics.first_paper` (10 chapters), `physics.second_paper` (11 chapters), `chemistry.first_paper` (5 chapters), `chemistry.second_paper` (5 chapters) — 31 chapters total, each with `chapter`, `id`, `name_en`, and Bangla `name_bn`.
  - `constants`: 61 entries across 21 distinct `constant_type` strings.
  - `mathematical_constants_used_in_physics_and_chemistry`: 4 entries (`pi`, `euler_number`, `natural_log_2`, `natural_log_10`).
  - `condition_dependent_reference_values`: 12 entries.
  - `common_hsc_atomic_masses`: 39 entries (`atomic_number`, `symbol`, `element`, `school_atomic_mass`, `unit`, `modern_CIAAW_value`).
  - `unit_conversions`: 14 entries (13 numeric `from`/`to`/`factor` sets + 1 formula-only `temperature` entry).
  - `derived_formula_reference`: 14 entries (`id`, `name`, `expression`, `value`, `unit`, `notes`).
  - `chapter_constant_map`: 31 entries mapping chapters to `constant_ids`, plus `chapter_map_aliases` (`pi`, `mathematical_constants`, `common_hsc_atomic_masses`, `temperature`, `calorie_to_joule`).
  - `not_fixed_constants_or_condition_dependent_tables`: 8 entries explaining why spring constant `k`, resistivity, `Kc`/`Kp`, etc. are not fixed constants.

## Build Order
1. **Step 0 — Recon**: Document recon findings, build order, resolved ambiguities, and suspicious dataset entries in `DEV_NOTES.md`.
2. **Phase 1 — Assets, Loader, Validation & Overrides**:
   - Copy `hsc_physics_chemistry_constants.json` unchanged into `app/src/main/assets/hsc_physics_chemistry_constants.json`.
   - Add `app/src/main/assets/constants_overrides.json` with a top how-to comment and per-id override support (`courseMantissa`, `courseExponent`, `tolerance`, `enabled`, `displaySymbol`, `extraConfusableIds`).
   - Remove the 3 sample entries from `ConstantsData.kt`.
   - Implement background-thread loading (`ConstantsRepository` / `ConstantsData`) with in-memory caching, debug-loud / release-skipping validation, chapter inversion (with `"General physics"` fallback for the 4 unmapped constants), 3-tier reliability classification, course value extraction, and automatic + overridden confusable resolution.
3. **Phase 2 — Rich-Text Normalization (Symbols & 85+ Units)**:
   - Normalize all symbols (short display symbol before first `=` or `(` for rows/drills; full symbol for detail view; multi-character subscripts like `N_A`, `v_esc,⊕`, `t_(1/2)`, `V_m°`, `m_e c²`, Unicode subscripts/superscripts) and all 85+ distinct unit strings (`m^3 kg^-1 s^-2`, `MeV/c^2`, `(mol L^-1)^2`, `cal_th`, `°C`, `°F`, `Å`, `Ω m`, `dimensionless`/`1` -> `"no unit"`) with zero raw `^` or `_` left on screen.
4. **Phase 3 — Revision Tabs & Bangla Verification**:
   - Update `RevisionScreen.kt` with Paper (`Physics 1st`, `Physics 2nd`, `Chemistry 1st`, `Chemistry 2nd`) and Chapter (`name_en` + Bangla `name_bn`) filters, reliability class chips, search, Constants / Reference Values / Math Constants list with expandable details, Atomic Masses table (39), Unit Conversions table (14), Derived Formulas list (14), `"About constants"` info card (`symbol_notes` + `not_fixed_constants_or_condition_dependent_tables`), and `sources`/`metadata` footer.
5. **Phase 4 — Constants Drill Upgrade**:
   - Upgrade `GameSetupScreen.kt`, `QuestionGenerator.kt`, `AnswerChecker.kt`, and `GameScreen.kt` for Constants drill: Paper/Chapter filters, reliability class toggles (all 3 on by default), `"Include alternate unit systems"` toggle (off by default; unit shown under name when enabled, hidden when SI-only), dual-key answer checking (accepting either precise value or course value within tolerance, with `0.01` absolute tolerance for zero-valued entries), post-answer feedback card (precise value, course value, unit, chapters, notes, confusables), and missed-item weighting with topic tag grouped under `"constants"`.
6. **Phase 5 — Extra Drills**:
   - Add drill support for Atomic Masses (symbol -> mass, element -> mass, tolerance `0.05`), Unit Conversions (forward and reverse numeric conversions, tolerance `1%`, skipping formula-only `temperature`), Math Constants (`pi`, `e`, `ln 2`, `ln 10` accepting any listed approximation including `22/7`), and Reference Values (12 condition-dependent entries).
7. **Phase 6 — Section 8 Unit Tests, Device Verification, Version Bump & Tag**:
   - Add Section 8 unit tests against the real JSON file, verify Bangla chapter rendering on device/emulator, bump version (`versionCode = 5`, `versionName = "1.2.0"`), update `CHANGELOG.md` and `DEV_NOTES.md`, commit, and tag `v1.2.0`.

## Ambiguities & Decisions Made
- **Primary Variant Rule**: The first `values` entry whose `system` starts with `"SI"`, otherwise `values.first()`. For the 6 constants whose first entry is non-SI (`fine_structure_constant`, `molar_volume_stp_100kpa`, `molar_volume_stp_1atm`, `ionic_product_water_25C`, `standard_hydrogen_electrode_potential`, `vacuum_refractive_index`), `values.first()` is treated as primary per Section 3 (`22.71095464 L mol^-1` and `22.41396954 L mol^-1` for the two molar volumes, with SI `m^3 mol^-1` treated as an alternate unit variant).
- **Course Value Extraction**: Precedence is (1) `constants_overrides.json`, (2) numeric approximation parsed from `common_hsc_approximation` when its unit/scale matches the variant's unit (including `1/137` -> `0.00729927...` for `fine_structure_constant`), (3) fallback to the precise value rounded to 4 significant figures (`BigDecimal(precise).round(MathContext(4, RoundingMode.HALF_UP))`). When `common_hsc_approximation` states an approximation in an alternate unit (e.g. `13.6 eV` for `rydberg_energy` whose primary variant is in `J`), the primary `J` variant uses the 4-sig-fig SI course value (`2.180 × 10⁻¹⁸ J`) while the `eV` alternate variant uses `13.6 eV`.
- **Automatic Confusable Grouping**: Constants are grouped into confusable sets by normalized base symbol root (e.g. `g`/`g₀`/`g_Moon`, `e`/`e/m_e`/`euler_number`, `n₀` pair, `R`/`R_H`/`R∞`/`R_⊕`/`R_☉`, `k_e`/`k_B`, `σ`, `μ₀`/`μ_N`/`μ_B`, `m_e`/`m_p`/`m_n`, `L_f`/`L_v`/`L_☉`, `M_⊕`/`M_☉`, `V_m`/`V_m°`, `T₀`/`T_room`, `c`/`c_water`, `ρ_*`, `γ` pair, `E_R`/`E°`) merged with `extraConfusableIds` from `constants_overrides.json`.

## Suspicious Entries in `hsc_physics_chemistry_constants.json` (Left Untouched in JSON)
1. **`rydberg_constant_infinity` (`R∞`)**: `common_hsc_approximation` is written as `"R_H ≈ 1.097 × 10^7 m^-1"`, using symbol `R_H` instead of `R∞` (even though `rydberg_constant_hydrogen` is a separate entry with symbol `R_H`).
2. **`water_absolute_zero`**: Placed in `condition_dependent_reference_values` with name `"Absolute zero"`, id `"water_absolute_zero"`, and symbol `"0 K"` (a value+unit string rather than a symbol, and not water-specific).
3. **Duplicate Symbols**:
   - `heat_capacity_ratio_monatomic_ideal_gas` and `heat_capacity_ratio_diatomic_ideal_gas` share the exact symbol `"γ = C_p/C_v"`.
   - `loschmidt_constant_100kpa` and `loschmidt_constant_1atm` share the exact symbol `"n₀"`.
4. **`vacuum_permeability` (`μ₀`)**: Contains two SI `H m^-1` entries in `values` — `"SI (CODATA 2022)"` (`1.25663706127e-06`) and `"school approximation"` (`1.2566370614359173e-06`, which is `4π × 10^-7` to 17 digits).
5. **Non-SI `common_hsc_approximation` on SI-Primary Constants**: `rydberg_energy` (`"13.6 eV"` vs primary `J`), `electron_rest_energy` (`"0.511 MeV"` vs primary `J`), `proton_rest_energy` (`"938 MeV"` vs primary `J`), `neutron_rest_energy` (`"939.6 MeV"` vs primary `J`), `earth_escape_speed_reference` (`"11.2 km/s"` vs primary `m s^-1`), and `standard_atmosphere` (`"1 atm = 760 mmHg = 101.325 kPa"` vs primary `101325 Pa`). Handled in the loader by matching approximation units to variant units and falling back to 4-sig-fig rounding for the SI primary variant.
6. **`common_hsc_atomic_masses` (`Ni` vs `Co` and `modern_CIAAW_value` formats)**: Both Nickel (`Ni`, `Z=28`, CIAAW `about 58.6934`) and Cobalt (`Co`, `Z=27`, CIAAW `about 58.933194`) list `school_atomic_mass = 59.0`. Additionally, `modern_CIAAW_value` mixes bracketed intervals (`"[1.00784, 1.00811]"`), parenthesized uncertainties (`"4.002602(2)"`), and prose (`"about 22.98976928"`), so it is kept as a display-only string.

### Phase 1 Completed — Loader, Validation & Overrides
- Copied `hsc_physics_chemistry_constants.json` unchanged into `app/src/main/assets/hsc_physics_chemistry_constants.json` and added `app/src/main/assets/constants_overrides.json` with a top-of-file `_comment` how-to guide.
- Removed the 3 hardcoded sample entries from `ConstantsData.kt` and wired background preload (`Dispatchers.IO`) in `MainActivity.kt` and `ZenMathViewModel.kt`.
- Implemented `SimpleJson.kt` (RFC 8259 parser) and `ConstantsLoader.getDataset()` in `ConstantsSchema.kt` with debug-loud / release-skipping validation, inverted chapter mapping (`General physics` fallback for the 4 unmapped entries), 3-tier reliability classification, course value extraction, and automatic + overridden confusables.

### Phase 2 Completed — Rich-Text Symbol & Unit Normalization
- Extended `RichTextHelper.kt` (`formatSymbol`, `formatRawSymbol`, `formatUnit`, `formatRawUnit`, `formatMathText`) to normalize all symbols (short form before `=` or `(` for list rows and drills vs full form for detail views) and all 85+ distinct unit strings across the dataset (`m^3 kg^-1 s^-2` -> `m³·kg⁻¹·s⁻²`, `(mol L^-1)^2` -> `(mol·L⁻¹)²`, `cal_th`, `MeV/c^2`, `°C`, `°F`, `Å`, `Ω m`, and `"dimensionless"`/`"1"` -> `"no unit"`).
- Added `testSymbolAndUnitNormalizationAcrossEntireDataset` in `ExampleUnitTest.kt` verifying that zero raw `^` or `_` characters remain in any rendered symbol, unit, conversion factor, or formula.

### Phase 3 Completed — Revision Tabs
- Built comprehensive Constants Revision Tab with 7 specialized sub-tabs:
  1. **Constants (61)**: Filterable by Paper (`All`, `Physics 1st`, `Physics 2nd`, `Chemistry 1st`, `Chemistry 2nd`, `General physics`), Chapter (bilingual English + Bangla names), and Reliability classes (`Fundamental`, `Conventions`, `Reference & Approx`). Cards show short symbol badge, precise value, course value with common HSC approximation, full symbol, definition, formulas, alternate unit values, notes, and confusable warnings.
  2. **Condition-Dependent Reference Values (12)**: Shows reference values with condition notes, paper/chapter mapping, and course forms.
  3. **Mathematical Constants (4)**: `π`, `e`, `ln 2`, `ln 10` with standard approximations (`22/7`, etc.) and curriculum applications.
  4. **Atomic Masses (39)**: Element symbol, name, school mass, and modern CIAAW value text.
  5. **Unit Conversions (14)**: Numeric conversion factors and temperature formulas.
  6. **Derived Formulas (14)**: 14 essential read-only formulas with descriptions and chapter mappings.
  7. **About Constants & Sources**: Disambiguation notes, the 8 tables explaining why certain physical quantities are not fixed constants, and dataset attribution footer.

### Phase 4 Completed — Constants Drill Upgrade
- Upgraded `GameSetupScreen.kt` with curriculum paper multi-select chips (`Physics 1st`, `Physics 2nd`, `Chemistry 1st`, `Chemistry 2nd`), bilingual chapter filter (`nameEn • nameBn`), reliability class toggles (`Fundamental`, `Conventions`, `Reference & Approx` — all on by default), and "Include alternate unit systems" toggle (default off).
- Updated `QuestionGenerator.kt`:
  - Alternate unit systems: SI-only questions keep unit hidden during question presentation; alternate unit questions display target unit (e.g. `[L·atm·mol⁻¹·K⁻¹]`) below the question title.
  - Dual-key answer acceptance: answers within tolerance of either the precise value OR the course value pass.
  - Zero-valued entries (`standard_hydrogen_electrode_potential` 0 V and `water_absolute_zero` 0 K) accept answers within `0.01` absolute tolerance in `AnswerChecker.kt`.
  - `g = 10` rejected at 1% tolerance (~1.9% away from 9.81 m/s²).
- Upgraded `GameScreen.kt`:
  - Question card renders subject chip (`"Physics 2nd, Static Electricity"`), short symbol in 36sp, full question title, and target unit (when alternate unit variant selected).
  - Feedback card displays precise value, course value, unit, chapters, notes, and amber "Do not confuse with" confusable list after answering.
  - Weighting prioritizes previously missed constants using constant ID under `"constants"` topic tag.

### Phase 5 Completed — Extra Drills
- Added 4 new drill modes to `GameModels.kt`:
  1. `ATOMIC_MASSES`: 39 elements, tests symbol -> school mass and element name -> school mass with `0.05` tolerance.
  2. `UNIT_CONVERSIONS`: 13 numeric factor groups, forward and reverse conversions (`1 cal = ? J`, `1 atm = ? kPa`) within 1% tolerance.
  3. `MATH_CONSTANTS`: 4 constants (`π`, `e`, `ln 2`, `ln 10`), accepting listed approximations including fraction `22/7` parsed via `ScientificInputParser`.
  4. `REFERENCE_VALUES`: 12 condition-dependent reference values running through the constants engine.
- Exposed all 4 drill modes in `MainMenuScreen.kt` (completing a 21-item 3-column grid) and added "Practice Drill ⚡" quick launch buttons directly on each corresponding sub-tab in `RevisionScreen.kt`.

### Phase 6 Completed — Validation, Section 8 Tests, Version Bump & Tag
- Added full Section 8 test suite in `ExampleUnitTest.kt`:
  - `testDatasetExactCountsIntegrityAndNoDuplicateIds`: verifies 61 constants, 4 math constants, 12 reference values, 39 atomic masses, 14 unit conversions, 14 derived formulas, 8 not-fixed tables, zero duplicate IDs across the entire dataset, and complete syllabus chapter resolution (`unmappedIds` fallback to `General physics`).
  - `testParsableApproximationsWithinOnePercentOfPreciseValue`: confirms all parsable approximations in `constants` are within 1.0% relative tolerance of their precise values.
  - `testPrimaryVariantRuleAndSkippedTrivialAlternates`: tests the 6 non-SI first constants, verifies dimensionless formatting (`"no unit"`), and asserts trivial alternates (`1 u`, `1 eV`, `1 au`) are skipped.
  - `testZeroValuedEntriesTolerance`: verifies `standard_hydrogen_electrode_potential` (0 V) and `water_absolute_zero` (0 K) are handled with `0.01` absolute tolerance in `AnswerChecker`.
  - `testDualKeyAnswerCheckingAndRejectionOfG10`: tests dual-key acceptance for `standard_acceleration_gravity` (accepting `9.80665`, `9.8`, `9.81`) and rejects `10.0` at 1% tolerance.
  - `testOverridesPrecedence`: verifies `override > extracted approximation > 4-sig-fig rounded fallback`.
  - `testExtraDrillsGeneratorsAndChecking`: tests problem generation and answer validation across Atomic Masses (tolerance `0.05`), Unit Conversions (tolerance `1%`), Math Constants (accepting `22/7` for `π`), and Reference Values.
- Ran full test suite (`./gradlew testDebugUnitTest`): 24 tests passed, 0 failures.
- Built debug APK (`./gradlew assembleDebug`): verified APK builds cleanly without errors.
- Bumped version in `app/build.gradle.kts`: `versionCode = 5`, `versionName = "1.2.0"`.
- Updated `CHANGELOG.md` with release notes for `1.2.0`.

---

# Targeted Revisions & Drill Fixes (v1.2.1)

### 1. Revision & Drill HSC Course Values
- In `RevisionScreen.kt`, updated `ConstantCard` so the unexpanded card row displays `courseRich` (`courseMantissa` & `courseExponent`) as primary rather than `preciseRich`, keeping units formatted with `RichTextHelper.formatUnit(entry.unit)`. The expanded card shows `Precise value: <preciseRich> <unit>` alongside textbook approximations.
- In `QuestionGenerator.kt`, decomposed `targetCourseVal` into `targetMantissa` and `targetExponent` in `generateConstantQuestion` and `generateReferenceValueQuestion` so `MathProblem.answer` and `AnswerSpec.ScientificValue` expect the HSC course value that students learn, while `alsoAcceptDoubles` keeps the precise value accepted as a secondary key.

### 2. Remove "Practice Drill ⚡" Button from Revision
- Removed `practice_drill_btn` container row and `onStartDrill` callback from `RevisionScreen.kt` and `ZenMathApp.kt`.
- Cleaned up unused `GameMode` import in `RevisionScreen.kt` and let the revision list reflow cleanly into the freed space. Drills remain fully accessible from the Main Menu grid.

### 3. Wrong Answer Single Tap & Visible Next Button
- Fixed root cause of double tap bug: wrong answers were either gated behind `settingsFlow.value.requireManualNextOnWrong` (which fell back to an 800ms delayed auto-advance where inputs were swallowed) or the Next button was placed below the multi-line Solution Steps card inside an unscrollable container, pushing it off-screen behind the keypad.
- Moved `next_question_btn` directly below the Answer/Feedback display, added `Modifier.verticalScroll(rememberScrollState())` to the question column, and ensured wrong answers always activate manual next so one tap on Next (or pressing Enter) immediately advances to the next question.
- Verified single-increment behavior: score and streak updates only occur once in `submitAnswer` and cannot double count.
- Fixed sub-nanoscale bug in `AnswerChecker.isWithinRelativeTolerance` where `expectedVal <= 1e-9` was erroneously treated as `0.0`.

### 4. Option Toggles UI Fixes
- In `GameSetupScreen.kt`, updated `ToggleCardRow` to give the title/subtitle Column `Modifier.weight(1f)` with wrapping and placed `ZenToggle` inside a fixed `Modifier.size(48.dp)` container with full-card clickable area.
- Reviewed and updated all option toggles across `GameSetupScreen.kt` and `SettingsScreen.kt` (mixed operations, memory categories, reliability classes, chapter filters, speed round topics) with non-shrinkable 48dp touch targets.

### 5. Verification & Version Bump
- Ran full test suite (`./gradlew testDebugUnitTest`): 25 tests passed, 0 failures.
- Built debug APK (`./gradlew assembleDebug`): verified APK builds cleanly.
- Bumped version in `app/build.gradle.kts`: `versionCode = 6`, `versionName = "1.2.1"`.
- Updated `CHANGELOG.md` with release notes for `1.2.1`.
