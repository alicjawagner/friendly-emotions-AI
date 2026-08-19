# Friendly Emotions – Functional Specification

## 1. Overview

**Friendly Emotions** (Polish title: *Przyjazne Emocje*) is an Android educational application for children with developmental needs (e.g. autism spectrum disorder and other developmental disorders). It is a sibling application to **Friendly Words** (*Przyjazne Słowa*) and reuses the majority of its logic, architecture, therapist workflow and data model.

The key difference: instead of teaching word–image associations, Friendly Emotions teaches **emotion recognition**. The child is presented with a set of photographs of people (or animals, emojis) expressing emotions and is asked to tap the image that matches the emotion written on the screen and spoken aloud.

The app ships as a single APK with two entry points:
- **Child App** — the learning game used during therapy sessions.
- **Therapist App** — configuration panel for therapists (manages teaching materials and learning steps).

Both sub-apps share the same local database. There is **no network communication**; all data is stored on the device.

---

## 2. Goals

- Enable therapists to teach children to recognize and identify emotions through image-based exercises.
- Provide both guided learning and assessment modes.
- Reuse and adapt the proven Friendly Words session flow, configuration wizard, and data architecture.
- Support grammatical gender in Polish (masculine / feminine / neuter forms of emotion adjectives) so prompts always match the correct answer image.
- Support Polish as the primary language and English as a secondary language (auto-detected from device locale).

---

## 3. Languages

| Language | Availability |
|---|---|
| Polish | Primary |
| English | Secondary; activated when the device system language is set to English |

### 3.1 Gender-Aware Language (Polish)

Emotion labels in Polish are adjectives that inflect by grammatical gender (in Polish called "rodzaje"):

- **Masculine** (rodzaj męski): e.g. *wesoły*, *smutny*
- **Feminine** (rodzaj żeński): e.g. *wesoła*, *smutna*
- **Neuter** (rodzaj nijaki): e.g. *wesołe*, *smutne*

Every image has a gender assigned to it. The prompt shown on screen and spoken by the TTS engine uses the form matching the gender of the **correct answer image** in the current round.

In English, emotion labels are gender-neutral adjectives (e.g. *happy*, *sad*) and no inflection is required.

---

## 4. Differences from Friendly Words

| Area | Friendly Words | Friendly Emotions |
|---|---|---|
| Teaching goal | Word–image association | Emotion recognition |
| Teaching material unit | Resource (word + images) | Emotion → Folder → Images |
| Material structure | Flat list of resources with categories | 6 fixed emotions, each containing user-managed folders and images |
| Gender on images | Not present | Each image has an assigned grammatical gender |
| Gender on folders | Not present | Each folder can have a fixed default gender assigned |
| Material management | Add/edit/delete resources; attach images | Add/edit/delete folders and images within each fixed emotion |
| Learning step: material selection | Select resources, then individual images | Select emotions, then images from within folders (all from a folder or specific ones) |
| Languages | Polish only | Polish (primary) + English (secondary) |

Everything else — session flow, round generation, hint system, reinforcement/praise system, test mode, configuration wizard tabs (Learning, Reinforcement, Test, Save), activation logic, and data architecture — is inherited from Friendly Words with only minor adjustments noted below.

---

## 5. Child App

### 5.1 Learning Session Flow

A learning session consists of consecutive rounds. Each round:
- Displays a configurable number of images (with/without labels).
- Plays a spoken instruction.
- Displays only the emotion label on screen.
- Waits for the child's answer.
- Applies hints if enabled.
- Shows reinforcement after successful completion.

### 5.2 Images

#### 5.2.1 Image Count

The therapist configures the number of images displayed simultaneously. Allowed values:
- minimum: 1
- maximum: 6
- default: 3

#### 5.2.2 Image Position Randomisation

To reduce position learning, image positions are shuffled between rounds.

When **three or more images** are displayed, all visible images are randomly rearranged among their current positions.

When **only one or two images** are displayed, the layout still uses the predefined three-position grid. Images move only between these predefined positions, ensuring they appear in different locations across rounds while maintaining a consistent visual layout. Example for one image in two rounds: xoo, oxo.

This prevents images from always appearing in the same position even when fewer than three images are shown.

### 5.3 Prompt Types

The therapist may choose one of several prompt templates. Available templates:

