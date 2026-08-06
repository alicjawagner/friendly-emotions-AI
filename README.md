# Friendly Emotions 😊

**Friendly Emotions** (*Przyjazne Emocje*) is an Android educational app for children with developmental needs (e.g. autism spectrum disorder). It is a sibling of **Friendly Words** (*Przyjazne Słowa*): instead of teaching word–image associations, it teaches **emotion recognition**.

The child is shown photographs of people (or animals, emojis) expressing emotions and asked to tap the image that matches the emotion written on screen and spoken aloud. Therapists configure materials and learning steps in a separate settings entry point. Both ship in a single APK, share a local Room database, and need no network.

---

## 🤖 AI-assisted development

This project is an experiment in building a production-minded Android app **with as much AI-assisted development as possible**. The workflow is documentation-first: establish a reliable knowledge base, specify the new product, plan implementation, then code phase by phase with AI — refining as needed.

### How the project was set up

1. **📚 Reference documentation (Friendly Words)**  
   Friendly Words is a similar app from the same family, already built by other students. Its codebase was reverse-documented with AI into a structured knowledge base covering inventory, domain model, learning session flow, therapist configuration, data architecture, and architecture improvement analysis (`docs/reference/friendly-words/`).

2. **📝 Target documentation & specification (Friendly Emotions)**  
   Using that knowledge base, AI-generated docs and a full functional specification for the new app were produced: domain model, architecture, project setup, ADRs, and the functional spec (`docs/target/`).

3. **🗺️ Implementation roadmap**  
   An implementation roadmap was generated from those specs (`docs/target/implementation-roadmap.md`), breaking the work into buildable, testable phases.

4. **🛠️ Phase-by-phase implementation**  
   Development follows that roadmap. Each AI coding session targets one phase; generated code is reviewed, and the plan or specs are refined when reality requires it.

UI layouts come from Figma (used as the visual source of truth during implementation). Architecture decisions are captured in ADRs (`docs/target/adr/`).

---

## 📱 App overview

| Entry point | Audience | Role |
|---|---|---|
| **Child app** | Children in therapy | Learning / assessment game (emotion matching) |
| **Therapist app** | Therapists | Manage materials and learning steps |

**Key differences from Friendly Words:** fixed emotion catalog (happy, sad, surprised, angry, scared, bored), material hierarchy `Emotion → Folder → Image`, grammatical gender on images for Polish prompts, and bilingual Polish / English support.

---

## ⚙️ Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | Clean Architecture, MVVM, feature modules |
| DI | Hilt |
| Persistence | Room (local only) |
| Modules | `:app`, `:domain`, `:data`, `:feature:child`, `:feature:therapist`, `:core:ui` |

---

## 📂 Documentation map

| Path | Contents |
|---|---|
| [`docs/reference/friendly-words/`](docs/reference/friendly-words/) | Knowledge base from the sibling Friendly Words app |
| [`docs/target/friendly-emotions-functional-specification.md`](docs/target/friendly-emotions-functional-specification.md) | Functional specification |
| [`docs/target/target-domain.md`](docs/target/target-domain.md) | Domain model |
| [`docs/target/target-architecture.md`](docs/target/target-architecture.md) | Architecture |
| [`docs/target/project-setup-specification.md`](docs/target/project-setup-specification.md) | Project / toolchain setup |
| [`docs/target/adr/`](docs/target/adr/) | Architecture Decision Records |
| [`docs/target/implementation-roadmap.md`](docs/target/implementation-roadmap.md) | Phased implementation plan (source of day-to-day work) |

---

## 🚀 Building

Requires JDK 17 and Android Studio (or the Android SDK / Gradle toolchain).

```bash
./gradlew assembleDebug
```

The debug build installs two launcher icons: one for the child app and one for therapist settings.

