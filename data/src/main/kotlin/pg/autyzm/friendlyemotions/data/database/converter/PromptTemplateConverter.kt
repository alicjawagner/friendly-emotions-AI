package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate

/**
 * Stores only the enum constant name; `polishTemplate`/`englishTemplate` are derived from the
 * constant itself and are never persisted separately.
 */
class PromptTemplateConverter {
    @TypeConverter
    fun fromPromptTemplate(value: PromptTemplate): String = value.name

    @TypeConverter
    fun toPromptTemplate(value: String): PromptTemplate = PromptTemplate.valueOf(value)
}
