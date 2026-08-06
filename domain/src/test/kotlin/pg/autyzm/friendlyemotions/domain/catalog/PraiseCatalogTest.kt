package pg.autyzm.friendlyemotions.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings

class PraiseCatalogTest {
    @Test
    fun `resolve returns Polish spoken form for pl locale`() {
        assertEquals("dobrze", PraiseCatalog.resolve("dobrze", EmotionCatalog.LOCALE_POLISH))
        assertEquals("świetnie", PraiseCatalog.resolve("świetnie", EmotionCatalog.LOCALE_POLISH))
        assertEquals("rewelacja", PraiseCatalog.resolve("rewelacja", EmotionCatalog.LOCALE_POLISH))
    }

    @Test
    fun `resolve returns English spoken form for en locale`() {
        assertEquals("good", PraiseCatalog.resolve("dobrze", EmotionCatalog.LOCALE_ENGLISH))
        assertEquals("great", PraiseCatalog.resolve("świetnie", EmotionCatalog.LOCALE_ENGLISH))
        assertEquals("awesome", PraiseCatalog.resolve("ekstra", EmotionCatalog.LOCALE_ENGLISH))
        assertEquals("amazing", PraiseCatalog.resolve("rewelacja", EmotionCatalog.LOCALE_ENGLISH))
        assertEquals("bravo", PraiseCatalog.resolve("brawo", EmotionCatalog.LOCALE_ENGLISH))
        assertEquals("super", PraiseCatalog.resolve("super", EmotionCatalog.LOCALE_ENGLISH))
    }

    @Test
    fun `resolve uses English for any non-pl locale`() {
        assertEquals("good", PraiseCatalog.resolve("dobrze", "de"))
        assertEquals("great", PraiseCatalog.resolve("świetnie", "fr"))
    }

    @Test
    fun `resolve falls back to key when unknown`() {
        assertEquals("custom", PraiseCatalog.resolve("custom", EmotionCatalog.LOCALE_POLISH))
        assertEquals("custom", PraiseCatalog.resolve("custom", EmotionCatalog.LOCALE_ENGLISH))
    }

    @Test
    fun `every default praise key has a Polish and English form`() {
        for (key in ReinforcementSettings.PRAISE_WORDS) {
            val polish = PraiseCatalog.resolve(key, EmotionCatalog.LOCALE_POLISH)
            val english = PraiseCatalog.resolve(key, EmotionCatalog.LOCALE_ENGLISH)
            assertEquals(key, polish)
            assertTrue(english.isNotBlank())
        }
    }
}
