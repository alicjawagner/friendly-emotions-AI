# Domain Model — Przyjazne Słowa v2

> **Status:** Business-domain analysis only. No UI, Compose, or implementation details.
> **Date:** 2026-06-18
> **Analyst role:** Senior Software Architect — Domain-Driven Design

---

## Table of Contents

1. [Core Business Concepts](#1-core-business-concepts)
2. [Domain Entities](#2-domain-entities)
3. [Domain Workflows](#3-domain-workflows)
4. [Rules and Constraints](#4-rules-and-constraints)
5. [State Machines](#5-state-machines)
6. [Domain Dependency Graph](#6-domain-dependency-graph)
7. [Reusable Domain Logic](#7-reusable-domain-logic)

---

## 1. Core Business Concepts

The domain is an **image-based word-learning system** designed for therapist-directed sessions with children. The vocabulary is taught through repeated matching of a spoken word to a photograph. The therapist controls every aspect of how sessions run; the child simply plays.

### Concept Glossary

| Term (Polish) | Term (English) | Definition |
|---|---|---|
| **Materiał edukacyjny** | Educational Material | A word that can be learned, together with one or more photographs illustrating it. |
| **Zasób** | Resource | The persisted record for an educational material: its display name, the word to be learned, and category. |
| **Obraz / Zdjęcie** | Image | A photograph stored on the device, associated with one or more Resources. |
| **Krok uczenia** | Learning Step | A named, self-contained learning plan. Specifies which materials to use and all session parameters. Analogous to a lesson plan. |
| **Konfiguracja** | Configuration | The technical name for a Learning Step. Contains embedded LearningSettings and TestSettings. |
| **Tryb uczenia** | Learning Mode | The mode of a session focused on teaching new words. Hints are permitted. |
| **Tryb testu** | Test Mode | The mode of a session focused on assessing knowledge. No hints; timed answer window. |
| **Runda** | Round / Trial | A single presentation of a target word + a set of image options. The child selects one image. |
| **Sesja** | Session | A sequence of rounds generated from the active Learning Step. Ends when all rounds are completed. |
| **Słowo docelowe** | Target Word / Learned Word | The word the child is expected to associate with an image. Read aloud as the command. |
| **Opcja** | Option | One image shown on screen during a round. Exactly one option is correct; the rest are distractors. |
| **Dystraktor** | Distractor | An incorrect image option drawn from a different Resource. |
| **Podpowiedź** | Hint | A visual aid that activates after a configurable delay to help the child identify the correct option. |
| **Wzmocnienie / Pochwała** | Reinforcement / Praise | Positive feedback (text + optional animation) delivered when the child gives a correct answer. |
| **Polecenie** | Prompt / Command | The spoken instruction presented with the target word. Has three forms: SHORT, WHERE_IS, SHOW_ME. |
| **Zasób z obrazami** | Resource With Images | The aggregate of a Resource together with its associated Images. Used at runtime for round generation. |
| **Element słownikowy** | Vocabulary Item | A Resource-with-images as it appears within a specific Configuration, including per-image flags for learning/test eligibility. |
| **Użycie obrazu** | Image Usage | The assignment of a specific Image to a specific Configuration, with flags indicating whether the image is eligible in learning mode, test mode, or both. |
| **Aktywna konfiguracja** | Active Configuration | Exactly one Configuration is marked active at any time. It is the one loaded by the child's game. |

---

## 2. Domain Entities

### 2.1 Resource (Zasób / Materiał edukacyjny)

**Purpose:** The atomic unit of educational content. Represents one learnable word and its category.

| Field | Type | Description |
|---|---|---|
| `id` | Long | Surrogate primary key |
| `name` | String | Display name shown to the therapist. Defaults to equal to `learnedWord` unless independently customised. |
| `learnedWord` | String | The word spoken as the command during the game. The word the child learns to associate with an image. |
| `category` | String | Optional grouping label (e.g. animals, colors). No enforcement logic observed. |
| `isExample` | Boolean | Marks pre-seeded demo content. Example materials can be hidden by therapist preference. |

**Relationships:**
- Has zero or more `Image` records (via `ResourceImage` junction).
- Can be included in zero or more `Configuration` records (via `ConfigurationResource` junction).

**Lifecycle:**
1. Created by therapist (name + learnedWord, optional category).
2. One or more images attached.
3. Included in one or more Configurations.
4. May be edited (name, learnedWord, images) at any time.
5. Deleted by therapist. Deletion does not cascade to Configurations automatically — the system queries which Configurations reference the Resource before allowing deletion.

---

### 2.2 Image (Obraz)

**Purpose:** A photograph stored at a local file path. Images are the visual stimulus shown to the child during a round.

| Field | Type | Description |
|---|---|---|
| `id` | Long | Surrogate primary key |
| `path` | String | Absolute file-system path or `file:///android_asset/…` URI for bundled example images. |

**Relationships:**
- Belongs to one or more `Resource` records (via `ResourceImage` junction).
- May be assigned to one or more `Configuration` records with per-mode flags (via `ConfigurationImageUsage`).

**Lifecycle:**
1. Imported from device gallery or camera; copied to app-internal storage immediately.
2. Linked to a `Resource` on save.
3. Unlinked from a Resource when the therapist removes it.
4. **Orphan cleanup:** Images with no remaining Resource links are deleted from storage when the therapist exits or saves a material.

---

### 2.3 Configuration (Krok uczenia)

**Purpose:** A complete, named learning plan. Bundles the material selection and all session-control parameters. Exactly one Configuration is the *active* one at any time.

| Field | Type | Description |
|---|---|---|
| `id` | Long | Surrogate primary key |
| `name` | String | Unique (case-insensitive) human-readable name chosen by the therapist. |
| `isActive` | Boolean | True for at most one Configuration at a time. |
| `activeMode` | String? | `"uczenie"` (learning) or `"test"` — the mode in which this Configuration is active. Null when inactive. |
| `isExample` | Boolean | Pre-seeded demo content flag. Example configurations can be hidden by therapist preference. |
| `learningSettings` | LearningSettings | Embedded — parameters for learning-mode sessions (see §2.4). |
| `testSettings` | TestSettings | Embedded — parameters for test-mode sessions (see §2.5). |

**Relationships:**
- References zero or more `Resource` records (via `ConfigurationResource` junction).
- References zero or more `Image` records with mode-flags (via `ConfigurationImageUsage` junction).

**Lifecycle:**
1. Created by therapist via multi-step wizard (Material → Learning → Reinforcement → Test → Save).
2. Saved with a unique name.
3. Can be edited (same wizard, pre-populated fields).
4. Can be duplicated. The copy receives an auto-generated name (`"{original} (kopia N)"`), is always inactive, and is never marked as example.
5. Can be activated — sets `isActive = true` and `activeMode` to a chosen mode; deactivates any previously active Configuration.
6. Can be deleted. If the deleted Configuration was active, the system automatically activates the first available example Configuration in learning mode as a fallback.

---

### 2.4 LearningSettings

**Purpose:** All parameters governing a learning-mode session. Embedded inside `Configuration`.

| Field | Type | Default | Description |
|---|---|---|---|
| `numberOfWords` | Int | 0 | How many distinct words (Resources) to include in one session. Derived at runtime from the material assignment. |
| `displayedImagesCount` | Int | 0 | How many image options to show per round (correct + distractors). Range: 1–6, capped by available images. |
| `repetitionPerWord` | Int | 2 | How many rounds each word appears in (minimum 1 enforced at runtime). |
| `commandType` | String | "" | One of `"SHORT"`, `"WHERE_IS"`, `"SHOW_ME"`. |
| `showLabelsUnderImages` | Boolean | true | Whether to show the word label beneath each image option. |
| `readCommand` | Boolean | true | Whether to read the command aloud (TTS). |
| `hintAfterSeconds` | Int | 3 | Delay in seconds before the hint is revealed. |
| `typesOfHints` | List\<String\> | [] | Active hint types. At least one must always be selected. |
| `typesOfPraises` | List\<String\> | [] | Active praise words delivered on correct answers. |
| `animationsEnabled` | Boolean | true | Whether praise animations play. |

---

### 2.5 TestSettings

**Purpose:** All parameters governing a test-mode session. Embedded inside `Configuration`. Defaults to a copy of LearningSettings unless independently edited by the therapist.

| Field | Type | Default | Description |
|---|---|---|---|
| `numberOfWords` | Int | 0 | Same derivation logic as in LearningSettings. |
| `displayedImagesCount` | Int | 0 | Options per round. |
| `repetitionPerWord` | Int | 0 | Rounds per word. |
| `commandType` | String | "" | Prompt form. |
| `showLabelsUnderImages` | Boolean | false | Label visibility. Defaults to false (stricter assessment). |
| `readCommand` | Boolean | false | TTS. Defaults to false. |
| `answerTimeSeconds` | Int | 0 | Time limit per round. Zero means no limit. |

**Key distinction from LearningSettings:** Test mode has no hints and has a timed answer window. The `typesOfHints`, `typesOfPraises`, and `animationsEnabled` fields do not exist on `TestSettings`.

---

### 2.6 ConfigurationResource (junction)

**Purpose:** Records which Resources are included in which Configuration. A Resource in this junction has been selected by the therapist but its individual images may still be filtered by mode (see `ConfigurationImageUsage`).

| Field | Description |
|---|---|
| `configurationId` | FK → Configuration (CASCADE delete) |
| `resourceId` | FK → Resource (CASCADE delete) |

---

### 2.7 ConfigurationImageUsage (junction)

**Purpose:** Records which specific Image from an included Resource is available for learning mode, test mode, or both. This is the fine-grained image-to-mode assignment.

| Field | Description |
|---|---|
| `configurationId` | FK → Configuration (CASCADE delete) |
| `imageId` | FK → Image (CASCADE delete) |
| `inLearning` | Boolean — this image may appear in learning-mode rounds |
| `inTest` | Boolean — this image may appear in test-mode rounds |

---

### 2.8 ResourceImage (junction)

**Purpose:** Associates Images with Resources at the library level (independent of any Configuration).

| Field | Description |
|---|---|
| `resourceId` | FK → Resource (CASCADE delete) |
| `imageId` | FK → Image (CASCADE delete) |

---

### 2.9 VocabularyItem (runtime aggregate)

**Purpose:** The in-memory representation of one Resource as configured within a specific Configuration. Not persisted directly — reconstructed from `ConfigurationResource` + `ConfigurationImageUsage` + `Resource` + `Image` records.

| Field | Description |
|---|---|
| `id` | Resource ID |
| `word` | Resource display name |
| `learnedWord` | Word spoken as the command |
| `selectedImages` | Per-image boolean: is the image included in this configuration? |
| `inLearningStates` | Per-image boolean: eligible for learning mode |
| `inTestStates` | Per-image boolean: eligible for test mode |
| `imagePaths` | Ordered list of image file paths |

---

### 2.10 GameRound (runtime value object)

**Purpose:** One trial — a target word paired with an ordered list of image options. Generated fresh for each session from the active Configuration. Not persisted.

| Field | Description |
|---|---|
| `correctItem` | The `GameItem` representing the correct answer |
| `options` | Shuffled list of `GameItem`s (correct + distractors) |

---

### 2.11 GameItem (runtime value object)

**Purpose:** A single selectable image option in a round.

| Field | Description |
|---|---|
| `label` | The `learnedWord` of the Resource this image belongs to |
| `imagePath` | File path to the image |

---

### 2.12 RoundSettings (interface)

**Purpose:** A shared abstraction over `LearningSettings` and `TestSettings` that isolates the three parameters needed for round generation. Allows the game engine to operate independently of which mode is active.

```
interface RoundSettings {
    val numberOfWords: Int
    val displayedImagesCount: Int
    val repetitionPerWord: Int
}
```

Both `LearningSettings` and `TestSettings` can be projected onto this interface via extension functions (`asRoundSettings()`).

---

## 3. Domain Workflows

### 3.1 Material Management Workflow

**Actor:** Therapist

#### 3.1.1 Create a Material

1. Therapist opens the new-material form.
2. Enters `learnedWord` (the word the child will learn).
3. Optionally customises the display `name` (defaults to equal `learnedWord`).
4. Optionally sets a `category`.
5. Adds one or more images (from gallery or camera). Each image is copied immediately to app-internal storage.
6. Submits the form.
7. **Validation:** Both `name` and `learnedWord` cannot both be blank. If a duplicate `name` already exists, the therapist is warned and must confirm.
8. The `Resource` record is inserted; all images are inserted and linked via `ResourceImage`.
9. Orphaned images (any image that was added but then removed before save) are deleted from storage.

#### 3.1.2 Edit a Material

1. Therapist opens the edit form for an existing Resource.
2. All current images are pre-loaded.
3. Therapist may change name, learnedWord, category, add images, or remove images.
4. Images marked for removal during editing are queued in `imagesToUnlink`.
5. On save: Resource is updated; new images are inserted and linked; removed images are unlinked. Orphaned images are purged.

#### 3.1.3 View Material Library

The therapist sees all Resources. Resources flagged `isExample` can be hidden via the `hide_example_materials` preference.

---

### 3.2 Configuration (Learning Step) Management Workflow

**Actor:** Therapist

#### 3.2.1 Create a Learning Step (5-step wizard)

**Step 1 — Material assignment**
1. Therapist selects Resources from the library to include.
2. For each included Resource, the therapist selects which of its Images are eligible, and for each eligible image, whether it applies to learning mode, test mode, or both.
3. Resources already in the configuration are excluded from the add-list.

**Step 2 — Learning settings**
1. Therapist sets: number of displayed images per round, repetitions per word, prompt type, label visibility, TTS enabled, hint delay, which hint types are active.

**Step 3 — Reinforcement settings**
1. Therapist selects which praise words are active (`dobrze`, `super`, `świetnie`, `ekstra`, `rewelacja`, `brawo`).
2. Therapist toggles praise animations on/off.

**Step 4 — Test settings**
1. By default, test settings are **derived from learning settings** (copy). The flag `testEditEnabled = false` indicates this.
2. Therapist may unlock independent test settings (`testEditEnabled = true`).
3. If the therapist re-locks test settings, they revert to the learning-settings values.
4. In addition to the learning parameters, the therapist sets an answer time limit (`answerTimeSeconds`).

**Step 5 — Save**
1. Therapist enters a unique name.
2. Duplicate name check (case-insensitive) runs against all existing Configurations.
3. On success: `Configuration` is inserted; `ConfigurationResource` links are inserted; `ConfigurationImageUsage` links are inserted.

#### 3.2.2 Edit a Learning Step

Identical to creation, but the wizard is pre-populated from the persisted `Configuration`. On save, existing resource links and image-usage links for the configuration are deleted and re-inserted.

The system also re-evaluates whether the persisted TestSettings differ from the derived test state — if they do, `testEditEnabled` is set to `true` upon loading.

#### 3.2.3 Duplicate a Learning Step

1. All settings are copied to a new `Configuration`.
2. A unique name is generated: `"{original} (kopia 1)"`, incrementing the suffix if needed.
3. The copy is never marked active or example.
4. `ConfigurationResource` and `ConfigurationImageUsage` links are duplicated.

#### 3.2.4 Activate a Learning Step

1. Therapist selects a Configuration and confirms activation.
2. `clearActiveConfiguration` deactivates all Configurations.
3. `activateConfiguration(id, mode)` sets `isActive = true` and `activeMode`.
4. Mode defaults to `"uczenie"` (learning) when activated from the list. The mode can also be changed independently via `SetActiveMode` without going through the activation dialog.

#### 3.2.5 Delete a Learning Step

1. Confirmation dialog is shown.
2. If the deleted Configuration was active, the system activates the first example Configuration in `"uczenie"` mode as a fallback.
3. Configuration is deleted; Room CASCADE removes all `ConfigurationResource` and `ConfigurationImageUsage` links.

---

### 3.3 Session (Game) Workflow

**Actor:** Child (guided by therapist setup)

#### 3.3.1 Session Start

1. The active `Configuration` is loaded from the database.
2. `activeMode` is read to determine whether this is a learning or test session.
3. The appropriate settings object is selected:
   - Learning: `LearningSettings.asRoundSettings()`
   - Test: `TestSettings.asRoundSettings()`
4. `getResourcesWithImagesForActiveConfigFiltered(isTestMode)` retrieves all Resources linked to the active Configuration, then filters each Resource's images to only those where `inLearning = true` (learning mode) or `inTest = true` (test mode). Resources with no eligible images are excluded.
5. `generateGameRounds(repository, settings, isTestMode)` generates the full round sequence (see §3.3.2).
6. The session begins on the first round.

#### 3.3.2 Round Generation

```
generateGameRounds(repository, settings, isTestMode):

1. Load eligible resources (mode-filtered, non-empty image sets).
2. Cap numberOfWords to min(settings.numberOfWords, available resource count).
3. Shuffle and take numberOfWords resources → selectedResources.
4. For each resource in selectedResources:
   a. Repeat settings.repetitionPerWord times (minimum 1):
      i.  Pick one random image from the resource's eligible images → correctImage.
      ii. Build correctItem = GameItem(label = learnedWord, imagePath = correctImage.path).
      iii.Take (displayedImagesCount - 1) distractor resources from the remaining pool
           (those with non-empty images, shuffled, no replacement).
      iv. For each distractor resource, pick one random image → build distractorItem.
      v.  Merge [distractors + correctItem] → shuffle → build GameRound.
5. Shuffle the complete list of rounds → return.
```

The final shuffle of all rounds ensures the child does not encounter words in a predictable order.

#### 3.3.3 Round Execution

1. The round is presented: the command (prompt) is displayed/spoken, and the image options are shown.
2. Before rendering, the correct-answer position is pseudorandomized using a **position history** to avoid repeating the same on-screen position as the previous round for the same word (`shuffledRoundAvoidingPrevious`).
3. The position is recorded in the history after shuffling.
4. The child taps an image.
5. The answer is judged (see §4 for judgment rules).
6. If correct: the `correctAnswersCount` is incremented; reinforcement may be delivered.
7. If incorrect: the `wrongAnswersCount` is incremented; `hadMistakeThisRound` is recorded.
8. The session advances to the next round via `goToNext`.

#### 3.3.4 Session End

1. When `currentRoundIndex` reaches the last round and the child advances, `onGameFinished` is called with final `correct` and `wrong` counts.
2. Counters and index are reset.
3. A new round sequence is immediately generated for the next session.
4. The end screen displays the correct/wrong counts.

---

### 3.4 Session Availability Check

Before the child can start playing, the system verifies that the active Configuration actually has usable material. `hasMaterialsForActiveConfig(isTestMode)` returns `true` only if `getResourcesWithImagesForActiveConfigFiltered(isTestMode)` returns a non-empty list. The `canPlay` flag prevents starting a session with no valid rounds.

---

## 4. Rules and Constraints

### 4.1 Active Configuration Rules

| Rule | Source |
|---|---|
| **At most one Configuration is active at any time.** Activating a new one atomically deactivates the current one. | `ConfigurationDao.clearActiveConfiguration()` + `activateConfiguration()` |
| **Active mode is always set when activating.** It is either `"uczenie"` or `"test"`. `activeMode` is `null` only when `isActive = false`. | `ConfigurationDao.activateConfiguration(id, mode)` |
| **Deleting the active Configuration must not leave the system without an active Configuration.** The system automatically activates the first example Configuration in learning mode. | `ConfigurationViewModel.onEvent(ConfirmDelete)` |

---

### 4.2 Configuration Name Rules

| Rule | Source |
|---|---|
| **Configuration names must be unique (case-insensitive).** A duplicate-name dialog blocks the save. | `ConfigurationSettingsViewModel` save handler |
| **Configuration names must not be blank.** An empty-name dialog blocks the save. | `ConfigurationSaveViewModel.reduce(ValidateName)` |
| **Duplicated configurations receive a generated name** of the form `"{original} (kopia N)"` that does not conflict with any existing name. | `generateCopyName()` in `ConfigurationViewModel` |

---

### 4.3 Material (Resource) Name Rules

| Rule | Source |
|---|---|
| **Both `name` and `learnedWord` cannot simultaneously be blank.** | `MaterialsCreatingNewMaterialViewModel` save handler |
| **Duplicate `name` warning with override.** If a Resource with the same name already exists (excluding the one being edited), a confirmation dialog appears. The therapist may choose to proceed anyway (`confirmingDuplicateSave = true`). | Same save handler |
| **Resource name auto-sync.** By default, `name` mirrors `learnedWord` unless `allowEditingResourceName = true`. | `LearnedWordChanged` event handler |

---

### 4.4 Hint Rules

| Rule | Source |
|---|---|
| **At least one hint type must always be selected.** Deselecting a hint is rejected if it is the only selected one (`selectedCount > 1` guard). | `ConfigurationLearningViewModel.reduce()` toggle handlers |
| **Hint types:** `"Obramuj poprawną"` (outline), `"Porusz poprawną"` (animate), `"Powiększ poprawną"` (scale), `"Wyszarz niepoprawne"` (dim incorrect). | `HintType` enum |
| **Hints are only available in learning mode.** `TestSettings` has no hint fields. | `TestSettings` definition |
| **Hint activates after `hintAfterSeconds` seconds** with no answer. | `LearningSettings.hintAfterSeconds` |

---

### 4.5 Round Generation Rules

| Rule | Source |
|---|---|
| **`numberOfWords` is capped at the number of available eligible resources.** `coerceAtMost(allResources.size)`. | `generateGameRounds()` |
| **`repetitionPerWord` minimum is 1.** `coerceAtLeast(1)`. | `generateGameRounds()` |
| **Distractor count = `displayedImagesCount - 1`.** If fewer than `displayedImagesCount - 1` other resources have eligible images, fewer distractors are included. | `generateGameRounds()` |
| **All distractors come from different Resources than the correct answer.** `(allResources - res)`. | `generateGameRounds()` |
| **The correct image is chosen randomly** from the Resource's mode-eligible images each repetition. | `allowedImagesForCorrect.random()` |
| **Distractor images are chosen randomly** one per distractor resource. | `dr.images.random()` |
| **Resources with no eligible images for the current mode are excluded** from both correct-answer selection and distractor pool. | `getResourcesWithImagesForActiveConfigFiltered()` |
| **The full round list is shuffled** before being returned, randomizing presentation order across words. | `rounds.shuffled()` at end of `generateGameRounds()` |

---

### 4.6 Correct-Answer Position Rules

| Rule | Source |
|---|---|
| **The correct answer avoids repeating its previous on-screen position** for the same word across consecutive appearances. | `noteCorrectPos()` + `shuffledRoundAvoidingPrevious()` |
| **If all positions have been used, the history is reset** (a new shuffle is performed without constraint). | `avoid.size >= options.size` branch |
| **Up to 50 candidate shuffles are attempted** to find a non-colliding position; the best result so far is used. | `repeat(50)` loop |
| **If only one option is displayed (`displayedImages <= 1`), the round is returned unchanged.** | `if (displayedImages <= 1) return round` |

---

### 4.7 Test-Settings Inheritance Rule

| Rule | Source |
|---|---|
| **Test settings default to a copy of learning settings** when `testEditEnabled = false`. | `ConfigurationLearningState.toDerivedTestState()` |
| **When the therapist re-disables independent test editing**, all test parameters are reset to match the current learning parameters. | `ConfigurationTestViewModel.reduce(SetEditEnabled / ToggleTestEdit)` |
| **On load, the system detects whether the persisted test settings differ from the derived state** and sets `testEditEnabled = true` accordingly. | `loadConfiguration()` in `ConfigurationSettingsViewModel` |

---

### 4.8 Image-Usage and Availability Rules

| Rule | Source |
|---|---|
| **An image can be assigned to a Configuration for learning mode only, test mode only, or both.** Flags `inLearning` and `inTest` on `ConfigurationImageUsage` are independent. | `ConfigurationImageUsage` entity |
| **The game only uses images matching the active mode.** `getResourcesWithImagesForActiveConfigFiltered(isTestMode)` filters by `inLearning` or `inTest`. | `ConfigurationRepository` |
| **Orphaned images are purged after any save or abandoned-edit event.** `ImageRepository.deleteUnassignedImages()`. | `MaterialsCreatingNewMaterialViewModel` |
| **The `availableWordsToAdd` list excludes Resources already in the Configuration** being edited. | `updateAvailableWordsToAdd()` in `ConfigurationSettingsViewModel` |
| **Displayed images per round is capped: maximum 6, minimum 1.** | `clampDisplayedImagesCount()` in `ConfigurationLearningState.kt` |

---

### 4.9 Prompt / Command Rules

| Rule | Source |
|---|---|
| **Three prompt forms are supported:** SHORT (`"{Słowo}"`), WHERE_IS (`"Gdzie jest {Słowo}"`), SHOW_ME (`"Pokaż gdzie jest {Słowo}"`). | `GameViewModel.commandType` derived state |
| **Prompt is always encoded as a constant string code** (`"SHORT"`, `"WHERE_IS"`, `"SHOW_ME"`) in storage and converted to display form at runtime. | `LearningSettings.commandType`, `TestSettings.commandType` |

---

### 4.10 Praise / Reinforcement Rules

| Rule | Source |
|---|---|
| **Six praise words exist:** `"dobrze"`, `"super"`, `"świetnie"`, `"ekstra"`, `"rewelacja"`, `"brawo"`. | `defaultPraiseMap()` |
| **All six are enabled by default.** | `defaultPraiseMap()` returning all `true` |
| **Each praise word can be individually toggled.** | `ConfigurationReinforcementViewModel.reduce(TogglePraise)` |
| **Animations are a separate toggle** from praise-word delivery. | `ConfigurationReinforcementState.animationsEnabled` |
| **Reinforcement is not delivered if the child had a mistake in the current round** (implied by `hadMistakeThisRound` flag). | `GameViewModel.hadMistakeThisRound` |

---

### 4.11 Example Content Rules

| Rule | Source |
|---|---|
| **Example Resources can be hidden** from the material library via `hide_example_materials` preference. | `PreferencesRepository.hideExampleMaterialsFlow` |
| **Example Configurations (Learning Steps) can be hidden** from the step list via `hide_example_steps` preference. | `PreferencesRepository.hideExampleStepsFlow` |
| **A Configuration saved by the therapist (create or edit) is always marked `isExample = false`**, even if it was derived from an example. | `ConfigurationSettingsViewModel` save handler |

---

## 5. State Machines

### 5.1 Child App Session Navigator

Governs which screen the child sees. Transitions are driven by a single `GoToNextScreen` event.

```
┌──────────────────────────────────────────────────────────────────┐
│                      Child App Screens                           │
│                                                                  │
│   ┌────────┐  (auto after 5s)  ┌────────┐  (Play tapped)        │
│   │  info  │ ─────────────────►│  main  │────────────────┐      │
│   └────────┘                   └────────┘                │      │
│                                     ▲                    ▼      │
│                              (auto / btn)          ┌────────┐   │
│                                     │              │  game  │   │
│                                ┌─────────┐         └────────┘   │
│                                │   end   │◄── (all rounds done) │
│                                └─────────┘                      │
└──────────────────────────────────────────────────────────────────┘
```

| State | Meaning |
|---|---|
| `"info"` | Introductory/splash screen. Auto-advances after 5 seconds. |
| `"main"` | Child home screen. Shows Play button. `canPlay` guard prevents starting if no valid material. |
| `"game"` | Active session. GameViewModel drives the round sequence. |
| `"end"` | Session summary. Shows `correctAnswers` and `wrongAnswers` counts. |

---

### 5.2 Game Round State Machine

Each round progresses through the following states (managed by flags in `GameViewModel`):

```
          ┌──────────────────────────────────────────────────────┐
          │                   Round Lifecycle                    │
          │                                                      │
          │  PENDING                                             │
          │  answerJudged = false                                │
          │  correctClicked = false                              │
          │  hadMistakeThisRound = false                         │
          │  showHint = false                                    │
          │  showCongratsScreen = false                          │
          │  shouldReinforce = false                             │
          │       │                                             │
          │       ▼ (hint timer elapses)                         │
          │  HINT VISIBLE                                        │
          │  showHint = true                                     │
          │       │                                             │
          │       ▼ (child taps an image)                        │
          │  JUDGED                                              │
          │  answerJudged = true                                 │
          │       │                 │                            │
          │  CORRECT                INCORRECT                   │
          │  correctClicked = true  hadMistakeThisRound = true   │
          │  shouldReinforce = ?    wrongAnswersCount++           │
          │  correctAnswersCount++                               │
          │       │                 │                            │
          │       ▼                 │                            │
          │  CONGRATS               │                            │
          │  showCongratsScreen=true│                            │
          │       │                 │                            │
          │       └────────┬────────┘                            │
          │                ▼ (goToNext)                          │
          │  NEXT ROUND (or SESSION END)                         │
          │  All flags reset to PENDING values                   │
          └──────────────────────────────────────────────────────┘
```

**Reinforcement trigger:** `shouldReinforce` is set when the child answers correctly. Reinforcement is visually suppressed when `hadMistakeThisRound = true`.

---

### 5.3 Configuration Wizard State Machine

The wizard progresses through five named tabs. The tabs are not strictly sequential — the therapist may navigate freely between them. Persistence only happens at the Save step.

```
Material ──► Learning ──► Reinforcement ──► Test ──► Save
   ▲             ▲              ▲             ▲       │
   └─────────────┴──────────────┴─────────────┘       │
         (free navigation between tabs)               │
                                                      ▼
                                             Configuration persisted
                                             Navigate to list
```

Exiting the wizard without saving triggers a confirmation dialog (`showExitDialog`). Confirming resets the entire `ConfigurationSettingsState` to defaults.

---

### 5.4 Configuration Activation Mode State Machine

A Configuration's `(isActive, activeMode)` fields form a small state machine:

```
                    ┌────────────┐
                    │  INACTIVE  │
                    │isActive=false│
                    │activeMode=null│
                    └────────────┘
                          │
            ┌─────────────┴──────────────┐
            │ activate("uczenie")        │ activate("test")
            ▼                            ▼
  ┌──────────────────┐       ┌──────────────────┐
  │ ACTIVE_LEARNING  │       │   ACTIVE_TEST    │
  │ isActive=true    │◄─────►│ isActive=true    │
  │ activeMode=      │       │ activeMode=      │
  │   "uczenie"      │       │   "test"         │
  └──────────────────┘       └──────────────────┘
            │                          │
            └─────────────┬────────────┘
                          │ deactivate (another config activated)
                          ▼
                    ┌────────────┐
                    │  INACTIVE  │
                    └────────────┘
```

---

## 6. Domain Dependency Graph

The following graph shows how domain concepts depend on each other, from leaf to root:

```
Image ◄──────────────────── ResourceImage ─────────────► Resource
  │                                                          │
  │                  ConfigurationImageUsage ◄──────────────┤
  │                  (inLearning, inTest)                    │
  │                       │                                  │
  └──────────────────────►│◄─── ConfigurationResource ──────┘
                          │
                    Configuration
                    ├── LearningSettings
                    │     ├── numberOfWords  (derived from material assignment)
                    │     ├── displayedImagesCount
                    │     ├── repetitionPerWord
                    │     ├── commandType
                    │     ├── showLabelsUnderImages
                    │     ├── readCommand
                    │     ├── hintAfterSeconds
                    │     ├── typesOfHints  ─── HintType (enum)
                    │     ├── typesOfPraises
                    │     └── animationsEnabled
                    └── TestSettings
                          ├── [same structural fields as LearningSettings]
                          └── answerTimeSeconds
                                          │
                                  [active Configuration]
                                          │
                                    RoundSettings (interface)
                                          │
                              generateGameRounds()
                                          │
                              List<GameRound>
                                   │
                              GameRound
                              ├── correctItem: GameItem
                              └── options: List<GameItem>
                                             │
                                        GameItem
                                        ├── label (learnedWord)
                                        └── imagePath
```

### Dependency summary

| Concept | Depends on |
|---|---|
| `GameItem` | (leaf — no domain dependencies) |
| `GameRound` | `GameItem` |
| `RoundSettings` | (interface — no dependencies) |
| `Image` | (leaf) |
| `Resource` | (leaf) |
| `ResourceImage` | `Resource`, `Image` |
| `ResourceWithImages` | `Resource`, `Image`, `ResourceImage` |
| `ConfigurationImageUsage` | `Configuration`, `Image` |
| `ConfigurationResource` | `Configuration`, `Resource` |
| `VocabularyItem` | `Resource`, `Image`, `ConfigurationImageUsage` |
| `LearningSettings` | `HintType` (values) |
| `TestSettings` | `LearningSettings` (default-derived) |
| `Configuration` | `LearningSettings`, `TestSettings` |
| `generateGameRounds` | `Configuration`, `RoundSettings`, `ResourceWithImages` |

---

## 7. Reusable Domain Logic

The following domain concepts are generic enough to be reused in any image-based learning application, independent of this specific codebase.

---

### 7.1 RoundSettings Interface

The `RoundSettings` interface (`numberOfWords`, `displayedImagesCount`, `repetitionPerWord`) is a clean abstraction over any settings object that controls round generation. Any learning app with a configurable multiple-choice quiz can use it without change.

---

### 7.2 Round Generation Algorithm

`generateGameRounds()` is fully generic. Its inputs are:
- A list of resources (any domain object with a label + a set of images)
- A `RoundSettings` instance

Its outputs are:
- A list of `GameRound` objects

The algorithm encapsulates:
- Word-pool capping
- Per-word repetition with minimum enforcement
- Random image selection from eligible images
- Distractor selection from other resources
- Correct-answer interspersing + shuffle
- Final sequence shuffle

This is reusable in any spaced-repetition, flashcard, or matching-quiz application.

---

### 7.3 Correct-Answer Position Pseudorandomization

`noteCorrectPos()` and `shuffledRoundAvoidingPrevious()` implement a **position history anti-repeat mechanism** for a multiple-choice layout. The algorithm tracks per-word positions and avoids repeating the last-used position on the next occurrence of the same word. This is a general technique applicable to any repeated multiple-choice learning game to reduce position-based habituation.

---

### 7.4 Test-Settings Inheritance Pattern

The rule "test settings mirror learning settings unless independently edited" is a transferable pattern for any system with two parameter profiles (e.g. practise vs. assessment), where the secondary profile defaults to the primary and can optionally diverge. The `toDerivedTestState()` function and `testEditEnabled` flag encode this cleanly.

---

### 7.5 Minimum-One-Selection Invariant

The logic in `ConfigurationLearningViewModel.reduce()` — "a toggle-off is rejected if it would leave zero selections" — is a reusable invariant for any multi-select UI where at least one item must always be chosen (hint types, notification channels, required permissions, etc.).

---

### 7.6 Configuration Copy with Auto-Generated Name

`generateCopyName(original, existing)` is a stateless pure function that produces an unused `"{original} (kopia N)"` name. The same pattern applies to any domain that allows duplication of named entities (copy of a lesson plan, a report template, a workflow, etc.).

---

### 7.7 Image-to-Mode Assignment Model

The `ConfigurationImageUsage` structure (`inLearning: Boolean`, `inTest: Boolean`) represents a general **content-to-context assignment model**: the same content item (image) can be enabled or disabled independently per execution context (mode). This is directly reusable in any learning system with multiple activity types (practise, assessment, homework, review) where the same material library must be filtered differently per context.

---

### 7.8 Orphan-Content Cleanup Policy

The rule that images without any Resource associations are purged (`deleteUnassignedImages`) is a general **orphan-cleanup pattern** applicable to any domain where media assets are attached to domain objects and must be removed when no longer referenced.

---

### 7.9 Session Availability Guard

`hasMaterialsForActiveConfig(isTestMode)` is a pre-flight check that prevents a session from starting if no valid material exists. This generalises to a **precondition check pattern** for any system that requires a minimum valid state before an operation begins (exam requires at least one question, playlist requires at least one track, etc.).

---

*End of Domain Model*
