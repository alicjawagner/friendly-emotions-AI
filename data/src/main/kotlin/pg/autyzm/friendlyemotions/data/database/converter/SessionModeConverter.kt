package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/** Binds/reads [SessionMode] query parameters (e.g. `ImageUsageDao`'s mode-filtered queries). */
class SessionModeConverter {
    @TypeConverter
    fun fromSessionMode(value: SessionMode?): String? = value?.name

    @TypeConverter
    fun toSessionMode(value: String?): SessionMode? = value?.let(SessionMode::valueOf)
}
