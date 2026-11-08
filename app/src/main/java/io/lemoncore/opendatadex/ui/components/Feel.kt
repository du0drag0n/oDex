package io.lemoncore.opendatadex.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Settings

/**
 * The app's two short sounds, loaded once. They play as UI sonification, so the phone's silent mode and volume apply.
 * Both are oDex's own (made for it), not game audio.
 */
class FeelSounds(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(2)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val snap = pool.load(context, R.raw.snap, 1)
    private val jingle = pool.load(context, R.raw.jingle, 1)

    fun snap() = pool.play(snap, 0.5f, 0.5f, 1, 0, 1f)
    fun jingle() = pool.play(jingle, 0.6f, 0.6f, 1, 0, 1f)
}

/**
 * Haptics and sounds for the moments that matter (marking caught, finishing a box, a shiny hunt), each checked against
 * the trainer's Haptics and Sounds settings.
 */
class Feel(private val view: View?, private val sounds: FeelSounds?, private val haptics: Boolean, private val soundsOn: Boolean) {
    /** A swipe has passed the point where letting go marks it. */
    fun threshold() = buzz(if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK)

    fun caught() {
        confirm()
        if (soundsOn) sounds?.snap()
    }

    fun unmarked() {
        if (Build.VERSION.SDK_INT >= 30) buzz(HapticFeedbackConstants.REJECT)
        else {
            buzz(HapticFeedbackConstants.VIRTUAL_KEY)
            view?.postDelayed({ buzz(HapticFeedbackConstants.VIRTUAL_KEY) }, 60)
        }
    }

    /** A box, generation or game has reached 100%. */
    fun complete() {
        confirm()
        view?.postDelayed({ confirm() }, 500)
        if (soundsOn) sounds?.jingle()
    }

    fun huntStep() = buzz(HapticFeedbackConstants.KEYBOARD_TAP)

    fun shinyFound() {
        confirm()
        if (soundsOn) sounds?.jingle()
    }

    private fun confirm() = buzz(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)

    private fun buzz(type: Int) {
        if (haptics) view?.performHapticFeedback(type)
    }

    companion object {
        val None = Feel(null, null, haptics = false, soundsOn = false)
    }
}

val LocalFeel = staticCompositionLocalOf { Feel.None }

/** The trainer's settings, for components deep in a screen (Swipe to mark). */
val LocalSettings = staticCompositionLocalOf { Settings() }
