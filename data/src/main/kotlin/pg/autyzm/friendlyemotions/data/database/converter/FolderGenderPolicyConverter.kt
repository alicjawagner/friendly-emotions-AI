package pg.autyzm.friendlyemotions.data.database.converter

import androidx.room.TypeConverter
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy

class FolderGenderPolicyConverter {
    @TypeConverter
    fun fromFolderGenderPolicy(value: FolderGenderPolicy): String = value.name

    @TypeConverter
    fun toFolderGenderPolicy(value: String): FolderGenderPolicy = FolderGenderPolicy.valueOf(value)
}
