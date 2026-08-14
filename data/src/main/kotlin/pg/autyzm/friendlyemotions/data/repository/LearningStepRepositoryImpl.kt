package pg.autyzm.friendlyemotions.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pg.autyzm.friendlyemotions.data.dao.EmotionImageDao
import pg.autyzm.friendlyemotions.data.dao.ImageUsageDao
import pg.autyzm.friendlyemotions.data.dao.LearningStepDao
import pg.autyzm.friendlyemotions.data.entity.LearningStepEntity
import pg.autyzm.friendlyemotions.data.mapper.toDomain
import pg.autyzm.friendlyemotions.data.mapper.toEntity
import pg.autyzm.friendlyemotions.data.mapper.toImageUsageEntities
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Implements [LearningStepRepository] over [LearningStepDao] (aggregate root row) joined with
 * [ImageUsageDao] (the one-to-many `MaterialSelection` table) and [EmotionImageDao] (for the
 * eligibility join in [getImagesEligibleForStep]).
 *
 * This class does **not** inject `LearningStepActivationService` from `:domain`. Per phase-3 plan
 * risk #4, the deletion-fallback and activation rules are instead mirrored directly in
 * [LearningStepDao]'s `@Transaction` SQL (`activateStep`/`deleteStepWithFallback`, written in
 * session 3.2), since Room transactions cannot call back into arbitrary Kotlin domain logic
 * mid-transaction. `LearningStepDao.deleteStepWithFallback`'s KDoc and
 * `LearningStepActivationServiceTest` pin the exact contract both sides must keep in sync.
 */
class LearningStepRepositoryImpl
    @Inject
    constructor(
        private val learningStepDao: LearningStepDao,
        private val imageUsageDao: ImageUsageDao,
        private val emotionImageDao: EmotionImageDao,
    ) : LearningStepRepository {
        override fun observeAllSteps(): Flow<List<LearningStep>> =
            learningStepDao.observeAll().map { entities -> entities.map { it.toDomainWithUsages() } }

        override fun observeActiveStep(): Flow<LearningStep?> =
            learningStepDao.observeActive().map { entity -> entity?.toDomainWithUsages() }

        override suspend fun getStepById(stepId: LearningStepId): LearningStep =
            requireEntity(stepId).toDomainWithUsages()

        override suspend fun getImagesEligibleForStep(
            stepId: LearningStepId,
            mode: SessionMode,
        ): List<EmotionImage> {
            val eligibleImageIds = imageUsageDao.getEligibleImageIdsForStepAndMode(stepId.value, mode)
            return emotionImageDao.getByIds(eligibleImageIds).map { it.toDomain() }
        }

        override suspend fun saveStep(draft: LearningStepDraft): LearningStepId {
            val stepId = LearningStepId(UUID.randomUUID().toString())
            learningStepDao.insert(
                draft.toEntity(id = stepId, isActive = false, mode = SessionMode.LEARNING, isExample = false),
            )
            imageUsageDao.insertAll(draft.toImageUsageEntities(stepId))
            return stepId
        }

        /** Preserves the existing row's `isActive`/`mode`/`isExample` — the draft never carries them (ADR-013). */
        override suspend fun updateStep(
            stepId: LearningStepId,
            draft: LearningStepDraft,
        ) {
            val existing = requireEntity(stepId)
            val entity =
                draft.toEntity(
                    id = stepId,
                    isActive = existing.isActive,
                    mode = SessionMode.valueOf(existing.mode),
                    isExample = existing.isExample,
                )
            learningStepDao.update(entity)
            imageUsageDao.replaceForStep(stepId.value, draft.toImageUsageEntities(stepId))
        }

        override suspend fun deleteStep(stepId: LearningStepId) {
            learningStepDao.deleteStepWithFallback(stepId.value)
        }

        override suspend fun activateStep(stepId: LearningStepId) {
            learningStepDao.activateStep(stepId.value)
        }

        override suspend fun setMode(
            stepId: LearningStepId,
            mode: SessionMode,
        ) {
            learningStepDao.updateMode(stepId.value, mode.name)
        }

        /**
         * Duplicates [stepId]'s parameters and material selection into a new, inactive, non-example
         * step (inheriting the source's [LearningStepEntity.mode]), generating a `"{name} (n)"` name
         * (smallest available positive integer).
         *
         * Name generation living inside a repository is an intentional, documented exception to
         * ADR-007's "no business logic in repositories" rule: target-architecture.md §11.2 explicitly
         * assigns it here because [LearningStepRepository.copyStep] takes no name parameter, so nothing
         * upstream in `:domain` could supply one.
         */
        override suspend fun copyStep(stepId: LearningStepId): LearningStepId {
            val source = requireEntity(stepId)
            val sourceUsages = imageUsageDao.getForStep(stepId.value)
            val newId = LearningStepId(UUID.randomUUID().toString())
            val newName = generateCopyName(source.name)

            learningStepDao.insert(
                source.copy(id = newId.value, name = newName, isActive = false, isExample = false),
            )
            imageUsageDao.insertAll(sourceUsages.map { it.copy(stepId = newId.value) })
            return newId
        }

        override suspend fun getAllStepNames(): List<String> = learningStepDao.getAllNames()

        private suspend fun requireEntity(stepId: LearningStepId): LearningStepEntity =
            requireNotNull(learningStepDao.getById(stepId.value)) { "No learning step with id $stepId" }

        private suspend fun LearningStepEntity.toDomainWithUsages(): LearningStep =
            toDomain(imageUsageDao.getForStep(id))

        /** Smallest positive integer `n` for which `"$originalName ($n)"` is not already taken. */
        private suspend fun generateCopyName(originalName: String): String {
            val existingNames = learningStepDao.getAllNames().toSet()
            var n = 1
            while ("$originalName ($n)" in existingNames) n++
            return "$originalName ($n)"
        }
    }
