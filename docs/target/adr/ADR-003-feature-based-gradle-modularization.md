# ADR-003: Feature-Based Gradle Modularization

# Status
Accepted

# Context
In the Friendly Words codebase, accidental cross-feature dependencies and data-layer leakage into the UI layer were discovered. Because the project was a single-module application, the compiler could not enforce dependency direction — only code review could catch violations, and it failed to do so reliably. The consequence was ViewModel classes that directly injected DAOs, and UI code that depended on Room entity types.

Friendly Emotions is designed from scratch and must prevent these violations structurally, not by convention alone.

# Decision
The project is organized into six Gradle modules with strict dependency rules enforced at compile time:

| Module | Type | Role |
|---|---|---|
| `:app` | application | Shell, Activities, Hilt root component |
| `:domain` | library (pure Kotlin) | Domain layer — entities, use cases, repository interfaces |
| `:data` | library | Data layer — Room, DAOs, repository implementations, DataStore |
| `:feature:child` | library | Child App screens and ViewModels |
| `:feature:therapist` | library | Therapist App screens and ViewModels |
| `:core:ui` | library | Shared Material3 theme and reusable Compose components |

**Allowed dependency edges:**
- `:app` → everything
- `:feature:child` → `:domain`, `:core:ui`
- `:feature:therapist` → `:domain`, `:core:ui`
- `:data` → `:domain`
- `:core:ui` → nothing (or only Compose/Material3)
- `:domain` → nothing

**Forbidden edges (compiler-enforced — any violation is a build error):**
- `:feature:child` ↛ `:data`
- `:feature:therapist` ↛ `:data`
- `:feature:child` ↛ `:feature:therapist`
- `:domain` ↛ anything

The `:app` module is the only place where Hilt wires concrete implementations to domain interfaces — feature modules never see the implementations.

# Consequences
**Benefits:**
- Dependency direction violations are impossible to introduce silently — they produce compile errors.
- Feature modules can be built and tested in isolation without pulling in Room or other data-layer machinery.
- Clear ownership boundaries: the child experience is entirely in `:feature:child`, the therapist experience entirely in `:feature:therapist`.
- Build times can be improved with parallel module compilation and build caching.

**Trade-offs:**
- More `build.gradle` files to maintain and keep synchronized.
- Moving a class between modules requires updating import paths and dependency declarations.
- Initial project setup is more complex than a single-module project.

**Limitations:**
- The six-module structure is a deliberate point-in-time decision appropriate for the current project size. If the codebase grows significantly, feature modules may need further subdivision (e.g. `:feature:child:session`, `:feature:child:home`).

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §3 Module Structure, §18 Module Dependency Diagram
- `docs/friendly-words/06-architecture-improvement-analysis.md` — H-3 finding (no module boundary enforcement)
