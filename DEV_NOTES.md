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
  - Next step: Phase 3 (New Content: Revision Tables and Drills).
