package com.andrewbibire.chessanalysis.audio

import android.media.MediaPlayer
import android.util.Log
import com.andrewbibire.chessanalysis.AppContextHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android sound player using MediaPlayer
 * Loads MP3 files from androidMain/res/raw
 */
class AndroidSoundPlayer : SoundPlayer {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentPlayer: MediaPlayer? = null

    private fun getResourceId(fileName: String): Int {
        val context = AppContextHolder.context
            ?: throw IllegalStateException("Context not initialized")

        // Remove file extension and convert to resource name
        val resourceName = fileName.substringBeforeLast(".")
        return context.resources.getIdentifier(resourceName, "raw", context.packageName)
    }

    override fun playSound(fileName: String) {
        scope.launch {
            try {
                Log.d("SoundPlayer", "Playing: $fileName")

                val context = AppContextHolder.context
                    ?: throw IllegalStateException("Context not initialized")

                val resourceId = getResourceId(fileName)
                if (resourceId == 0) {
                    Log.e("SoundPlayer", "Resource not found: $fileName")
                    return@launch
                }

                // Release previous player
                currentPlayer?.release()

                // Create and play new sound from raw resources
                currentPlayer = MediaPlayer.create(context, resourceId)?.apply {
                    setOnCompletionListener {
                        it.release()
                    }
                    start()
                }

                Log.d("SoundPlayer", "Playing sound: $fileName")
            } catch (e: Exception) {
                Log.e("SoundPlayer", "Error playing sound", e)
            }
        }
    }

    override fun playDelayedSound(fileName: String, delayMillis: Long) {
        scope.launch {
            kotlinx.coroutines.delay(delayMillis)
            playSound(fileName)
        }
    }

    override fun playSequentialSounds(vararg fileNames: String) {
        scope.launch {
            for (fileName in fileNames) {
                try {
                    Log.d("SoundPlayer", "Playing sequential sound: $fileName")

                    val context = AppContextHolder.context
                        ?: throw IllegalStateException("Context not initialized")

                    val resourceId = getResourceId(fileName)
                    if (resourceId == 0) {
                        Log.e("SoundPlayer", "Resource not found: $fileName")
                        continue
                    }

                    // Release previous player
                    currentPlayer?.release()

                    // Create and play new sound, wait for completion
                    var isPlaying = true
                    currentPlayer = MediaPlayer.create(context, resourceId)?.apply {
                        setOnCompletionListener {
                            isPlaying = false
                            it.release()
                        }
                        start()
                    }

                    // Wait for sound to finish
                    while (isPlaying) {
                        kotlinx.coroutines.delay(10)
                    }

                    // Tiny delay between sounds for clean transition
                    kotlinx.coroutines.delay(20)

                    Log.d("SoundPlayer", "Sequential sound $fileName finished")
                } catch (e: Exception) {
                    Log.e("SoundPlayer", "Error in sequential playback", e)
                }
            }
        }
    }

    override fun release() {
        currentPlayer?.release()
        currentPlayer = null
    }
}

actual fun createSoundPlayer(): SoundPlayer = AndroidSoundPlayer()
