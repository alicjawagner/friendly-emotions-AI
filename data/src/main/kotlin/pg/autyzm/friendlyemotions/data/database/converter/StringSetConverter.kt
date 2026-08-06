package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter

/**
 * Comma-joins a `Set<String>`; used for praise-word and animation-theme sets on
 * `ReinforcementSettingsEmbedded`. Safe because those values never contain commas.
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
