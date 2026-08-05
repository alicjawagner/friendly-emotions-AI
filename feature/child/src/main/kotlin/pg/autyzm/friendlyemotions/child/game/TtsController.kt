package pg.autyzm.friendlyemotions.child.game

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import java.util.Locale

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

    private var textToSpeech: TextToSpeech? =
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = localeFor(localeCode)
            }
        }

    /** @param queueMode one of [QUEUE_FLUSH]/[QUEUE_ADD]. */
    fun speak(
        text: String,
        queueMode: Int,
    ) {
        textToSpeech?.speak(text, queueMode, null, null)
    }

    override fun onDestroy(owner: LifecycleOwner) = shutdown()

    fun shutdown() {
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
