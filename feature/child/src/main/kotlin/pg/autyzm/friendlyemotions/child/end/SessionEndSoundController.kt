package pg.autyzm.friendlyemotions.child.end

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import pg.autyzm.friendlyemotions.child.R

/**
 * Owns end-of-session fanfare playback (target-domain.md §8.7, `ReinforcementSettings.endSessionFanfareEnabled`).
 * `@ViewModelScoped`, injected into `SessionEndViewModel` — mirrors [pg.autyzm.friendlyemotions.child.game.TtsController]'s
 * shape (plain constructor class, provided via `ChildModule`, cleaned up from `onCleared()` since
 * `@ViewModelScoped` triggers no automatic cleanup).
 *
 * Uses plain `android.media.MediaPlayer` rather than a new Gradle dependency — a one-shot short
 * sound effect at session end has no low-latency requirement that would call for `SoundPool`/media3.
 */
class SessionEndSoundController(private val context: Context) : DefaultLifecycleObserver {
    private var mediaPlayer: MediaPlayer? = null

    /** Plays `res/raw/fanfare.wav` once; silently no-ops if the resource can't be decoded. */
    fun playFanfare() {
        release()
        mediaPlayer =
            MediaPlayer.create(context, R.raw.fanfare)?.apply {
                setOnCompletionListener { release() }
                start()
            }
    }

    override fun onDestroy(owner: LifecycleOwner) = release()

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
