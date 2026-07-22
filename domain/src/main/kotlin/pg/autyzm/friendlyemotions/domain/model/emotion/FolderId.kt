package pg.autyzm.friendlyemotions.domain.model.emotion

@JvmInline
value class FolderId(val value: String) : Comparable<FolderId> {
    override fun compareTo(other: FolderId): Int = value.compareTo(other.value)
}
