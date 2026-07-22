package pg.autyzm.friendlyemotions.domain.error

/**
 * The fallible-operation return type used by every use case (ADR-004, ADR-011,
 * target-architecture.md §11.4, §15.2) — distinct from `kotlin.Result`, which carries a
 * [Throwable] rather than a typed [DomainError].
 */
sealed class Result<out T, out E> {
    data class Success<out T>(val value: T) : Result<T, Nothing>()

    data class Failure<out E>(val error: E) : Result<Nothing, E>()
}
