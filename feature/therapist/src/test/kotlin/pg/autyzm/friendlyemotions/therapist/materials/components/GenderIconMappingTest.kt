package pg.autyzm.friendlyemotions.therapist.materials.components

import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.therapist.R

class GenderIconMappingTest {
    @Test
    fun `cycled starts at feminine when null`() {
        assertEquals(GrammaticalGender.FEMININE, null.cycled())
    }

    @Test
    fun `cycled rotates feminine to masculine to neuter back to feminine`() {
        assertEquals(GrammaticalGender.MASCULINE, GrammaticalGender.FEMININE.cycled())
        assertEquals(GrammaticalGender.NEUTER, GrammaticalGender.MASCULINE.cycled())
        assertEquals(GrammaticalGender.FEMININE, GrammaticalGender.NEUTER.cycled())
    }

    @Test
    fun `badgeIconResOrEmpty falls back to empty_set drawable when null`() {
        assertEquals(R.drawable.empty_set, null.badgeIconResOrEmpty())
    }

    @Test
    fun `badgeIconResOrEmpty delegates to badgeIconRes when assigned`() {
        assertEquals(GrammaticalGender.MASCULINE.badgeIconRes(), GrammaticalGender.MASCULINE.badgeIconResOrEmpty())
    }
}
