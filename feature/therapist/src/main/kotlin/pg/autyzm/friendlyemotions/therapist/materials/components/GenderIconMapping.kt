package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.therapist.R

/**
 * Maps folder/image gender values onto the badge drawables the user supplied
 * (`face_man`/`face_woman`/`circle`/`empty_set`) and the Figma legend's 4 labels
 * (`friendly-emotions-functional-specification.md` §6.3-6.4). [GrammaticalGender] has no MIXED
 * case — only a folder's policy can be "no assigned gender" — so [empty_set] never applies to
 * a single image.
 */
@DrawableRes
fun FolderGenderPolicy.badgeIconRes(): Int =
    when (this) {
        FolderGenderPolicy.MASCULINE -> R.drawable.face_man
        FolderGenderPolicy.FEMININE -> R.drawable.face_woman
        FolderGenderPolicy.NEUTER -> R.drawable.circle
        FolderGenderPolicy.MIXED -> R.drawable.empty_set
    }

@StringRes
fun FolderGenderPolicy.descriptionRes(): Int =
    when (this) {
        FolderGenderPolicy.MASCULINE -> R.string.therapist_materials_gender_masculine
        FolderGenderPolicy.FEMININE -> R.string.therapist_materials_gender_feminine
        FolderGenderPolicy.NEUTER -> R.string.therapist_materials_gender_neuter
        FolderGenderPolicy.MIXED -> R.string.therapist_materials_gender_mixed
    }

@DrawableRes
fun GrammaticalGender.badgeIconRes(): Int =
    when (this) {
        GrammaticalGender.MASCULINE -> R.drawable.face_man
        GrammaticalGender.FEMININE -> R.drawable.face_woman
        GrammaticalGender.NEUTER -> R.drawable.circle
    }

/**
 * A not-yet-assigned image gender (Phase 11 MIXED-folder upload flow) has no [GrammaticalGender]
 * value yet — displayed with the same `empty_set` icon a MIXED folder itself uses.
 */
@DrawableRes
fun GrammaticalGender?.badgeIconResOrEmpty(): Int = this?.badgeIconRes() ?: R.drawable.empty_set

/**
 * Cycles a per-image gender assignment feminine -> masculine -> neuter -> feminine -> ...,
 * starting from feminine the first time it's assigned (i.e. when [this] is `null`).
 */
fun GrammaticalGender?.cycled(): GrammaticalGender =
    when (this) {
        null -> GrammaticalGender.FEMININE
        GrammaticalGender.FEMININE -> GrammaticalGender.MASCULINE
        GrammaticalGender.MASCULINE -> GrammaticalGender.NEUTER
        GrammaticalGender.NEUTER -> GrammaticalGender.FEMININE
    }
