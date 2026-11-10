# Changelog

All notable changes to ZenMath will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.2] - 2026-11-10

### Added
- **Factorials (`1!` to `10!`) in Math Constants & Revision Course Values**:
  - Added factorials `1! = 1` through `10! = 3628800` to `CourseValuesData.FACTORIALS`.
  - Added the **Factorials (`1! – 10!`)** reference table inside the **Course Values** revision tab (`MemoryTablesTab` in `RevisionScreen.kt`) as well as under **Constants → Math Constants** (`MathConstantsSubTab`).
  - Added the **Factorials (`1! – 10!`)** drill (`QuestionGenerator.generateFactorialQuestion` and `MathConstantSubtype`: *All Math*, *π, e, ln*, and *Factorials 1!–10!*) inside the **Mathematics** category of the **Constants** drill (`GameMode.CONSTANTS`), with full 2×2 Multiple Choice and exact-answer checking support.

## [1.3.1] - 2026-11-10

### Changed
- **Tables & Factor Finding Multiplier Range (`1..12` → `1..10`)**:
  - Updated Multiplication Tables revision cards (`RevisionScreen.kt`) to display `n × 1` through `n × 10`.
  - Updated Multiplication Tables (`generateTableQuestion`) and Factor Finding (`generateFactorFindingQuestion`) drills (`QuestionGenerator.kt` and `DrillRegistry.kt`) so the second multiplier / missing factor is sampled in `1..10`.
  - Updated Custom Range default end value in `SpecialMenuScreen.kt` from `12` to `10`.

### Fixed
- **Bottom Hover Navbar Selection Synchronization**: Fixed `ZenMathViewModel.navigateTo` and `onQuitGame` so returning to the Main Menu (`ZenScreen.MENU`) after launching a practice session from Statistics (`MainTab.STATS`, e.g. Weak Spots / Mistake Review) properly synchronizes the active bottom navbar tab back to `MainTab.PRACTICE` (Home).

## [1.3.0] - 2026-11-10

### Added
- **AI-Agent Markdown & Human PDF Statistics Exports (Part C)**:
  - Added a top-right **Export** button and `ExportDialog` on the Statistics screen supporting **Markdown (`.md`)** *(for AI agents / LLMs)* and **PDF (`.pdf`)** *(for humans)* via Android Share Sheet (`FileProvider`) and SAF "Save File".
  - Added `StatsSnapshotBuilder` computing Wilson 95% confidence lower bounds, median/P90 response latencies, MCQ vs Typed accuracy, weekly/daily trends, mistake pattern classifications, and day-of-week × hour-of-day punch-card matrices.
  - Added `MarkdownReportRenderer` generating self-contained LLM coaching reports with YAML front matter, AI coaching instructions, scoring glossary, the complete `DrillRegistry` handbook, performance tables, and up to 250 item-level mistakes.
  - Added `PdfReportRenderer`, `PdfLayout`, and `ChartKit` using Android's native `PdfDocument` and `Canvas` to render multi-page A4 reports with KPI summary cards, a 26-week calendar heatmap, a 30-day volume & accuracy chart, a category donut chart, per-drill accuracy & Wilson 95% confidence bars, diagnostic insights, and a full Drill Guide.
- **Mistake Review Explorer & Tracked Practice Sessions (Part A5, B2, B3)**:
  - Added `MistakeReviewScreen` and `MistakeSelector` accessible from both the post-session Results screen (*Session Mistakes* scope) and Statistics Weak Spots Review (*All-Time Weak Spots* scope with `Unresolved` / `All Mistakes` filter toggles).
  - Grouped missed questions by `DrillCategory` → `GameMode` → individual question rows with user wrong answer, expected answer, miss frequency, expandable solution steps, and multi-select checkboxes.
  - Tracked mistake-practice runs as full Room sessions (`sessionType = "mistake_review"`, `mode = "mistake-review"`, `sourceSessionId`, `scopeLabel`) visible in History, Statistics, and Exports.
- **Database Schema v3 (`MIGRATION_2_3`) & `ProblemCodec`**:
  - Added lossless `MathProblem` / `AnswerSpec` JSON serialization (`ProblemCodec`) and Room v2 → v3 migration with SQL backfill of `drillMode` for historical Speed Round and Weak Spots records.

