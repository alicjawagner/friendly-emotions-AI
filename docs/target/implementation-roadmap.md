# Implementation Roadmap — Friendly Emotions

> **Role:** Senior Android Tech Lead  
> **Date:** 2026-07-17  
> **Target:** Production-ready Android application from an empty project

---

## Sources of Truth

| Source | File |
|---|---|
| Functional Specification | `friendly-emotions-functional-specification.md` |
| Target Domain | `target-domain.md` |
| Target Architecture | `target-architecture.md` |
| ADRs | `adr/ADR-001` through `adr/ADR-015` |
| Project Setup | `project-setup-specification.md` |
| UI Design | Figma (accessed via Figma MCP) |

---

## Development Conventions

- Every phase produces a **buildable, launchable, manually testable** increment.
- AI coding sessions operate within **one phase at a time**. Review generated code before proceeding.
- After each phase: build → install → manually test → verify Definition of Done.
- Figma is the **source of truth for all visual decisions** (layouts, colors, typography, spacing, component design, interactions). Use `get_design_context` before implementing any screen.
- Architecture rules from ADR-001 through ADR-015 apply from the first commit.

---

## Phase Overview

| # | Phase | Deliverable |
|---|---|---|
| 1 | Project Setup | Buildable 6-module project with two launcher icons |
| 2 | Domain Layer | All domain models, services, use case contracts |
| 3 | Data Layer | Room database, DAOs, seeding, repositories |
| 4 | Core UI Theme | Material3 theme, shared components |
| 5 | Child App — Home | Info splash + main screen with active step |
| 6 | Child App — Basic Session | Trial presentation and answer evaluation |
| 7 | Child App — Session Mechanics | Hints, TTS, error correction, reinforcement |
| 8 | Child App — Test Mode & End | Test mode, session end screen |
| 9 | Therapist App — Navigation Shell | Home screen and navigation skeleton |
| 10 | Therapist App — Material Browse | Emotion → Folder → Image read-only navigation |
| 11 | Therapist App — Material Management | Create/rename/delete folders; add/edit/delete images |
| 12 | Therapist App — Learning Step List | List, activate, toggle mode, copy, delete steps |
| 13 | Therapist App — Wizard | 5-tab wizard: create and edit learning steps |
| 14 | Integration & Polish | End-to-end flows, reinforcement animations, cleanup |

---

## Phase 1 — Project Setup

**Goal:** Create a valid, buildable Android project with the correct 6-module structure, dependencies, and two launcher icons.