| PL | EN |
| --- | ---|
| {emocja} | {emotion} |
| Gdzie jest {emocja}? | Where is {emotion}? |
| Pokaż, gdzie jest {emocja}. | Show me {emotion}. |
| Znajdź, gdzie jest {emocja}. | Find {emotion}. |
| Dotknij, gdzie jest {emocja}. | Touch {emotion}. |
| Wskaż, gdzie jest {emocja}. | Point to {emotion}. |
| Wybierz, gdzie jest {emocja}. | Choose {emotion}. |

The text displayed on screen always contains only the emotion name, while the TTS engine speaks the entire selected prompt (e.g. Displayed: "Happy", Spoken: "Where is happy?").

### 5.4 Gender Handling

The grammatical gender used in the prompt is determined exclusively by the correct answer image. Incorrect answers may have different grammatical genders. Example:
- Prompt: Gdzie jest wesoły?
- Displayed images: smutna, wesoły, złe

This is valid because only the correct answer determines the prompt.

### 5.5 Answer Evaluation

A response is considered successful only when:

- the correct image is selected,
- the answer is given before the hint timer expires,
- no hint has been shown.

Otherwise the round is considered unsuccessful.

### 5.6 Learning Mode

Learning mode provides assistance. Chacteristics:
- spoken prompts;
- hints;
- praise animations;
- reinforcement;
- repeated practice.

Learning mode does not display scores or statistics.

### 5.7 Test Mode

Test mode evaluates performance without assistance. Characteristics:
- timer enabled;
- no hints;
- no reinforcement during rounds;
- score recorded.

At the end of the session the application displays:

- percentage score;
- number of correct answers;
- total number of attempts.

Example: 85% (17 / 20)

### 5.8 Hint System

Hints become available after a configurable delay. Hint behaviour is inherited from Friendly Words. Default delay: 5 seconds

### 5.9 Reinforcement

The application supports multiple reinforcement animation themes, e.g.:
- flowers
- butterflies
- balloons
- cars

The therapist may choose the preferred animation set.

### 5.10 Randomisation

Images are presented randomly. The application should avoid repeating images until every eligible image has been presented once. After the complete pool has been exhausted, a new random cycle begins.

### 5.11 Unchanged Behaviours

- 5-second information screen on launch.
- Play button disabled if no materials are configured for the active learning step.
- Hint system (outline, animate, scale correct, dim incorrect).
- Reinforcement / praise animations and TTS after a clean correct answer.
- Repeat-stage logic for failed rounds.
- Test mode (silent, timed, no hints/praise).

---

## 6. Teaching Materials

### 6.1 Structure

Teaching materials are organised as:

```
Emotion
  └── Folder
        └── Image
```

The six emotions are fixed (en: happy, sad, surprised, angry, scared, bored; pl: wesoły, smutny, zdziwiony, zły, przestraszony, znudzony). Folders belong to exactly one emotion. A folder cannot belong to multiple emotions.

### 6.2 Images

Every image has:
- photograph
- grammatical gender (mandatory)

Supported genders (rodzaje):
- masculine (męski)
- feminine (żeński)
- neuter (nijaki)

Therapists add images from the device gallery or by taking a photo with the camera.

### 6.3 Folder Types

Folders may have one of four configurations:
- masculine (męski)
- feminine (żeński)
- neuter (nijaki)
- no gender (bez rodzaju)

A folder with a fixed gender automatically assigns that gender to newly added images. A no gender folder requires every image to have its gender assigned individually.

Therapists can **create** and **delete** folders. Deleting a folder deletes all images inside it.

Folder names are user-defined.

###  6.4 Example Folders

Example folders may include:

| Folder name |	Default Gender |
| --- | --- |
| Women |	Feminine |
| Men	| Masculine |
| Emojis	| Neuter |
| Other	| No gender |

Example folders cannot be deleted. Example images may be hidden but not deleted.

### 6.5 Folder Management

Therapists may:
- create folders;
- rename folders;
- delete user-created folders;
- add images;
- remove images.

Folder gender cannot be modified after creation. This prevents inconsistencies in image gender assignments.

### 6.6 Image Upload

When adding images to a mixed folder:
- every image must receive a grammatical gender;
- saving is blocked until every image has one;
- images missing gender assignment are highlighted when trying to save.

---

## 7. Therapist App

### 7.1 Welcome Screen

