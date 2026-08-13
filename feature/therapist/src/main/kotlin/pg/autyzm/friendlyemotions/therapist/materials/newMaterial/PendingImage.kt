package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender

/**
 * An image added on `MaterialsNewMaterialScreen` but not yet persisted (target-domain.md §9.4's
 * `AWAITING_GENDER` state). [localId] is a client-generated id — nothing has an
 * [pg.autyzm.friendlyemotions.domain.model.emotion.ImageId]
 * until [pg.autyzm.friendlyemotions.domain.usecase.material.AssignImagesUseCase] persists it.
 * [gender] is `null` only while awaiting assignment in a MIXED folder; fixed-gender folders stamp
 * it immediately when the image is added.
 */
data class PendingImage(
    val localId: String,
    val filePath: String,
    val gender: GrammaticalGender?,
)
