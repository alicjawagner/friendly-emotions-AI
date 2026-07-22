package pg.autyzm.friendlyemotions.domain.model.emotion

@JvmInline
value class ImageId(val value: String) : Comparable<ImageId> {
    override fun compareTo(other: ImageId): Int = value.compareTo(other.value)
}
