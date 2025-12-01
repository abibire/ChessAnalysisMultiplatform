package com.andrewbibire.chessanalysis.audio

import chessanalysis.composeapp.generated.resources.Res
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

/**
 * iOS sound player using AVAudioPlayer
 * Loads MP3 files from iosMain/composeResources
 */
class IOSSoundPlayer : SoundPlayer {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentPlayer: AVAudioPlayer? = null

    @OptIn(ExperimentalForeignApi::class)
    override fun playSound(fileName: String) {
        scope.launch {
            try {
                // Load from iOS-specific compose resources
                val uri = Res.getUri("files/$fileName")
                val url = NSURL.URLWithString(uri) ?: run {
                    return@launch
                }

                // Create and play audio player
                currentPlayer?.stop()
                currentPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
                currentPlayer?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun playDelayedSound(fileName: String, delayMillis: Long) {
        scope.launch {
            kotlinx.coroutines.delay(delayMillis)
            playSound(fileName)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun playSequentialSounds(vararg fileNames: String) {
        scope.launch {
            for (fileName in fileNames) {
                try {
                    // Load from iOS-specific compose resources
                    val uri = Res.getUri("files/$fileName")
                    val url = NSURL.URLWithString(uri) ?: run {
                        continue
                    }

                    // Create and play audio player
                    currentPlayer?.stop()
                    currentPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
                    val player = currentPlayer ?: continue
                    player.play()

                    // Wait for sound duration plus a tiny buffer for clean transition
                    val durationMs = (player.duration * 1000).toLong()
                    kotlinx.coroutines.delay(durationMs + 20)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun release() {
        currentPlayer?.stop()
        currentPlayer = null
    }
}

actual fun createSoundPlayer(): SoundPlayer = IOSSoundPlayer()
