package pg.autyzm.friendlyemotions.child.game

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import java.util.Locale

private const val TAG = "TtsController"

/**
 * Owns the single `TextToSpeech` instance for a game session (ADR-015, target-architecture.md
 * §7.4). `@ViewModelScoped`, injected into `GameViewModel`, which delegates to [shutdown] from
 * `onCleared()` since `@ViewModelScoped` does not invoke any cleanup automatically.
 *
 * This is the only class in `:feature:child` allowed to touch `android.speech.tts.*`/[Locale] —
 * [GameViewModel] reads [localeCode] instead, so it can pass a plain string into
 * `PromptRenderer.render(...)` without any `Locale` API call of its own.
 */
class TtsController(context: Context) : DefaultLifecycleObserver {
    /** [EmotionCatalog.LOCALE_POLISH] / [EmotionCatalog.LOCALE_ENGLISH], detected from the device locale. */
    val localeCode: String = detectLocaleCode()

    private var isReady = false

    /**
     * `TextToSpeech`'s engine init is async, but the first trial calls [speak] as soon as
     * `GameUiState.Content` is rendered — often before [status] SUCCESS fires — so those
     * utterances would otherwise be silently dropped. Buffered here and replayed once ready.
     */
    private val pendingUtterances = mutableListOf<PendingUtterance>()

    private val _languageUnavailable = MutableStateFlow(false)

    /**
     * True once engine init has finished and the device's TTS engine has no installed voice data
     * for [localeCode] (`setLanguage` returned [TextToSpeech.LANG_MISSING_DATA] /
     * [TextToSpeech.LANG_NOT_SUPPORTED]) — e.g. some OEM engines (observed on Samsung One UI
     * devices) ship without the Polish voice pre-installed. Unlike a silently-dropped [speak] call,
     * this is surfaced so `GameViewModel`/`GameScreen` can tell the user to install the voice
     * instead of the prompt just staying silent with no explanation.
     */
    val languageUnavailable: StateFlow<Boolean> = _languageUnavailable.asStateFlow()

    private var textToSpeech: TextToSpeech? =
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                when (val languageResult = textToSpeech?.setLanguage(localeFor(localeCode))) {
                    TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.w(
                            TAG,
                            "TTS voice data missing/unsupported for locale '$localeCode' (result=$languageResult)",
                        )
                        _languageUnavailable.value = true
                    }

                    else -> {
                        isReady = true
                        pendingUtterances.forEach { (text, queueMode) ->
                            textToSpeech?.speak(text, queueMode, null, null)
                        }
                        pendingUtterances.clear()
                    }
                }
            } else {
                Log.e(TAG, "TextToSpeech engine initialization failed (status=$status)")
            }
        }

    /** @param queueMode one of [QUEUE_FLUSH]/[QUEUE_ADD]. */
    fun speak(
        text: String,
        queueMode: Int,
    ) {
        if (isReady) {
            textToSpeech?.speak(text, queueMode, null, null)
        } else {
            if (queueMode == QUEUE_FLUSH) pendingUtterances.clear()
            pendingUtterances.add(PendingUtterance(text, queueMode))
        }
    }

    override fun onDestroy(owner: LifecycleOwner) = shutdown()

    fun shutdown() {
        pendingUtterances.clear()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun detectLocaleCode(): String =
        if (Locale.getDefault().language == Locale(EmotionCatalog.LOCALE_POLISH).language) {
            EmotionCatalog.LOCALE_POLISH
        } else {
            EmotionCatalog.LOCALE_ENGLISH
        }

    private fun localeFor(code: String): Locale =
        if (code == EmotionCatalog.LOCALE_POLISH) Locale(EmotionCatalog.LOCALE_POLISH) else Locale.ENGLISH

    companion object {
        const val QUEUE_FLUSH = TextToSpeech.QUEUE_FLUSH
        const val QUEUE_ADD = TextToSpeech.QUEUE_ADD
    }
}

private data class PendingUtterance(val text: String, val queueMode: Int)