### Changed
- **Roots Merged into Squares Drill (Part A4)**: Expanded `GameMode.SQUARE` with `SquaresSubtype` (`ALL`, `SQUARES` $n^2$, `PERFECT_ROOTS` $\sqrt{n^2}$, `IRRATIONAL_ROOTS` $\sqrt{2}\text{–}\sqrt{10}$ with $\pm 0.05$ course-value tolerance) and 2×2 Multiple Choice support; removed the separate Roots tile from Home.
- **Math Constants Folded into Constants Drill (Part A3)**: Added `Mathematics (π, e, ln 2, ln 10)` as a selectable category inside `GameMode.CONSTANTS`, enabled `/` keypad input for `math-constant-*` questions, and removed the separate Math Constants Home tile.
- **Reference Values Removed (Part A1)**: Removed Reference Values from the Constants revision tab, Home screen, and active question generation while preserving historical session compatibility.
- **Settings Version Display**: Linked the Settings footer and Export metadata directly to `BuildConfig.VERSION_NAME` (`v1.3.0`).

### Fixed
- **Post-Session Results Scroll Overflow (Part B1)**: Made the `ResultsScreen` performance and full topic breakdown area vertically scrollable while keeping bottom action buttons (`VIEW MISTAKES (N)`, `RETRY SESSION`, `MENU`) pinned and always reachable.
- **Speed Round MCQ Fractions Distractors (Part A2)**: Fixed `QuestionGenerator.attachMcqOptions` so Decimal-to-Fraction questions generate 4 distinct reduced-fraction (`n/d`) options instead of integer distractors.

## [1.2.1] - 2026-10-28

