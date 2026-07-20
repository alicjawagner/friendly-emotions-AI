# ADR-015: TTS Lifecycle Managed by a ViewModel Delegate

# Status
Accepted

# Context
The Child App uses Android's `TextToSpeech` engine to speak prompts and praise words during a session. `TextToSpeech` is a resource that must be initialized asynchronously, tied to a lifecycle, and shut down explicitly when no longer needed. If not shut down correctly, it leaks resources and can produce audio output after the session has ended.

The Friendly Words analysis (C-4 finding) identified a prior implementation where TTS was managed inside composable functions. This caused two problems: TTS was re-initialized on every recomposition, and its lifecycle was tied to the composition rather than to the ViewModel that actually drives the session.

# Decision
`TtsController` is a dedicated class that owns the `TextToSpeech` instance. It implements `DefaultLifecycleObserver` and is:

- Created and injected into `GameViewModel` via Hilt with `@ViewModelScoped` binding.
- Responsible for initializing the TTS engine, setting the locale (Polish or English based on device locale), exposing a `speak(text, queueMode)` function, and calling `shutdown()` when the ViewModel is cleared.
- Observed by `GameViewModel` — the ViewModel calls `ttsController.speak(...)` in response to trial state transitions. The composable never calls TTS directly.

`GameViewModel.onCleared()` delegates to `TtsController` to shut down the engine. Because `TtsController` is `@ViewModelScoped`, it shares the ViewModel's lifecycle — it is created once when the ViewModel is created and destroyed when the ViewModel is cleared (either by navigating away from the session or by process death).

No composable function holds a reference to `TtsController` or `TextToSpeech`. Composables emit `UiEvent`s; the ViewModel decides when and what to speak.

# Consequences
**Benefits:**
- TTS is initialized exactly once per session and shut down exactly once when the session ends — no re-initialization on recomposition.
- Composables remain stateless renderers with no Android resource ownership.
- TTS behavior is fully controllable in ViewModel unit tests by substituting a fake `TtsController`.
- The locale-detection logic is encapsulated in `TtsController`, keeping `GameViewModel` free of Android locale API calls.

**Trade-offs:**
- `TtsController` is a non-trivial Android class that requires a `Context`, making it an Android dependency in the presentation layer. This is appropriate — it is explicitly in `:feature:child`, not in `:domain`.
- Testing `TtsController` in isolation requires a real or Robolectric Android context, making it an instrumented test target.

**Limitations:**
- `TtsController` is scoped to `GameViewModel`, which covers the active session. If TTS is needed on other screens in the future (e.g. reading the info splash), a separate controller instance or a broader scoping strategy would be required.

# Related Documents
- `docs/friendly-emotions/target-architecture.md` — §7.4 TTS Management, §12 ViewModel Responsibilities, §20 Architecture Decision Summary (TTS ownership row)
- `docs/friendly-words/06-architecture-improvement-analysis.md` — C-4 finding (TTS lifecycle tied to composable)
