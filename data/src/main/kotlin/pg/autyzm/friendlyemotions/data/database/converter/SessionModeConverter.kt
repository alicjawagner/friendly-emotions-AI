package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/** Nullable since `LearningStepEntity.activeMode` is null whenever the step is not the active one. */
class SessionModeConverter {
    @TypeConverter
    fun fromSessionMode(value: SessionMode?): String? = value?.name

    @TypeConverter
    fun toSessionMode(value: String?): SessionMode? = value?.let(SessionMode::valueOf)
}