### Fixed
- **Revision & Drill HSC Course Values**: Displayed HSC textbook course values (`courseMantissa` / `courseExponent` / `courseValue`) as primary on revision cards and in drills (`MathProblem.answer` and decomposition), while continuing to accept precise values as secondary keys.
- **Revision Instant Drill Button Removed**: Removed the practice drill shortcut button (`practice_drill_btn`) and container row from `RevisionScreen` to free vertical space and let the revision list reflow cleanly, while keeping drills accessible from the Main Menu.
- **Gameplay Next Question Flow**: Fixed wrong-answer flow where missing manual next button or coroutine delay caused input drops and required a double tap. The visible "Next Question" button is now positioned directly below the answer/feedback display, the central question container is scrollable (`Modifier.verticalScroll`), and tapping Next (or pressing Enter) advances to the next question with a single tap across all drill types without double-triggering streak or score updates.
- **Option Toggles Layout**: Fixed squeezed toggle controls in `GameSetupScreen` (e.g. "Include alternate unit systems" and "Multiple Choice") by assigning text descriptions `Modifier.weight(1f)` with wrapping and giving the toggle a fixed non-shrinkable 48dp touch target container.
- **Answer Checking Relative Tolerance**: Fixed `AnswerChecker.isWithinRelativeTolerance` treating constants `< 1e-9` (such as Planck's constant or electron mass) as 0.0.
- **Speed Round MCQ Option**: Re-enabled 2×2 Multiple Choice mode (`supportsMcq = true`) on Speed Round so players can toggle MCQ format in setup and answer rapid-fire questions via options grid.

## [1.2.0] - 2026-10-28

### Added
- Imported `hsc_physics_chemistry_constants.json` into `app/src/main/assets/` unchanged, with `constants_overrides.json` per-ID override support and pure-Kotlin RFC 8259 JSON parser (`SimpleJson.kt`).
- Removed placeholder sample entries from `ConstantsData.kt` and wired runtime background loading and caching (`ConstantsLoader`).
- Implemented syllabus chapter inversion (`General physics` fallback for unmapped constants), 3-tier reliability classification, course value extraction, and automatic confusable grouping across all 61 constants, 12 reference values, 4 math constants, 39 atomic masses, 14 unit conversion groups, 14 derived formulas, and 8 non-fixed tables.
- Enhanced `RichTextHelper` to normalize all short/full symbols and all 85+ distinct unit strings (`(mol L^-1)^2`, `cal_th`, `MeV/c^2`, `°C`, `°F`, `Å`, `Ω m`, and `"dimensionless"`/`"1"` -> `"no unit"`) with zero raw `^` or `_` characters remaining on screen.
- Expanded Constants Revision screen with 7 specialized sub-tabs: Constants (61), Reference Values (12), Math Constants (4), Atomic Masses (39), Unit Conversions (14), Derived Formulas (14), and About & Sources with bilingual chapter filters and reliability classes.
- Upgraded Constants Drill with paper multi-select filters (`Physics 1st`, `Physics 2nd`, `Chemistry 1st`, `Chemistry 2nd`), bilingual chapter filters, reliability toggles (`Fundamental`, `Conventions`, `Reference & Approx`), alternate unit systems toggle (`Include alternate unit systems`), dual-key answer acceptance (precise value or course value; `0.01` absolute tolerance for zero-valued entries; `g = 10` rejected at 1% tolerance), and post-answer solution card with confusable warnings.
- Added 4 extra drills from the dataset: Atomic Masses (39 elements, tolerance `0.05`), Unit Conversions (13 numeric factor groups, forward/reverse, tolerance `1%`), Math Constants (accepting listed approximations including fraction `22/7`), and Reference Values (12 condition-dependent values).
- Added quick "Practice Drill ⚡" launch buttons directly from each corresponding Revision sub-tab and added all 4 new drill modes to the Main Menu grid.
- Added comprehensive Section 8 test suite in `ExampleUnitTest.kt` covering exact dataset counts, no duplicate IDs, syllabus chapter resolution, parsable approximation tolerances, primary variant rules, skipped trivial alternates, zero-valued entries, overrides precedence, dual-key checking, and extra drill mechanics.

## [1.1.0] - 2026-10-24

### Added
- Extended `MathProblem` with `topicTag`, `answerSpec`, `unitText`, `subjectLabel`, and `solutionSteps`.
- Added central `AnswerChecker` supporting Exact, AbsoluteTolerance, RelativeTolerance, ScientificValue, Choice (MCQ), and FractionSpec, with Settings tolerance presets (Strict 0.5%, Normal 1%, Lenient 2%).
- Added `×10ⁿ` scientific exponent input key on `Keypad` and `ScientificInputParser` with cap raised to 20 characters.
- Added `RichTextHelper` using Compose `AnnotatedString` spans (`BaselineShift.Superscript` / `Subscript`) for scientific notation, formulas, and units.
- Added Room database v1 -> v2 migration (`MIGRATION_1_2`) with exported schemas (`app/schemas`), adding `questionText`, `topicTag`, `hintUsed` to `questions` and `localDate`, `sessionType` to `sessions`.
- Added `DateBucketUtils` for timezone-aware local calendar day bucketing and `MonotonicClock` for millisecond-accurate timing.
- Added dedicated data files (`CourseValuesData.kt`, `PhSpeciesData.kt`, `ConstantsSchema.kt`, `ConstantsData.kt`).
- Added validated custom range editors for Square and Square Root modes and extended Fraction denominators up to 15 with a "Course Values" toggle.
- Added Memory Tables drill (Forward, Reverse, Mixed) and Revision tab for Square Roots, Cube Roots, Base-10 Logs, and Reciprocals (`1/11..1/15`).
- Added Scientific Notation drill (Normalize, Decimal Shift, Multiply/Divide, and Square Roots of Powers of 10).
- Added pH & pOH drill (Concentration M/mM to pH, Scientific `[H+]` course rounding to pH, Reverse pH to `[H+]`, pH ↔ pOH, and `% w/v` of strong base to pH) and Revision tab with worked examples.
- Added Composite Logs & Roots (`log(k×10^n)`, `log(a×b)`, `√(a×10^n)`) and Cubes & Powers (`n^3` 1..15, `2^n/3^n/5^n`, `∛(n^3)`) drills and Revision sections.
- Added 4-option 2×2 Multiple Choice mode toggle for Approximation and new scientific/table drills with options spaced at least `3 × tolerance` apart.
- Added in-game `Hint` button and post-mistake `Solution Steps` card with configurable manual `Next Question` advancement.
- Added **Weak Spots Review** section in Statistics (worst topics and toughest individual questions) with a one-tap **Practice Weak Spots** mode, plus expandable question records in History.
- Added **Speed Round** mode (1, 2, 3, or 5 minutes with custom multi-select topic pool, total countdown timer, questions-per-minute metric, and per-topic breakdown).
- Added modular **Constants** framework (`ConstantsData.kt`, `ConstantsSchema.kt`, debug/release validation, Revision tab with subject filter & search, and Constants Drill with subject chip, symbol, unit, confusable warnings, and missed-constant weighting) shipping with 3 clearly flagged sample entries.

### Fixed
- Hid Difficulty slider on setup screens for modes that do not use difficulty.
- Differentiated Medium vs Hard generators in Percentage, Number Series, and Chain Calculation, and Easy vs Medium in Ratio (`A:B:C` three-term ratios).
- Reset `allowRemainder` and `allowNegativeResults` when switching modes and exposed them on the Mixed mode setup screen.
- Added downward step-down (`< 50%` accuracy) to Adaptive Difficulty.

## [1.0.5] - 2026-10-23
### Changed
- Stabilized APK signing and removed unused network/telemetry dependencies for clean offline execution.
- Updated `applicationId` to `com.shihabulsakib.zenmath`.

## [1.0.4] - 2026-10-23
### Fixed
- Fixed scheduled daily reminders on Android 12+ and added custom reminder time slots.
