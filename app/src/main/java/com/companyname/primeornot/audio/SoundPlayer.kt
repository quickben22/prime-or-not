package com.companyname.primeornot.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.companyname.primeornot.R
import java.util.Collections

enum class Sound(val resId: Int) {
    Swipe(R.raw.swipe),
    Correct(R.raw.correct),
    Wrong(R.raw.wrong),
    Tick(R.raw.tick),
    LevelUp(R.raw.levelup),
}

/**
 * Thin SoundPool wrapper. Owned by the Activity and released in `onDestroy`.
 *
 * Playback is skipped while a sample is still loading, when media volume is
 * zero, or when the ringer is silent, so the game never makes noise the user
 * did not ask for.
 */
class SoundPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loadedIds: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf<Int>())
    private val ids: Map<Sound, Int>
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedIds.add(sampleId)
        }
        ids = Sound.entries.associateWith { sound ->
            pool.load(appContext, sound.resId, 1)
        }
    }

    fun play(sound: Sound, rate: Float = 1f) {
        if (released) return
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        val id = ids[sound] ?: return
        if (!loadedIds.contains(id)) return
        pool.play(id, VOLUME, VOLUME, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun playSwipe() = play(Sound.Swipe, 0.95f)

    /** The ding climbs in pitch with the streak, up to an octave-ish. */
    fun playCorrect(streak: Int) =
        play(Sound.Correct, 1f + 0.06f * streak.coerceIn(0, 12))

    fun playWrong() = play(Sound.Wrong)

    fun playTick() = play(Sound.Tick)

    fun playLevelUp() = play(Sound.LevelUp)

    fun release() {
        released = true
        pool.release()
    }

    private companion object {
        const val VOLUME = 0.85f
    }
}
