# 05 — Data Architecture

> **Scope:** Friendly Words v2 — Android educational application for children with autism.
> Both sub-applications (therapist configuration screen and child game screen) run inside a single APK, sharing one Room database and one Hilt DI graph. This document describes every persistence mechanism used in the project.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Database Schema](#2-database-schema)
   - 2.1 [Entity: `resources`](#21-entity-resources)
   - 2.2 [Entity: `images`](#22-entity-images)
   - 2.3 [Entity: `configurations`](#23-entity-configurations)
   - 2.4 [Join table: `resource_images`](#24-join-table-resource_images)
   - 2.5 [Join table: `configuration_resources`](#25-join-table-configuration_resources)
   - 2.6 [Join table: `configuration_image_usages`](#26-join-table-configuration_image_usages)
   - 2.7 [Entity Relationship Diagram](#27-entity-relationship-diagram)
3. [Type Converters](#3-type-converters)
4. [DAOs](#4-daos)
   - 4.1 [ResourceDao](#41-resourcedao)
   - 4.2 [ImageDao](#42-imagedao)
   - 4.3 [ConfigurationDao](#43-configurationdao)
   - 4.4 [ConfigurationResourceDao](#44-configurationresourcedao)
5. [Repositories](#5-repositories)
   - 5.1 [ResourceRepository](#51-resourcerepository)
   - 5.2 [ImageRepository](#52-imagerepository)
   - 5.3 [ConfigurationRepository](#53-configurationrepository)
   - 5.4 [PreferencesRepository](#54-preferencesrepository)
6. [Shared Data Between Applications](#6-shared-data-between-applications)
7. [Data Lifecycle](#7-data-lifecycle)
8. [Configuration Storage](#8-configuration-storage)
9. [Media Storage](#9-media-storage)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│  Single APK  (com.example.friendly_words)                           │
│                                                                     │
│  ┌─────────────────────────┐   ┌─────────────────────────────────┐  │
│  │  MainActivity           │   │  MainActivityChild              │  │
│  │  (Therapist Settings)   │   │  (Child Game)                   │  │
│  └────────────┬────────────┘   └───────────────┬─────────────────┘  │
│               │                                │                    │
│               └───────────────┬────────────────┘                    │
│                               │  Hilt Singleton scope               │
│                               ▼                                     │
│            ┌──────────────────────────────────────────┐             │
│            │          Repositories (shared)           │             │
│            │  ConfigurationRepository                 │             │
│            │  ResourceRepository                      │             │
│            │  ImageRepository                         │             │
│            └──────────────────┬───────────────────────┘             │
│                               │                                     │
│            ┌──────────────────▼───────────────────────┐             │
│            │   Room AppDatabase  "friendly_words"      │             │
│            │   SQLite file in app private storage      │             │
│            └──────────────────────────────────────────┘             │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Jetpack DataStore  "user_preferences"  (therapist only)   │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Internal File Storage  context.filesDir                   │   │
│   │  (user-captured / gallery images)                          │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  Android Assets  assets/exemplary_photos/                  │   │
│   │  (bundled example images, read-only)                       │   │
│   └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

**Key facts:**
- There is **no IPC, no ContentProvider, no cross-process sharing** — both activities live in the same process and access the same Hilt singleton graph.
- The Room database is a standard SQLite file located at `databases/friendly_words` inside the app's private data directory.
- Foreign key enforcement is enabled at runtime via `PRAGMA foreign_keys=ON` in an `onOpen` callback (Room does not enable this by default).
- Database version is currently **27**; there are no schema migration files — `fallbackToDestructiveMigration(true)` means every schema change wipes all user data.

---

## 2. Database Schema

The database is declared in `shared/src/main/java/com/example/shared/data/database/AppDatabase.kt`:

```kotlin
@Database(
    entities = [
        Resource::class,
        Image::class,
        Configuration::class,
        ConfigurationResource::class,
        ConfigurationImageUsage::class,
        ResourceImage::class
    ],
    version = 27,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

### 2.1 Entity: `resources`

**File:** `shared/.../data/entities/Resource.kt`

| Column       | SQLite type | Constraints              | Description                                            |
|--------------|-------------|--------------------------|--------------------------------------------------------|
| `id`         | INTEGER     | PK, auto-increment       | Surrogate key                                          |
| `name`       | TEXT        | NOT NULL                 | Display label shown in the therapist UI                |
| `learnedWord`| TEXT        | NOT NULL                 | The Polish word spoken aloud during the child session  |
| `category`   | TEXT        | NOT NULL, default `""`   | Free-text category tag (e.g. `"zwierzęta"`, `"jedzenie"`) |
| `isExample`  | INTEGER     | NOT NULL, default `0`    | Marks seeded demo resources; used for UI filtering     |

A **resource** represents one vocabulary item — a concept the child is learning to identify (e.g. "Kot" / Cat).

### 2.2 Entity: `images`

**File:** `shared/.../data/entities/Image.kt`

| Column | SQLite type | Constraints        | Description                                                                                    |
|--------|-------------|---------------------|-----------------------------------------------------------------------------------------------|
| `id`   | INTEGER     | PK, auto-increment  | Surrogate key                                                                                  |
| `path` | TEXT        | NOT NULL            | Absolute file-system path or `file:///android_asset/...` URI pointing to the image file       |

An **image** record is purely a path pointer. The actual binary file resides either in `context.filesDir` (user-added images) or in the APK's asset folder (example images). Images are associated with resources via the `resource_images` join table.

### 2.3 Entity: `configurations`

**File:** `shared/.../data/entities/Configuration.kt`

The `configurations` table stores therapist-defined learning programmes. `LearningSettings` and `TestSettings` are Kotlin data classes embedded directly into this table via Room's `@Embedded` annotation, so all their fields become columns with a prefix.

**Core columns:**

| Column       | SQLite type | Constraints           | Description                                        |
|--------------|-------------|-----------------------|----------------------------------------------------|
| `id`         | INTEGER     | PK, auto-increment    | Surrogate key                                      |
| `name`       | TEXT        | NOT NULL              | Human-readable name of the configuration step      |
| `isActive`   | INTEGER     | NOT NULL, default `0` | Exactly one row should have `isActive = 1`         |
| `activeMode` | TEXT        | nullable              | `"uczenie"` (learning) or `"test"` — the active mode |
| `isExample`  | INTEGER     | NOT NULL, default `0` | Marks seeded example configurations                |

**`@Embedded(prefix = "learning_")` — from `LearningSettings`:**

| Column                          | Type    | Default       | Description                                                                  |
|---------------------------------|---------|---------------|------------------------------------------------------------------------------|
| `learning_numberOfWords`        | INTEGER | `0`           | Total vocabulary count for the learning round                                |
| `learning_displayedImagesCount` | INTEGER | `0`           | How many image choices are shown per trial                                   |
| `learning_repetitionPerWord`    | INTEGER | `2`           | How many times each word is repeated in a round                              |
| `learning_commandType`          | TEXT    | `""`          | Prompt template key: `"SHORT"`, `"WHERE_IS"`, `"SHOW_ME"`                   |
| `learning_showLabelsUnderImages`| INTEGER | `1` (true)    | Whether text captions are shown beneath images                               |
| `learning_readCommand`          | TEXT    | `1` (true)    | Whether the command is read aloud via TTS                                    |
| `learning_hintAfterSeconds`     | INTEGER | `3`           | Seconds of no response before a hint is triggered                            |
| `learning_typesOfHints`         | TEXT    | `""`          | `\|\|`-delimited list of hint names (e.g. `"Obramuj poprawną\|\|Porusz poprawną"`) |
| `learning_typesOfPraises`       | TEXT    | `""`          | `\|\|`-delimited list of praise words (e.g. `"dobrze\|\|super"`)            |
| `learning_animationsEnabled`    | INTEGER | `1` (true)    | Whether reward animations play after a correct answer                        |

**`@Embedded(prefix = "test_")` — from `TestSettings`:**

| Column                       | Type    | Default      | Description                                        |
|------------------------------|---------|--------------|----------------------------------------------------|
| `test_numberOfWords`         | INTEGER | `0`          | Vocabulary count for the test round                |
| `test_displayedImagesCount`  | INTEGER | `0`          | Image choices per trial in test mode               |
| `test_repetitionPerWord`     | INTEGER | `0`          | Repetitions per word in test mode                  |
| `test_commandType`           | TEXT    | `""`         | Prompt template key (same values as learning)      |
| `test_showLabelsUnderImages` | INTEGER | `0` (false)  | Text captions under images during test             |
| `test_readCommand`           | INTEGER | `0` (false)  | TTS read-aloud in test mode                        |
| `test_answerTimeSeconds`     | INTEGER | `0`          | Maximum time allowed for an answer                 |

### 2.4 Join table: `resource_images`

**File:** `shared/.../data/entities/ResourceImage.kt`

| Column       | SQLite type | Constraints                                       |
|--------------|-------------|---------------------------------------------------|
| `resourceId` | INTEGER     | PK (composite), FK → `resources.id` ON DELETE CASCADE |
| `imageId`    | INTEGER     | PK (composite), FK → `images.id` ON DELETE CASCADE    |

**Indexes:** `resourceId`, `imageId`

This is the **many-to-many link** between resources and their available images. Deleting a resource automatically removes all its `resource_images` rows via the CASCADE rule. An image's `getReferenceCount` query counts rows in this table to determine if the image is still in use.

### 2.5 Join table: `configuration_resources`

**File:** `shared/.../data/entities/ConfigurationResource.kt`

| Column            | SQLite type | Constraints                                              |
|-------------------|-------------|----------------------------------------------------------|
| `configurationId` | INTEGER     | PK (composite), FK → `configurations.id` ON DELETE CASCADE |
| `resourceId`      | INTEGER     | PK (composite), FK → `resources.id` ON DELETE CASCADE       |

**Index:** `resourceId`

Records which resources are included in a given configuration. Deleting either the configuration or the resource will automatically remove the link row.

### 2.6 Join table: `configuration_image_usages`

**File:** `shared/.../data/entities/ConfigurationImageUsage.kt`

| Column            | SQLite type | Constraints                                              |
|-------------------|-------------|----------------------------------------------------------|
| `configurationId` | INTEGER     | PK (composite), FK → `configurations.id` ON DELETE CASCADE |
| `imageId`         | INTEGER     | PK (composite), FK → `images.id` ON DELETE CASCADE          |
| `inLearning`      | INTEGER     | NOT NULL (boolean)                                       |
| `inTest`          | INTEGER     | NOT NULL (boolean)                                       |

**Index:** `imageId`

This table gives the therapist per-image control over which images appear in the **learning phase** vs the **test phase** of a given configuration. The child app reads this table through `getResourcesWithImagesForActiveConfigFiltered(isTestMode)` to receive only images appropriate for the current mode.

### 2.7 Entity Relationship Diagram

```
resources ──────────────────────────────────────────────┐
  │  id (PK)                                            │
  │  name                                               │
  │  learnedWord                                        │
  │  category                                           │
  │  isExample                                          │
  │                                                     │
  │  1                                                  │
  ▼  N                                                  │
resource_images                                          │
  │  resourceId (FK → resources.id, CASCADE)            │
  │  imageId    (FK → images.id,    CASCADE)            │
  │                                                      │
  │  N                                                  │
  ▼  1                                                  │
images                                                   │
  id (PK)                                               │
  path                                                  │
  ▲                                                     │
  │  N                                                  │
  │                                                     │
configuration_image_usages                              │
  │  configurationId (FK → configurations.id, CASCADE)  │
  │  imageId         (FK → images.id, CASCADE)          │
  │  inLearning                                         │
  │  inTest                                             │
  │                                                     │
  │  N                                                  │
  ▼  1                                                  │
configurations                                           │
  id (PK)                                               │
  name                                                  │
  isActive                                              │
  activeMode                                            │
  isExample                                             │
  learning_* (embedded LearningSettings)                │
  test_*     (embedded TestSettings)                    │
  ▲                                                     │
  │  1                                                  │
  │  N                                                  │
configuration_resources ◄────────────────────────────────┘
  configurationId (FK → configurations.id, CASCADE)
  resourceId      (FK → resources.id, CASCADE)
```

---

## 3. Type Converters

**File:** `shared/src/main/java/com/example/shared/data/another/Converters.kt`

Room cannot natively store `List<String>` or `Map<String, Boolean>` as SQLite column values. The `Converters` class handles serialization for these types.

### `List<String>` ↔ `TEXT`

Used for `learning_typesOfHints` and `learning_typesOfPraises`.

| Direction   | Logic                                         | Example                            |
|-------------|-----------------------------------------------|------------------------------------|
| To database | `list.joinToString(separator = "||")`         | `"dobrze||super||świetnie"`        |
| From database | `data.split("||")` (empty string → empty list) | `["dobrze", "super", "świetnie"]` |

### `Map<String, Boolean>` ↔ `TEXT`

Defined in the converter class but **not currently referenced by any `@Entity` column** — the column `learning_typesOfPraises` stores a `List<String>` of praise words whose enabled/disabled state is reconstructed at runtime by comparing against a hardcoded canonical praise list.

| Direction   | Logic                                                          |
|-------------|----------------------------------------------------------------|
| To database | `entries.joinToString(";;") { "${key}::${value}" }`           |
| From database | split by `";;"`, then by `"::"`, merge with `defaultPraiseMap()` |

---

## 4. DAOs

All DAOs are in `shared/src/main/java/com/example/shared/data/daos/`.

### 4.1 `ResourceDao`

**Responsibilities:** CRUD operations on the `resources` table; loading resources with their linked images via a `@Transaction` query.

| Method                    | Return type                    | Notes                                                |
|---------------------------|--------------------------------|------------------------------------------------------|
| `insert(resource)`        | `Long` (new row id)            | `suspend`                                            |
| `getAll()`                | `Flow<List<Resource>>`         | Reactive — emits on every database change            |
| `getAllOnce()`             | `List<Resource>`               | `suspend` — one-shot read                            |
| `getById(resourceId)`     | `Resource`                     | `suspend`                                            |
| `getByName(name)`         | `Resource?`                    | `suspend`, returns `null` if not found               |
| `delete(resource)`        | `Unit`                         | `suspend`, cascades to `resource_images`             |
| `update(resource)`        | `Unit`                         | `suspend`                                            |
| `getResourcesWithImages()`| `List<ResourceWithImages>`     | `@Transaction` + `@Relation` — loads complete graph  |

`getResourcesWithImages()` returns a list of `ResourceWithImages` objects where each wraps a `Resource` and its associated `List<Image>`, resolved through the `resource_images` junction via Room's `@Relation` + `Junction` mechanism.

### 4.2 `ImageDao`

**Responsibilities:** CRUD on the `images` table; managing `resource_images` join rows.

| Method                              | Return type      | Notes                                                         |
|-------------------------------------|------------------|---------------------------------------------------------------|
| `insert(image)`                     | `Long`           | `suspend`, `REPLACE` on conflict                              |
| `insertMany(images)`                | `List<Long>`     | `suspend`, batch insert, returns generated IDs                |
| `getAll()`                          | `List<Image>`    | `suspend` — one-shot                                          |
| `getReferenceCount(imageId)`        | `Int`            | `suspend` — counts rows in `resource_images` for this imageId |
| `getByResourceId(resourceId)`       | `List<Image>`    | `suspend` — inner join through `resource_images`              |
| `delete(image)`                     | `Unit`           | `suspend`                                                     |
| `deleteImageLinksForResource(resourceId)` | `Unit`     | `suspend` — removes all `resource_images` rows for a resource |
| `insertResourceImageLinks(links)`   | `Unit`           | `suspend` — batch insert into `resource_images`               |
| `deleteSpecificImageLinks(resourceId, imageIds)` | `Unit` | `suspend` — selective unlink                               |

### 4.3 `ConfigurationDao`

**Responsibilities:** CRUD on `configurations`; active configuration management; CRUD on both join tables (`configuration_resources` and `configuration_image_usages`).

This is the largest DAO, managing three tables through a single interface.

**Configuration CRUD:**

| Method                          | Return type             | Notes                                              |
|---------------------------------|-------------------------|----------------------------------------------------|
| `getAll()`                      | `Flow<List<Configuration>>` | Reactive                                       |
| `getAllOnce()`                   | `List<Configuration>`   | `suspend`                                          |
| `getById(id)`                   | `Configuration`         | `suspend`                                          |
| `getByIds(ids)`                 | `List<Configuration>`   | `suspend` — batch fetch by ID list                 |
| `insert(configuration)`         | `Long`                  | `suspend`, `REPLACE` on conflict                   |
| `delete(configuration)`         | `Unit`                  | `suspend`, cascades to both join tables             |
| `update(configuration)`         | `Unit`                  | `suspend`                                          |

**Active configuration management:**

| Method                              | Return type         | Notes                                                       |
|-------------------------------------|---------------------|-------------------------------------------------------------|
| `getActiveConfiguration()`          | `Flow<Configuration?>` | Reactive — `null` when no configuration is active        |
| `clearActiveConfiguration()`        | `Unit`              | `UPDATE ... SET isActive=0, activeMode=NULL WHERE isActive=1` |
| `activateConfiguration(id, mode)`   | `Unit`              | `UPDATE ... SET isActive=1, activeMode=:mode WHERE id=:id`  |

**`configuration_resources` join management:**

| Method                                          | Notes                                    |
|-------------------------------------------------|------------------------------------------|
| `insertConfigurationResource(link)`             | Single insert, `REPLACE` on conflict     |
| `insertConfigurationResources(resources)`       | Batch insert, `REPLACE` on conflict      |
| `getConfigurationResources(configurationId)`    | Returns all resources in a configuration |
| `deleteSingleConfigurationResource(configId, resourceId)` | Removes one link               |
| `deleteConfigurationResourcesByConfigId(configId)` | Removes all links for a configuration |

**`configuration_image_usages` join management:**

| Method                                              | Notes                                        |
|-----------------------------------------------------|----------------------------------------------|
| `insertConfigurationImageUsage(usage)`              | Single insert, `REPLACE` on conflict         |
| `insertConfigurationImageUsages(usages)`            | Batch insert, `REPLACE` on conflict          |
| `getConfigurationImageUsages(configurationId)`      | Returns all image usage rows for a config    |
| `deleteConfigurationImageUsagesForConfiguration(configId)` | Removes all usages for a configuration |
| `deleteSingleConfigurationImageUsage(configId, imageId)` | Removes one specific usage row         |
| `deleteConfigurationImageUsagesByConfigId(configId)` | Alias for the bulk delete above             |

### 4.4 `ConfigurationResourceDao`

**Responsibilities:** Reverse lookup — find all configurations that contain a given resource.

| Method                    | Return type                    | Notes                                               |
|---------------------------|--------------------------------|-----------------------------------------------------|
| `getByResourceId(resourceId)` | `List<ConfigurationResource>` | `suspend` — used to show a warning before deleting a resource that is referenced by configurations |

---

## 5. Repositories

All repositories in `shared/` are provided as Hilt singletons (or scoped to ViewModel) and are consumed by both the therapist and child ViewModels.

### 5.1 `ResourceRepository`

**File:** `shared/.../repositories/ResourceRepository.kt`  
**DI scope:** Unscoped (new instance per injection point, but injected DAO is a singleton)  
**Dependencies:** `ResourceDao`

A thin façade over `ResourceDao`. Provides:

- `insert(resource): Long` — creates a new vocabulary entry; returns its generated ID.
- `getAll(): Flow<List<Resource>>` — reactive stream of all resources; used in therapist's materials list screen.
- `getAllOnce(): List<Resource>` — one-shot read; used during initial data seeding.
- `getById(resourceId): Resource` — fetch by PK.
- `getByName(name): Resource` — fetch by display name; throws `IllegalArgumentException` if not found.
- `delete(resource)` — deletes a resource row; the database CASCADE removes linked `resource_images` rows.
- `update(resource)` — updates an existing resource's fields.

### 5.2 `ImageRepository`

**File:** `shared/.../repositories/ImageRepository.kt`  
**DI scope:** Unscoped  
**Dependencies:** `ImageDao`

Manages image records and the `resource_images` link table. Key logic above simple DAO delegation:

**`deleteUnassignedImages()`** — Scans all `Image` rows, calls `getReferenceCount` for each, and deletes any with 0 references. This is the only garbage-collection mechanism for orphaned image records. Note: it does **not** delete the corresponding files from `filesDir`; file cleanup must be handled separately.

**`linkImagesToResource(resourceId, imageIds)`** — Replaces the entire set of images for a resource:
1. Calls `deleteImageLinksForResource(resourceId)` — removes all existing `resource_images` rows for this resource.
2. Calls `insertResourceImageLinks(...)` — inserts new `ResourceImage` rows for the provided `imageIds`.

**`unlinkImagesFromResource(resourceId, imageIds)`** — Selectively removes specific image links without deleting all of them.

### 5.3 `ConfigurationRepository`

**File:** `shared/.../repositories/ConfigurationRepository.kt`  
**DI scope:** `@Singleton`  
**Dependencies:** `ConfigurationDao`, `ResourceDao`, `ConfigurationResourceDao`

This is the most complex repository, coordinating reads that span multiple tables. Key responsibilities beyond DAO pass-through:

**`setActiveConfiguration(configuration, mode)`**  
Atomically changes which configuration is active:
1. `clearActiveConfiguration()` — sets `isActive=0` and `activeMode=NULL` for all rows.
2. `activateConfiguration(configuration.id, mode)` — sets `isActive=1` and `activeMode=mode` for the chosen row.

These two SQL statements are executed sequentially as `suspend` coroutine calls within a single coroutine context but are **not wrapped in an explicit transaction** — a crash between the two calls could leave no configuration active.

**`getResourcesWithImagesForActiveConfig(): List<ResourceWithImages>`**  
1. Reads the active `Configuration` via `getActiveConfiguration().firstOrNull()`.
2. Fetches the list of `ConfigurationResource` join rows for that configuration.
3. Loads all `ResourceWithImages` (full join across `resources` → `resource_images` → `images`).
4. Filters to only those resources whose IDs appear in the configuration's resource list.

**`getResourcesWithImagesForActiveConfigFiltered(isTestMode): List<ResourceWithImages>`**  
Extends the above with image-level filtering:
1. Loads the active configuration and its linked resource IDs.
2. Loads `ConfigurationImageUsage` rows for the active configuration.
3. Keeps only `imageId`s where `inLearning == true` (learning mode) or `inTest == true` (test mode).
4. Returns `ResourceWithImages` objects with `images` lists filtered to the allowed IDs; entries where no images remain after filtering are removed.

**`hasMaterialsForActiveConfig(isTestMode): Boolean`**  
Returns `true` if `getResourcesWithImagesForActiveConfigFiltered` returns at least one entry. Used by the child app to decide whether a session can start.

**`getConfigurationNamesUsingResource(resourceId): List<String>`**  
Used to display a warning dialog in the therapist UI when the user attempts to delete a resource that is referenced by one or more configurations.

### 5.4 `PreferencesRepository`

**File:** `app/.../therapist/data/PreferencesRepository.kt`  
**DI scope:** `ViewModelComponent` (new instance per ViewModel, but DataStore delegate is process-singleton)  
**Dependencies:** Android `Context` (for the DataStore delegate)

Stores therapist UI preferences using **Jetpack DataStore Preferences** (not SharedPreferences). The DataStore file is named `user_preferences` and stored in the app's private DataStore directory (`datastore/user_preferences.preferences_pb`).

| Key                       | Type    | Default | Description                                                                  |
|---------------------------|---------|---------|------------------------------------------------------------------------------|
| `hide_example_materials`  | Boolean | `false` | When `true`, the materials list hides items with `isExample = true`          |
| `hide_example_steps`      | Boolean | `false` | When `true`, the configuration list hides items with `isExample = true`      |

Both values are exposed as `Flow<Boolean>` streams and written via `suspend` editing functions. The DataStore is scoped to the therapist UI only — the child app does not read these preferences.

---

## 6. Shared Data Between Applications

**There is no cross-process data sharing.** The therapist launcher (`MainActivity`) and the child launcher (`MainActivityChild`) are two `Activity` classes compiled into the same APK, running in the same Linux process, sharing the same Hilt `SingletonComponent`.

Data sharing happens through the normal Kotlin/DI object graph:

```
MainActivity (therapist)          MainActivityChild (child)
       │                                    │
       ▼                                    ▼
 HiltViewModel                       HiltViewModel
       │                                    │
       │     same Hilt SingletonComponent   │
       └──────────────┬─────────────────────┘
                      ▼
        ConfigurationRepository (@Singleton)
        ResourceRepository
        ImageRepository
                      │
                      ▼
           AppDatabase (Room, @Singleton)
           SQLite file: databases/friendly_words
```

**Communication mechanism:**  
The therapist configures a session (selects resources, images, settings) and activates a configuration by calling `ConfigurationRepository.setActiveConfiguration(config, mode)`. This sets `isActive=1` on the chosen row.

The child app observes `ConfigurationRepository.getActiveConfiguration()` — a `Flow<Configuration?>` that emits whenever the `configurations` table changes. When the therapist activates a configuration, this flow emits the new active `Configuration` and the child app reacts accordingly.

**No explicit synchronization primitive** (broadcast, intent, socket, etc.) is needed because both activities share the same in-process Room database and Kotlin Coroutines Flow infrastructure.

---

## 7. Data Lifecycle

### Creation

**Example data (first launch):**  
On first launch, `MainScreenViewModel.init` runs `ensureExampleMaterials()` followed by `ensureExampleConfigurations()`. These methods check whether the `resources` and `configurations` tables are empty and, if so, insert seeded example data.

- **50 example `Resource` rows** spanning categories: animals, food, clothing, toys, vehicles, activities, rooms, furniture, places, drinks, and equipment.
- For each resource, **3 `Image` records** are inserted with paths of the form `file:///android_asset/exemplary_photos/{normalized_name}_{1..3}.webp` (referencing APK assets).
- `ResourceImage` links are created for each resource ↔ image pair.
- **2 example `Configuration` rows** are inserted, each pre-linked to a subset of the example resources and images, with one configuration set as active (`isActive=1`, `activeMode="uczenie"`).

**User-created data:**  
The therapist creates resources and configurations through the therapist UI:
- A new `Resource` is inserted via `ResourceRepository.insert()`.
- Images are copied to `context.filesDir`, `Image` records are inserted, and `ResourceImage` links are created via `ImageRepository`.
- A new `Configuration` is inserted via `ConfigurationRepository.insert()`.
- Resources are linked to the configuration via `ConfigurationRepository.addResource()` / `insertResources()`.
- Image usage flags (`inLearning`, `inTest`) are recorded via `ConfigurationRepository.addImageUsage()` / `insertImageUsages()`.

### Updates

- **Resource updates:** The therapist can edit a resource's `name`, `learnedWord`, and `category` via `ResourceRepository.update()`. Associated images are managed by relinking through `ImageRepository.linkImagesToResource()`.
- **Configuration updates:** `ConfigurationRepository.update()` replaces all embedded settings columns. Changes to linked resources or image usages require separate DAO calls to modify the join tables.
- **Active configuration switch:** `ConfigurationRepository.setActiveConfiguration()` clears the flag on all rows and sets it on the chosen one.

### Deletion

**Deleting a Resource:**
1. `ResourceRepository.delete(resource)` — Room executes `DELETE FROM resources WHERE id = ?`.
2. The `CASCADE` foreign key on `resource_images` automatically removes all `ResourceImage` rows for this resource.
3. The `CASCADE` on `configuration_resources` removes all configuration links for this resource.
4. `ImageRepository.deleteUnassignedImages()` must be called afterwards to clean up `Image` rows whose reference count dropped to zero.
5. Physical image files in `filesDir` are **not deleted** automatically — there is no file cleanup code hooked into the deletion flow.

**Deleting a Configuration:**
1. `ConfigurationRepository.delete(configuration)` — Room deletes the `configurations` row.
2. `CASCADE` on `configuration_resources` removes all resource links.
3. `CASCADE` on `configuration_image_usages` removes all image usage flags.

### Synchronization

There is no synchronization with an external service. All data is local to the device. The Room database itself is thread-safe (accessed through suspend coroutines on the appropriate dispatcher). Reactive `Flow` queries automatically notify observers when underlying data changes, which is the only intra-app "sync" mechanism.

---

## 8. Configuration Storage

Learning settings are stored in two complementary systems:

### Room Database (primary — session configuration)

The `configurations` table holds all behavioural parameters for a therapy session. The `@Embedded` `LearningSettings` and `TestSettings` objects are flattened into the row with `learning_` / `test_` column prefixes.

At runtime, the `LearningSettings` is converted to domain-level state objects by extension functions:

| Extension function                               | Output type                    | Purpose                                              |
|--------------------------------------------------|--------------------------------|------------------------------------------------------|
| `LearningSettings.toConfigurationLearningState()`| `ConfigurationLearningState`   | Drives learning-round display & hint behaviour       |
| `LearningSettings.toConfigurationReinforcementState()` | `ConfigurationReinforcementState` | Controls praise words and animations          |
| `LearningSettings.asRoundSettings()`             | `RoundSettings`                | Supplies word count and repetition count to the game engine |
| `TestSettings.toConfigurationTestState()`        | `ConfigurationTestState`       | Drives test-round display & timing behaviour         |
| `TestSettings.asRoundSettings()`                 | `RoundSettings`                | Same as above for test mode                          |

The `commandType` string stored in the database (`"SHORT"`, `"WHERE_IS"`, `"SHOW_ME"`) is mapped to a Polish-language prompt template string at the point of conversion, not at storage time.

### Jetpack DataStore (secondary — UI preferences)

The `PreferencesRepository` stores two therapist UI flags:

| Key                      | Effect                                                                   |
|--------------------------|--------------------------------------------------------------------------|
| `hide_example_materials` | Hides `isExample=true` resource rows from the therapist materials list   |
| `hide_example_steps`     | Hides `isExample=true` configuration rows from the therapist config list |

These flags affect only the therapist UI and are never read by the child app. They survive app restarts (stored in a Proto DataStore file) but are not backed up or synchronized.

---

## 9. Media Storage

The application uses two distinct media storage locations, distinguished by the `path` column in the `images` table.

### Bundled Example Images (Read-only, APK assets)

**Path format:** `file:///android_asset/exemplary_photos/{name}_{index}.webp`  
**Example:** `file:///android_asset/exemplary_photos/kot_1.webp`

Example images are `.webp` files bundled inside `assets/exemplary_photos/` in the APK. They are referenced by URI string and never copied to disk. Name normalization is applied when constructing the path — Polish diacritics are removed and spaces are replaced with underscores:

```kotlin
private fun normalizeName(name: String): String {
    val map = mapOf(
        'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l',
        'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
        // uppercase variants...
    )
    return name.map { map[it] ?: it }.joinToString("")
}
// "Żaba" → "Zaba" → paths: zaba_1.webp, zaba_2.webp, zaba_3.webp
```

Each example resource expects exactly 3 asset images (index 1–3).

### User-Added Images (Read-write, internal storage)

**Path format:** absolute path in `context.filesDir`  
**Example:** `/data/data/com.example.friendly_words/files/image_1719310234567.jpg`

User-added images are written to the app's private internal storage directory (`context.filesDir`). There are two entry points:

**From the device gallery:**
```kotlin
// ActivityResultContracts.GetContent() with "image/*"
// URI opened from ContentResolver, copied byte-by-byte to filesDir
val fileName = "image_${System.currentTimeMillis()}.jpg"
val file = File(context.filesDir, fileName)
context.contentResolver.openInputStream(uri)?.copyTo(file.outputStream())
```

**From the camera (thumbnail):**
```kotlin
// ActivityResultContracts.TakePicturePreview() — returns a Bitmap
val file = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
FileOutputStream(file).use { out ->
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
}
```

> **Limitation:** `TakePicturePreview` returns a thumbnail-resolution Bitmap from the camera, not a full-resolution photo. No `FileProvider` is configured, so full-resolution camera capture (`ACTION_IMAGE_CAPTURE` with a file URI) is not available.

**Access:** Images stored in `filesDir` are private to the app process and are not accessible to other apps or the Android media scanner. No `FileProvider` is declared in `AndroidManifest.xml`.

**Orphan cleanup:** `ImageRepository.deleteUnassignedImages()` removes `Image` database rows whose `resource_images` reference count is 0. However, this method does **not** delete the corresponding files from `filesDir`. Over time, removing resources can leave unreferenced `.jpg` files on disk with no cleanup mechanism.

**Audio files:** There is no audio file storage in the codebase. Any speech/TTS functionality is handled at the system level (Android TTS engine) without storing audio files locally.
