package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter
import pg.autyzm.friendlyemotions.domain.model.session.HintType

/** Comma-joins [HintType] enum names; used for `LearningParametersEmbedded.activeHintTypes`. */
class HintTypeSetConverter {
    @TypeConverter
    fun fromHintTypeSet(value: Set<HintType>): String = value.joinToString(separator = ",") { it.name }

    @TypeConverter
    fun toHintTypeSet(value: String): Set<HintType> =
        if (value.isEmpty()) {
            emptySet()
        } else {
            value.split(",").map(HintType::valueOf).toSet()
        }
}
