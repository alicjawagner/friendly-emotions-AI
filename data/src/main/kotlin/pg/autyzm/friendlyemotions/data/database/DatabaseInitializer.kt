package pg.autyzm.friendlyemotions.data.database

import pg.autyzm.friendlyemotions.data.dao.EmotionFolderDao
import pg.autyzm.friendlyemotions.data.dao.EmotionImageDao
import pg.autyzm.friendlyemotions.data.dao.ImageUsageDao
import pg.autyzm.friendlyemotions.data.dao.LearningStepDao
import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity
import pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity
import pg.autyzm.friendlyemotions.data.mapper.toEntity
import pg.autyzm.friendlyemotions.data.mapper.toImageUsageEntities
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the example content described in the phase-3 plan on first launch only, detected by
 * checking whether any [pg.autyzm.friendlyemotions.data.entity.LearningStepEntity] exists yet
 * (ADR-010, target-architecture.md §9.6). Called once from `FriendlyEmotionsApp.onCreate()` via an
 * app-scoped coroutine — never from a ViewModel/Activity.
 *
 * The seeded folder/image/step ids are deliberately deterministic human-readable strings rather than
 * `UUID.randomUUID()` (unlike the repository `create*`/`save*` methods) — this is fixed, reproducible
 * seed content, not therapist-created data, so stable ids make manual DB inspection and debugging
 * easier without changing any observable behavior.
 *
 * The 48 referenced image files (e.g. `file:///android_asset/example_images/happy_kobiety_1.png`)
 * are throwaway placeholders — simple solid-background PNGs with the emotion's Polish label
 * rendered on them, explicitly not final creative assets, swappable for real photography later
 * with no code change. See the module README for the exact list of expected filenames.
 */
