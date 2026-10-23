# Changelog

All notable changes to ZenMath will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Extended `MathProblem` with `topicTag`, `answerSpec`, `unitText`, `subjectLabel`, and `solutionSteps`.
- Added central `AnswerChecker` supporting Exact, AbsoluteTolerance, RelativeTolerance, ScientificValue, Choice (MCQ), and FractionSpec, with Settings tolerance presets (Strict 0.5%, Normal 1%, Lenient 2%).
- Added `×10ⁿ` scientific exponent input key on `Keypad` and `ScientificInputParser` with cap raised to 20 characters.
- Added `RichTextHelper` using Compose `AnnotatedString` spans (`BaselineShift.Superscript` / `Subscript`) for scientific notation, formulas, and units.
- Added Room database v1 -> v2 migration (`MIGRATION_1_2`) with exported schemas (`app/schemas`), adding `questionText`, `topicTag`, `hintUsed` to `questions` and `localDate`, `sessionType` to `sessions`.
- Added `DateBucketUtils` for timezone-aware local calendar day bucketing and `MonotonicClock` for millisecond-accurate timing.
- Added dedicated data files (`CourseValuesData.kt`, `PhSpeciesData.kt`, `ConstantsSchema.kt`, `ConstantsData.kt`).

## [1.0.5] - 2026-10-23
### Changed
- Stabilized APK signing and removed unused network/telemetry dependencies for clean offline execution.
- Updated `applicationId` to `com.shihabulsakib.zenmath`.

## [1.0.4] - 2026-10-23
### Fixed
- Fixed scheduled daily reminders on Android 12+ and added custom reminder time slots.
