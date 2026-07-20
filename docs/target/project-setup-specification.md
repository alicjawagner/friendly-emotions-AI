# Project Setup Specification — Friendly Emotions

> **Role:** Senior Android Engineer  
> **Date:** 2026-07-17  
> **Scope:** Project setup and development environment only. No business logic, UI design, or implementation decisions.  
> **Baseline:** Friendly Words v2 platform configuration. Approved deviations are marked **[CHANGED]** or **[NEW]**.

---

## Quick Reference

| Property | Value |
|---|---|
| Application ID | `pg.autyzm.friendlyemotions` |
| Compile SDK | 36 |
| Target SDK | 36 |
| Min SDK | 24 |
| Kotlin | 2.1.20 |
| AGP | 8.9.1 |
| Gradle | 8.11.1 |
| Java | 17 |
| Compose BOM | 2025.09.01 |
| Room | 2.7.1 |
| Hilt | 2.56.2 |
| Navigation Compose | 2.9.0 |
| Kotlin Coroutines | 1.9.0 |

---

## 1. Development Environment

| Tool | Version |
|---|---|
| **Android Studio** | latest stable (2026.x) |
| **JDK** | 17 (LTS — required by AGP 8.x) |

---

## 2. Android Platform Configuration

Applies uniformly to all Gradle modules unless noted.

| Property | Value | Notes |
|---|---|---|
| **compileSdk** | 36 | All modules |
| **targetSdk** | 36 | `:app` only |
| **minSdk** | 24 | All modules |
| **Build tools** | AGP-managed | — |

---

## 3. Build System

| Tool | Version | Notes |
|---|---|---|
| **Android Gradle Plugin (AGP)** | 8.9.1 | SDK 36 support |
| **Gradle Wrapper** | 8.11.1 | Required by AGP 8.9.x |
| **Kotlin** | 2.1.20 | Enables `kotlin.compose` compiler plugin |
| **KSP** | 2.1.20-1.0.32 | Must match Kotlin major version |

All dependency versions are declared in a single `gradle/libs.versions.toml` version catalog shared across all modules. No version strings appear in individual `build.gradle.kts` files.

---

## 4. Module Structure and Gradle Plugins

The project consists of six Gradle modules. Each module declares only the plugins it actually needs.

| Module | Gradle plugin(s) |
|---|---|
| `:app` | `com.android.application`, `kotlin.android`, `kotlin.plugin.compose`, `ksp`, `hilt.android` |
| `:domain` | `com.android.library`, `kotlin.android` |
| `:data` | `com.android.library`, `kotlin.android`, `ksp`, `hilt.android` |
| `:feature:child` | `com.android.library`, `kotlin.android`, `kotlin.plugin.compose`, `ksp`, `hilt.android` |
| `:feature:therapist` | `com.android.library`, `kotlin.android`, `kotlin.plugin.compose`, `kotlin.plugin.serialization`, `ksp`, `hilt.android` |
| `:core:ui` | `com.android.library`, `kotlin.android`, `kotlin.plugin.compose` |

> **`:domain` note:** The module type is `com.android.library` per the target architecture, but it must import zero `android.*` or `androidx.*` APIs. All code in this module is pure Kotlin and is unit-tested on the JVM without robolectric.

---

## 5. Key Dependencies

### 5.1 Jetpack Compose

| Library | Version | Notes |
|---|---|---|
| **Compose BOM** | `2025.09.01` | **[CHANGED]** from FW `2024.04.01` — see rationale below |
| Compose UI, Runtime, Foundation | BOM-managed | — |
| Compose Animation | BOM-managed | Reinforcement sprite animations |
| Compose UI Tooling Preview | BOM-managed | `debugImplementation` |

**Material Design library:** `androidx.compose.material3` (BOM-managed).  
Material M2 (`androidx.compose.material:material`) is **intentionally excluded**. The architecture specifies Material3 as the sole design system.

> **Rationale for BOM upgrade:** The Friendly Words BOM (`2024.04.01`) is over two years old at the time this project starts. Using a two-year-old BOM on a greenfield codebase means inheriting known bugs, missing compiler performance improvements, and accumulating immediate technical debt. There is no compatibility constraint that requires holding back.

### 5.2 Architecture and Navigation

| Library | Version | Notes |
|---|---|---|
| **Navigation Compose** | 2.9.0 | Type-safe routes API (2.8+) used in `:feature:therapist` |
| **lifecycle-viewmodel-compose** | 2.9.0 | ViewModel scoping in Compose |
| **activity-compose** | 1.10.1 | `setContent {}` in Activities |

### 5.3 Dependency Injection

