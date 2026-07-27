package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender

class GrammaticalGenderConverter {
    @TypeConverter
    fun fromGrammaticalGender(value: GrammaticalGender): String = value.name

    @TypeConverter
    fun toGrammaticalGender(value: String): GrammaticalGender = GrammaticalGender.valueOf(value)
}
