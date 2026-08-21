package pg.autyzm.friendlyemotions.domain.error

import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId

sealed class DomainError {
    data class FolderNotFound(val folderId: FolderId) : DomainError()

    data class DuplicateStepName(val name: String) : DomainError()

    object StepNameBlank : DomainError()

    object NoMaterialSelected : DomainError()

    object NoExampleStepAvailable : DomainError()

    data class GenderNotAssigned(val imageIds: List<ImageId>) : DomainError()

    object ExampleContentNotDeletable : DomainError()

    object ExampleContentNotEditable : DomainError()

    object InsufficientMaterialForSession : DomainError()

    data class FileOperationFailed(val cause: Throwable) : DomainError()
}
