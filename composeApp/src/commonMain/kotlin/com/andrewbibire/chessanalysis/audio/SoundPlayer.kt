package com.andrewbibire.chessanalysis.audio

/**
 * Simple multiplatform sound player interface.
 * Platform-specific implementations handle the actual audio playback.
 */
interface SoundPlayer {
    fun playSound(fileName: String)
    fun playDelayedSound(fileName: String, delayMillis: Long)
    fun playSequentialSounds(vararg fileNames: String)
    fun release()
}

/**
 * Platform-specific sound player implementation
 */
expect fun createSoundPlayer(): SoundPlayer
