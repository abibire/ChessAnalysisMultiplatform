package com.andrewbibire.chessanalysis.audio

/**
 * Platform-specific audio configuration
 */
expect object PlatformAudioConfig {
    /**
     * Returns the appropriate file extension for audio files on this platform
     * (e.g., "wav" for JVM, "mp3" for Android/iOS)
     */
    val audioExtension: String
}