| Library | Version | Scope |
|---|---|---|
| **Hilt** (`dagger.hilt.android`) | 2.56.2 | `implementation` |
| **Hilt Compiler** (`hilt-compiler`) | 2.56.2 | `ksp` |
| **hilt-navigation-compose** | 1.2.0 | `implementation` — `hiltViewModel()` inside NavHost |

### 5.4 Persistence

| Library | Version | Scope |
|---|---|---|
| **Room Runtime** | 2.7.1 | `implementation` |
| **Room KTX** | 2.7.1 | `implementation` |
| **Room Compiler** | 2.7.1 | `ksp` |
| **DataStore Preferences** | 1.1.7 | `implementation` — therapist UI preferences |

Room database configuration:

| Setting | Value |
|---|---|
| Database name | `friendly_emotions` |
| Starting version | 1 |
| `exportSchema` | `true` |
| Schema directory | `$projectDir/schemas/` (committed to source control) |
| `fallbackToDestructiveMigration` | **not set** (never enabled) |
| Foreign key enforcement | `onOpen` callback |

### 5.5 Asynchronous and Reactive

| Library | Version | Notes |
|---|---|---|
| **kotlinx-coroutines-core** | 1.9.0 | **[CHANGED]** from FW `1.7.3` — improved structured concurrency, `Flow.chunked`, resolved `StateFlow` edge cases |
| **kotlinx-coroutines-android** | 1.9.0 | Main-thread dispatcher |

### 5.6 Serialization **[NEW]**

| Library | Version | Notes |
|---|---|---|
| **kotlinx-serialization-json** | 1.7.3 | Required by Navigation 2.8+ type-safe routes in `:feature:therapist` |

Plugin `org.jetbrains.kotlin.plugin.serialization` is applied to `:feature:therapist` and `:app`.  
This dependency is absent from Friendly Words because that project uses string-based route constants. Friendly Emotions uses typed sealed class routes (`TherapistRoutes.kt`), which require the serialization runtime at compile time.

### 5.7 Image Loading

| Library | Version | Notes |
|---|---|---|
| **Coil Compose** (`io.coil-kt.coil3:coil-compose`) | 3.0.4 | **[CHANGED]** from FW `io.coil-kt:coil-compose:2.3.0` |

> **Coil 3 migration note:** The Maven group changed from `io.coil-kt` to `io.coil-kt.coil3`. The `AsyncImage` composable API is unchanged. Coil 3 drops the legacy OkHttp dependency and integrates directly with Kotlin coroutines.

### 5.8 Unit Testing **[NEW]**

| Library | Version | Scope |
|---|---|---|
| **JUnit 4** | 4.13.2 | `testImplementation` |
| **MockK** | 1.13.12 | `testImplementation` — use case and repository faking |
| **kotlinx-coroutines-test** | 1.9.0 | `testImplementation` — `TestDispatcher`, `runTest`, `advanceTimeBy` |
| **Hilt Android Testing** | 2.56.2 | `testImplementation` |

### 5.9 UI Testing **[NEW]**

| Library | Version | Scope |
|---|---|---|
| **Compose UI Test JUnit4** | BOM-managed | `androidTestImplementation` |
| **Compose UI Test Manifest** | BOM-managed | `debugImplementation` |
| **Hilt Android Testing** | 2.56.2 | `androidTestImplementation` |
| **Hilt Android Compiler** | 2.56.2 | `androidTestImplementation` (KSP) |

---

## 6. Required Android Permissions

Declared in `:app/src/main/AndroidManifest.xml`:

| Permission | Reason |
|---|---|
| `android.permission.CAMERA` | Full-resolution camera capture for therapist image upload |

No `INTERNET`, `ACCESS_NETWORK_STATE`, or storage permissions are required. The app is fully offline. Gallery access uses the system photo picker (`ActivityResultContracts.PickVisualMedia`), which requires no permission on Android 13+ and gracefully degrades on older versions within minSdk 24.

**FileProvider** must be declared in `:app/AndroidManifest.xml` to support full-resolution camera capture via `ActivityResultContracts.TakePicture()`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

`res/xml/file_paths.xml` must declare the `files-dir` path corresponding to `context.filesDir/images/`.

---

## 7. Build Variants

Standard Android build types only. No custom product flavors required.

| Build Type | Purpose |
|---|---|
| `debug` | Local development; debuggable; no R8 |
| `release` | Production; R8 minification enabled; signing config required |

The two-sub-application structure (child + therapist) is implemented via two `<activity>` launcher entries in a single `AndroidManifest.xml`, not via product flavors.

---

## 8. Application Configuration

