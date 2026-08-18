package pg.autyzm.friendlyemotions.therapist.materials.components

import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import java.util.Locale

/** Mirrors [pg.autyzm.friendlyemotions.child.game.TtsController]'s locale-code detection. */
internal fun currentLocaleCode(): String =
    if (Locale.getDefault().language == Locale(EmotionCatalog.LOCALE_POLISH).language) {
        EmotionCatalog.LOCALE_POLISH
    } else {
        EmotionCatalog.LOCALE_ENGLISH
    }