@Singleton
class DatabaseInitializer
    @Inject
    constructor(
        private val emotionFolderDao: EmotionFolderDao,
        private val emotionImageDao: EmotionImageDao,
        private val learningStepDao: LearningStepDao,
        private val imageUsageDao: ImageUsageDao,
    ) {
        suspend fun seedIfNeeded() {
            if (learningStepDao.getAllNames().isNotEmpty()) return

            val seededImages = seedFoldersAndImages()
            seedExampleSteps(seededImages)
        }

        private suspend fun seedFoldersAndImages(): List<SeededImage> {
            val images = mutableListOf<EmotionImageEntity>()
            val seededImages = mutableListOf<SeededImage>()
            for (emotionId in EmotionId.entries) {
                for (spec in FOLDER_SPECS) {
                    val folderId = "${emotionId.name.lowercase()}_${spec.key}"
                    emotionFolderDao.insert(
                        EmotionFolderEntity(
                            id = folderId,
                            emotionId = emotionId.name,
                            name = spec.displayName,
                            genderPolicy = spec.genderPolicy.name,
                            isExample = true,
                        ),
                    )
                    spec.imageGenders.forEachIndexed { index, gender ->
                        val baseName = "${emotionId.name.lowercase()}_${spec.key}_${index + 1}"
                        images +=
                            EmotionImageEntity(
                                id = baseName,
                                folderId = folderId,
                                filePath = "file:///android_asset/$EXAMPLE_IMAGES_ASSET_DIR/$baseName.png",
                                gender = gender.name,
                                isExample = true,
                            )
                        seededImages += SeededImage(id = ImageId(baseName), emotionId = emotionId, folderKey = spec.key)
                    }
                }
            }
            emotionImageDao.insertAll(images)
            return seededImages
        }

        /**
         * "Zaawansowany" selects every seeded image, for both modes, so it is immediately playable
         * with the full catalog. "Podstawowy" is intentionally restricted to a beginner-friendly
         * subset: only the HAPPY/SAD/ANGRY emotions, and only their emotikony/inne folders.
         */
        private suspend fun seedExampleSteps(seededImages: List<SeededImage>) {
            val allMaterialSelection =
                MaterialSelection(seededImages.map { ImageUsage(it.id, inLearning = true, inTest = true) })

            val podstawowyEmotions = setOf(EmotionId.HAPPY, EmotionId.SAD, EmotionId.ANGRY)
            val podstawowyFolderKeys = setOf("emotikony", "inne")
            val podstawowyMaterialSelection =
                MaterialSelection(
                    seededImages
                        .filter { it.emotionId in podstawowyEmotions && it.folderKey in podstawowyFolderKeys }
                        .map { ImageUsage(it.id, inLearning = true, inTest = true) },
                )

            val podstawowyLearning =
                LearningParameters(
                    displayedImageCount = 2,
                    repetitionsPerEmotion = 2,
                    promptTemplate = PromptTemplate.WHERE_IS,
                    captionsEnabled = true,
                    hintDelaySeconds = 6,
                    activeHintTypes = HintType.entries.toSet(),
                    mixedGenderInAnswers = true,
                )
            val podstawowyTest =
                TestParameters(
                    overridesLearning = false,
                    displayedImageCount = podstawowyLearning.displayedImageCount,
                    repetitionsPerEmotion = podstawowyLearning.repetitionsPerEmotion,
                    promptTemplate = podstawowyLearning.promptTemplate,
                    ttsEnabled = podstawowyLearning.ttsEnabled,
                    captionsEnabled = podstawowyLearning.captionsEnabled,
                    mixedGenderInAnswers = podstawowyLearning.mixedGenderInAnswers,
                )
            seedStep(
                id = LearningStepId(PODSTAWOWY_ID),
                isActive = true,
                mode = SessionMode.LEARNING,
                draft =
                    LearningStepDraft(
                        name = "Podstawowy (krok przykładowy)",
                        materialSelection = podstawowyMaterialSelection,
                        learningParameters = podstawowyLearning,
                        testParameters = podstawowyTest,
                        reinforcementSettings =
                            ReinforcementSettings(
                                enabledAnimationThemes = setOf("balloons", "cars", "flowers"),
                            ),
                    ),
            )

            val zaawansowanyLearning =
                LearningParameters(
                    displayedImageCount = 4,
                    repetitionsPerEmotion = 3,
                    promptTemplate = PromptTemplate.TOUCH,
                    captionsEnabled = false,
                    mixedGenderInAnswers = false,
                    activeHintTypes = setOf(HintType.ANIMATE_CORRECT),
                )
            val zaawansowanyTest =
                TestParameters(
                    overridesLearning = true,
                    displayedImageCount = zaawansowanyLearning.displayedImageCount,
                    repetitionsPerEmotion = 1,
                    promptTemplate = PromptTemplate.POINT_TO,
                    ttsEnabled = true,
                    captionsEnabled = false,
                    mixedGenderInAnswers = false,
                )
            seedStep(
                id = LearningStepId(ZAAWANSOWANY_ID),
                isActive = false,
                mode = SessionMode.LEARNING,
                draft =
                    LearningStepDraft(
                        name = "Zaawansowany (krok przykładowy)",
                        materialSelection = allMaterialSelection,
                        learningParameters = zaawansowanyLearning,
                        testParameters = zaawansowanyTest,
                        reinforcementSettings = ReinforcementSettings(),
                    ),
            )
        }

        private suspend fun seedStep(
            id: LearningStepId,
            isActive: Boolean,
            mode: SessionMode,
            draft: LearningStepDraft,
        ) {
            learningStepDao.insert(
                draft.toEntity(id = id, isActive = isActive, mode = mode, isExample = true),
            )
            imageUsageDao.insertAll(draft.toImageUsageEntities(id))
        }

        private data class SeededImage(val id: ImageId, val emotionId: EmotionId, val folderKey: String)

        private data class FolderSpec(
            val displayName: String,
            val key: String,
            val genderPolicy: FolderGenderPolicy,
            /** One [GrammaticalGender] per seeded image in this folder (2 images per folder). */
            val imageGenders: List<GrammaticalGender>,
        )

        companion object {
            const val EXAMPLE_IMAGES_ASSET_DIR = "example_images"
            private const val PODSTAWOWY_ID = "example-step-podstawowy"
            private const val ZAAWANSOWANY_ID = "example-step-zaawansowany"

            /**
             * The 4 example folders seeded per emotion (functional spec §6.4). `Inne` (MIXED policy)
             * seeds one MASCULINE and one FEMININE image to demonstrate mixed-gender material within a
             * single folder; the other 3 folders' images all match their folder's fixed gender policy.
             */
            private val FOLDER_SPECS =
                listOf(
                    FolderSpec(
                        displayName = "Kobiety",
                        key = "kobiety",
                        genderPolicy = FolderGenderPolicy.FEMININE,
                        imageGenders = listOf(GrammaticalGender.FEMININE, GrammaticalGender.FEMININE),
                    ),
                    FolderSpec(
                        displayName = "Mężczyźni",
                        key = "mezczyzni",
                        genderPolicy = FolderGenderPolicy.MASCULINE,
                        imageGenders = listOf(GrammaticalGender.MASCULINE, GrammaticalGender.MASCULINE),
                    ),
                    FolderSpec(
                        displayName = "Emotikony",
                        key = "emotikony",
                        genderPolicy = FolderGenderPolicy.NEUTER,
                        imageGenders = listOf(GrammaticalGender.NEUTER, GrammaticalGender.NEUTER),
                    ),
                    FolderSpec(
                        displayName = "Inne",
                        key = "inne",
                        genderPolicy = FolderGenderPolicy.MIXED,
                        imageGenders = listOf(GrammaticalGender.MASCULINE, GrammaticalGender.FEMININE),
                    ),
                )
        }
    }