The welcome screen states that the application is intended for: children with autism spectrum disorder and other developmental disorders. The screen also contains links to supporting resources.

### 7.2 Main menu

After 5 seconds or when the screen is clicked, the main menu appears. It contains two buttons: Materials and Learning Steps which navigate to their respective flows, described below.

### 7.3 Delete Confirmation

Every destructive action (e.g. delete folder, delete image, delete learning step) requires confirmation.

### 7.4 Example / Seed Data

The app contains example data so the therapist can immediately run a demo session.

---

## 8. Managing Materials (Therapist App)

The therapist navigates to the materials section, which shows the 6 emotions. Selecting an emotion shows its folders. Selecting a folder shows its images.

From this view the therapist can:
- Create a new folder (choose name, optionally assign a default gender). Folder creation occurs only within the currently selected emotion.
- Delete an existing folder (with all its images).
- Edit folder name.
- Add images to a folder (from gallery or camera; assign gender per image if the folder in "no gender").
- Delete individual images.
- Edit gender of an existing image.

Folders display their assigned gender. A help info box explains the meaning of grammatical gender.

---

## 9. Learning Step Configuration (Therapist App)

A Learning Step (*Krok Uczenia*) defines the parameters of a child's session. The configuration wizard retains the 5-tab structure from Friendly Words with the changes described below.

Two example learning steps are included:
- Basic - Simple configuration intended for beginners (e.g. 2 images, 2 repetitions).
- Advanced - Demonstrates more advanced configuration possibilities.

### 9.1 Material Tab (Changed)

Instead of selecting resources from a flat list, the therapist:

1. Picks an **emotion** from the 6 fixed emotions.
2. All folders for that emotion are displayed. Initially no folders are selected. Selecting a folder automatically selects: Learning and Test. Therapists may open the folder and deselect individual images.
3. The Materials tab does not display grammatical gender icons for selected images.
4. Repeats across as many emotions as desired.

Per-image mode assignment (Learning / Test checkboxes) works the same as in Friendly Words once an image has been added.

**Note:** Because images within a single emotion may come from multiple folders and have different genders, the prompt in each round is determined by the specific correct-answer image's gender, not by an emotion-level setting.

### 9.2 Learning Tab (Almost Unchanged)

Identical to Friendly Words: image count per round (1–6, default 3), repetitions per emotion (1–10, default 2), command type, TTS toggle, captions toggle, hint timer (3–10 seconds, default 5), hint type checkboxes, **mixed gender in answers set** (disabled: if the question and the correct answer is e.g. feminine, all other images sholud also be feminine, e.g. correct answer: znudzona, available choices: znudzona, smutna, wesoła; enabled: only the correct answer and the question must be the same gender, the rest can be arbitrary, e.g. correct answer: znudzony, available choices: znudzony, smutna, zdziwiony).

### 9.3 Reinforcements Tab (Almost Unchanged)

Praise words checkboxes, animation sprites checkboxes, **animation at the end of the session** toggable, **the sound of fanfares at the end of the session** toggable.

### 9.4 Test Tab (Unchanged)

Inherits learning settings by default; independent settings unlockable via checkbox. Hints and reinforcements inactive. Identical to Friendly Words (but based on Friendly Emotions learning tab).

### 9.5 Summary Tab (Unchanged)

Name input + read-only summary comparison table. Identical to Friendly Words (but containing all Friendly Emotions settings listed).

### 9.6 Navigation

Every configuration page contains a Next button. The final layout follows the Friendly Words wizard while ensuring consistent forward navigation. A Home button is available from every therapist screen and returns to the main menu.

---

## 10. Inherited Unchanged from Friendly Words

The following behaviours are carried over without modification:

- Single APK with two launcher icons.
- No network connectivity; fully offline.
- Room database as the sole persistent store.
- DataStore for therapist UI preferences (e.g. hide example materials/steps).
- Therapist wizard architecture.
- Active learning step singleton (at most one active at a time; fallback to example on delete).
- Configuration activation / mode switching (learning ↔ test).
- Example configurations: not editable, not deletable, copyable.
- Image storage in app-private `filesDir`.
- Android TTS for all audio output.
- Reinforcement sprite animations (flowers, balloons, cars).
- Position anti-repeat mechanism for correct-answer tile placement.
- Round shuffle and repeat-stage recovery logic.
- All session-flow state machines (info → main → game → end).
