package pg.autyzm.friendlyemotions.domain.model.session

@JvmInline
value class LearningStepId(val value: String) : Comparable<LearningStepId> {
    override fun compareTo(other: LearningStepId): Int = value.compareTo(other.value)
}
