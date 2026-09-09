# Target Domain Model — Friendly Emotions

> **Role:** Principal Software Architect — Domain-Driven Design
> **Date:** 2026-07-16
> **Scope:** Business domain only. No Android, Jetpack Compose, Room, or persistence details.
> **Sources:** `friendly-emotions-functional-specification.md`, `02-domain-model.md`, `03-learning-session-flow.md`, `04-therapist-configuration-flow.md`, `06-architecture-improvement-analysis.md`

---

## Table of Contents

1. [Domain Design Principles](#1-domain-design-principles)
2. [Core Business Concepts](#2-core-business-concepts)
3. [Domain Entities](#3-domain-entities)
4. [Value Objects](#4-value-objects)
5. [Aggregates](#5-aggregates)
6. [Relationships](#6-relationships)
7. [Domain Services](#7-domain-services)
8. [Business Rules](#8-business-rules)
9. [State Transitions](#9-state-transitions)
10. [Learning Session Lifecycle](#10-learning-session-lifecycle)
11. [Learning Step Lifecycle](#11-learning-step-lifecycle)
12. [Trial Lifecycle](#12-trial-lifecycle)
13. [Reinforcement Model](#13-reinforcement-model)
14. [Error Correction Model](#14-error-correction-model)
15. [Configuration Model](#15-configuration-model-business-perspective)
16. [Domain Events](#16-domain-events)

---

## 1. Domain Design Principles

1. **Platform independence.** All domain objects are pure data and behavior with no dependency on Android, persistence, or UI frameworks.
2. **Gender as a first-class concept.** `GrammaticalGender` pervades the material model and drives trial prompt generation. It is not a display concern; it is a domain invariant.
3. **Emotion-first hierarchy.** Material is organized as `Emotion → Folder → Image`, replacing the flat `Resource` model from Friendly Words. This accurately reflects the therapist's mental model of teaching material.
4. **Fixed emotion catalog.** The six emotions are domain constants, not user-creatable entities. This simplifies the material model and reflects the real-world therapeutic context.
5. **Reuse of proven session mechanics.** The trial lifecycle, error correction model, reinforcement model, and position anti-repeat mechanism are inherited from Friendly Words and applied without modification. Only material-loading and prompt-rendering are redesigned.
6. **Aggregate per concern.** `LearningStep` owns all session configuration. `EmotionFolder` is the effective aggregate root for material management within one emotion.
7. **Bilingual by design.** Language support is a first-class domain concern: `EmotionLabel` carries all gender/locale forms. Prompt rendering is locale-aware.
8. **Separation of configuration from execution.** LearningParameters and TestParameters are configuration-time decisions. Trial, RenderedPrompt, and TrialOption are runtime value objects that never persist.
9. **Extensibility via thin abstractions.** Core session mechanics are expressed through value objects and domain services that can accommodate new stimulus types, session modes, and reinforcement patterns without structural changes.

---

## 2. Core Business Concepts

### Glossary

| Term (EN) | Term (PL) | Definition |
|---|---|---|
| **Emotion** | Emocja | One of 6 fixed categories (happy, sad, surprised, angry, scared, bored) that the child learns to recognize. A catalog constant; not user-created or deleted. |
| **Emotion Folder** | Folder emocji | A therapist-managed grouping of images within one emotion (e.g. "Men", "Women", "Emojis"). Has a gender policy that governs images added to it. |
| **Emotion Image** | Zdjęcie emocji | A photograph depicting a person (or emoji/animal) expressing one specific emotion, with an assigned grammatical gender. The visual stimulus shown during trials. |
| **Grammatical Gender** | Rodzaj gramatyczny | Linguistic gender (masculine/feminine/neuter) of a Polish emotion adjective. Drives the inflected form used in the trial prompt. Comes from the correct-answer image. |
| **Folder Gender Policy** | Polityka rodzaju folderu | Rule governing how images in a folder receive their grammatical gender: automatically from the folder (fixed policy) or individually assigned (mixed policy). |
| **Emotion Label** | Etykieta emocji | The localized, gender-inflected name of an emotion. Polish requires three gendered forms; English uses one neutral form. |
| **Prompt** | Polecenie | The instruction displayed on screen (emotion name only) and spoken aloud (full template phrase). Its Polish form is gender-inflected from the correct-answer image. |
| **Prompt Template** | Szablon polecenia | The sentence structure wrapping the emotion name (e.g. "Where is {emotion}?"). Chosen by the therapist. |
| **Learning Step** | Krok uczenia | A named, complete session plan. Specifies which images to use and all session parameters. Analogous to a lesson plan. Exactly one is active at any time. |
| **Session** | Sesja | A sequence of trials generated from the active Learning Step. Runs until all trials are completed. |
| **Trial** | Próba / Runda | A single presentation: a gender-inflected prompt + a set of image options. The child taps the image matching the prompt. |
| **Option** | Opcja | One image shown on screen during a trial. Exactly one is correct; the rest are distractors. |
| **Distractor** | Dystraktor | An incorrect option drawn from a different emotion. May or may not share the correct image's grammatical gender depending on configuration. |
| **Clean Correct** | Czysta poprawna | A correct response given with no prior mistake and no hint shown in the same trial. Only clean-correct answers trigger reinforcement. |
| **Reinforcement** | Wzmocnienie | Positive feedback (verbal praise + optional sprite animation) delivered on a clean-correct answer in learning mode. |
| **Hint** | Podpowiedź | A visual aid (outline, scale, animate, dim) that reveals the correct answer after a configurable delay. Available in learning mode only. |
| **Session Mode** | Tryb sesji | `LEARNING` (guided: hints, reinforcement, error correction) or `TEST` (silent, timed, no assistance, scores recorded). |
| **Active Learning Step** | Aktywny krok uczenia | The single Learning Step currently marked active. The child app always uses it. Fallback to an example step if the active one is deleted. |
| **Example Content** | Dane przykładowe | Pre-seeded folders, images, and learning steps. Folders and steps are non-deletable (images may be hidden). Provides immediate demo functionality on first launch. |
| **Image Exhaustion** | Wyczerpanie puli | All eligible images for an emotion have been shown at least once. A new random cycle begins after exhaustion. Prevents image repetition until the full pool is consumed. |

---

## 3. Domain Entities

### 3.1 Emotion (Catalog Item)

The six emotions are fixed domain constants. They are never created, renamed, or deleted by users. They exist as a stable catalog that the entire material hierarchy references.

| Concept | Description |
|---|---|
| `EmotionId` | Stable identifier: `HAPPY`, `SAD`, `SURPRISED`, `ANGRY`, `SCARED`, `BORED` |
| `labels` | Map of locale → `EmotionLabel` (Polish gendered forms; English neutral form) |

**English names:** happy, sad, surprised, angry, scared, bored
**Polish names (masculine / feminine / neuter):** wesoły/wesoła/wesołe · smutny/smutna/smutne · zdziwiony/zdziwiona/zdziwione · zły/zła/złe · przestraszony/przestraszona/przestraszone · znudzony/znudzona/znudzone

**Responsibility:** Serve as the top-level grouping for all teaching material; provide gender-inflected labels for prompt rendering.

---

### 3.2 EmotionFolder (Entity)

A therapist-managed grouping of images within exactly one emotion.

| Field | Type | Constraints |
|---|---|---|
| `id` | `FolderId` | Unique identifier |
| `emotionId` | `EmotionId` | Which of the 6 fixed emotions this folder belongs to; immutable after creation |
| `name` | `String` | User-defined display name (e.g. "Women", "Emojis", "Other") |
| `genderPolicy` | `FolderGenderPolicy` | `MASCULINE` / `FEMININE` / `NEUTER` / `MIXED`; **immutable after creation** |
| `isExample` | `Boolean` | Pre-seeded folder; cannot be deleted; images can be hidden but not deleted |

**Responsibilities:**
- Group images within one emotion by subject category.
- Enforce gender assignment for newly added images: automatic for fixed-gender policies, mandatory individual assignment for `MIXED`.
- Cascade deletion to all contained images.

**Invariants:**
- `genderPolicy` cannot change after creation.
- A folder belongs to exactly one emotion and cannot be reassigned.
- Deleting a user-created folder deletes all its images (files included).
- Example folders cannot be deleted.

---

### 3.3 EmotionImage (Entity)

A photograph stored in device storage, belonging to exactly one folder, with an assigned grammatical gender.

| Field | Type | Constraints |
|---|---|---|
| `id` | `ImageId` | Unique identifier |
| `folderId` | `FolderId` | Owning folder; immutable after creation |
| `filePath` | `String` | Device file path or bundled asset URI |
| `gender` | `GrammaticalGender` | `MASCULINE` / `FEMININE` / `NEUTER`; mandatory; editable after creation |
| `isExample` | `Boolean` | Pre-seeded; may be hidden by therapist preference but not deleted |

**Responsibility:** Be the visual stimulus selected during a trial. Carry the grammatical gender that drives prompt inflection.

**Invariants:**
- Every image must have exactly one `GrammaticalGender`.
- Images in a fixed-gender folder inherit that gender automatically on creation.
- Images in a `MIXED` folder require individual gender assignment; saving is blocked until every image has one.
- Deleting an image removes its physical file from storage.

---

### 3.4 LearningStep (Aggregate Root)

A complete, named session plan. The primary aggregate of the configuration domain. At most one is active at any time.

| Field | Type | Constraints |
|---|---|---|
| `id` | `LearningStepId` | Unique identifier |
| `name` | `String` | Unique (case-insensitive) across all LearningSteps |
| `isActive` | `Boolean` | True for at most one LearningStep at a time |
| `mode` | `SessionMode` | `LEARNING` or `TEST`; always present. The mode this step runs in when activated. Persists independently of `isActive` and survives across app restarts, including while the step is inactive. |
| `isExample` | `Boolean` | Pre-seeded; not editable or deletable, but copyable |
| `materialSelection` | `MaterialSelection` | Which images are included and in which modes |
| `learningParameters` | `LearningParameters` | All learning-mode session parameters |
| `testParameters` | `TestParameters` | All test-mode session parameters |
| `reinforcementSettings` | `ReinforcementSettings` | Praise and animation settings |

**Responsibilities:**
- Define what the child experiences in a session and how the session behaves.
- Enforce name uniqueness and mode consistency.
- Own the material selection and all configuration parameters.

**Invariants:**
- `name` is unique (case-insensitive).
- `mode` is always present (non-null) regardless of `isActive`; activating or deactivating a
  step never changes its stored `mode`, except the automatic example-step fallback on deletion,
  which always forces the fallback's `mode` to `LEARNING` (§8.4, §9.1).
- `TestParameters.overridesLearning = false` means test parameters mirror learning parameters.
- Example steps cannot be edited or deleted; copies are always inactive and non-example.

---

## 4. Value Objects

### 4.1 GrammaticalGender

```
MASCULINE | FEMININE | NEUTER
```

Determines the Polish adjective form used in the trial prompt. Always derived from the **correct-answer image** of the trial being presented. This rule is absolute — distractors' genders are irrelevant to the prompt.

---

### 4.2 FolderGenderPolicy

```
MASCULINE | FEMININE | NEUTER | MIXED
```

- `MASCULINE`, `FEMININE`, `NEUTER`: all images added to this folder automatically receive the corresponding gender.
- `MIXED`: each image requires individual gender assignment; saving is blocked until every image has a gender.
- Cannot be changed after folder creation. This protects existing image gender assignments from becoming inconsistent.

---

### 4.3 EmotionLabel

Carries all locale and gender forms for one emotion's name.

| Field | Description |
|---|---|
| `masculine` | Polish masculine adjective form (e.g. "wesoły") |
| `feminine` | Polish feminine adjective form (e.g. "wesoła") |
| `neuter` | Polish neuter adjective form (e.g. "wesołe") |
| `neutral` | Gender-neutral form; used in English and as display-only label (e.g. "happy") |

**Selection rule:** for a prompt in Polish, the form is selected by the correct-answer image's `GrammaticalGender`. In English, `neutral` is always used.

---

### 4.4 PromptTemplate

Defines the sentence structure wrapping the emotion label. Seven templates are available:

| Template | Polish spoken | English spoken |
|---|---|---|
| `EMOTION_ONLY` | `{emocja}` | `{emotion}` |
| `WHERE_IS` | `Gdzie jest {emocja}?` | `Where is {emotion}?` |
| `SHOW_ME` | `Pokaż, gdzie jest {emocja}.` | `Show me {emotion}.` |
| `FIND` | `Znajdź, gdzie jest {emocja}.` | `Find {emotion}.` |
| `TOUCH` | `Dotknij, gdzie jest {emocja}.` | `Touch {emotion}.` |
| `POINT_TO` | `Wskaż, gdzie jest {emocja}.` | `Point to {emotion}.` |
| `CHOOSE` | `Wybierz, gdzie jest {emocja}.` | `Choose {emotion}.` |

The screen always displays only the emotion name (never the full phrase). TTS speaks the full phrase.

---

### 4.5 SessionMode

```
LEARNING | TEST
```

`LEARNING`: spoken prompts, hints, praise animations, reinforcement, error-correction re-queuing. No scores displayed during session.
`TEST`: timed, no hints, no reinforcement during rounds. Score (percentage correct, counts) recorded and shown at session end.

---

### 4.6 HintType

```
OUTLINE_CORRECT | ANIMATE_CORRECT | SCALE_CORRECT | DIM_INCORRECT
```

At least one hint type must remain active in `LearningParameters` at all times. When a hint triggers, all currently active hint types fire simultaneously. Hints are never shown in `TEST` mode.

---

### 4.7 MaterialSelection

Owned by `LearningStep`. Records which images are included and for which modes.

| Field | Type | Description |
|---|---|---|
| `imageUsages` | `List<ImageUsage>` | One entry per selected image |

---

### 4.8 ImageUsage

| Field | Type | Description |
|---|---|---|
| `imageId` | `ImageId` | Reference to the selected `EmotionImage` |
| `inLearning` | `Boolean` | Eligible as a trial option in learning mode |
| `inTest` | `Boolean` | Eligible as a trial option in test mode |

An image may be assigned to learning only, test only, or both. An image with both flags false is effectively deselected.

---

### 4.9 LearningParameters

Controls all aspects of learning-mode trial generation and presentation.

| Field | Type | Default | Range |
|---|---|---|---|
| `displayedImageCount` | `Int` | 3 | 1–6 |
| `repetitionsPerEmotion` | `Int` | 2 | 1–10 |
| `promptTemplate` | `PromptTemplate` | `EMOTION_ONLY` | — |
| `ttsEnabled` | `Boolean` | `true` | — |
| `captionsEnabled` | `Boolean` | `true` | — |
| `hintDelaySeconds` | `Int` | 5 | 3–10 |
| `activeHintTypes` | `Set<HintType>` | `{DIM_INCORRECT}` | ≥1 element |
| `mixedGenderInAnswers` | `Boolean` | `true` | — |

`mixedGenderInAnswers`: when `false`, distractor images are constrained to share the correct image's grammatical gender, making trials more uniform and easier.

`hintDelaySeconds` doubles as the test-mode answer time limit (see §4.10).

---

### 4.10 TestParameters

Controls test-mode session behavior. By default mirrors `LearningParameters`.

| Field | Type | Default | Notes |
|---|---|---|---|
| `overridesLearning` | `Boolean` | `false` | When `false`, all other fields mirror `LearningParameters` |
| `displayedImageCount` | `Int` | — | Inherited or independent |
| `repetitionsPerEmotion` | `Int` | — | Inherited or independent |
| `promptTemplate` | `PromptTemplate` | — | Inherited or independent |
| `ttsEnabled` | `Boolean` | `false` | Inherited; default off for independent setting |
| `captionsEnabled` | `Boolean` | `false` | Inherited; default off for independent setting |
| `mixedGenderInAnswers` | `Boolean` | `true` | Inherited or independent |

**Answer time limit:** shared with `LearningParameters.hintDelaySeconds`. No separate field — when the timer fires in test mode it counts the trial as wrong and advances.

**Inheritance rule:** setting `overridesLearning` back to `false` immediately reverts all test parameters to current learning parameter values.

---

### 4.11 ReinforcementSettings

| Field | Type | Default | Description |
|---|---|---|---|
| `enabledPraiseWords` | `Set<String>` | all 6 | Active verbal praise words; one selected randomly per reinforcement event |
| `enabledAnimationThemes` | `Set<String>` | all 5 | Active sprite animation themes; one selected randomly per reinforcement event when animations are on |
| `animationsEnabled` | `Boolean` | `true` | Sprite animations after clean-correct answers |
| `endSessionAnimationEnabled` | `Boolean` | `true` | Animation played at session end |
| `endSessionFanfareEnabled` | `Boolean` | `true` | Audio fanfare played at session end |

Available praise word keys (stored in settings): "dobrze", "super", "świetnie", "ekstra", "rewelacja", "brawo". Spoken form follows device locale via `PraiseCatalog`: Polish for `pl`, English otherwise (`dobrze`→good, `super`→super, `świetnie`→great, `ekstra`→awesome, `rewelacja`→amazing, `brawo`→bravo).
Available animation themes: flowers, butterflies, balloons, cars, balls. One theme selected at random from `enabledAnimationThemes` per event.

---

### 4.12 RenderedPrompt (runtime)

Produced by `PromptRenderer` at trial presentation time. Not persisted.

| Field | Description |
|---|---|
| `displayText` | Emotion name only, gender-inflected (Polish) or neutral (English) |
| `spokenText` | Full template phrase, gender-inflected for the correct-answer image |

---

### 4.13 Trial (runtime)

Generated by `TrialGenerator` for each session. Not persisted.

| Field | Type | Description |
|---|---|---|
| `targetEmotionId` | `EmotionId` | The emotion the child must identify |
| `promptGender` | `GrammaticalGender` | Derived from the correct option's gender; immutable after generation |
| `correctOption` | `TrialOption` | The correct answer |
| `allOptions` | `List<TrialOption>` | Shuffled list of all options (correct + distractors) |

---

### 4.14 TrialOption (runtime)

One selectable image within a trial. Not persisted.

| Field | Type | Description |
|---|---|---|
| `imageId` | `ImageId` | Source image reference |
| `imagePath` | `String` | File path for display |
| `emotionId` | `EmotionId` | Which emotion this image depicts |
| `gender` | `GrammaticalGender` | Grammatical gender of this image |

---

## 5. Aggregates

### 5.1 Material Catalog Aggregate

Emotions are the conceptual root of the material hierarchy but are fixed catalog items. The **EmotionFolder** is the effective aggregate root for material management: therapists create, name, and delete folders; images are managed through folders.

```
Emotion (catalog — 6 fixed instances)
  └── EmotionFolder (aggregate root for material)
        └── EmotionImage (leaf entity)
```

**Invariants enforced at folder boundary:**
- Gender policy is immutable after creation.
- Images are added and removed through the folder.
- Folder deletion cascades to all images and their physical files.
- Example folders and their images are protected from deletion.

### 5.2 LearningStep Aggregate

```
LearningStep (aggregate root)
  ├── MaterialSelection
  │     └── ImageUsage[]     (per selected EmotionImage)
  ├── LearningParameters
  │     └── activeHintTypes  (Set<HintType>)
  ├── TestParameters
  └── ReinforcementSettings
        ├── enabledPraiseWords (Set<String>)
        └── enabledAnimationThemes (Set<String>)
```

**Invariants enforced at LearningStep boundary:**
- Name uniqueness is checked across all LearningSteps before save.
- `activeMode` is null iff `isActive` is false (enforced via `LearningStepActivationService`).
- `TestParameters` consistency: when `overridesLearning = false`, test fields mirror learning fields exactly.

---

## 6. Relationships

```
Emotion (6 fixed) ──1:N──< EmotionFolder >──1:N──< EmotionImage
                                                         │
                                               ImageUsage (inLearning, inTest)
                                                         │
                                                LearningStep ──< MaterialSelection
                                                     │
                                           LearningParameters
                                           TestParameters
                                           ReinforcementSettings
```

| Relationship | Cardinality | Notes |
|---|---|---|
| `Emotion → EmotionFolder` | 1 : N | Each folder belongs to exactly one emotion |
| `EmotionFolder → EmotionImage` | 1 : N | Each image belongs to exactly one folder |
| `EmotionImage → ImageUsage` | 0..N | An image may be selected in zero or many LearningSteps |
| `LearningStep → ImageUsage` | 1 : N | A step references images across any number of emotions and folders |

---

## 7. Domain Services

### 7.1 TrialGenerator

Generates the ordered list of trials for one session.

**Inputs:** eligible `EmotionImage` records filtered by mode, active `LearningParameters` (or `TestParameters`), `mixedGenderInAnswers` flag
**Output:** `List<Trial>`

**Algorithm:**
1. Group eligible images by `emotionId`. Discard emotion groups with no eligible images.
2. Cap: `min(available emotion groups, learningParameters.repetitionsPerEmotion * available groups / repetitionsPerEmotion)` — effectively capped by available emotions.
3. Shuffle and select the working emotion pool.
4. For each selected emotion, repeat `repetitionsPerEmotion` times:
   a. Pick one image as `correctOption` using `ImageExhaustionTracker` (cycle before repeating).
   b. Set `promptGender` from the correct image's `gender`.
   c. Select `displayedImageCount − 1` distractor images from other emotion groups. If `mixedGenderInAnswers = false`, distractors **must** be images whose `gender` matches `promptGender`; if insufficient images of that gender exist, the trial is generated with fewer distractors.
   d. Assemble `allOptions` (correct + distractors), shuffled. Record the Trial.
5. Shuffle the complete trial list.

### 7.2 PromptRenderer

Produces a `RenderedPrompt` for a given trial.

**Inputs:** `PromptTemplate`, `EmotionId`, `GrammaticalGender`, `Locale`
**Output:** `RenderedPrompt`

Selects the correct `EmotionLabel` form: the gender-specific Polish form or the neutral English form. Instantiates the template to produce `displayText` (emotion name only) and `spokenText` (full phrase).

### 7.3 LearningStepActivationService

Enforces the at-most-one-active invariant.

- `activate(step, mode)`: atomically deactivates any currently active step and activates the target step in the given mode.
- `setMode(step, mode)`: changes the mode of the already-active step without deactivation (e.g. toggle between `LEARNING` and `TEST`).
- `handleDeletion(step)`: if the deleted step was active, activates the first available example step in `LEARNING` mode as fallback.

**Critical:** `activate` must be executed atomically. Deactivating the old step and activating the new one are not two separate operations — they must succeed or fail together.

### 7.4 SessionEligibilityChecker

Pre-flight check before the child starts a session.

Returns `canPlay = true` only when: an active LearningStep exists AND at least one emotion group has at least one eligible image for the current mode.

### 7.5 ImageExhaustionTracker

Ensures images are not repeated within an emotion until all eligible images for that emotion have been shown at least once. After exhaustion, starts a new random cycle.

Maintained per-emotion within a session. Resets at session start.

### 7.6 TrialPositionRandomizer

Prevents the correct-answer image from occupying the same on-screen position across consecutive appearances of the same emotion.

- Maintains a per-emotion position history within a session.
- Attempts up to N shuffle candidates to find a non-colliding position arrangement.
- If all positions have been used for a given emotion, resets the history and shuffles freely.
- When `displayedImageCount ≤ 1`, returns the trial unchanged.

### 7.7 ErrorCorrectionController

Manages the repeat-stage mechanism. Active in `LEARNING` mode only. See §14 for the full state machine.

---

## 8. Business Rules

### 8.1 Emotion Catalog

- There are exactly 6 emotions: happy, sad, surprised, angry, scared, bored.
- Emotions cannot be created, renamed, or deleted.

### 8.2 Folder Rules

- A folder belongs to exactly one emotion and cannot be reassigned.
- `genderPolicy` is set at creation and is permanently immutable.
- A fixed-gender folder (`MASCULINE`, `FEMININE`, or `NEUTER`) automatically assigns its gender to every newly added image.
- A `MIXED` folder requires individual gender assignment for every image. Saving is blocked until every image has a gender assigned; images missing assignment are highlighted.
- User-created folders can be deleted (cascades to all images and files). Example folders cannot be deleted.

### 8.3 Image Rules

- Every image must have exactly one `GrammaticalGender` assigned at all times.
- Example images can be hidden via therapist preference but cannot be deleted.
- Deleting an image removes its physical file from device storage.
- The gender of an existing image can be edited after creation.

### 8.4 Learning Step Rules

- At most one LearningStep is active at any time.
- `mode` is always present and is set independently of `isActive` — therapists may pick a
  step's mode via its list-row toggle before ever activating it (via its row checkbox), and the
  value is retained whether or not that step is currently active.
- LearningStep names must be unique (case-insensitive).
- Deleting the active LearningStep triggers automatic fallback: the first available example step is activated in `LEARNING` mode.
- Example steps cannot be edited or deleted. They can be copied.
- A copy receives an auto-generated unique name of the form `"{original} ({n})"`, where `n` is
  the smallest positive integer for which that name does not already exist (e.g. "Podstawowy
  (1)", then "Podstawowy (2)" if "(1)" is taken). The copy is always inactive, is never marked
  as example, and inherits the source step's `mode`.

### 8.5 Hint Rules

- Hints are available in `LEARNING` mode only. They are never shown in `TEST` mode.
- At least one `HintType` must be active in `LearningParameters` at all times.
- In learning mode, a wrong tap immediately shows all active hints. If no tap occurs before `hintDelaySeconds`, all active hints are shown automatically.
- In test mode, when `hintDelaySeconds` elapses with no correct answer, the trial is counted as wrong and advances — no visual hint is shown.

### 8.6 Trial Correctness Rules

- A trial is **clean correct** only if: the correct image was tapped AND no prior mistake occurred in the same trial AND no hint had been shown.
- In learning mode: the first wrong tap within a trial immediately shows hints and records the trial as incorrect; the child may still answer correctly after the hint, but it will not be clean.
- In test mode: a wrong tap has no immediate effect; only the timer expiry counts as wrong and advances the trial. No partial-credit mechanism.

### 8.7 Reinforcement Rules

- Reinforcement fires only on clean-correct answers in `LEARNING` mode.
- `TEST` mode has no in-trial reinforcement.
- One praise word is selected randomly from `enabledPraiseWords` at the moment of reinforcement.
- When `animationsEnabled` is true, one animation theme is selected randomly from `enabledAnimationThemes`.
- Sprite animation is a separate toggle from verbal praise.
- End-of-session reinforcement (animation, fanfare) is governed by `endSessionAnimationEnabled` and `endSessionFanfareEnabled`.

### 8.8 Gender-Prompt Rule

- The grammatical gender used in the prompt is **always and exclusively** derived from the correct-answer image's gender.
- `mixedGenderInAnswers = true` (enabled): distractors may have any grammatical gender. Only the correct answer and the prompt share the same gender. Example: correct answer "znudzony" (masculine) → choices: "znudzony" · "smutna" (feminine) · "zdziwiony" (masculine) — mixed gender is permitted.
- `mixedGenderInAnswers = false` (disabled): all displayed images — correct answer and all distractors — must share the same grammatical gender as the prompt. Example: correct answer "znudzona" (feminine) → choices: "znudzona" · "smutna" (feminine) · "wesoła" (feminine) — all feminine.

### 8.9 Image Randomization and Cycle Rules

- Within a session, all eligible images for an emotion are cycled before any image repeats as the correct answer.
- Image positions on screen are shuffled between trials to prevent position habituation.
- The correct-answer position avoids repeating its previous on-screen location for the same emotion.

### 8.10 Test Parameters Inheritance Rule

- When `TestParameters.overridesLearning = false`, all test parameters exactly mirror `LearningParameters`.
- When `overridesLearning` is toggled back to `false`, all test parameters immediately revert to the current learning parameter values.
- The answer time limit in test mode equals `LearningParameters.hintDelaySeconds` — there is no separate field.

### 8.11 Example Content Rules

- Example folders, images, and steps are pre-seeded on first launch.
- Example folders and steps are not editable or deletable. Example images may be hidden by preference but not deleted.
- A step saved by the therapist (create or edit) is always `isExample = false`, even if derived from an example.

---

## 9. State Transitions

### 9.1 LearningStep Activation

```
                     ┌──────────────────────┐
                     │       INACTIVE       │
                     │  isActive = false    │
                     │  activeMode = null   │
                     └──────────┬───────────┘
                                │
           activate(LEARNING) ──┴── activate(TEST)
                                │
                     ┌──────────┴───────────┐
                     ▼                      ▼
         ┌───────────────────┐   ┌───────────────────┐
         │  ACTIVE_LEARNING  │   │   ACTIVE_TEST     │
         │  isActive = true  │   │  isActive = true  │
         │  activeMode =     │   │  activeMode =     │
         │  LEARNING         │   │  TEST             │
         └─────────┬─────────┘   └─────────┬─────────┘
                   │  setMode(TEST) ───────►│
                   │◄──── setMode(LEARNING) │
                   └──────────┬─────────────┘
                              │ another step activated
                              ▼
                     ┌──────────────────────┐
                     │       INACTIVE       │
                     └──────────────────────┘
```

**Activation** (`activate(step)`): atomically deactivates any currently active step (→
INACTIVE, `mode` left unchanged) and activates the target step (→ ACTIVE, using whichever
`mode` is already stored on it — activation never changes `mode`). Both state changes happen
together or not at all.

**Mode toggle** (`setMode(step, mode)`): sets a step's stored `mode`. Unlike earlier revisions
of this document, this is valid for *any* step, not only the currently active one — an inactive
step's mode can be pre-selected via its list-row toggle and takes effect the next time that step
is activated. When applied to the currently active step it switches it between
`ACTIVE_LEARNING` and `ACTIVE_TEST` without deactivating it first.

**Deletion**: when an active step is deleted, it does not transition to `INACTIVE` — it is
removed entirely. `LearningStepActivationService` immediately activates the first available
example step, explicitly forcing its `mode` to `LEARNING` as fallback (overriding whatever
`mode` that example step had stored), ensuring the system is never left without an active step.

### 9.2 Session

```
NOT_STARTED ──(canPlay=true, child taps Play)──► IN_PROGRESS
IN_PROGRESS ──(all trials completed)──────────► COMPLETED
COMPLETED   ──(Play Again / back)──────────────► NOT_STARTED
```

### 9.3 Trial

```
AWAITING_RESPONSE
  ├─(wrong tap, LEARNING)──────────────► HINT_VISIBLE
  │                                            │
  │                                    (correct tap)
  │                                            │
  ├─(timer expires, LEARNING)──────────► HINT_VISIBLE ──────────► JUDGED_CORRECT_AFTER_HINT
  │
  ├─(correct tap, no hint shown)───────────────────────────────► JUDGED_CLEAN_CORRECT
  │
  └─(timer expires, TEST)─────────────────────────────────────► JUDGED_TIMEOUT
```

```
JUDGED_CLEAN_CORRECT      ──► REINFORCEMENT (4 s) ──► ErrorCorrectionController ──► next trial
JUDGED_CORRECT_AFTER_HINT ──► (no reinforcement)  ──► ErrorCorrectionController ──► next trial
JUDGED_TIMEOUT            ──► (immediate advance) ──► next trial
```

### 9.4 MIXED Folder Image Upload

```
AWAITING_GENDER     (image added to MIXED folder, no gender yet)
      │ gender assigned
      ▼
GENDER_ASSIGNED
      (save permitted only when all images in batch are GENDER_ASSIGNED)
```

---

## 10. Learning Session Lifecycle

1. **Pre-flight check.** `SessionEligibilityChecker` verifies the active LearningStep exists and has at least one eligible image for the active mode. If not, the Play button is disabled.

2. **Session initialization.** On Play tap: load active LearningStep → determine `SessionMode` → filter images by mode → `TrialGenerator` produces the full `List<Trial>`.

3. **Trial presentation.** For each trial:
   a. `TrialPositionRandomizer` determines on-screen positions of all options.
   b. `PromptRenderer` produces the gender-inflected `RenderedPrompt`.
   c. `displayText` is shown on screen; TTS speaks `spokenText` (if `ttsEnabled`).
   d. Hint timer starts (`hintDelaySeconds`).

4. **Response handling.** Child taps an image. Answer is evaluated against `correctOption`.

5. **Feedback.**
   - **Learning, clean correct:** reinforcement fires (TTS word + optional praise + optional animation); congrats screen shown for 4 seconds.
   - **Learning, correct after mistake/hint:** no reinforcement; congrats screen shown briefly.
   - **Learning, wrong tap:** hints immediately shown; trial counted incorrect; child may still answer.
   - **Test, correct tap:** trial advances immediately; no congrats screen.
   - **Test, wrong tap:** ignored; child continues until correct or timeout.
   - **Test, timeout:** trial counted wrong; advances immediately.

6. **Error correction (LEARNING only).** `ErrorCorrectionController` decides whether to re-queue the trial (see §14).

7. **Session end.** All trials complete:
   - Learning mode: session summary not mandatory (scores not displayed in learning).
   - Test mode: percentage score + correct/total counts displayed.
   - Next session's trial list is pre-generated asynchronously for immediate readiness.

---

## 11. Learning Step Lifecycle

```
DRAFT ──── (wizard, in-memory only) ──── therapist saves ──► SAVED / INACTIVE
                                                                      │
                               ┌──────────────────────────────────────┤
                               │ therapist edits                      │
                               ▼                                      │
                         SAVED / INACTIVE ◄──── deactivated ◄─┐      │
                               │                               │      │
                     therapist activates                       │      │
                               │                               │      │
                         ACTIVE_LEARNING ◄──setMode()──► ACTIVE_TEST  │
                               │                                      │
                     therapist deletes ─────────────────────────────► DELETED
                         (if was active: fallback activation fires)   │
                     therapist copies ───────────────────────────────► new SAVED / INACTIVE
```

**Draft state:** All wizard state exists in memory only. No auto-save. Exiting without saving (and confirming) discards all changes.

---

## 12. Trial Lifecycle

A trial is created by `TrialGenerator` and is immutable once generated, except for its option ordering when re-queued by `ErrorCorrectionController` with a shuffled layout.

**Key constraints during execution:**
- `promptGender` is fixed at generation time (from the correct image's gender).
- The first wrong tap in LEARNING mode triggers hints and marks the trial "has mistake".
- Once "has mistake" is recorded, it cannot be cleared within the same trial instance.
- A clean-correct verdict requires: correct tap AND `hasMistake = false` AND `hintShown = false`.
- `ImageExhaustionTracker` ensures the correct-answer image is not reused within the same emotion until all eligible images have been shown.

---

## 13. Reinforcement Model

Reinforcement applies in `LEARNING` mode only. It consists of two independent components: verbal praise (TTS) and visual animation.

| Situation | Verbal praise | Animation |
|---|---|---|
| Clean-correct, `animationsEnabled = true` | Yes — random word from `enabledPraiseWords` | Yes — random theme from `enabledAnimationThemes` |
| Clean-correct, `animationsEnabled = false` | Yes | No |
| Correct after mistake or hint | No | No |
| Any correct in TEST mode | No | No |

**End-of-session reinforcement** is separate: `endSessionAnimationEnabled` and `endSessionFanfareEnabled` control what plays when the session summary appears, independently of in-trial reinforcement.

**Verbal praise:** one key chosen uniformly at random from `enabledPraiseWords` at the moment the congrats screen appears, then resolved to a spoken form for the device locale (`PraiseCatalog`: `pl` or English). The TTS sequence is: speak the emotion name → (if reinforcement) speak the localized praise word.

**Animation themes:** flowers, butterflies, balloons, cars, balls. One theme selected uniformly at random from `enabledAnimationThemes` per reinforcement event when `animationsEnabled` is true.

---

## 14. Error Correction Model

Active in `LEARNING` mode only. The mechanism re-queues failed trials within the same session to provide immediate corrective practice. A `repeatStage` counter (0, 1, or 2) persists for the duration of the session and resets at session end.

| `repeatStage` | Outcome of this trial | Next action | New `repeatStage` |
|---|---|---|---|
| 0 | Clean correct | No requeue | 0 |
| 0 | Mistake | Requeue same layout immediately after current position | 1 |
| 1 | Mistake | Requeue same layout | 1 |
| 1 | Correct (not clean) | Requeue shuffled layout | 2 |
| 2 | Mistake | Requeue shuffled layout | 2 |
| 2 | Correct (not clean) | No requeue | 0 (recovery complete) |

**Same layout requeue:** the trial is re-inserted with identical option positions.
**Shuffled layout requeue:** `TrialPositionRandomizer` produces a different on-screen arrangement to prevent position memorization.

The re-inserted trial is placed immediately after the current position in the trial list, so the child encounters the failed emotion again on the very next trial.

---

## 15. Configuration Model (Business Perspective)

The therapist configures a LearningStep through a five-tab wizard. All state is held in memory until saved.

| Tab | Concern | Key decisions |
|---|---|---|
| **Materials** | What the child practices | Select emotions → select folders → optionally deselect individual images; assign each image to LEARNING/TEST/BOTH |
| **Learning** | How learning-mode sessions work | Image count per trial (1–6); repetitions per emotion (1–3); prompt template; TTS; captions; hint delay (1–10 s); active hint types (≥1); mixed gender in answers |
| **Reinforcement** | Motivational feedback | Active praise words; in-trial animations; end-of-session animation; end-of-session fanfare |
| **Test** | Assessment behavior | Override learning settings or inherit; if independent: image count, repetitions, prompt, TTS, captions, mixed gender in answers |
| **Save** | Persistence | Unique name (required, case-insensitive duplicate check); read-only summary comparing all learning vs. test parameters |

**Material selection detail:**
1. Therapist selects one of the 6 emotions.
2. All folders for that emotion are listed. Selecting a folder auto-selects all its images for both LEARNING and TEST.
3. Therapist may expand a folder to deselect individual images or remove them from specific modes.
4. Repeated across as many emotions as desired.

**Name validation:** blank name blocked; duplicate name blocked. On edit, the step's own name is excluded from the uniqueness check.

**Activation:** separate from creation. After saving, the therapist activates the step from the list screen via its row checkbox, using its already-stored mode. The mode can be toggled independently for the active step at any time. Tapping the row itself (not the checkbox) reopens the step for editing instead of activating it.

---

## 16. Domain Events

| Event | Trigger | Significance |
|---|---|---|
| `LearningStepActivated(stepId, mode)` | A step is activated | Child app re-evaluates `canPlay`; active configuration reloaded |
| `LearningStepModeChanged(stepId, mode)` | Active step mode toggled | Child app adapts to new mode for next session |
| `LearningStepDeactivated(stepId)` | Another step activated | Previous step marked inactive |
| `LearningStepDeleted(stepId, wasActive)` | Therapist confirms deletion | If `wasActive`, fallback activation fires |
| `SessionStarted(stepId, mode)` | Child taps Play | `TrialGenerator` initializes trial list; `ImageExhaustionTracker` resets |
| `SessionCompleted(correct, total, mode)` | All trials done | Summary screen populated; next session pre-generated |
| `TrialAnsweredClean(trialIndex)` | Correct tap, no prior mistake, no hint | `ReinforcementModel` fires; `ErrorCorrectionController` evaluates stage 0 |
| `TrialAnsweredAfterHint(trialIndex)` | Correct tap after hint shown | No reinforcement; `ErrorCorrectionController` evaluates stage 1 or 2 |
| `TrialTimeout(trialIndex, mode)` | Timer expired | LEARNING: hint shows; TEST: trial counted wrong, advances |
| `HintActivated(trialIndex)` | Wrong tap or timer (learning) | All active `HintType`s applied visually |
| `ReinforcementTriggered(praiseWord, theme)` | Clean-correct in learning | TTS speaks praise word; animation plays |
| `FolderCreated(folderId, emotionId)` | Therapist saves new folder | Available material updated |
| `FolderDeleted(folderId, imageIds)` | Therapist confirms folder deletion | Images removed from storage; affected LearningSteps notified |
| `ImageAdded(imageId, folderId)` | Therapist adds image | Available material updated |
| `ImageRemoved(imageId)` | Therapist removes image | Physical file deleted; affected LearningSteps notified |

---

*End of Target Domain Model*