| Property | Value |
|---|---|
| **Application ID** | `pg.autyzm.friendlyemotions` |
| **Namespace** (`:app`) | `pg.autyzm.friendlyemotions` |
| **Application class** | `FriendlyEmotionsApp` — `@HiltAndroidApp` |
| **TherapistActivity** | `singleTask`, forced landscape |
| **ChildActivity** | `singleTask`, forced landscape |

Both Activities are declared in `:app/AndroidManifest.xml` with `android.intent.action.MAIN` + `android.intent.category.LAUNCHER`, producing two separate launcher icons.

---

## 9. Code Formatting and Linting

| Tool | Version | Notes |
|---|---|---|
| **ktlint** (via `org.jlleitschuh.gradle.ktlint` plugin) | 12.x | Applied at root; all modules inherit |
| **Android Lint** | AGP-bundled | `abortOnError = true` in all modules |

`.editorconfig` settings: 4-space indent, 120-character line length, Kotlin style guide.

ktlint runs as a Gradle task (`./gradlew ktlintCheck`) and should be wired into CI. Run `./gradlew ktlintGenerateBaseline` on the empty project before the first commit.

---

## 10. Recommended Project Structure

```
friendly-emotions/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts                        ← root; plugin declarations only, no apply
├── settings.gradle.kts                     ← module includes, repository config
│
├── app/
│   ├── build.gradle.kts
│   ├── schemas/                            ← Room schema JSON (committed)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/xml/file_paths.xml
│       └── kotlin/pg/autyzm/friendlyemotions/
│           ├── FriendlyEmotionsApp.kt
│           ├── TherapistActivity.kt
│           └── ChildActivity.kt
│
├── domain/
│   ├── build.gradle.kts
│   └── src/main/kotlin/pg/autyzm/friendlyemotions/domain/
│       ├── catalog/
│       ├── error/
│       ├── model/
│       │   ├── emotion/
│       │   ├── runtime/
│       │   └── session/
│       ├── repository/
│       ├── service/
│       └── usecase/
│           ├── learningStep/
│           ├── material/
│           └── session/
│
├── data/
│   ├── build.gradle.kts
│   └── src/main/kotlin/pg/autyzm/friendlyemotions/data/
│       ├── dao/
│       ├── database/
│       ├── datastore/
│       ├── di/
│       ├── entity/
│       ├── mapper/
│       └── repository/
│
├── feature/
│   ├── child/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/pg/autyzm/friendlyemotions/child/
│   │       ├── di/
│   │       ├── end/
│   │       ├── home/
│   │       ├── navigation/
│   │       └── session/
│   └── therapist/
│       ├── build.gradle.kts
│       └── src/main/kotlin/pg/autyzm/friendlyemotions/therapist/
│           ├── home/
│           ├── learningStep/
│           │   ├── list/
│           │   └── wizard/
│           ├── materials/
│           │   ├── emotionList/
│           │   ├── folderDetail/
│           │   └── folderList/
│           └── navigation/
│
└── core/
    └── ui/
        ├── build.gradle.kts
        └── src/main/kotlin/pg/autyzm/friendlyemotions/ui/
            ├── components/
            └── theme/
```

Package naming convention: `pg.autyzm.friendlyemotions.<module-path>`, mirroring the Gradle module hierarchy.

---

## 11. Pre-Implementation Checklist

Complete all items before opening the first feature branch.

| # | Task |
|---|---|
| 1 | **Room schema export** — Configure KSP arg `room.schemaLocation` to `"$projectDir/schemas"` in `:data/build.gradle.kts`. Commit the `schemas/` directory. Set `version = 1` on `AppDatabase`. |
| 2 | **FileProvider resource** — Create `app/src/main/res/xml/file_paths.xml` with `<files-path name="images" path="images/" />`. Declare the provider in `AndroidManifest.xml`. |
| 3 | **Lint gate** — Add `lint { abortOnError = true }` to the root `build.gradle.kts` and confirm all library modules inherit it. Run `./gradlew lint` on the empty project to establish a clean baseline. |
| 4 | **Launcher icons** — Add `friendly_emotions` (child) and `friendly_emotions_settings` (therapist) drawable resources to `:app`. Placeholder drawables are acceptable during development. |
| 5 | **Manifest launchers** — Declare both `TherapistActivity` and `ChildActivity` in `AndroidManifest.xml` with `action.MAIN` + `category.LAUNCHER` intent filters and `singleTask` launch mode. |
| 6 | **Module boundary verification** — After all modules are created, run `./gradlew :feature:child:dependencies` and `./gradlew :feature:therapist:dependencies`. Confirm that neither module's compile classpath contains any path to `:data`. |

---

*End of Project Setup Specification — Friendly Emotions*
