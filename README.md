# Chess Analysis ♟️

A compose multiplatform chess game analyzer with Stockfish engine integration and move classification.
Available on iOS, Android, Windows, MacOS, and Linux

**Platforms:** Android • iOS • Desktop (Windows, macOS, Linux)

![Screenshot placeholder - Add app screenshots here]

> Analyze your games with professional-grade analysis powered by Stockfish, available on all your devices.

## 📂 Project

This is a Kotlin Multiplatform project built with Compose Multiplatform.

### Project Structure

- **`/composeApp`** - Shared code for all platforms
  - `commonMain` - Cross-platform business logic and UI
  - `androidMain` - Android-specific implementations
  - `iosMain` - iOS-specific implementations
  - `jvmMain` - Desktop-specific implementations

- **`/iosApp`** - iOS application entry point with SwiftUI integration

## ✨ Features

- **Stockfish Analysis** - Deep position analysis with the world's strongest chess engine
- **Move Classification** - Automatic move quality ratings (Best, Excellent, Good, Inaccuracy, Mistake, Blunder)
- **Alternative Lines** - Explore multiple best move variations
- **Opening Database** - Automatic opening detection with ECO codes
- **Online Integration** - Import games from Chess.com and Lichess
- **Game Statistics** - Accuracy calculation and detailed move breakdowns
- **Cross-Platform** - Native experience on mobile and desktop

## 🚀 Getting Started

### Prerequisites

- JDK 21 or higher
- Android Studio (for Android/Desktop)
- Xcode (for iOS, macOS only)

### Build and Run

**Android**
```bash
./gradlew :composeApp:installDebug
```

**Desktop**
```bash
./gradlew :composeApp:run
```

**iOS**
```bash
# Open iosApp/ in Xcode and run
```

## 🎮 Usage

1. **Import a game** - Paste PGN or search for a player on Chess.com/Lichess
2. **Analyze** - Click "Analyze Game" and wait for Stockfish analysis
3. **Navigate** - Use arrow keys (desktop) or swipe (mobile) to review moves
4. **Explore alternatives** - Click "Compute Alternatives" to see engine variations

## 🛠️ Tech Stack

- **Kotlin Multiplatform** - Shared code across all platforms
- **Compose Multiplatform** - Unified UI framework
- **Stockfish** - Chess engine for analysis
- **KChessLib** - Chess logic and validation
- **Ktor** - Network client for online game fetching

## 📄 License

This project is licensed under the GPL 3 License
