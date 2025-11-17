package com.andrewbibire.chessanalysis

enum class PlatformType {
    ANDROID,
    IOS,
    JVM_DESKTOP
}

expect fun getCurrentPlatform(): PlatformType
