package com.andrewbibire.chessanalysis.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

/**
 * Desktop/JVM sound player using javax.sound.sampled
 * Loads WAV files from jvmMain/resources
 */
class JvmSoundPlayer : SoundPlayer {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun playSound(fileName: String) {
        scope.launch {
            try {

                // Load from platform-specific resources
                val resourcePath = "/files/$fileName"
                val inputStream = this::class.java.getResourceAsStream(resourcePath)
                    ?: throw IllegalArgumentException("Resource not found: $resourcePath")

                val bytes = inputStream.readBytes()

                // Create audio input stream from bytes
                val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(
                    BufferedInputStream(bytes.inputStream())
                )


                // Create clip and play
                val clip = AudioSystem.getClip()
                clip.open(audioInputStream)

                // Auto-close clip when done playing
                clip.addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP) {
                        clip.close()
                    }
                }

                clip.start()

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

    /**
     * Play a sound and wait for it to complete before playing the next sound
     */
    override fun playSequentialSounds(vararg fileNames: String) {
        scope.launch {
            for (fileName in fileNames) {
                try {

                    // Load from platform-specific resources
                    val resourcePath = "/files/$fileName"
                    val inputStream = this::class.java.getResourceAsStream(resourcePath)
                        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

                    val bytes = inputStream.readBytes()
                    val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(
                        BufferedInputStream(bytes.inputStream())
                    )

                    // Create clip and play
                    val clip = AudioSystem.getClip()
                    clip.open(audioInputStream)

                    // Wait for clip to finish
                    var isPlaying = true
                    clip.addLineListener { event ->
                        if (event.type == LineEvent.Type.STOP) {
                            isPlaying = false
                            clip.close()
                        }
                    }

                    clip.start()

                    // Wait until clip finishes playing
                    while (isPlaying && clip.isOpen) {
                        kotlinx.coroutines.delay(10)
                    }

                    // Tiny delay between sounds for clean transition
                    kotlinx.coroutines.delay(20)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun release() {
        // No cleanup needed - clips auto-close when done
    }
}

actual fun createSoundPlayer(): SoundPlayer = JvmSoundPlayer()
