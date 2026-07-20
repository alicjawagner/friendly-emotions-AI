# Therapist Configuration Flow — Przyjazne Słowa v2

> **Status:** Analysis based exclusively on source code.
> **Date:** 2026-06-18
> **Scope:** Complete therapist workflow from app launch to a Learning Step ready for child execution.

---

## Table of Contents

1. [Configuration Session Start](#1-configuration-session-start)
2. [Learning Step Creation Flow](#2-learning-step-creation-flow)
3. [Stimulus Configuration (Material Tab)](#3-stimulus-configuration-material-tab)
4. [Audio / Prompt Configuration (Learning Tab)](#4-audio--prompt-configuration-learning-tab)
5. [Timing and Session Behavior Configuration (Learning Tab)](#5-timing-and-session-behavior-configuration-learning-tab)
6. [Reinforcement Configuration (Reinforcement Tab)](#6-reinforcement-configuration-reinforcement-tab)
7. [Error Handling / Correction Configuration (Learning Tab)](#7-error-handling--correction-configuration-learning-tab)
8. [Test Mode Configuration (Test Tab)](#8-test-mode-configuration-test-tab)
9. [Learning Step Validation (Save Tab)](#9-learning-step-validation-save-tab)
10. [Saving and Persistence (Save Tab)](#10-saving-and-persistence-save-tab)
11. [Linking Configuration to the Runtime Learning Engine](#11-linking-configuration-to-the-runtime-learning-engine)
12. [Full Therapist Workflow — End to End](#12-full-therapist-workflow--end-to-end)
13. [State Model of the Configuration Process](#13-state-model-of-the-configuration-process)

---

## 1. Configuration Session Start

### Entry Point

The therapist app has a dedicated launcher Activity separate from the child app. Both share the same APK.

| Property | Value |
|---|---|
| **Activity** | `MainActivity` (`com.example.friendly_words.therapist.ui.main.MainActivity`) |
| **Launcher label** | "Przyjazne Słowa Ustawienia" |
| **Icon** | `@drawable/friendly_words_settings` |
| **launchMode** | `singleTask` |
| **Orientation** | Landscape (forced: `SCREEN_ORIENTATION_REVERSE_LANDSCAPE`) |

### Boot Sequence

```
Therapist taps launcher icon
        │
        ▼
MainActivity.onCreate()
  requestedOrientation = REVERSE_LANDSCAPE
  setContent { ScreenNavigation() }
        │
        ▼
ScreenNavigation():
  showFirstScreen = true
  LaunchedEffect(Unit): delay(5000ms) → showFirstScreen = false
  → renders InformationScreen() for 5 seconds  (shared with child app)
  → renders MainScreen() after 5 seconds
        │
        ▼
MainScreen():
  rememberNavController()
  MainScreenViewModel = hiltViewModel()
        │
        ▼
MainScreenViewModel.init {
  ensureExampleMaterials()          // first-launch seeding
  ensureExampleConfigurations()     // first-launch seeding
  findActiveConfiguration()         // live DB stream
}
```

### First-Launch Data Seeding

`MainScreenViewModel` checks whether any Resources and Configurations exist. If the database is empty (first launch), it seeds example data:

**`ensureExampleMaterials()`:**
- Inserts 50 example `Resource` records across 9 categories: `zwierzęta`, `zabawki`, `jedzenie`, `ubrania`, `pojazdy`, `czynności`, `pomieszczenia`, `miejsca`, `meble`, etc.
- For each resource, generates 3 image paths pointing to bundled assets: `file:///android_asset/exemplary_photos/{normalizedName}_{1|2|3}.webp`
- Polish diacritics are normalised in the filename (`normalizeName()`: ą→a, ę→e, ó→o, etc.)
- Each resource is marked `isExample = true`

**`ensureExampleConfigurations()`:**
- Creates 2 example `Configuration` records if none exist.
- `"Krok przykładowy 1"`: 3 animal resources, `displayedImagesCount=3`, `repetitionPerWord=2`, all 4 hint types, all 6 praise words, `isActive=true`, `activeMode="uczenie"`
- `"Krok przykładowy 2"`: 6 toy resources, `displayedImagesCount=6`, `repetitionPerWord=1`, only dim-incorrect hint, all praises, inactive
- For each configuration, `ConfigurationResource` and `ConfigurationImageUsage` links are inserted with `inLearning=true`, `inTest=true` for all images.

### MainContent Dashboard

After boot, the therapist sees `MainContent` — a simple dashboard showing:
- Active configuration name and mode ("Aktywny krok uczenia: … (tryb: …)" or "Brak aktywnego kroku uczenia")
- Two navigation buttons: **MATERIAŁY EDUKACYJNE** and **KROKI UCZENIA**
- An info (ⓘ) button per section with explanatory dialogs
- A global info icon showing the app's purpose

---

## 2. Learning Step Creation Flow

### Navigation Graph

All routes are defined in `NavRoutes` (`MainScreen.kt`):

```
NavHost(startDestination = "main")
  ├── "main"                          → MainContent (dashboard)
  ├── "materials"                     → MaterialsListScreen
  ├── "materials/create"              → MaterialsCreatingNewMaterialScreen (create)
  ├── "materials/edit/{resourceId}"   → MaterialsCreatingNewMaterialScreen (edit)
  ├── "config"                        → ConfigurationsListScreen
  ├── "config/create"                 → ConfigurationSettingsScreen (new)
  └── "config/edit/{configId}"        → ConfigurationSettingsScreen (edit, pre-loaded)
```

### Entering Configuration Creation

```
MainContent → tap "KROKI UCZENIA"
  → navController.navigate("config")
  → ConfigurationsListScreen rendered

ConfigurationsListScreen → tap "UTWÓRZ" (top-right in TopAppBar)
  → navController.navigate("config/create")
  → ConfigurationSettingsScreen(configId = null)
```

### ConfigurationSettingsScreen — Master Wizard

`ConfigurationSettingsScreen` is the container for the entire configuration creation wizard. It:
- Creates a single `ConfigurationSettingsViewModel` (`@HiltViewModel`) that holds all wizard state
- Renders a **tab bar** (`NewConfigurationTopTabs`) with 5 tabs
- Delegates each tab's content to a sub-composable
- All sub-composables share the same `ConfigurationSettingsViewModel` — they pass their events upward as `ConfigurationSettingsEvent.Material(...)`, `ConfigurationSettingsEvent.Learning(...)`, etc.

### Tab Structure

| Tab index | Tab label | Composable | Sub-state |
|---|---|---|---|
| 0 | MATERIAŁ | `ConfigurationMaterialScreen` | `materialState: ConfigurationMaterialState` |
| 1 | UCZENIE | `ConfigurationLearningScreen` | `learningState: ConfigurationLearningState` |
| 2 | WZMOCNIENIA | `ConfigurationReinforcementScreen` | `reinforcementState: ConfigurationReinforcementState` |
| 3 | TEST | `ConfigurationTestScreen` | `testState: ConfigurationTestState` |
| 4 | ZAPISZ | `ConfigurationSaveScreen` | `saveState: ConfigurationSaveState` |

Navigation between tabs is **free** — the therapist can switch to any tab at any time by tapping the tab labels. There is no enforced linear order.

### Initial State

When the wizard is opened for a new configuration, `ConfigurationSettingsState` is initialised with all defaults:

| State field | Default value |
|---|---|
| `materialState.vocabItems` | empty list |
| `materialState.availableWordsToAdd` | all Resources not yet in wizard (loaded asynchronously) |
| `learningState.imageCount` | 3 |
| `learningState.repetitionCount` | 2 |
| `learningState.timeCount` | 6 |
| `learningState.selectedPrompt` | `"{Słowo}"` |
| `learningState.captionsEnabled` | `true` |
| `learningState.readingEnabled` | `true` |
| `learningState.outlineCorrect` | `false` |
| `learningState.animateCorrect` | `false` |
| `learningState.scaleCorrect` | `false` |
| `learningState.dimIncorrect` | `true` |
| `reinforcementState.praiseStates` | all 6 words = `true` |
| `reinforcementState.animationsEnabled` | `true` |
| `testState.testEditEnabled` | `false` (inherits from learning) |
| `testState.answerTime` | `0` |
| `saveState.stepName` | `""` |
| `saveState.editingConfigId` | `null` |

### Back / Exit Handling

Tapping the back arrow in `NewConfigurationTopBar` fires `ShowExitDialog` → a `YesNoDialog` appears: "Czy chcesz wyjść bez zapisywania?". On confirm: `ConfirmExitDialog` resets `ConfigurationSettingsState` to defaults and navigates back.

---

## 3. Stimulus Configuration (Material Tab)

### Screen: `ConfigurationMaterialScreen`

**Tab index:** 0

The Material tab is a two-panel layout:
- **Left panel:** list of words already added to this configuration
- **Right panel:** image assignment for the currently selected word

### Adding a Word (Resource) to the Configuration

```
Therapist taps "DODAJ" button (bottom of left panel)
  → onEvent(ConfigurationMaterialEvent.ShowAddDialog)
  → materialState.showAddDialog = true
  → modal overlay appears
```

The Add Dialog shows:
- All Resources from `materialState.availableWordsToAdd` — this list is computed by `updateAvailableWordsToAdd()` and excludes all Resources already in `materialState.vocabItems`
- If `hideExamples = true`, example resources are filtered out from the list
- Search field filters by `name` OR `category` (case-insensitive)
- List is sorted alphabetically by name (`sortedBy { it.name.lowercase() }`)
- Each row shows `resource.name` + `(resource.category)` if category is non-blank

```
Therapist taps a resource in the dialog
  → closeDialog()  (clears search, hides dialog)
  → onEvent(ConfigurationMaterialEvent.AddWord(resource.id))
        │
        ▼
ConfigurationSettingsViewModel handles AddWord:
  resourceRepository.getById(materialEvent.id)          ← DB read
  imageRepository.getByResourceId(resource.id)          ← DB read

  VocabularyItem created:
    id = resource.id
    word = resource.name
    learnedWord = resource.learnedWord
    selectedImages = List(images.size) { true }         ← all images selected
    inLearningStates = List(images.size) { true }       ← all eligible for learning
    inTestStates = List(images.size) { true }           ← all eligible for test
    imagePaths = images.map { it.path }

  materialState.vocabItems += newVocabularyItem
  selectedWordIndex = vocabItems.lastIndex  (newly added word is selected)
  availableWordsToAdd updated (new word removed from available list)
```

**What is required vs. optional:**
- At least one Resource must be added before the configuration can function in any mode.
- Per-image mode assignment (`inLearning`, `inTest`) is **optional** — defaults to all images in both modes.

### Left Panel — Word List

Each row shows:
- `learnedWord` text (bold, 30 sp)
- ✓ (green) or ✗ (red) indicator for "W UCZENIU" — whether the word has at least one image with `inLearning=true` AND `selectedImages=true`
- ✓ (green) or ✗ (red) indicator for "W TEŚCIE" — same check for `inTest`
- Delete (🗑) button → `WordDeleted(index)` → `showDeleteDialog=true` → confirmation dialog → `ConfirmDelete(index)` removes the item

Tapping a row → `WordSelected(index)` → updates `selectedWordIndex` → right panel updates.

### Right Panel — Image Assignment

Shows images for the currently selected `VocabularyItem`.

Each image displays:
1. **Top checkbox** (select/deselect image entirely)
   - Checked = this image is included in the configuration
   - Uncheck removes it from both `inLearning` and `inTest`
   - Check adds it to both `inLearning` and `inTest`
2. **Image thumbnail** (loaded via Coil, 200dp height)
3. **Two mode checkboxes** (bottom row): "Uczenie" / "Test"
   - If "Uczenie" checkbox is unchecked and "Test" is also unchecked → top checkbox deselected (image removed)
   - If either is checked → top checkbox stays selected

Events fired per interaction:

| Interaction | Events fired |
|---|---|
| Top checkbox toggled | `ImageSelectionChanged(newList)` + `LearningTestChanged(i, newChecked, newChecked)` |
| "Uczenie" checkbox toggled | `ImageSelectionChanged(...)` + `LearningTestChanged(i, newLearning, currentTest)` |
| "Test" checkbox toggled | `ImageSelectionChanged(...)` + `LearningTestChanged(i, currentLearning, newTest)` |

If a word has no images at all, the right panel shows the message: *"Ten materiał nie zawiera żadnych zdjęć. Aby je dodać, przejdź do sekcji 'MATERIAŁY EDUKACYJNE'."*

### No Ordering Controls

There is no UI for ordering words or images. Word order in the list reflects insertion order. Images are displayed in the order returned by `imageRepository.getByResourceId()`. Round generation always shuffles, so display order in the list has no effect on session execution.

### Implicit `availableImagesForLearning` Computation

`ConfigurationSettingsScreen` computes `availableImagesForLearning` and `availableImagesForTest` and passes them to the Learning and Test tabs:

```kotlin
val availableForLearning = material.vocabItems
    .count { item -> item.inLearningStates.any { it == true } }
    .coerceAtMost(6)

val availableForTest = material.vocabItems
    .count { item -> item.inTestStates.any { it == true } }
    .coerceAtMost(6)
```

This count is the number of **words** (not images) that have at least one image eligible for each mode, capped at 6. It controls the maximum allowed value for `displayedImagesCount` in the Learning and Test tabs.

---

## 4. Audio / Prompt Configuration (Learning Tab)

### Screen: `ConfigurationLearningScreen`

**Tab index:** 1

The Learning tab is a two-panel layout:
- **Left panel:** "Ustawienia próby" (Trial Settings)
- **Right panel:** "Ustawienia uczenia" (Learning Settings)

### Prompt (Command) Type

Located in the left panel. A non-editable dropdown (`OutlinedTextField`, `readOnly=true`) with three options:

| Display value | Stored as | Runtime text |
|---|---|---|
| `{Słowo}` | `"SHORT"` (converted at save) | word only |
| `Gdzie jest {Słowo}` | `"WHERE_IS"` | "Gdzie jest {word}" |
| `Pokaż gdzie jest {Słowo}` | `"SHOW_ME"` | "Pokaż gdzie jest {word}" |

Event: `ConfigurationLearningEvent.SetPrompt(prompt)` → `learningState.selectedPrompt = prompt`

**Note:** The dropdown stores the display string (e.g., `"{Słowo}"`), not the code. Conversion to `"SHORT"` / `"WHERE_IS"` / `"SHOW_ME"` happens inside `toLearningSettings()` at save time.

### TTS (Voice Playback)

Located in the right panel:

| Control | Type | Default | Event |
|---|---|---|---|
| "Głosowe odtwarzanie polecenia" | Switch | `true` | `ToggleReading(enabled)` |

When `readingEnabled = true`, the runtime TTS engine speaks `commandText` at the start of each round. No audio recording or custom audio upload is available — TTS is exclusively Android's built-in `TextToSpeech` API with `Locale("pl", "PL")`.

### Label Display (Captions)

| Control | Type | Default | Event |
|---|---|---|---|
| "Podpisy pod obrazkami" | Switch | `true` | `ToggleCaptions(enabled)` |

Shows or hides the `learnedWord` label beneath each image option during the game.

---

## 5. Timing and Session Behavior Configuration (Learning Tab)

### Trial Count Controls (Left Panel)

| Control | Type | Range | Default | Event |
|---|---|---|---|---|
| "Liczba obrazków wyświetlanych na ekranie" | `NumberSelectorForPictures` | 1 – `availableForLearning` (max 6) | 3 (auto-set from available) | `SetImageCount(count)` |
| "Liczba powtórzeń dla każdego słowa" | `NumberSelector` | 1 – 3 | 2 | `SetRepetitionCount(count)` |

**`imageCount` auto-correction:** `ConfigurationLearningScreen` has a `LaunchedEffect(available)` that automatically corrects `imageCount` when `availableImagesForLearning` changes:
- If `available == 0` → set `imageCount = 0`
- If `imageCount == 0` and `available > 0` → set to `min(available, 3)`
- If `imageCount < 1` → set to 1
- If `imageCount > available` → set to `available`

When `imageCount` hits its minimum or maximum, tapping the ± buttons shows an informational dialog explaining why the limit cannot be exceeded.

### Hint Timer (Right Panel)

| Control | Type | Range | Default | Event |
|---|---|---|---|---|
| "Pokaż podpowiedź po (sekundach)" | `NumberSelector` | 1 – 10 | 6 | `SetTimeCount(count)` |

This is the delay (in seconds) after round start before the hint is automatically revealed (in learning mode) or before the round is auto-failed (in test mode).

### No Delay-Between-Trials Configuration

There is no configurable gap between trials. The transition is immediate: after the 4-second congrats screen (learning) or immediate advance (test), the next round begins instantly.

### Session Length

`numberOfWords` (from `LearningSettings`) is derived at runtime from `materialState.vocabItems.size` — it equals the number of words in the configuration, not a separately configurable value. The total number of rounds = `numberOfWords × repetitionPerWord` (plus any repetitions added by the error-correction mechanism).

---

## 6. Reinforcement Configuration (Reinforcement Tab)

### Screen: `ConfigurationReinforcementScreen`

**Tab index:** 2

The tab is split into two columns:
- **Left column:** "Wzmocnienia słowne" (verbal praise)
- **Right column:** "Wzmocnienia wizualne" (visual animations)

### Verbal Praise (Left Column)

The six praise words are always the same set: `"dobrze"`, `"super"`, `"świetnie"`, `"ekstra"`, `"rewelacja"`, `"brawo"`. They are displayed in a 3×2 grid of checkboxes.

| Interaction | Event |
|---|---|
| Toggle a praise word checkbox | `ConfigurationReinforcementEvent.TogglePraise(word, enabled)` |

State: `reinforcementState.praiseStates: Map<String, Boolean>` — each word maps to enabled/disabled.

Default: all six = `true` (all enabled).

A fixed info box states: *"Czytanie pochwał słownych po poprawnej odpowiedzi jest zawsze włączone."* — meaning the TTS reading of praise cannot be turned off, only which words are spoken can change.

**There is no control for configuring the order of praise delivery.** One word is selected at random from the enabled set at the moment the congrats screen appears.

### Visual Animations (Right Column)

| Control | Type | Default | Event |
|---|---|---|---|
| "Animacje" | Switch | `true` | `ConfigurationReinforcementEvent.ToggleAnimations(enabled)` |

Turning this off suppresses the floating-sprite animation (flowers, balloons, cars) on the congrats screen.

### Reinforcement Not Available in Test Mode

No reinforcement configuration UI exists for test mode. The `ConfigurationReinforcementScreen` only configures learning-mode behaviour. The `TestSettings` entity has no praise or animation fields.

---

## 7. Error Handling / Correction Configuration (Learning Tab)

### Hint Type Selection (Right Panel of Learning Tab)

The therapist configures **which visual hints appear** when the child answers incorrectly or the hint timer fires.

Four independent checkboxes — at least one must always remain checked:

| Checkbox label | Stored as | Visual effect |
|---|---|---|
| "Obramuj poprawną" | `typesOfHints.contains("Obramuj poprawną")` | Outline/border appears on correct image |
| "Porusz poprawną" | `typesOfHints.contains("Porusz poprawną")` | Correct image animates (movement) |
| "Powiększ poprawną" | `typesOfHints.contains("Powiększ poprawną")` | Correct image scales up |
| "Wyszarz niepoprawne" | `typesOfHints.contains("Wyszarz niepoprawne")` | Incorrect images are dimmed |

**Minimum-one constraint:** Unchecking a hint type is rejected if it is the only one currently checked (`selectedCount > 1` guard in `ConfigurationLearningViewModel.reduce()`). The state simply does not change.

### What Cannot Be Configured

There is **no configuration for the error-correction repetition strategy** (the `repeatStage` mechanism). The number of additional repetitions after a mistake (up to 2 re-queues) is hardcoded in `GameScreen.onTimeout`. The therapist cannot:
- Disable error-correction repetition
- Change the number of correction attempts
- Change whether the same layout or shuffled layout is used for repetition

### No Prompting Hierarchy

There is no configurable prompting hierarchy (e.g., physical prompt → gestural prompt → verbal prompt). The only corrective support is the hint timer + visual hints.

---

## 8. Test Mode Configuration (Test Tab)

### Screen: `ConfigurationTestScreen`

**Tab index:** 3

### Default State: Inherits from Learning

By default, `testState.testEditEnabled = false`. In this state:
- All controls are greyed out (disabled)
- Test settings **mirror** learning settings automatically
- A `LaunchedEffect` continuously syncs test values from learning whenever `testEditEnabled = false`

### Enabling Independent Test Settings

A checkbox at the top of the screen: "Zmień dla testu" → `ConfigurationTestEvent.ToggleTestEdit`

When toggled to `true`:
- Controls become enabled
- Test settings can be independently set
- A `LaunchedEffect` still corrects `imageCount` against `availableForTest`

When toggled back to `false`:
- Test settings revert to the current learning settings values
- `imageCount`, `repetitionCount`, `selectedPrompt`, `captionsEnabled`, `readingEnabled` are all copied from `learningState`

### Configurable Test Parameters

| Parameter | Control | Range | Notes |
|---|---|---|---|
| "Liczba obrazków wyświetlanych na ekranie" | `NumberSelectorForPictures` | 1 – `availableForTest` (max 6) | Disabled when `testEditEnabled=false` |
| "Liczba powtórzeń dla każdego słowa" | `NumberSelector` | 1 – 3 | Disabled when `testEditEnabled=false` |
| "Rodzaj polecenia" | `ExposedDropdownMenuBox` | SHORT / WHERE_IS / SHOW_ME | Disabled when `testEditEnabled=false` |
| "Podpisy pod obrazkami" | Switch | — | Disabled when `testEditEnabled=false` |
| "Głosowe odtwarzanie polecenia" | Switch | — | Disabled when `testEditEnabled=false` |

**`answerTimeSeconds`** (`TestSettings.answerTimeSeconds`) has **no configuration UI** on the Test tab. The info box on the Test tab states: *"Jeśli dziecko nie wybierze odpowiedzi, ekran zmieni się automatycznie po czasie ustawionym w sekcji UCZENIE – Pokaż podpowiedź po (sekundach)."* This confirms that the test-mode timeout is the same value as `learningState.timeCount` (stored in `LearningSettings.hintAfterSeconds`).

### Auto-Clamping on Image Count (Test Tab)

A `LaunchedEffect(effectiveAvailable, learningImageCount, testEditEnabled)` runs:

- If `testEditEnabled=false`:
  - If `availableForLearning == 0` → `imageCount = 0`
  - Otherwise: inherit `learningImageCount` clamped to `min(learningImageCount, availableForTest)`; if clamped, show dialog: *"W uczeniu wybrano N, ale w teście dostępnych jest tylko M. Ustawiono maksymalną liczbę dla testu."*
- If `testEditEnabled=true`:
  - Ensure `imageCount` stays within `[1, availableForTest]`; if out of range, auto-correct and show a dialog.

---

## 9. Learning Step Validation (Save Tab)

### Screen: `ConfigurationSaveScreen`

**Tab index:** 4

### UI Layout

The Save tab is a two-column layout:
- **Left column (30% width):** Name input + Save button
- **Right column (70% width):** Read-only summary of all settings side-by-side for LEARNING and TEST modes

### Summary Table

The right column shows a scrollable table comparing all key parameters for learning vs. test mode:

| Field | Learning value | Test value |
|---|---|---|
| Liczba uczonych słów | count of words with ≥1 `inLearning` image | count with ≥1 `inTest` image |
| Uczone słowa | comma-separated `word` names | same for test |
| Liczba wyświetlanych zdjęć | `learningState.imageCount` | `testState.imageCount` |
| Liczba powtórzeń | `learningState.repetitionCount` | `testState.repetitionCount` |
| Rodzaj polecenia | `learningState.selectedPrompt` | `testState.selectedPrompt` |
| Podpisy pod obrazkami | Tak/Nie | Tak/Nie |
| Czytanie polecenia | Tak/Nie | Tak/Nie |
| Pokaż podpowiedź po | `{timeCount} s` | `X` (N/A) |
| Podpowiedzi | active hint names | `X` |
| Pochwały słowne | enabled praise words | `X` |
| Animacje | Tak/Nie | `X` |

`X` in the test column means the parameter is not applicable in test mode.

### Name Input Behaviour

- `OutlinedTextField` receives auto-focus (`FocusRequester`) and the soft keyboard is shown automatically on tab open
- Text cursor is placed at the end of any pre-existing name (for edit mode)

### Validation Flow

```
Therapist taps "Zapisz" button
        │
        ▼
ConfigurationSaveScreen.onEvent(ValidateName)
  if (stepName.text.trim().isBlank()):
    saveState.showNameError = true
    saveState.showEmptyNameDialog = true
    → InfoDialog: "Nazwa kroku nie może być pusta!"
    → STOP

if (stepName.text.trim().isNotBlank()):
  onSettingsEvent(Save(SaveConfiguration(name = stepName.text.trim())))
        │
        ▼
ConfigurationSettingsViewModel handles SaveConfiguration:
  existingConfigurations = configurationRepository.getAllOnce()    ← DB read
  alreadyExists = existingConfigurations.any {
    it.name.equals(name, ignoreCase = true) &&
    (editingConfigId == null || it.id != editingConfigId)
  }
  if (alreadyExists):
    saveState.showDuplicateNameDialog = true
    → InfoDialog: "Krok uczenia o takiej nazwie już istnieje."
    → STOP
  else:
    → proceed to persist (§10)
```

### Required Fields

| Field | Required | Validation |
|---|---|---|
| `stepName` | Yes | Non-blank |
| `stepName` uniqueness | Yes | Case-insensitive, excluding self on edit |
| `vocabItems` | No validation enforced | Can save with empty material list |

**There is no validation that material has been added, that hint types are set, or that any other parameter meets a minimum.** The only enforced constraint is the step name.

---

## 10. Saving and Persistence (Save Tab)

### Create Path (new configuration)

```kotlin
// ConfigurationSettingsViewModel — SaveConfiguration handler

val learningSettings = learningState.toLearningSettings(
    materialState = materialState,
    reinforcementState = reinforcementState
)
val testSettings = testState.toTestSettings(
    materialState = materialState
)

val configuration = Configuration(
    name = saveEvent.name,
    isExample = false,
    learningSettings = learningSettings,
    testSettings = testSettings
)

val newId = configurationRepository.insert(configuration)      ← INSERT configurations

val resourceLinks = materialState.vocabItems
    .map { it.id }
    .distinct()
    .map { ConfigurationResource(configurationId = newId, resourceId = it) }
configurationRepository.insertResources(resourceLinks)         ← INSERT configuration_resources

val imageUsages = materialState.toConfigurationImageUsages(
    configurationId = newId,
    imageRepository = imageRepository
)
configurationRepository.insertImageUsages(imageUsages)         ← INSERT configuration_image_usages
```

### Update Path (edit existing configuration)

```kotlin
val originalConfiguration = existingConfigurations.first { it.id == currentId }
val updated = originalConfiguration.copy(
    name = currentName,
    isExample = false,
    learningSettings = learningSettings,
    testSettings = testSettings
)
configurationRepository.update(updated)                              ← UPDATE configurations

configurationRepository.deleteResourcesByConfigId(currentId)         ← DELETE all configuration_resources
configurationRepository.deleteImageUsagesByConfigId(currentId)       ← DELETE all configuration_image_usages

configurationRepository.insertResources(resourceLinks)               ← INSERT new configuration_resources
configurationRepository.insertImageUsages(imageUsages)               ← INSERT new configuration_image_usages
```

### `toLearningSettings()` Mapping

`ConfigurationLearningState.toLearningSettings(materialState, reinforcementState)` constructs a `LearningSettings`:

```
LearningSettings(
  numberOfWords        = materialState.vocabItems.size,
  displayedImagesCount = learningState.imageCount,
  repetitionPerWord    = learningState.repetitionCount,
  commandType          = when (selectedPrompt) {
                           "{Słowo}"                  → "SHORT"
                           "Gdzie jest {Słowo}"       → "WHERE_IS"
                           "Pokaż gdzie jest {Słowo}" → "SHOW_ME"
                         },
  showLabelsUnderImages = learningState.captionsEnabled,
  readCommand           = learningState.readingEnabled,
  hintAfterSeconds      = learningState.timeCount,
  typesOfHints          = [enabled hint type strings],
  typesOfPraises        = [enabled praise word strings],
  animationsEnabled     = reinforcementState.animationsEnabled
)
```

### `toConfigurationImageUsages()` Mapping

`ConfigurationMaterialState.toConfigurationImageUsages(configurationId, imageRepository)`:

```
For each vocabItem:
  images = imageRepository.getByResourceId(item.id)   ← DB read per item
  For each image at index i:
    if (item.selectedImages[i] == true):
      ConfigurationImageUsage(
        configurationId = configurationId,
        imageId         = image.id,
        inLearning      = item.inLearningStates[i],
        inTest          = item.inTestStates[i]
      )
```

### Tables Written

| Table | Operation | Trigger |
|---|---|---|
| `configurations` | INSERT or UPDATE | Save (create or edit) |
| `configuration_resources` | DELETE all + INSERT new | Save (both paths) |
| `configuration_image_usages` | DELETE all + INSERT new | Save (both paths) |

### Draft State

**There is no draft mechanism.** All configuration state exists only in `ConfigurationSettingsState` (in-memory). If the therapist navigates away without saving (and confirms the exit dialog), the state is reset and all unsaved changes are lost. There is no auto-save, no temp table, and no partial persistence.

### Post-Save Navigation

After successful persistence:
```kotlin
_state.update {
    it.copy(
        lastSavedConfigId = newId,
        message = "Pomyślnie dodano nowy krok uczenia"  // or "zaktualizowano"
        navigateToList = true
    )
}
```

`LaunchedEffect(state.navigateToList)` in `ConfigurationSettingsScreen` detects `navigateToList = true`, passes `newlyAddedConfigId` and `message` to the previous back-stack entry's `SavedStateHandle`, then calls `onBackClick()`.

`ConfigurationsListScreen` reads these from `SavedStateHandle` on entry:
- Displays a Snackbar with the success message
- Scrolls the list to the newly saved/updated configuration

---

## 11. Linking Configuration to the Runtime Learning Engine

### Activation

Before the child can use a configuration, the therapist must **activate** it from `ConfigurationsListScreen`.

Each configuration row in the list shows:
- A **checkbox** (left) — indicates active/inactive; tapping it when inactive opens an activation confirmation dialog
- A **name label** — also opens the activation confirmation dialog when tapped (only if inactive)
- A **mode switch** (centre) — "Uczenie" ↔ "Test für ucznia"; only enabled for the active configuration
- Action icons (right): Edit (✏), Copy (⧉), Delete (🗑) — Edit and Delete only shown for non-example configurations

### Activation Flow

**Via checkbox / name tap (confirmation dialog):**
```
ActivateRequested(config)
  → showActivateDialogFor = config
  → YesNoDialogWithName: "Czy chcesz aktywować krok uczenia: {name}?"
  → ConfirmActivate(config)
        │
        ▼
configurationRepository.setActiveConfiguration(config, "uczenie")
  → dao.clearActiveConfiguration()   ← UPDATE SET isActive=0, activeMode=NULL WHERE isActive=1
  → dao.activateConfiguration(id, "uczenie")  ← UPDATE SET isActive=1, activeMode="uczenie" WHERE id=N
```

Default mode on activation via confirmation dialog is always `"uczenie"`.

**Via mode switch (already-active configuration):**
```
Switch toggled (Uczenie ↔ Test)
  → SetActiveMode(config, "test" | "uczenie")
  → configurationRepository.setActiveConfiguration(config, mode)
```

This allows the therapist to toggle between learning and test mode for the active configuration without going through the confirmation dialog.

### Launching the Child App from the Therapist App

`ConfigurationsListScreen` has a green Play (▶) button in the search bar row:

```kotlin
val intent = Intent(context, MainActivityChild::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
}
context.startActivity(intent)
```

This launches `MainActivityChild` as a new task, clearing the therapist app's back stack. The child app then loads whatever configuration is currently active.

### How Configuration Data Flows to the Game Engine

```
Active Configuration in DB
        │
        ▼
GameViewModel.init {
  configurationRepository.getActiveConfiguration().collect { config →
    isTestMode = (config.activeMode == "test")

    // LearningSettings embedded in config → activeLearningSettings
    activeLearningSettings = config.toLearningSettings(
        materialState    = repository.getMaterialState(config.id),
        reinforcementState = repository.getReinforcementState(config.id)
    )
    // TestSettings embedded in config → activeTestSettings
    activeTestSettings = config.testSettings

    // Choose RoundSettings adapter
    roundSettings = if (isTestMode)
        activeTestSettings.asRoundSettings()
    else
        activeLearningSettings.asRoundSettings()

    // Generate all rounds
    rounds = generateGameRounds(repository, roundSettings, isTestMode)
  }
}
```

**Runtime mapping table:**

| Configured field | Consumed as |
|---|---|
| `LearningSettings.numberOfWords` | Word pool cap in `generateGameRounds` |
| `LearningSettings.displayedImagesCount` | Distractor count = `displayedImagesCount - 1` |
| `LearningSettings.repetitionPerWord` | Rounds per word (minimum 1) |
| `LearningSettings.commandType` | `GameViewModel.commandType` derived state |
| `LearningSettings.showLabelsUnderImages` | `showLabels` in `GameScreen` |
| `LearningSettings.readCommand` | `readCommand` controls TTS trigger |
| `LearningSettings.hintAfterSeconds` | Timer in `LaunchedEffect` (both modes) |
| `LearningSettings.typesOfHints` | `dimIncorrect`, `scaleCorrect`, `animateCorrect`, `outlineCorrect` derived states |
| `LearningSettings.typesOfPraises` | Random selection at congrats screen |
| `LearningSettings.animationsEnabled` | Sprite layer visibility |
| `ConfigurationImageUsage.inLearning` | Image eligibility filter in learning mode |
| `ConfigurationImageUsage.inTest` | Image eligibility filter in test mode |
| `Configuration.activeMode` | Selects `LearningSettings` vs. `TestSettings` path |

---

## 12. Full Therapist Workflow — End to End

```
STEP 1: Launch therapist app
  → MainActivity opens
  → InformationScreen shown for 5 seconds
  → MainScreenViewModel seeds example materials and configurations (first launch only)
  → MainContent dashboard displayed

STEP 2 (optional — if materials need creating): Navigate to Materials
  → Tap "MATERIAŁY EDUKACYJNE"
  → MaterialsListScreen displayed (list of all resources)
  → Tap "UTWÓRZ" in top bar
  → MaterialsCreatingNewMaterialScreen opened (empty form)

  STEP 2a: Enter word data
    → Type learnedWord (the word the child will learn)
    → Optionally customise name (defaults to learnedWord)
    → Optionally set category

  STEP 2b: Add images
    → Tap gallery icon → image picker → select image → copied to app storage
    → OR tap camera icon → take photo → saved to app storage
    → Repeat for more images (each added to the list)

  STEP 2c: Save material
    → Tap save icon
    → Name + learnedWord validated (both non-blank)
    → Duplicate name check runs
    → Resource inserted, images inserted + linked via resource_images
    → Orphaned images purged
    → navController.popBackStack() → back to MaterialsListScreen
    → Snackbar: "Pomyślnie dodano nowy materiał"

  → Repeat STEP 2 for all desired materials

STEP 3: Navigate to Learning Steps
  → From MainContent: tap "KROKI UCZENIA"
  → OR from MaterialsListScreen: navigate back to MainContent first
  → ConfigurationsListScreen displayed

STEP 4: Create a Learning Step
  → Tap "UTWÓRZ" in top bar
  → ConfigurationSettingsScreen opened (all 5 tabs, all defaults)

  TAB 0 — MATERIAŁ:
  STEP 4a: Add words to the configuration
    → Tap "DODAJ" button
    → Add Dialog opens (list of available resources)
    → Search/filter as needed
    → Tap a resource to add it
    → Dialog closes; resource appears in left panel
    → Repeat for all desired words

  STEP 4b: Configure image usage per word
    → Tap a word in the left panel to select it
    → Right panel shows that word's images
    → For each image:
      - Top checkbox: include/exclude image
      - "Uczenie" checkbox: eligible for learning mode
      - "Test" checkbox: eligible for test mode
    → Repeat for all words (optional: defaults are all images in both modes)

  TAB 1 — UCZENIE:
  STEP 4c: Configure trial parameters
    → "Liczba obrazków wyświetlanych na ekranie": set with ± buttons (1–6, capped by available)
    → "Liczba powtórzeń dla każdego słowa": set with ± buttons (1–3)
    → "Rodzaj polecenia": select from dropdown ({Słowo} / Gdzie jest / Pokaż gdzie jest)

  STEP 4d: Configure learning behaviour
    → "Podpisy pod obrazkami": toggle (default on)
    → "Głosowe odtwarzanie polecenia": toggle (default on)
    → "Pokaż podpowiedź po (sekundach)": set with ± buttons (1–10)
    → Hint type checkboxes: check/uncheck (at least one must remain checked)

  TAB 2 — WZMOCNIENIA:
  STEP 4e: Configure reinforcement
    → Praise word checkboxes: enable/disable individual words (default all on)
    → "Animacje" switch: enable/disable sprite animations (default on)

  TAB 3 — TEST:
  STEP 4f: Configure test mode (optional)
    → "Zmień dla testu" checkbox: leave unchecked to inherit from learning (default)
    → If checked: independently set imageCount, repetitionCount, prompt, captions, readCommand
    → answerTimeSeconds has no separate control: inherits from hintAfterSeconds

  TAB 4 — ZAPISZ:
  STEP 4g: Review and save
    → Right panel shows summary of all parameters for both modes
    → Type a unique name in the text field (left panel)
    → Tap "Zapisz"
    → Validation:
      - Name blank check → dialog if empty
      - Duplicate name check → dialog if duplicate
    → Configuration inserted to DB
    → Resource and image usage links inserted
    → navigateToList = true → back to ConfigurationsListScreen
    → Snackbar: "Pomyślnie dodano nowy krok uczenia"
    → List scrolls to newly created configuration

STEP 5: Activate the Learning Step
  → In ConfigurationsListScreen, find the new configuration
  → Tap the checkbox (or the name) → Activation confirmation dialog
  → Tap "TAK" → ConfirmActivate → isActive=1, activeMode="uczenie"
  → Configuration now shows "(aktywny krok w trybie: uczenie)"

STEP 6 (optional): Change mode to Test
  → Toggle the "Uczenie ↔ Test dla ucznia" switch on the active row
  → activeMode updated to "test" immediately (no confirmation)

STEP 7: Hand device to child or launch child app directly
  → OPTION A: Tap the green ▶ button in ConfigurationsListScreen
    → Launches MainActivityChild with FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
  → OPTION B: Child opens "Przyjazne Słowa Gra" launcher icon independently

LEARNING STEP IS NOW ACTIVE AND READY FOR EXECUTION
```

---

## 13. State Model of the Configuration Process

The configuration process does not have a formally implemented state machine with named states. Instead, it is governed by two flags and the navigation back-stack. The following model reconstructs the implicit states from the code.

### Wizard States (in-memory only)

```
┌──────────────────────────────────────────────────────────────────┐
│                  ConfigurationSettingsState                      │
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐ │
│  │  COMPOSING   │   │  VALIDATING  │   │      PERSISTING      │ │
│  │ (all tabs)   │──►│ (Save tab,   │──►│  (ViewModel launch)  │ │
│  │              │   │  Save button)│   │                      │ │
│  └──────────────┘   └──────────────┘   └──────────────────────┘ │
│         ▲                  │ reject              │ success       │
│         │                  ▼                     ▼               │
│         │           ┌──────────────┐   ┌──────────────────────┐ │
│         │           │  VALIDATION  │   │    navigateToList    │ │
│         │           │    ERROR     │   │    = true            │ │
│         │           │ (name empty  │   │    (triggers nav     │ │
│         │           │  or dup)     │   │     back)            │ │
│         └───────────┴──────────────┘   └──────────────────────┘ │
│                (dismiss dialog, fix name)                        │
└──────────────────────────────────────────────────────────────────┘
```

### Draft State

**Does not exist.** There is no draft. If the therapist exits the wizard without saving (via `ConfirmExitDialog`), `ConfigurationSettingsState` is reset to its default values:

```kotlin
is ConfigurationSettingsEvent.ConfirmExitDialog -> {
    _state.value = ConfigurationSettingsState()   // full reset
}
```

All work is lost. This is the only "cancel" path.

### Edit State vs. Create State

Differentiated by `saveState.editingConfigId`:

| `editingConfigId` | Wizard mode | `ConfigurationSettingsScreen` param |
|---|---|---|
| `null` | Create new | `configId = null` |
| non-null Long | Edit existing | `configId = configId` |

On edit, `loadConfiguration(configId)` pre-populates all 5 sub-states from the DB. The `testEditEnabled` flag is determined dynamically: if stored `testSettings != derived test state from learning settings`, then `testEditEnabled = true`.

### Configuration Entity States

```
┌────────────┐                                            ┌────────────┐
│  INACTIVE  │◄──── clearActiveConfiguration() ──────────│   ACTIVE   │
│ isActive=0 │                                            │ isActive=1 │
│activeMode= │                                            │activeMode= │
│    null    │────── activateConfiguration(id,mode) ─────►  "uczenie" │
└────────────┘                                            │    or      │
      ▲                                                   │   "test"   │
      │                                                   └────────────┘
  created                                                      ▲ ▼
  (isActive=0                                         setActiveMode() ←→
   by default)                                       toggle mode for
      │                                              already-active
      │                                              configuration
   deleted
(Room CASCADE removes
 configuration_resources
 configuration_image_usages)
```

### Example vs. User-Created State

| Property | `isExample=true` | `isExample=false` |
|---|---|---|
| Source | Seeded by `MainScreenViewModel` | Created/edited by therapist |
| Editable (wizard) | No — Edit button hidden | Yes |
| Deletable | No — Delete button hidden | Yes |
| Copyable | Yes — Copy button shown | Yes |
| Copy result | `isExample=false`, inactive | `isExample=false`, inactive |
| Hidden by preference | Yes (if `hideExampleSteps=true`) | No |

---

*End of Therapist Configuration Flow*
