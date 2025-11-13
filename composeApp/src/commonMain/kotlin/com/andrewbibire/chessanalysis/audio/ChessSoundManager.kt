package com.andrewbibire.chessanalysis.audio

/**
 * Manages chess move sounds, determining which sound to play based on move type.
 * Mimics chess.com's sound system.
 * Uses platform-specific audio formats (WAV for desktop, MP3 for mobile).
 */
class ChessSoundManager(
    private val soundPlayer: SoundPlayer = createSoundPlayer()
) {
    private val ext = PlatformAudioConfig.audioExtension

    /**
     * Play sound for a chess move based on SAN notation parsing
     * @param isCapture Whether the move was a capture (from SAN)
     * @param isCheck Whether the move resulted in check (from SAN)
     * @param isCheckmate Whether the move resulted in checkmate (from SAN)
     * @param isPromotion Whether the move was a pawn promotion (from SAN)
     * @param isCastling Whether the move was castling (from SAN)
     */
    fun playMoveSound(
        isCapture: Boolean = false,
        isCheck: Boolean = false,
        isCheckmate: Boolean = false,
        isPromotion: Boolean = false,
        isCastling: Boolean = false
    ) {
        println("ChessSoundManager: isCapture=$isCapture, isCheck=$isCheck, isCheckmate=$isCheckmate, isPromotion=$isPromotion, isCastling=$isCastling")

        // Priority order:
        // 1. Checkmate: check sound + game end sound
        // 2. Check: check sound only
        // 3. Promotion: promote sound only
        // 4. Castling: castle sound only
        // 5. Capture: capture sound only
        // 6. Regular move: move sound

        if (isCheckmate) {
            // For checkmate: play check sound, wait for it to finish, then play game end
            println("ChessSoundManager: Playing checkmate sounds: check.$ext -> gameend.$ext")
            playCheckmateSound()
        } else {
            val soundFile = when {
                isCheck -> "check.$ext"
                isPromotion -> "promote.$ext"
                isCastling -> "castle.$ext"
                isCapture -> "capture.$ext"
                else -> "move.$ext"
            }

            println("ChessSoundManager: Playing sound: $soundFile")
            soundPlayer.playSound(soundFile)
        }
    }

    /**
     * Play checkmate sound sequence (check followed by game end)
     */
    private fun playCheckmateSound() {
        // Use sequential playback to ensure check plays before gameend
        soundPlayer.playSequentialSounds("check.$ext", "gameend.$ext")
    }

    /**
     * Play game end sound
     */
    fun playGameEndSound() {
        soundPlayer.playSound("gameend.$ext")
    }

    /**
     * Play game end sound with a delay
     */
    fun playDelayedGameEndSound(delayMillis: Long) {
        soundPlayer.playDelayedSound("gameend.$ext", delayMillis)
    }

    /**
     * Release resources
     */
    fun release() {
        soundPlayer.release()
    }
}
