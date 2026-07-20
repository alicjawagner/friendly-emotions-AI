# Learning Session Flow — Przyjazne Słowa v2

> **Status:** Execution-flow analysis based exclusively on source code.
> **Date:** 2026-06-18
> **Scope:** Complete flow from child launching the app to session end, covering both learning and test modes.

---

## Table of Contents

1. [Session Start](#1-session-start)
2. [Trial Generation](#2-trial-generation)
3. [Prompt Presentation](#3-prompt-presentation)
4. [Audio Playback](#4-audio-playback)
5. [User Response](#5-user-response)
6. [Answer Evaluation](#6-answer-evaluation)
7. [Reinforcement](#7-reinforcement)
8. [Error Handling](#8-error-handling)
9. [Repetition Logic](#9-repetition-logic)
10. [Session Completion](#10-session-completion)

---

## 1. Session Start

### Classes Involved

| Class | Role |
|---|---|
| `MainActivityChild` | Android entry point; forces landscape orientation; mounts the Compose tree |
| `ChildMainViewModel` | Owns the screen-state machine; executes the `canPlay` pre-flight check |
| `ScreenNavigationGame` | Root composable; routes between screens based on `ChildMainState.screenState` |
| `InformationScreen` | Splash/info composable shown for 5 seconds on first launch |
| `MainScreen` | Home screen composable; reads the active configuration and exposes Play button |
| `ConfigurationDao` | Directly injected into `ChildMainViewModel`; used to stream the active configuration |
| `ConfigurationRepository` | Used for `hasMaterialsForActiveConfig` check |

### Repositories / Database Access

| Operation | Class | Query |
|---|---|---|
| Stream active configuration | `ConfigurationDao.getActiveConfiguration()` | `SELECT * FROM configurations WHERE isActive = 1` |
| Check material availability | `ConfigurationRepository.hasMaterialsForActiveConfig(isTestMode)` | Calls `getResourcesWithImagesForActiveConfigFiltered(isTestMode)` — see §2 |

### Sequence

```
Child taps "Przyjazne Słowa Gra" launcher icon
        │
        ▼
MainActivityChild.onCreate()
  requestedOrientation = REVERSE_LANDSCAPE
  setContent { ScreenNavigationGame(hiltViewModel()) }
        │
        ▼
ChildMainViewModel created (Hilt)
  _state = ChildMainState(screenState = "info", canPlay = false)
        │
        ▼
ScreenNavigationGame observes state.screenState == "info"
  → renders InformationScreen()
  → LaunchedEffect(Unit): delay(5000ms)
  → onEvent(GoToNextScreen)  →  screenState = "main"
        │
        ▼
ScreenNavigationGame observes state.screenState == "main"
  → LaunchedEffect(Unit): viewModel.refreshCanPlay()
        │
        ▼
ChildMainViewModel.refreshCanPlay()
  configurationDao.getActiveConfiguration().firstOrNull()    ← DB read
    → isTest = (active?.activeMode == "test")
  configurationRepository.hasMaterialsForActiveConfig(isTest) ← DB read (see §2)
    → _state.update { canPlay = result }
        │
        ▼
MainScreen rendered
  configurationDao.getActiveConfiguration().collectAsState()  ← live DB stream
  → displays active configuration name + mode
  → Play button enabled iff canPlay == true
        │
        ▼
Child taps Play (only if canPlay == true)
  onEvent(GoToNextScreen)  →  screenState = "game"
```

**Detailed `refreshCanPlay` flow:**

`hasMaterialsForActiveConfig(isTestMode)` calls `getResourcesWithImagesForActiveConfigFiltered(isTestMode)`:
1. Reads active configuration ID from DB.
2. Reads all `ConfigurationResource` links for that ID.
3. Reads all `ConfigurationImageUsage` records for that ID; filters to those where `inLearning = true` (learning) or `inTest = true` (test).
4. Returns `true` if at least one resource has at least one eligible image.

If `canPlay = false`, the Play button is disabled and a warning message is displayed: *"Brak materiałów w kroku uczenia. Dodaj materiały lub zmień krok uczenia w aplikacji terapeuty."*

---

## 2. Trial Generation

### Classes Involved

| Class | Role |
|---|---|
| `GameViewModel` | Created when `screenState = "game"`; triggers round generation in `init {}` |
| `generateGameRounds()` | Top-level suspend function in `GameRoundManager.kt`; core generation algorithm |
| `ConfigurationRepository` | Provides active configuration and filtered resources |
| `RoundSettings` | Interface abstraction; implemented by `LearningSettings.asRoundSettings()` or `TestSettings.asRoundSettings()` |
| `GameRound` | Value object: `correctItem: GameItem` + `options: List<GameItem>` |
| `GameItem` | Value object: `label: String` (learnedWord) + `imagePath: String` |

### Repositories / Database Access

| Operation | Class | Query |
|---|---|---|
| Load active configuration | `ConfigurationDao.getActiveConfiguration()` | `SELECT * FROM configurations WHERE isActive = 1` |
| Load reinforcement state | `ConfigurationRepository.getReinforcementState(configId)` | Returns hardcoded default (all praises enabled); no DB query at this stage |
| Load material state | `ConfigurationRepository.getMaterialState(configId)` | Calls `getResourcesWithImagesForActiveConfig()` → joins resources + images |
| Load filtered resources | `ConfigurationRepository.getResourcesWithImagesForActiveConfigFiltered(isTestMode)` | Joins `resources`, `images`, `resource_images`, filters by `configuration_resources` and `configuration_image_usages` |

### Sequence

```
GameViewModel.init {
  configurationRepository.getActiveConfiguration().collect { config ->
    if (config != null) {
      isTestMode = (config.activeMode == "test")

      materialState    = repository.getMaterialState(config.id)         ← DB
      reinforcementState = repository.getReinforcementState(config.id)  ← DB (hardcoded)

      activeLearningSettings = config.toLearningSettings(
          materialState, reinforcementState
      )
      activeTestSettings = config.testSettings

      roundSettings = if (isTestMode)
          activeTestSettings.asRoundSettings()
      else
          activeLearningSettings.asRoundSettings()

      rounds = generateGameRounds(repository, roundSettings, isTestMode)
    }
  }
}
```

### `generateGameRounds` Algorithm (step by step)

```
generateGameRounds(repository, settings, isTestMode):

Step 1: Load eligible resources
  allResources = repository.getResourcesWithImagesForActiveConfigFiltered(isTestMode)
  if (allResources.isEmpty()) return emptyList()

Step 2: Select word pool
  numberOfWords = min(settings.numberOfWords, allResources.size)
  selectedResources = allResources.shuffled().take(numberOfWords)

Step 3: Generate rounds per word
  for each resource in selectedResources:
    allowedImages = resource.images          ← already mode-filtered
    if (allowedImages.isEmpty()) skip

    repeat max(settings.repetitionPerWord, 1) times:

      Step 3a: Pick correct answer
        correctImage = allowedImages.random()
        correctItem  = GameItem(label = resource.learnedWord,
                                imagePath = correctImage.path)

      Step 3b: Pick distractors
        distractorPool = (allResources − current resource)
                           .filter { it.images.isNotEmpty() }
                           .shuffled()
                           .take(max(settings.displayedImagesCount − 1, 0))

        distractors = distractorPool.map { dr →
            GameItem(label = dr.resource.learnedWord,
                     imagePath = dr.images.random().path)
        }

      Step 3c: Build round
        options = (distractors + correctItem).shuffled()
        rounds.add(GameRound(correctItem, options))

Step 4: Final shuffle
  return rounds.shuffled()
```

### `getResourcesWithImagesForActiveConfigFiltered` — Database Detail

```
1. SELECT * FROM configurations WHERE isActive = 1
   → activeConfig

2. SELECT * FROM configuration_resources WHERE configurationId = activeConfig.id
   → configResources  (set of resourceIds)

3. SELECT * FROM configuration_image_usages WHERE configurationId = activeConfig.id
   → usages
   → allowedImageIds = usages
       .filter { if (isTestMode) it.inTest else it.inLearning }
       .map { it.imageId }
       .toSet()

4. resourceDao.getResourcesWithImages()
   → all Resource + Image records joined via resource_images (Room @Relation)

5. Filter:
   - Keep only resources whose id ∈ configResources
   - For each resource, keep only images whose id ∈ allowedImageIds
   - Drop resources with no remaining images
```

---

## 3. Prompt Presentation

### Classes Involved

| Class | Role |
|---|---|
| `GameScreen` | Composable; builds and displays the command text |
| `GameViewModel` | Holds `commandType` (derived state) and `activeLearningSettings`/`activeTestSettings` |

### Command Text Assembly

The command text is assembled at the start of each round directly in `GameScreen`:

```kotlin
val commandType = viewModel.commandType.value          // derived state in ViewModel
val commandText = commandType.replace("{Słowo}", correctItem.label)
```

`GameViewModel.commandType` is a `derivedStateOf`:

```kotlin
val commandType: State<String> = derivedStateOf {
    if (isTestMode.value) {
        when (activeTestSettings.value?.commandType) {
            "SHORT"    -> "{Słowo}"
            "WHERE_IS" -> "Gdzie jest {Słowo}"
            "SHOW_ME"  -> "Pokaż gdzie jest {Słowo}"
            else       -> "{Słowo}"
        }
    } else {
        when (activeLearningSettings.value?.commandType) {
            "SHORT"    -> "{Słowo}"
            "WHERE_IS" -> "Gdzie jest {Słowo}"
            "SHOW_ME"  -> "Pokaż gdzie jest {Słowo}"
            else       -> "{Słowo}"
        }
    }
}
```

The `{Słowo}` placeholder is replaced at the call site with `correctItem.label` (the `learnedWord` of the target Resource).

### Result Examples

| `commandType` stored | `learnedWord` | Rendered text |
|---|---|---|
| `"SHORT"` | `"pies"` | `"pies"` |
| `"WHERE_IS"` | `"pies"` | `"Gdzie jest pies"` |
| `"SHOW_ME"` | `"pies"` | `"Pokaż gdzie jest pies"` |

The rendered text is displayed at **60 sp** bold white on the game screen.

### Label Display (under images)

Optionally, the `learnedWord` is shown beneath each image tile:
```kotlin
val showLabels = if (isTest)
    viewModel.activeTestSettings.value?.showLabelsUnderImages ?: false
else
    viewModel.activeLearningSettings.value?.showLabelsUnderImages ?: true
```

In learning mode labels are **on by default**; in test mode they are **off by default**.

---

## 4. Audio Playback

### Technology

Audio is delivered exclusively via **Android's built-in `android.speech.tts.TextToSpeech` API**. No third-party audio library is used.

### Classes Involved

| Class | Role |
|---|---|
| `GameScreen` | Creates, configures, and owns the `TextToSpeech` instance |
| `CorrectAnswerScreen` | Invokes `speakWordAndPraise` lambda passed from `GameScreen` |
| `TextToSpeech` (Android SDK) | Platform TTS engine |

### TTS Lifecycle

```kotlin
// Created once per GameScreen composition
val tts = remember {
    TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) ttsReady = true
    }
}

// Language set to Polish when engine is ready
LaunchedEffect(ttsReady) {
    if (ttsReady) tts.language = Locale("pl", "PL")
}

// Shutdown on screen disposal
DisposableEffect(Unit) {
    onDispose { tts.stop(); tts.shutdown() }
}
```

### Audio Triggered at Round Start

```kotlin
LaunchedEffect(currentRoundIndex, ttsReady) {
    // ...position tracking and state reset...

    if (ttsReady && readCommand) {
        tts.speak(commandText, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    // ...hint timer...
}
```

- `readCommand` is read from `activeLearningSettings.readCommand` (learning) or `activeTestSettings.readCommand` (test).
- `QUEUE_FLUSH` cancels any in-progress speech before starting the new utterance.
- If `readCommand = false`, no speech is produced for the command.

### Audio Triggered on Correct Answer (Learning Mode Only)

`CorrectAnswerScreen` receives a `speakWordAndPraise` lambda:

```kotlin
speakWordAndPraise = {
    if (ttsReady) {
        tts.speak(correctItem.label, TextToSpeech.QUEUE_FLUSH, null, "word")
        if (shouldReinforce && currentPraise.isNotBlank()) {
            tts.speak(currentPraise, TextToSpeech.QUEUE_ADD, null, "praise")
        }
    }
}
```

- First utterance: speaks the **target word** (`correctItem.label`) with `QUEUE_FLUSH`.
- Second utterance (conditional): speaks the **praise word** with `QUEUE_ADD`, so it plays after the word.
- The praise word is selected randomly from `activeLearningSettings.typesOfPraises` at the moment the congrats screen is shown.
- If `shouldReinforce = false` (the child had a prior mistake or hint was shown), only the word is spoken; no praise.

### Audio Summary Table

| Trigger | Content spoken | TTS mode | Condition |
|---|---|---|---|
| Round start | `commandText` | `QUEUE_FLUSH` | `readCommand = true` |
| Correct answer (learning) | `correctItem.label` | `QUEUE_FLUSH` | Always |
| Praise (learning) | random praise word | `QUEUE_ADD` | `shouldReinforce = true` AND praise word non-blank |

---

## 5. User Response

### Classes Involved

| Class | Role |
|---|---|
| `GameScreen` | Hosts the `handleAnswer(item)` local function; all input processing happens here |
| `RoundOptionsLayout` | Composable that renders the image grid and calls `onClick` for each tile |
| `GameViewModel` | Holds all response-tracking state flags |

### Input Flow

```
Child taps an image tile
        │
        ▼
RoundOptionsLayout.onClick(item: GameItem)
        │
        ▼
GameScreen.handleAnswer(item: GameItem):
  if (showCongratsScreen) return   ← ignore taps during congrats display

  val isCorrectClick = (item == correctItem)   ← identity comparison by value equality

  if (isCorrectClick && !correctClicked) → [see §6 Correct path]
  else if (!isCorrectClick)              → [see §8 Error path]
```

**Guard condition:** Taps are silently ignored if `showCongratsScreen = true` (congrats screen is showing). This prevents double-tap artifacts.

**Correctness evaluation:** `item == correctItem` uses Kotlin data class structural equality — the tap is correct if and only if the tapped `GameItem`'s `label` and `imagePath` both match those of `currentRound.correctItem`.

### `RoundOptionsLayout` → `handleAnswer` Mapping

The onClick lambda in `GameScreen` maps tapped items from the displayed list back to the canonical `currentRound.options` list to avoid identity issues:
```kotlin
onClick = { item ->
    currentRound.options.find { it.label == item.label }?.let { handleAnswer(it) }
}
```
Matching is done by `label` (the `learnedWord`) since multiple display items are created from the same source list.

---

## 6. Answer Evaluation

### Learning Mode — Correct Tap

```kotlin
if (isCorrectClick && !correctClicked) {
    correctClicked = true

    val noMistake = !hadMistakeThisRound && !showHint
    shouldReinforce = noMistake          // reinforcement only on clean correct

    if (!hadMistakeThisRound) correctAnswersCount++   // count only clean correct

    showCongratsScreen = true            // show congrats (learning mode)
    answerJudged = true
}
```

**Key rule:** `correctAnswersCount` is incremented **only** if `hadMistakeThisRound = false` at the time of the correct tap. A correct tap after a mistake or after a hint was shown is counted in the logic but does **not** increment `correctAnswersCount`.

### Learning Mode — Incorrect Tap

```kotlin
else if (!isCorrectClick) {
    if (!isTest && !showHint) {
        showHint = true       // immediately trigger hint
    }
}
```

In learning mode, the **first wrong tap** immediately triggers the hint. See §8 for what happens next.

### Test Mode — Correct Tap

```kotlin
if (isCorrectClick && !correctClicked) {
    correctClicked = true
    shouldReinforce = !hadMistakeThisRound && !showHint   // always false in test (no hints)
    if (!hadMistakeThisRound) correctAnswersCount++
    goNextAfterCongrats = true    // no congrats screen in test; advance immediately
    answerJudged = true
}
```

In test mode there is **no congrats screen**; the round advances immediately after a correct tap.

### Test Mode — Incorrect Tap

```kotlin
else if (!isCorrectClick) {
    if (!isTest && !showHint) { ... }   // condition is false in test mode → nothing
}
```

In test mode, **an incorrect tap has no immediate effect**. The child may continue tapping. The round only ends via correct tap or timer expiry (see §8).

### State After Evaluation

| Flag | Correct (clean) | Correct (after mistake) | Incorrect |
|---|---|---|---|
| `correctClicked` | `true` | `true` | unchanged |
| `answerJudged` | `true` | `true` | unchanged (learning) / unchanged (test) |
| `shouldReinforce` | `true` | `false` | unchanged |
| `correctAnswersCount` | `+1` | unchanged | unchanged |
| `showCongratsScreen` | `true` (learning) | `true` (learning) | unchanged |
| `goNextAfterCongrats` | `true` (test) | `true` (test) | unchanged |
| `showHint` | unchanged | unchanged | `true` (learning) |

---

## 7. Reinforcement

Reinforcement is only active in **learning mode**. It consists of two components: spoken praise and animated sprites.

### Classes Involved

| Class | Role |
|---|---|
| `GameScreen` | Selects the sprite set; builds `speakWordAndPraise` lambda; conditionally renders `CorrectAnswerScreen` |
| `CorrectAnswerScreen` | Invokes `speakWordAndPraise`; renders `FloatingSpritesLayer` |
| `FloatingSpritesLayer` | Renders N animated sprite instances across the screen |
| `FloatingSprite` | Single animated sprite with linear travel and optional rotation |
| `GameViewModel` | Provides `shouldReinforce`, `animationsEnabled`, `typesOfPraises` |
| `TextToSpeech` | Delivers spoken praise audio |

### Trigger Condition

Reinforcement fires **only** when:
- `shouldReinforce = true` — set iff the child answered correctly with no prior mistake (`!hadMistakeThisRound`) AND the hint had not yet been shown (`!showHint`).
- This means reinforcement fires on the **first correct tap without any errors or hints in that round**.

### Sequence

```
handleAnswer(correctItem) → correctClicked = true
                          → shouldReinforce = (!hadMistakeThisRound && !showHint)
                          → showCongratsScreen = true
        │
        ▼
GameScreen detects showCongratsScreen == true

LaunchedEffect(showCongratsScreen):
  praises = activeLearningSettings.typesOfPraises
  currentPraise = praises.randomOrNull().orEmpty()     ← random praise word selected

Sprite set selected:
  allSpriteSets = [
    (flowers [orange, pink, purple], direction=UP),
    (balloons [blue, yellow, green, red], direction=UP),
    (cars [red, yellow, green], direction=RIGHT)
  ]
  (chosenSprites, chosenDirection) =
    if (animationsEnabled && shouldReinforce) allSpriteSets.random()
    else (emptyList, UP)

CorrectAnswerScreen rendered:
  speakWordAndPraise lambda → tts.speak(word, QUEUE_FLUSH)
                           → if (shouldReinforce && praise.isNotBlank())
                               tts.speak(praise, QUEUE_ADD)

  FloatingSpritesLayer rendered (if chosenSprites non-empty):
    count = 20 (UP direction) or 7 (RIGHT direction)
    Each FloatingSprite:
      - starts below/beside screen
      - travels to opposite side with LinearEasing
      - travel duration = baseTravelMs ± 500ms random jitter
      - optional rotation (flowers, balloons rotate; cars do not)
      - scale = 0.4 to 0.55 (random per sprite)

After delay(4000ms) in CorrectAnswerScreen:
  onTimeout() → [see §9 Repetition Logic]
  goNextAfterCongrats = true → advance to next round
```

### Sprite Sets

| Set | Drawables | Direction | Count | Rotation |
|---|---|---|---|---|
| Flowers | `flower_orange`, `flower_pink`, `flower_purple` | UP | 20 | Yes |
| Balloons | `balloon_blue`, `balloon_yellow`, `balloon_green`, `balloon_red` | UP | 20 | Yes |
| Cars | `car_red`, `car_yellow`, `car_green` | RIGHT | 7 | No |

### Reinforcement vs. Non-Reinforcement Correct

| Condition | TTS word | TTS praise | Sprites |
|---|---|---|---|
| First correct, no errors (`shouldReinforce=true`, `animationsEnabled=true`) | Yes | Yes (random) | Yes (random set) |
| First correct, no errors (`shouldReinforce=true`, `animationsEnabled=false`) | Yes | Yes (random) | No |
| Correct after mistake/hint (`shouldReinforce=false`) | Yes | No | No |

---

## 8. Error Handling

### Learning Mode Error Flow

```
Child taps incorrect image
        │
        ▼
handleAnswer: isCorrectClick = false
  → if (!isTest && !showHint): showHint = true
        │
        ▼
LaunchedEffect(showHint):
  if (!isTest && showHint && !hadMistakeThisRound && !correctClicked):
    wrongAnswersCount++
    hadMistakeThisRound = true
    answerJudged = true
```

**Effect on the displayed round:** `showHint = true` activates all configured hint types simultaneously:

| Hint type (string value) | Visual effect | Driven by |
|---|---|---|
| `"Wyszarz niepoprawne"` | Incorrect images become dimmed/greyed | `dimIncorrect` derived state |
| `"Powiększ poprawną"` | Correct image scales up | `scaleCorrect` derived state |
| `"Porusz poprawną"` | Correct image animates (movement) | `animateCorrect` derived state |
| `"Obramuj poprawną"` | Correct image gets a border/outline | `outlineCorrect` derived state |

All hint visuals evaluate `showHint && !isTest` — hints are **never shown in test mode**.

The child may still tap after the hint appears. The round only ends when the child taps the correct image.

### Learning Mode — Hint Timer (no tap before deadline)

The hint timer runs concurrently with waiting for user input:

```kotlin
LaunchedEffect(currentRoundIndex, ttsReady) {
    // ... TTS, state reset ...

    if (!isTest && roundTimeoutMillis > 0L) {
        delay(roundTimeoutMillis)                    // wait hintAfterSeconds × 1000ms
        if (!correctClicked && !answerJudged && !showHint) {
            showHint = true                          // auto-trigger hint if no answer yet
        }
    }
}
```

`roundTimeoutMillis = hintAfterSeconds × 1000`. The hint timer fires only if the child has not yet answered correctly AND the hint is not already showing.

### Test Mode Error Flow

In test mode there are **no hints** and a wrong tap is silently ignored:

```kotlin
// handleAnswer — test mode wrong tap: nothing happens
if (!isTest && !showHint) { showHint = true }   // condition false in test
```

The timer fires after `hintAfterSeconds` (from `LearningSettings` — **note:** `TestSettings.answerTimeSeconds` is **not** used for the timeout in the current implementation):

```kotlin
} else if (isTest && roundTimeoutMillis > 0L) {
    delay(roundTimeoutMillis)
    if (!correctClicked && !answerJudged) {
        wrongAnswersCount++
        answerJudged = true
        goNextAfterCongrats = true          // advance to next round (no congrats screen)
    }
}
```

### Error State Summary

| Scenario | `wrongAnswersCount` | `hadMistakeThisRound` | `answerJudged` | Visual |
|---|---|---|---|---|
| Learning: wrong tap (first time) | `+1` | `true` | `true` | Hints shown |
| Learning: hint timer fires (no answer) | `+1` | `true` | `true` | Hints shown |
| Learning: wrong tap after hint already showing | no change | unchanged | unchanged | Hints already visible |
| Test: wrong tap | no change | unchanged | unchanged | Nothing |
| Test: timer expires | `+1` | unchanged | `true` | Round advances |

---

## 9. Repetition Logic

Repetition is the mechanism that re-queues a failed trial into the upcoming round sequence. It is executed in the `onTimeout` callback of `CorrectAnswerScreen`, which fires 4000ms after the congrats screen appears.

**This mechanism is active in learning mode only.** In test mode there is no congrats screen and `onTimeout` is never called.

### State Variable: `repeatStage`

`repeatStage: MutableState<Int>` in `GameViewModel` tracks where a word is in its error-recovery cycle. It starts at `0` and persists across rounds within a session.

### Algorithm

```
onTimeout() called after 4000ms on CorrectAnswerScreen:

evaluate (repeatStage, hadMistakeThisRound):

  CASE repeatStage == 0:
    if (hadMistakeThisRound):
      → insert currentRound at position [currentRoundIndex + 1]  (same layout)
      → repeatStage = 1
    else:
      → (no insertion — word was answered cleanly, no repeat needed)

  CASE repeatStage == 1:
    if (hadMistakeThisRound):
      → insert currentRound at position [currentRoundIndex + 1]  (same layout)
      → repeatStage stays 1
    else:
      → insert shuffledRound (different option positions) at [currentRoundIndex + 1]
      → repeatStage = 2

  CASE repeatStage == 2:
    if (hadMistakeThisRound):
      → insert shuffledRound (different option positions) at [currentRoundIndex + 1]
      → repeatStage stays 2
    else:
      → repeatStage = 0   (recovery complete, no repeat)

after all cases:
  hadMistakeThisRound = false    ← reset for the upcoming repeated round
  goNextAfterCongrats = true     ← advance
```

### Round Insertion

When a round is re-queued, it is inserted **immediately after the current round** in the live `rounds` list:
```kotlin
val roundsList = rounds.toMutableList()
roundsList.add(currentRoundIndex + 1, currentRound)   // or shuffledRound
viewModel.rounds.value = roundsList
```

This means the child encounters the failed word again on the very next trial.

### Shuffled Repeat

For stage-1 → stage-2 transitions (correct after one mistake) and stage-2 continuations, a **different option layout** is used to prevent position memorisation:

```kotlin
val shuffledRound = viewModel.shuffledRoundAvoidingPrevious(
    currentRound,
    activeLearningSettings.displayedImagesCount
)
```

`shuffledRoundAvoidingPrevious` tries up to 50 candidate shuffles to find one where the correct answer does not appear in the same on-screen position it occupied in the previous occurrence of this word.

### Repetition State Machine

```
          ┌─────────────────────────────────────────────────────────┐
          │                   repeatStage transitions               │
          │                                                         │
          │         answered cleanly                                │
          │  ╔═══╗ ─────────────────────────────► (no repeat)      │
          │  ║ 0 ║                                                  │
          │  ╚═══╝ ──(mistake)──► ╔═══╗                            │
          │    ▲                  ║ 1 ║                             │
          │    │      (mistake)   ╚═══╝ ──(correct 2nd time)──►    │
          │    │    ┌─────────────► │                               │
          │    │    │               │   ╔═══╗                       │
          │    │    │               └──►║ 2 ║                       │
          │    │    │     (mistake)     ╚═══╝                       │
          │    │    └────────────────────┤                          │
          │    │                        │ (correct)                 │
          │    └────────────────────────┘                           │
          │              reset to 0                                 │
          └─────────────────────────────────────────────────────────┘
```

### Repetition Summary

| `repeatStage` | `hadMistakeThisRound` | Action | New `repeatStage` |
|---|---|---|---|
| 0 | false | No repeat | 0 |
| 0 | true | Requeue same layout | 1 |
| 1 | true | Requeue same layout | 1 |
| 1 | false | Requeue shuffled layout | 2 |
| 2 | true | Requeue shuffled layout | 2 |
| 2 | false | No repeat | 0 |

---

## 10. Session Completion

### Classes Involved

| Class | Role |
|---|---|
| `GameViewModel.goToNext()` | Detects last round; calls `onGameFinished`; resets state; regenerates rounds |
| `GameScreen` | Passes `onGameFinished` lambda to `GameViewModel.goToNext()` |
| `ScreenNavigationGame` | Receives `onGameFinished` callback; updates `ChildMainViewModel` state |
| `ChildMainViewModel` | Stores `correctAnswers` and `wrongAnswers`; transitions to `"end"` screen |
| `GameEndScreen` | Displays final counts; provides Play Again button |

### Sequence

```
GameViewModel.goToNext(onGameFinished):
  if (currentRoundIndex < rounds.lastIndex):
    currentRoundIndex++       ← normal round advance
  else:
    onGameFinished(correctAnswersCount, wrongAnswersCount)  ← session complete

    // Reset all counters for next session
    correctAnswersCount = 0
    wrongAnswersCount   = 0
    currentRoundIndex   = 0
    repeatStage         = 0

    // Regenerate rounds immediately for the next session
    launch {
      roundSettings = (isTestMode ? activeTestSettings : activeLearningSettings)
                      .asRoundSettings()
      rounds = generateGameRounds(repository, roundSettings, isTestMode)
    }

  // Per-round flag reset (always)
  answerJudged        = false
  correctClicked      = false
  hadMistakeThisRound = false
  showCongratsScreen  = false
  goNextAfterCongrats = false
  showHint            = false
  shouldReinforce     = false
```

**Note:** `rounds` is regenerated asynchronously immediately after session end, so the next session is ready before the child taps Play Again.

### ScreenNavigationGame Callback

```
onGameFinished(correct, wrong) in ScreenNavigationGame:
  viewModel.onEvent(SetCorrectAnswers(correct))  → state.correctAnswers = correct
  viewModel.onEvent(SetWrongAnswers(wrong))       → state.wrongAnswers   = wrong
  viewModel.onEvent(GoToNextScreen)              → state.screenState    = "end"
```

### GameEndScreen

```
GameEndScreen:
  displays "Koniec gry!"
  displays "Poprawne odpowiedzi: {correctAnswers}"
  displays "Niepoprawne odpowiedzi: {wrongAnswers}"
  renders PlayButton

Child taps PlayButton:
  onPlayAgain()
  → viewModel.onEvent(GoToNextScreen)  → state.screenState = "main"
```

After returning to `"main"`, `refreshCanPlay()` is called again (see §1) to verify the active configuration is still valid before the next session.

---

## Complete Session Sequence Diagram

```
Child         MainActivityChild   ChildMainViewModel   GameViewModel       DB
  │                  │                    │                  │              │
  │──tap launcher───►│                    │                  │              │
  │                  │──hiltViewModel()──►│                  │              │
  │                  │                   │──screenState="info"              │
  │                  │◄──ScreenNavigationGame(vm)                           │
  │                  │    [InformationScreen shown 5s]                      │
  │                  │                   │←─delay(5000)─────────────────────│
  │                  │                   │──screenState="main"              │
  │                  │◄──MainScreen                          │              │
  │                  │                   │──refreshCanPlay()─►              │
  │                  │                   │                  │──SELECT configs WHERE isActive=1
  │                  │                   │                  │◄──activeConfig│
  │                  │                   │                  │──SELECT config_resources + usages
  │                  │                   │                  │◄──filtered resources
  │                  │                   │◄──canPlay=true────              │
  │                  │◄──Play button enabled                │              │
  │──tap Play───────►│                   │                  │              │
  │                  │                   │──screenState="game"             │
  │                  │◄──GameScreen(hiltViewModel())         │              │
  │                  │                   │            GameViewModel.init{}  │
  │                  │                   │                  │──getActiveConfiguration().collect
  │                  │                   │                  │──getMaterialState() ────────────►DB
  │                  │                   │                  │◄─materialState──────────────────│
  │                  │                   │                  │──getReinforcementState()────────►DB
  │                  │                   │                  │◄─reinforcementState─────────────│
  │                  │                   │                  │──generateGameRounds()────────────►DB
  │                  │                   │                  │◄─List<GameRound>─────────────────│
  │                  │◄──rounds rendered (RoundOptionsLayout)│              │
  │                  │   [TTS speaks commandText]            │              │
  │                  │                   [hint timer starts] │              │
  │                  │                                       │              │
  │  [Incorrect tap] │                                       │              │
  │──tap wrong image►│──handleAnswer(wrong)──────────────────►              │
  │                  │                   showHint=true       │              │
  │                  │◄──hints shown (dim/scale/outline/move)│              │
  │                  │   wrongAnswersCount++                 │              │
  │                  │                                       │              │
  │  [Correct tap]   │                                       │              │
  │──tap correct────►│──handleAnswer(correct)────────────────►              │
  │                  │  correctClicked=true                  │              │
  │                  │  shouldReinforce=(hadMistake ? false : true)         │
  │                  │  correctAnswersCount++ (if no prior mistake)         │
  │                  │  showCongratsScreen=true              │              │
  │                  │◄──CorrectAnswerScreen shown           │              │
  │                  │   [TTS speaks word + praise]          │              │
  │                  │   [FloatingSprites if shouldReinforce]│              │
  │                  │                   [onTimeout after 4000ms]           │
  │                  │                   ── repeatStage logic (§9)          │
  │                  │                   ── goNextAfterCongrats=true        │
  │                  │                   GameViewModel.goToNext()            │
  │                  │                     if lastRound:                    │
  │                  │◄──onGameFinished(correct, wrong)───────              │
  │                  │                   │──SetCorrectAnswers(n)            │
  │                  │                   │──SetWrongAnswers(n)              │
  │                  │                   │──GoToNextScreen → "end"          │
  │                  │◄──GameEndScreen   │                  │              │
  │                  │   [rounds regenerated asynchronously]│──generateGameRounds()──►DB
  │──tap PlayAgain──►│                   │                  │              │
  │                  │                   │──GoToNextScreen → "main"        │
  │                  │                   │──refreshCanPlay() ──────────────►DB
  │                  │◄──MainScreen      │                  │              │
```

---

## Appendix A — `GameViewModel` State Flag Reference

| Flag | Type | Initial | Set to `true` | Set to `false` |
|---|---|---|---|---|
| `isTestMode` | `Boolean` | `false` | Active config `activeMode == "test"` | Active config changes |
| `correctClicked` | `Boolean` | `false` | Correct tap received | `goToNext()` |
| `answerJudged` | `Boolean` | `false` | Any judgment (correct tap, hint fires, timer expires) | `goToNext()` |
| `hadMistakeThisRound` | `Boolean` | `false` | Wrong tap in learning mode / hint fires | `goToNext()` + after `onTimeout` |
| `showHint` | `Boolean` | `false` | Wrong tap (learning) / hint timer fires | `goToNext()` |
| `showCongratsScreen` | `Boolean` | `false` | Correct tap (learning mode) | `goToNext()` |
| `goNextAfterCongrats` | `Boolean` | `false` | `onTimeout()` / test correct tap / test timer | `goToNext()` start |
| `shouldReinforce` | `Boolean` | `false` | Correct tap AND no prior mistake AND no hint | `goToNext()` |
| `correctAnswersCount` | `Int` | `0` | Correct tap (no prior mistake) | `goToNext()` on session end |
| `wrongAnswersCount` | `Int` | `0` | Hint fires (learning) / timer expires (test) | `goToNext()` on session end |
| `repeatStage` | `Int` | `0` | See §9 | `goToNext()` on session end / §9 recovery |

---

## Appendix B — Mode Comparison Table

| Behaviour | Learning Mode | Test Mode |
|---|---|---|
| Hints shown | Yes (on wrong tap or timer) | Never |
| Hint timer action | Show hint | Count wrong + advance round |
| Timer value used | `learningSettings.hintAfterSeconds` | `learningSettings.hintAfterSeconds` (same field) |
| Labels under images (default) | Yes | No |
| TTS command (default) | Yes | No |
| Congrats screen | Yes (4s) | No |
| Reinforcement animations | Yes (if clean correct) | No |
| Reinforcement TTS praise | Yes (if clean correct) | No |
| Target word spoken on correct | Yes | No |
| Wrong tap increments counter | Via hint trigger | Never (only timer) |
| Repetition on error | Yes (`repeatStage` logic) | No |

---

*End of Learning Session Flow*