**Components:**
- Root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` (all versions from Project Setup Specification)
- Six `build.gradle.kts` files: `:app`, `:domain`, `:data`, `:feature:child`, `:feature:therapist`, `:core:ui`
- `FriendlyEmotionsApp` (`@HiltAndroidApp`), `TherapistActivity`, `ChildActivity` (empty `setContent {}`)
- `AndroidManifest.xml`: two launcher activities (`singleTask`, landscape), `CAMERA` permission, `FileProvider` declaration, `file_paths.xml`
- Placeholder launcher icons: `friendly_emotions` (child) and `friendly_emotions_settings` (therapist)
- `.editorconfig`, ktlint plugin applied at root

**Documents:** `project-setup-specification.md` §1–10, ADR-003

**Expected Outcome:** Project builds, installs, and shows two launcher icons. Both activities launch to a blank screen.

**Manual Testing Checklist:**
- [ ] `./gradlew assembleDebug` completes without error
- [ ] Two icons appear on device launcher
- [ ] Child icon launches `ChildActivity`
- [ ] Therapist icon launches `TherapistActivity`
- [ ] `./gradlew :feature:child:dependencies` output contains no `:data` path
- [ ] `./gradlew :feature:therapist:dependencies` output contains no `:data` path

**Dependencies:** None

**Definition of Done:** Clean build, two working launcher entries, module boundary verification passing, ktlint baseline generated.

---

## Phase 2 — Domain Layer

**Goal:** Implement all pure Kotlin domain models, value objects, aggregates, repository interfaces, domain services, use case skeletons, and the error model. No Android dependencies anywhere in `:domain`.

**Components:**

*Models (`domain/model/`):*
- `emotion/`: `EmotionId`, `Emotion`, `EmotionLabel`, `EmotionFolder`, `EmotionImage`, `GrammaticalGender`, `FolderGenderPolicy`
- `session/`: `LearningStep`, `SessionMode`, `MaterialSelection`, `ImageUsage`, `LearningParameters`, `TestParameters`, `ReinforcementSettings`, `HintType`, `PromptTemplate`
- `runtime/`: `Trial`, `TrialOption`, `TrialState`, `TrialVerdict`, `RenderedPrompt`, `SessionResult`

*Catalog:*
- `EmotionCatalog` — 6 fixed `Emotion` constants with all Polish (M/F/N) and English label forms

*Repository interfaces (`domain/repository/`):*
- `EmotionFolderRepository`, `EmotionImageRepository`, `LearningStepRepository`, `PreferencesRepository`

*Domain services (`domain/service/`):*
- `TrialGenerator`, `PromptRenderer`, `ImageExhaustionTracker`, `TrialPositionRandomizer`, `ErrorCorrectionController`, `ReinforcementEngine`, `SessionEligibilityChecker`, `LearningStepActivationService`

*Use case skeletons (`domain/usecase/`):*
- All use cases listed in `target-architecture.md` §11 — implemented with `TODO()` bodies or minimal logic where pure-Kotlin-possible (e.g. `ValidateLearningStepNameUseCase`, `DeriveTestParametersUseCase`)

*Error model:* `DomainError` sealed class

**Documents:** `target-domain.md` §3–7, `target-architecture.md` §6, ADR-001, ADR-004, ADR-008, ADR-011

**Expected Outcome:** `:domain` compiles cleanly. All domain service logic (`TrialGenerator`, `PromptRenderer`, `ErrorCorrectionController`, etc.) is unit-testable with plain Kotlin. No `android.*` or `androidx.*` imports in `:domain`.

**Manual Testing Checklist:**
- [ ] `./gradlew :domain:compileDebugKotlin` succeeds
- [ ] `./gradlew :domain:test` passes (unit tests for `TrialGenerator`, `PromptRenderer`, `ErrorCorrectionController`, `ImageExhaustionTracker`, `TrialPositionRandomizer`)
- [ ] `EmotionCatalog` contains all 6 emotions with correct Polish forms verified manually
- [ ] No Android imports in any `:domain` file (grep check)

**Dependencies:** Phase 1

**Definition of Done:** Domain module compiles, all domain service unit tests pass, zero Android imports in `:domain`.

---

## Phase 3 — Data Layer

**Goal:** Implement the Room database, entities, DAOs, mappers, repository implementations, DataStore, and database seeding. The app should launch with example data ready.

**Components:**

*Database:*
- `AppDatabase` (version 1, `exportSchema = true`, foreign key enforcement)
- TypeConverters: `GrammaticalGenderConverter`, `FolderGenderPolicyConverter`, `HintTypeSetConverter`, `PromptTemplateConverter`, `SessionModeConverter`

*Entities:* `EmotionFolderEntity`, `EmotionImageEntity`, `LearningStepEntity`, `ImageUsageEntity`, `LearningParametersEmbedded`, `TestParametersEmbedded`, `ReinforcementSettingsEmbedded`

*DAOs:* `EmotionFolderDao`, `EmotionImageDao`, `LearningStepDao`, `ImageUsageDao`  
Key DAO requirements: `@Transaction` on `activateStep`/`deactivateStep`, `observeActiveStep(): Flow<LearningStepEntity?>`

*Mappers:* `EmotionFolderMapper`, `EmotionImageMapper`, `LearningStepMapper`

*Repository implementations:* `EmotionFolderRepositoryImpl`, `EmotionImageRepositoryImpl`, `LearningStepRepositoryImpl`, `PreferencesRepositoryImpl`

*DataStore:* `TherapistPreferencesDataStore` (`hide_example_folders`, `hide_example_steps`)

*Seeding:* `DatabaseInitializer` — seeds example folders (Women/F, Men/M, Emojis/N, Other/MIXED per emotion), example images (asset URIs), two example learning steps on first launch

*DI:* `DatabaseModule`, `RepositoryModule`, `StorageModule`

*Use cases — full implementations:* All material use cases, all learning step use cases, session use cases (connect to repository implementations)

**Documents:** `target-architecture.md` §9–10, ADR-005, ADR-008, ADR-009, ADR-010, ADR-014

**Expected Outcome:** App launches and `DatabaseInitializer` seeds example data. Repositories emit correct `Flow<T>` values. All use cases route through the data layer.

**Manual Testing Checklist:**
- [ ] App launches without crash
- [ ] Room schema JSON exported to `schemas/` directory
- [ ] `DatabaseInitializer` logs confirm seeding ran once
- [ ] Verify via `App Inspection` → Database Inspector that `emotion_folders`, `emotion_images`, `learning_steps` tables are populated
- [ ] Second launch does not re-seed (idempotency)
- [ ] `LearningStepRepositoryImpl.observeActiveStep()` emits the first example step (integration test)

**Dependencies:** Phase 1, Phase 2

**Definition of Done:** Database created at version 1, schema file committed, example data seeded, all repository integration tests pass.

---

## Phase 4 — Core UI Theme

**Goal:** Implement the Material3 theme and the shared UI component library used by both feature modules.

**Components:**
- `FriendlyEmotionsTheme` — color scheme, typography, shapes derived from Figma design tokens
- Shared components: `NumberSelector`, `YesNoConfirmationDialog`, `InfoDialog`, `CheckboxRow`, `LoadingScreen`, `ErrorScreen`
- Theme preview composables (`@Preview`)

**Documents:** `target-architecture.md` §16.2; **Figma** — use `get_design_context` on the design system / theme node to extract all tokens before implementation

**Expected Outcome:** `FriendlyEmotionsTheme` is applied in both Activities. Shared components render correctly in Compose Preview.

**Manual Testing Checklist:**
- [ ] `./gradlew :core:ui:compileDebugKotlin` succeeds
- [ ] `FriendlyEmotionsTheme` applied in both `ChildActivity` and `TherapistActivity`
- [ ] All shared components visible in `@Preview` with correct Figma-specified styling
- [ ] Colors, typography, and shapes match Figma design

**Dependencies:** Phase 1

**Definition of Done:** Theme applied app-wide, all shared components preview correctly, matches Figma.

---

## Phase 5 — Child App: Info Splash + Main Screen

**Goal:** Implement the child app navigation architecture and the two pre-session screens: the 5-second info splash and the main screen showing the active step.

**Components:**
- `ChildScreen` sealed class navigation state (`Info`, `Main`, `Game`, `End`)
- `ChildNavigationHost` — single composable router driven by `StateFlow<ChildScreen>`
- `ChildHomeViewModel` — observes `observeActiveStep()` via use case, evaluates `canPlay`, drives `Info → Main` auto-transition after 5 seconds
- `ChildHomeScreen` — info splash (5 s auto-advance or tap), main screen (step name, mode badge, Play button disabled when `!canPlay`)
- `ChildHomeUiState`
- `ChildModule` (Hilt)

**Documents:** `target-architecture.md` §5.1, §12.1, §13.1–13.2; `friendly-emotions-functional-specification.md` §5.11; ADR-012; **Figma** — info screen, child main screen

**Expected Outcome:** Child app shows info splash for 5 seconds (or less on tap), transitions to main screen. Play button is enabled only when example step is active and has eligible images.

**Manual Testing Checklist:**
- [ ] Info screen appears on launch
- [ ] Tapping info screen advances to main screen immediately
- [ ] Auto-advance to main screen after 5 seconds
- [ ] Main screen shows name of active example learning step
- [ ] Play button visible and enabled (example data is seeded)
- [ ] No back-navigation possible from child app (system back does nothing)
- [ ] UI matches Figma for both screens

**Dependencies:** Phase 2, Phase 3, Phase 4

**Definition of Done:** Info and main screens functional, active step displayed, `canPlay` guard working.

---

## Phase 6 — Child App: Basic Session Flow

**Goal:** Implement the game screen with trial presentation, option display, answer evaluation, and basic correct/incorrect feedback. No hints, TTS, or reinforcement yet.

**Components:**
- `GameViewModel` — initializes session via `InitializeSessionUseCase`, holds `StateFlow<GameUiState>`, advances trial index on answer
- `SessionOrchestrator` delegate — manages trial list, current index, answer evaluation
- `GameScreen` — displays prompt (emotion label only), image option grid (1–6 images), handles tap input
- `GameUiState`, `GameUiEvent`
- `TrialPositionRandomizer` integration — shuffle positions between rounds
- `ImageExhaustionTracker` integration — cycle images per emotion before repeat

**Documents:** `target-domain.md` §10–12, §8.5–8.6; `target-architecture.md` §7.1–7.3; `friendly-emotions-functional-specification.md` §5.1–5.5; **Figma** — game screen, option grid layouts

**Expected Outcome:** Starting a session shows trials. Tapping correct image advances to next trial. Tapping wrong image does not advance (learning mode). Session completes after all trials.

**Manual Testing Checklist:**
- [ ] Tapping Play starts session
- [ ] Correct number of images displayed per trial (default 3)
- [ ] Emotion label displayed as `displayText` only (not full prompt phrase)
- [ ] Tapping correct image advances trial
- [ ] Image positions are shuffled between trials
- [ ] Same image does not repeat as correct answer until pool exhausted
- [ ] Session ends when all trials are complete
- [ ] UI matches Figma for game screen

**Dependencies:** Phase 5

**Definition of Done:** Full trial loop functional for learning mode without assistance features.

---

## Phase 7 — Child App: Session Mechanics

**Goal:** Add all learning-mode assistance features: TTS prompt, hint system, error correction, and reinforcement. Also implement gender-inflected prompts.

**Components:**
- `TtsController` (`DefaultLifecycleObserver`, `@ViewModelScoped`) — initialize TTS, set locale (PL/EN), `speak()`, shutdown on ViewModel cleared
- `PromptRenderer` integration — gender-inflected `displayText` + `spokenText` from correct option's gender
- Hint timer — coroutine in `viewModelScope`; on expiry: show hints; first wrong tap in learning mode: show hints immediately
- Hint visuals — `OUTLINE_CORRECT`, `ANIMATE_CORRECT`, `SCALE_CORRECT`, `DIM_INCORRECT` applied to image tiles
- Reinforcement — `ReinforcementEngine` on clean-correct: TTS speaks praise word + sprite animation (flowers/butterflies/balloons/cars) for 4 s
- `ErrorCorrectionController` integration — `repeatStage` logic, same-layout / shuffled-layout requeue
- Gender handling — correct option gender drives Polish prompt form; `mixedGenderInAnswers` flag respected in distractor selection

**Documents:** `target-domain.md` §12–14, §8.5–8.8; `target-architecture.md` §7.2–7.4; ADR-015; `friendly-emotions-functional-specification.md` §5.3–5.9; **Figma** — hint overlays, reinforcement/praise screen

**Expected Outcome:** Full learning-mode session works correctly with audio prompts, hints on wrong/timeout, error correction re-queues, and praise animations after clean correct answers.

**Manual Testing Checklist:**
- [ ] TTS speaks the full prompt phrase on each trial
- [ ] Display shows only the emotion name (not the full phrase)
- [ ] Polish prompt uses correct gender form (matches correct image's gender)
- [ ] Wrong tap immediately shows all active hint types
- [ ] Correct tap after 5 s auto-triggers hints
- [ ] Correct tap after hint shows no reinforcement
- [ ] Clean correct tap shows praise word (TTS) and animation
- [ ] Animation plays for ~4 seconds then advances
- [ ] Failed trial is re-queued (appears again before session ends)
- [ ] Error correction `repeatStage` logic verified manually (fail → requeue same → fail → requeue same → correct → requeue shuffled → correct → done)

**Dependencies:** Phase 6

**Definition of Done:** All learning-mode mechanics functional including TTS, all hint types, reinforcement animations, and error correction.

---

## Phase 8 — Child App: Test Mode & Session End

**Goal:** Implement test mode (timed, no hints, no reinforcement) and the session end screen with result display. Add end-of-session animation and fanfare.

**Components:**
- Test mode trial behavior in `GameViewModel` — wrong taps ignored, timer counts as wrong and advances, no reinforcement
- `SessionEndScreen` — displays percentage score + correct/total (test mode only)
- `SessionEndViewModel` — calculates `SessionResult`, triggers end-of-session animation/fanfare if enabled
- `SessionEndUiState`
- End-of-session animation and fanfare TTS
- Session end navigation back to main screen (Play Again)

**Documents:** `target-domain.md` §8.5–8.7, §9.2; `friendly-emotions-functional-specification.md` §5.7; `target-architecture.md` §7.3; **Figma** — test mode game screen, session end screen

**Expected Outcome:** Test mode runs silently with timer. Session end shows score. End-of-session animation/fanfare plays if configured. Play Again returns to main screen.

**Manual Testing Checklist:**
- [ ] Switching active step to TEST mode changes behavior (enable via DB Inspector or wait for Phase 12)
- [ ] Wrong tap in test mode does not show hints
- [ ] Timer expiry counts trial as wrong and advances automatically
- [ ] Session end screen appears after all test trials
- [ ] Score displayed as `NN% (X / Y)`
- [ ] End-of-session animation plays (if enabled in step settings)
- [ ] Play Again returns to child main screen

**Dependencies:** Phase 7

**Definition of Done:** Full test mode session functional, session results displayed correctly.

---

## Phase 9 — Therapist App: Navigation Shell

**Goal:** Implement the therapist app welcome screen, main menu, and the full navigation graph skeleton. All routes declared; screens may be placeholder composables.

**Components:**
- `TherapistNavGraph`, `TherapistRoutes` (typed sealed class routes, Kotlin Serialization)
- `TherapistHomeScreen` — welcome message (autism/developmental disorders context) + links + auto-advance to menu after 5 s (or tap)
- `TherapistHomeViewModel`
- Main menu screen — two buttons: "Materials" and "Learning Steps" navigating to their respective roots
- Placeholder screens for all remaining routes (to be filled in later phases)
- Home button on every therapist screen (returns to main menu)

**Documents:** `target-architecture.md` §5.2, §13.3; `friendly-emotions-functional-specification.md` §7.1–7.2; ADR-012; **Figma** — therapist welcome screen, main menu

**Expected Outcome:** Therapist app launches, shows welcome screen, advances to main menu, both buttons navigate to placeholder screens. Home button works from all screens.

**Manual Testing Checklist:**
- [ ] Welcome screen appears on therapist launch
- [ ] Auto-advance to main menu after 5 s
- [ ] Tap advances to main menu immediately
- [ ] "Materials" button navigates to materials placeholder
- [ ] "Learning Steps" button navigates to steps placeholder
- [ ] Home button returns to main menu from any therapist screen
- [ ] UI matches Figma

**Dependencies:** Phase 2, Phase 4

**Definition of Done:** Full therapist navigation structure in place, welcome and main menu screens functional.

---

## Phase 10 — Therapist App: Material Browse

**Goal:** Implement read-only navigation through the material catalog: emotion list → folder list → folder detail (image list). Therapist can browse all example content.

**Components:**
- `EmotionListScreen` + `EmotionListViewModel` — static list of 6 emotions from `EmotionCatalog`; navigate to folder list
- `FolderListScreen` + `FolderListViewModel` — `observeFoldersForEmotion()`, list folders with gender policy badge; filter example folders per `PreferencesRepository`
- `FolderDetailScreen` + `FolderDetailViewModel` — `observeImagesForFolder()`, display images in grid with gender label; hide example images per preferences
- Hide Example toggle (DataStore preference) accessible from folder list and image list

**Documents:** `target-architecture.md` §8.2; `friendly-emotions-functional-specification.md` §6, §8; `target-domain.md` §3.2–3.3; **Figma** — emotion list, folder list, image grid screens

**Expected Outcome:** Full browse of all 6 emotions, example folders, and example images. Gender badges visible. Example content filtered when preference is toggled.

**Manual Testing Checklist:**
- [ ] All 6 emotions listed with correct names (Polish + English)
- [ ] Selecting an emotion shows its folders
- [ ] Folder gender policy displayed on folder row
- [ ] Selecting a folder shows its images in a grid
- [ ] Image gender label displayed on each image tile
- [ ] Toggle hide example folders/images works and persists across app restarts
- [ ] UI matches Figma

**Dependencies:** Phase 3, Phase 9

**Definition of Done:** Browse navigation fully functional with example data visible.

---

## Phase 11 — Therapist App: Material Management

**Goal:** Implement full CRUD for folders and images, including camera/gallery image upload, gender assignment for MIXED folders, and all delete confirmations.

**Components:**
- Create folder: name input + gender policy selector (M/F/N/MIXED) + `CreateFolderUseCase`
- Rename folder: inline edit + `RenameFolderUseCase`
- Delete folder: `YesNoConfirmationDialog` + `DeleteFolderUseCase` (cascade + file deletion); block on example folders
- Add images: `PickVisualMedia` (gallery) + `TakePicture()` + FileProvider (camera)
- Gender assignment for MIXED folders: per-image gender selector; `canSave` gated on all images assigned; highlight missing assignments
- Edit image gender: `UpdateImageGenderUseCase`
- Delete image: `YesNoConfirmationDialog` + `DeleteImageUseCase`; block on example images
- Gender info dialog explaining grammatical gender concept
- Error states: `DomainError` mapped to inline errors and dialogs

**Documents:** `target-domain.md` §3.2–3.3, §8.2–8.3, §9.4; `target-architecture.md` §8.2, §9.5; `friendly-emotions-functional-specification.md` §6.3–6.6, §8; ADR-009, ADR-011; **Figma** — folder creation, image upload, gender assignment, delete confirmation

**Expected Outcome:** Therapist can create custom folders (with any gender policy), add photos, assign genders, and delete user content. All destructive actions require confirmation. Example content cannot be deleted.

**Manual Testing Checklist:**
- [ ] Create folder with each gender policy (M/F/N/MIXED)
- [ ] Add image from gallery — saved to `filesDir/images/`
- [ ] Add image from camera — full resolution, FileProvider URI used
- [ ] MIXED folder: Save blocked until all images have gender; missing images highlighted
- [ ] Fixed-gender folder: gender auto-assigned to new images
- [ ] Delete image (user): confirmation required; file deleted from storage
- [ ] Delete folder (user): confirmation required; all images deleted
- [ ] Delete example folder: operation blocked, error shown
- [ ] Rename folder: updated immediately in list
- [ ] Edit existing image gender: updated correctly

**Dependencies:** Phase 10

**Definition of Done:** Full material CRUD operational with all business rules enforced.

---

## Phase 12 — Therapist App: Learning Step List

**Goal:** Implement the learning step list screen with activate, toggle mode, copy, and delete operations.

**Components:**
- `LearningStepListScreen` + `LearningStepListViewModel` + `LearningStepListUiState`
- Display step list: name, active badge, mode badge (LEARNING/TEST), example badge
- Activate step: mode picker dialog → `ActivateLearningStepUseCase` (atomic transaction)
- Toggle active mode: `SetActiveModeUseCase` for already-active step
- Copy step: `CopyLearningStepUseCase` (auto-generated name, always inactive)
- Delete step: `YesNoConfirmationDialog` + `DeleteLearningStepUseCase` (fallback activation if active); block on example steps
- Filter hide example steps toggle

**Documents:** `target-domain.md` §8.4, §9.1, §11; `target-architecture.md` §8.3; ADR-009; **Figma** — learning step list screen

**Expected Outcome:** Therapist can view all steps, activate any step in LEARNING or TEST mode, toggle active step's mode, copy example steps, and delete user steps. Active step propagates to child app.

**Manual Testing Checklist:**
- [ ] All learning steps listed (example + user-created)
- [ ] Active step highlighted with correct mode badge
- [ ] Activating a different step deactivates the previous one atomically
- [ ] Toggling mode on active step changes mode immediately
- [ ] Copy of example step created with "(kopia)" suffix; marked inactive; not example
- [ ] Delete user step with confirmation
- [ ] Delete active step: fallback example step becomes active automatically
- [ ] Delete example step: blocked
- [ ] Child app main screen reflects newly activated step (without restart)

**Dependencies:** Phase 3, Phase 9

**Definition of Done:** All learning step list operations functional, active step singleton invariant enforced.

---

## Phase 13 — Therapist App: Learning Step Wizard

**Goal:** Implement the full 5-tab wizard for creating and editing learning steps.

**Components:**
- `WizardContainerViewModel` + `WizardStepDraft` — shared draft state, scoped to wizard back-stack entry via `hiltNavGraphViewModel()`
- **Materials Tab** — `MaterialTabViewModel` + `MaterialTabScreen`: pick emotion → view folders → select/deselect folders (auto-selects all images for L+T) → expand folder to deselect individual images → per-image LEARNING/TEST checkboxes
- **Learning Tab** — `LearningTabViewModel` + `LearningTabScreen`: `NumberSelector` for image count (1–6) and repetitions (1–3), prompt template picker, TTS toggle, captions toggle, hint delay slider, hint type checkboxes (≥1 required), mixed gender toggle
- **Reinforcement Tab** — `ReinforcementTabViewModel` + `ReinforcementTabScreen`: praise word checkboxes, animation toggle, end-of-session animation toggle, end-of-session fanfare toggle
- **Test Tab** — `TestTabViewModel` + `TestTabScreen`: override toggle; when overriding: independent image count, repetitions, prompt, TTS, captions; uses `DeriveTestParametersUseCase` when not overriding
- **Save Tab** — `SaveTabViewModel` + `SaveTabScreen`: name input with validation (blank / duplicate check via `ValidateLearningStepNameUseCase`), read-only summary table (learning vs. test parameters), Save button → `SaveLearningStepUseCase`
- Edit mode: pre-populate wizard from existing `LearningStep` via `GetLearningStepUseCase`
- Discard confirmation on back navigation

**Documents:** `target-domain.md` §15; `target-architecture.md` §8.1; ADR-013; `friendly-emotions-functional-specification.md` §9; **Figma** — all 5 wizard tab screens, navigation between tabs

**Expected Outcome:** Therapist can create a new learning step selecting any subset of emotions/images with full configuration, save it, and then activate it from the list screen. Editing existing steps pre-populates all tabs correctly.

**Manual Testing Checklist:**
- [ ] Wizard opens on Materials tab
- [ ] Select emotion → folders displayed → select folder → all images selected for L+T
- [ ] Deselect individual image from folder expansion view
- [ ] Image count `NumberSelector` respects 1–6 bounds
- [ ] Repetitions `NumberSelector` respects 1–3 bounds
- [ ] Hint type: cannot deselect last active hint type
- [ ] Test tab: override off → mirrors learning params; override on → independent fields
- [ ] Toggling override back to off reverts test params immediately
- [ ] Save tab: blank name shows error
- [ ] Save tab: duplicate name shows error
- [ ] Save tab: summary table shows all parameter values correctly
- [ ] Save creates step, navigates to list; step visible and inactive
- [ ] Edit pre-populates all 5 tabs
- [ ] Navigating away without saving shows discard confirmation
- [ ] Example steps cannot be edited (edit is disabled or opens as copy)

**Dependencies:** Phase 12

**Definition of Done:** Full wizard functional for create and edit; all validation rules enforced; saved step appears in list and can be activated.

---

## Phase 14 — Integration & Polish

**Goal:** Validate end-to-end flows across both apps, add missing reinforcement animation assets, clean up orphaned images, and ensure all business rules are correctly wired together.

**Components:**
- Reinforcement sprite animations: integrate animation asset sheets (flowers, butterflies, balloons, cars) into `GameScreen` using `Compose Animation`
- `CleanOrphanImagesUseCase` — called periodically or on folder/image deletion to remove unreferenced files
- End-to-end flow validation: therapist creates step → activates → child plays session (learning + test)
- `SessionEligibilityChecker` integration — Play button disabled with explanatory message when no eligible images
- Edge case handling: deleting a folder that is referenced in an active learning step; image files missing from `filesDir`
- UI polish pass against Figma for every screen
- Ensure Polish and English locale switching works correctly (prompt language, gender forms)

**Documents:** All sources; **Figma** — full visual review of all screens

**Expected Outcome:** Complete application working from therapist configuration through child session in both modes, with all animations, audio, and error states handled.

**Manual Testing Checklist:**
- [ ] Full E2E: therapist creates step → activates → child plays learning session → session ends
- [ ] Full E2E: therapist sets step to TEST mode → child plays test session → score displayed
- [ ] Reinforcement animations play correctly (all 4 themes appear across multiple sessions)
- [ ] End-of-session animation and fanfare play (when enabled)
- [ ] Play button disabled when no images eligible for current mode
- [ ] Switching device language to English: prompts switch to English, gender-neutral forms
- [ ] Deleting active step from therapist: child app main screen updates to fallback example step
- [ ] Orphan image files cleaned from `filesDir` after folder deletion
- [ ] All screens match Figma on a reference device
- [ ] `./gradlew lint` passes with no errors

**Dependencies:** All previous phases

**Definition of Done:** All end-to-end flows verified, all animations and audio functional, zero lint errors, all screens match Figma.

---

## Architectural Constraints (Enforced Throughout All Phases)

1. `:domain` has **zero** `android.*`/`androidx.*` imports — verified with grep after every Phase 2+ session.
2. `ViewModels` inject **use cases only** — never repositories or DAOs directly.
3. All multi-table writes that enforce a domain invariant use `@Transaction`.
4. `Trial`, `RenderedPrompt`, `TrialOption`, `SessionResult` are **never persisted** to Room.
5. Navigation events use `Channel<NavigationEvent>` — never Boolean flags in `UiState`.
6. Room entities **never leave** `:data` — only domain models cross the boundary.
7. `:feature:child` and `:feature:therapist` **never depend** on `:data` or each other.
8. `fallbackToDestructiveMigration` is **never set**.
9. Every schema change ships with an explicit Room migration.
10. Composables are **stateless renderers** — no timers, TTS, or business logic inside composables.

---

*End of Implementation Roadmap — Friendly Emotions*
