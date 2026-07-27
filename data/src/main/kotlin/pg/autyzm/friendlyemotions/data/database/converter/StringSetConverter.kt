package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter

/**
 * Comma-joins a `Set<String>`; used for `ReinforcementSettingsEmbedded.enabledPraiseWords`. Safe
 * because praise words are single Polish words that never contain commas.
 */
class StringSetConverter {
    @TypeConverter
    fun fromStringSet(value: Set<String>): String = value.joinToString(separator = ",")

    @TypeConverter
    fun toStringSet(value: String): Set<String> =
        if (value.isEmpty()) {
            emptySet()
        } else {
            value.split(",").toSet()
        }
}
