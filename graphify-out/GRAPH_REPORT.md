# Graph Report - ZenMath_Android  (2026-11-09)

## Corpus Check
- 87 files · ~108,738 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 19 file(s) not represented in the graph (top: .xml 11, (none) 2, .properties 2)

## Summary
- 918 nodes · 2419 edges · 67 communities (28 shown, 39 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS · INFERRED: 5 edges (avg confidence: 0.87)
- Token cost: 12,000 input · 2,500 output

## Community Hubs (Navigation)
- Database Entities & Room Schema
- Answer Checking & Math Evaluation
- Game Modes & Core Drills
- Course Values & Memory Tables
- SimpleJson RFC8259 Parser
- Revision Screen & Subtabs
- ZenMath ViewModel & State Operations
- Constants Data Loader & Overrides
- RichText Math Formatting
- User Preferences & Daily Goals
- Constants Schema & Dataset Types
- Custom Math Keypad
- Session Lifecycle & Adaptive Difficulty
- Robolectric Unit Tests
- Special Menu & Secondary Navigation
- Reminder Receiver & Notification Alarms
- Game Models & Statistics Types
- Notification Scheduler
- Common UI Components & Progress
- Main Activity Lifecycle
- Room Database Configuration
- Navbar Tabs & Navigation State
- App Navigation Screens
- Constants Subject Classification
- Game Setup Range Selectors
- Compose Theme & Palette
- Constants Revision Navigation
- Revision Main Tabs
- Instrumented Android Tests
- Symbol Parsing & Confusable Grouping
- pH Drill Subtypes
- Composite Logs & Roots Subtypes
- Cubes & Powers Subtypes
- Constants Reliability Classification
- Structured Unit Representation
- Results Cards & Performance Display
- ZenMath Color Palette
- Gameplay Feedback States
- Room Database Migration V1-V2
- Design Rationale & Notes
- Zen Math Feature Requirements
- Chart Metrics Enum
- Constants Release 1.2.0 History
- Release 1.2.1 Fixes
- App Icon Asset
- Notification Icon Asset
- Status Notification Icon Asset
- Task Icon Asset
- Launcher Icon Asset
- Launcher Foreground Asset
- Launcher Round Icon Asset
- Constants Addition Rules
- Release CI/CD Workflow

## God Nodes (most connected - your core abstractions)
1. `ZenMathViewModel` - 84 edges
2. `GameMode` - 44 edges
3. `SessionEntity` - 38 edges
4. `ConstantsLoader` - 32 edges
5. `MathProblem` - 31 edges
6. `QuestionGenerator` - 30 edges
7. `ConstantEntry` - 24 edges
8. `SimpleJsonObject` - 24 edges
9. `PreferencesManager` - 23 edges
10. `ZenMathRepository` - 23 edges

## Surprising Connections (you probably didn't know these)
- `ZenMath Release 1.2.1` --references--> `ZenMath Targeted Fixes Specification`  [INFERRED]
  CHANGELOG.md → docs/.agents/new_fix_prompt.md
- `HSC Physics & Chemistry Constants Import Spec` --conceptually_related_to--> `Constants Dataset Preload & Ingestion Pipeline`  [INFERRED]
  docs/.agents/ZENMATH_CONSTANTS_IMPORT.md → DEV_NOTES.md
- `ConstantsData` --references--> `ChapterRef`  [EXTRACTED]
  app/src/main/java/com/example/data/ConstantsData.kt → app/src/main/java/com/example/data/ConstantsSchema.kt
- `ConstantsData` --references--> `ConstantEntry`  [EXTRACTED]
  app/src/main/java/com/example/data/ConstantsData.kt → app/src/main/java/com/example/data/ConstantsSchema.kt
- `GameSetupScreen()` --references--> `HscPaper`  [EXTRACTED]
  app/src/main/java/com/example/ui/screens/GameSetupScreen.kt → app/src/main/java/com/example/data/ConstantsSchema.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **ZenMath Iconography & Visual Notification Assets** — app_src_main_res_drawable_hdpi_ic_notification_asset, app_src_main_res_drawable_hdpi_ic_stat_notify_asset, app_src_main_res_drawable_app_icon_asset, app_src_main_res_drawable_ic_task_icon_asset, app_src_main_res_mipmap_hdpi_ic_launcher_asset, app_src_main_res_mipmap_hdpi_ic_launcher_foreground_asset [EXTRACTED 1.00]

## Communities (67 total, 39 thin omitted)

### Community 0 - "Database Entities & Room Schema"
Cohesion: 0.06
Nodes (16): QuestionRecordEntity, SessionEntity, ZenMathDao, ModeAccumulator, ZenMathRepository, DateBucketUtils, DayBucket, RecentPerformanceDay (+8 more)

### Community 1 - "Answer Checking & Math Evaluation"
Cohesion: 0.08
Nodes (17): AbsoluteTolerance, AnswerChecker, AnswerSpec, Choice, Exact, FractionSpec, RelativeTolerance, ScientificInputParser (+9 more)

### Community 2 - "Game Modes & Core Drills"
Cohesion: 0.06
Nodes (35): GameMode, ADDITION, APPROXIMATION, ATOMIC_MASSES, CHAIN_CALCULATION, COMPOSITE_LOGS_ROOTS, CONSTANTS, CUBES_AND_POWERS (+27 more)

### Community 3 - "Course Values & Memory Tables"
Cohesion: 0.08
Nodes (16): CourseValuesData, MemoryTableCategory, CUBE_ROOT, LOG_10, RECIPROCAL, SQUARE_ROOT, MemoryTableEntry, ChemicalSpecies (+8 more)

### Community 4 - "SimpleJson RFC8259 Parser"
Cohesion: 0.10
Nodes (3): SimpleJsonArray, SimpleJsonObject, SimpleJsonTokenizer

### Community 5 - "Revision Screen & Subtabs"
Cohesion: 0.14
Nodes (14): AboutAndSourcesSubTab(), AtomicMassesSubTab(), ConstantsRevisionTab(), DatasetAttributionFooter(), DerivedFormulasSubTab(), FilterChipBox(), FractionsTab(), MathConstantsSubTab() (+6 more)

### Community 7 - "Constants Data Loader & Overrides"
Cohesion: 0.15
Nodes (11): AlternateConstantValue, ChapterRef, ConstantEntry, ConstantOverride, ConstantsLoader, HscPaper, CHEMISTRY_1ST, CHEMISTRY_2ND (+3 more)

### Community 8 - "RichText Math Formatting"
Cohesion: 0.17
Nodes (3): RichTextHelper, GameScreen(), ConstantCard()

### Community 10 - "User Preferences & Daily Goals"
Cohesion: 0.14
Nodes (6): PreferencesManager, TolerancePreset, LENIENT, NORMAL, STRICT, GameSettings

### Community 11 - "Constants Schema & Dataset Types"
Cohesion: 0.20
Nodes (10): ConstantsData, AtomicMassEntry, ConstantsDataset, ConstantsMetadata, ConstantSourceEntry, ConversionValueItem, DerivedFormulaEntry, MathConstantEntry (+2 more)

### Community 21 - "Special Menu & Secondary Navigation"
Cohesion: 0.14
Nodes (3): HistoryScreen(), SpecialMenuScreen(), ZenMathApp()

### Community 23 - "Game Models & Statistics Types"
Cohesion: 0.13
Nodes (14): DrillDirection, FORWARD, MIXED, REVERSE, ModeStats, MonotonicClock, SciNotationSubtype, ALL (+6 more)

### Community 24 - "Notification Scheduler"
Cohesion: 0.23
Nodes (4): NotificationScheduler, formatTimeChipLabel(), SectionHeader(), SettingsScreen()

### Community 25 - "Common UI Components & Progress"
Cohesion: 0.19
Nodes (4): ZenProgressBar(), ZenSlider(), ZenToggle(), ZenTheme

### Community 29 - "Navbar Tabs & Navigation State"
Cohesion: 0.18
Nodes (7): MainTab, HISTORY, PRACTICE, REVISION, SETTINGS, STATS, ZenNavbar()

### Community 30 - "App Navigation Screens"
Cohesion: 0.20
Nodes (10): ZenScreen, HISTORY, MENU, PLAYING, RESULTS, REVISION, SETTINGS, SETUP (+2 more)

### Community 31 - "Constants Subject Classification"
Cohesion: 0.22
Nodes (9): ConstantSubject, CHEMISTRY, CURRENT_ELECTRICITY, ELECTROSTATICS, MAGNETISM, MECHANICS, MODERN_PHYSICS, THERMODYNAMICS (+1 more)

### Community 32 - "Game Setup Range Selectors"
Cohesion: 0.31
Nodes (4): CustomRangeEditor(), GameSetupScreen(), SectionHeader(), ToggleCardRow()

### Community 34 - "Constants Revision Navigation"
Cohesion: 0.25
Nodes (8): ConstantsSubTab, ABOUT_AND_SOURCES, ATOMIC_MASSES, CONSTANTS, DERIVED_FORMULAS, MATH_CONSTANTS, REFERENCE_VALUES, UNIT_CONVERSIONS

### Community 35 - "Revision Main Tabs"
Cohesion: 0.25
Nodes (7): RevisionTab, CONSTANTS, FRACTIONS, MEMORY, PH, SQUARES, TABLES

### Community 40 - "pH Drill Subtypes"
Cohesion: 0.29
Nodes (7): PhDrillSubtype, ALL, CONC_TO_PH, PERCENT_WV_TO_PH, PH_POH_CONV, PH_TO_CONC, SCI_CONC_TO_PH

### Community 41 - "Composite Logs & Roots Subtypes"
Cohesion: 0.33
Nodes (5): CompositeLogRootSubtype, ALL, LOG_PRODUCT, LOG_SCI, ROOT_SCI

### Community 42 - "Cubes & Powers Subtypes"
Cohesion: 0.33
Nodes (5): CubesPowersSubtype, ALL, CUBES, PERFECT_CUBE_ROOTS, POWERS

### Community 43 - "Constants Reliability Classification"
Cohesion: 0.40
Nodes (4): ReliabilityClass, CONVENTIONS, FUNDAMENTAL, REFERENCE_AND_APPROXIMATE

### Community 47 - "Gameplay Feedback States"
Cohesion: 0.40
Nodes (5): FeedbackType, CORRECT, INCORRECT, NONE, TIMEOUT

### Community 49 - "Design Rationale & Notes"
Cohesion: 0.50
Nodes (3): ZenMath Architecture & Design Decisions, Constants Dataset Preload & Ingestion Pipeline, HSC Physics & Chemistry Constants Import Spec

### Community 50 - "Zen Math Feature Requirements"
Cohesion: 0.50
Nodes (4): Zen Math Change & Feature Prompt Spec, Part A: Minor Drill Changes & Reorganization, Part B: Post-Session Stats Window Improvements, Part C: Statistics Export for AI and Humans

### Community 52 - "Chart Metrics Enum"
Cohesion: 0.67
Nodes (3): ChartMetric, QUESTIONS, SESSIONS

## Knowledge Gaps
- **129 isolated node(s):** `MECHANICS`, `THERMODYNAMICS`, `ELECTROSTATICS`, `CURRENT_ELECTRICITY`, `MAGNETISM` (+124 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 328 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **39 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ZenMathViewModel` connect `ZenMath ViewModel & State Operations` to `Database Entities & Room Schema`, `Answer Checking & Math Evaluation`, `Game Modes & Core Drills`, `Course Values & Memory Tables`, `Composite Logs & Roots Subtypes`, `User Preferences & Daily Goals`, `Cubes & Powers Subtypes`, `Gameplay Feedback States`, `Session Lifecycle & Adaptive Difficulty`, `Special Menu & Secondary Navigation`, `Game Models & Statistics Types`, `Notification Scheduler`, `ViewModel Subtypes & Directions`, `Room Database Configuration`, `Navbar Tabs & Navigation State`, `App Navigation Screens`?**
  _High betweenness centrality (0.150) - this node is a cross-community bridge._
- **What connects `MECHANICS`, `THERMODYNAMICS`, `ELECTROSTATICS` to the rest of the system?**
  _129 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Database Entities & Room Schema` be split into smaller, more focused modules?**
  _Cohesion score 0.05742296918767507 - nodes in this community are weakly interconnected._
- **Why does `GameMode` connect `Game Modes & Core Drills` to `Game Setup Range Selectors`, `JVM Unit Tests`, `ZenMath ViewModel & State Operations`, `RichText Math Formatting`, `Game Screen & Session UI`, `Game Setup & Drill Selection`, `Main Menu & Typography`, `Special Menu & Secondary Navigation`, `Game Models & Statistics Types`, `ViewModel Subtypes & Directions`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Should `Answer Checking & Math Evaluation` be split into smaller, more focused modules?**
  _Cohesion score 0.07708521529659741 - nodes in this community are weakly interconnected._
- **Why does `SessionEntity` connect `Database Entities & Room Schema` to `JVM Unit Tests`, `ZenMath ViewModel & State Operations`, `Stats Components & Charts`, `Navigation Bar & Session History`, `Session Lifecycle & Adaptive Difficulty`, `Stats Screen & Data Backup`, `Special Menu & Secondary Navigation`, `ViewModel Subtypes & Directions`, `Room Database Configuration`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Should `Game Modes & Core Drills` be split into smaller, more focused modules?**
  _Cohesion score 0.05807200929152149 - nodes in this community are weakly interconnected._