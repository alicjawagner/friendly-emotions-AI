# ADR-013: Wizard Architecture with Container ViewModel and Shared Draft State

# Status
Accepted

# Context
The Learning Step configuration wizard has five tabs: Materials, Learning, Reinforcement, Test, and Save. The tabs are not independent — the Learning tab drives the default values for the Test tab (test parameters inheritance), and the Materials tab determines the valid range for image count in the Learning tab. All five tabs contribute to a single aggregate (`LearningStepDraft`) that is saved atomically only when the therapist confirms on the Save tab.

The Friendly Words analysis (C-1 and C-2 findings) identified that this kind of wizard was implemented with a single God-ViewModel that owned all state for all five tabs, resulting in a class with hundreds of lines of mixed concerns that was untestable in isolation.

Two alternatives were considered:
1. **God-ViewModel:** One ViewModel owns all wizard state. Simple to implement, but grows unbounded and impossible to unit-test by tab.
2. **Independent tab ViewModels with no shared state:** Each tab ViewModel is isolated. Fails because tabs need to share the draft (e.g. Test tab must read Learning tab's parameters).

# Decision
The wizard is coordinated by a two-tier ViewModel structure:

- **`WizardContainerViewModel`** (`@HiltViewModel`, scoped to the wizard navigation back-stack entry via `hiltNavGraphViewModel()`): owns the single `WizardStepDraft` as a `StateFlow`. Exposes update functions that tab ViewModels call to write their slice of the draft. Owns the final save operation (`SaveLearningStepUseCase`). All five tab ViewModels share this single instance.

- **Five focused tab ViewModels** (one per tab): each ViewModel reads the slice of `WizardStepDraft` it needs, exposes its own `UiState`, and calls the appropriate update function on `WizardContainerViewModel` when the user makes a change. Tab ViewModels have no direct reference to each other.

Cross-tab state flow:
- `TestTabViewModel` reads `WizardStepDraft.learningParameters` and calls `DeriveTestParametersUseCase` whenever `overridesLearning = false` and learning parameters change.
- `LearningTabViewModel` reads `WizardStepDraft.materialSelection.imageCount` to enforce the valid display image count range.

The draft exists only in memory. There is no auto-save to the database. Navigating away without saving triggers a discard confirmation dialog.

# Consequences
**Benefits:**
- Each tab ViewModel is independently testable — it can be exercised with a fake `WizardContainerViewModel` without instantiating the full wizard.
- `WizardContainerViewModel` is the single source of truth for the draft — there is no possibility of divergent state between tabs.
- The wizard's concerns are cleanly partitioned: the container owns persistence and draft integrity; each tab ViewModel owns its tab's UX logic.
- `hiltNavGraphViewModel()` scoping ensures the draft survives tab navigation within the wizard and is discarded when the wizard is popped from the back stack.

**Trade-offs:**
- Tab ViewModels depend on `WizardContainerViewModel`'s API — changes to the draft structure require updating all tab ViewModels that read or write the changed slice.
- The two-tier ViewModel pattern is unconventional and requires documentation so new developers do not revert to a God-ViewModel.

**Limitations:**
- Draft persistence (surviving process death) is not implemented. If the process is killed mid-wizard, the draft is lost. This is an acceptable limitation for the initial version; if required, `SavedStateHandle` could be used to persist the draft.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §8.1 Wizard Architecture, §12.1 Key ViewModels (WizardContainerViewModel)
- `docs/friendly-emotions/target-domain.md` — §15 Configuration Model (the five-tab wizard business perspective)
- `docs/friendly-words/06-architecture-improvement-analysis.md` — C-1, C-2 findings (God-ViewModel in wizard)
