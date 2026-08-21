package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveFoldersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveImagesForFolderUseCase

/**
 * The full folder/image catalog across every emotion — shared by [WizardMaterialViewModel] (which
 * needs it to render the Material tab's gallery) and `WizardSummaryViewModel` (which needs it to
 * resolve which emotions a draft's bare [pg.autyzm.friendlyemotions.domain.model.emotion.ImageId]s
 * belong to, independent of whether the Material tab has been visited this wizard session —
 * `WizardContainerState.materialBrowsing.addedEmotionIds` is transient Material-tab UI state, not
 * safe for Summary to depend on since [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar]
 * allows jumping to Summary before Material's own catalog load finishes).
 */
data class MaterialCatalogSnapshot(
    val foldersByEmotion: Map<EmotionId, List<EmotionFolder>> = emptyMap(),
    val imagesByFolder: Map<FolderId, List<EmotionImage>> = emptyMap(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
fun observeMaterialCatalog(
    observeFoldersUseCase: ObserveFoldersUseCase,
    observeImagesForFolderUseCase: ObserveImagesForFolderUseCase,
): Flow<MaterialCatalogSnapshot> {
    val emotionFolders: Flow<Map<EmotionId, List<EmotionFolder>>> =
        combine(
            EmotionCatalog.all.map { emotion -> observeFoldersUseCase(emotion.id).map { emotion.id to it } },
        ) { pairs -> pairs.toMap() }

    val folderImages: Flow<Map<FolderId, List<EmotionImage>>> =
        emotionFolders.flatMapLatest { byEmotion ->
            val folders = byEmotion.values.flatten()
            if (folders.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    folders.map { folder -> observeImagesForFolderUseCase(folder.id).map { folder.id to it } },
                ) { pairs -> pairs.toMap() }
            }
        }

    return combine(emotionFolders, folderImages) { folders, images ->
        MaterialCatalogSnapshot(foldersByEmotion = folders, imagesByFolder = images, isLoading = false)
    }
}

/**
 * Which emotions have at least one image in [imageUsages] satisfying [selector] (e.g. `inLearning`
 * or `inTest`), resolved via this catalog's folder→emotion / image→folder mapping.
 */
fun MaterialCatalogSnapshot.touchedEmotionIds(
    imageUsages: List<ImageUsage>,
    selector: (ImageUsage) -> Boolean = { true },
): Set<EmotionId> {
    val touchedImageIds = imageUsages.filter(selector).map { it.imageId }.toSet()
    if (touchedImageIds.isEmpty()) return emptySet()
    return foldersByEmotion
        .filterValues { folders ->
            folders.any { folder -> imagesByFolder[folder.id].orEmpty().any { it.id in touchedImageIds } }
        }.keys
}
