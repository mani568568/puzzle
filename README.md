# Mosaic Blocks - Offline Block Puzzle Game

A polished, relaxing block puzzle game built with Kotlin and Jetpack Compose.

## Features

- **Offline Play**: Works without internet connection
- **Clean Architecture**: Separated game logic from UI code
- **Modern UI**: Jetpack Compose with Material Design 3
- **Persistence**: Settings and game state saved with DataStore
- **Responsive**: Adapts to different phone sizes

## Architecture

```
MosaicBlocks/
├── domain/              # Game logic & rules
│   ├── model/          # Piece, Cell, Board, GameState
│   └── GameEngine.kt   # Core game mechanics
├── data/               # Data persistence
│   └── GameDatastore.kt
└── ui/                 # UI components
    ├── theme/          # Colors and themes
    ├── components/     # Reusable UI elements
    └── screens/        # Main screens
```

## Game Rules

1. **Board**: 8×8 grid
2. **Pieces**: 3 random pieces at a time (lines, squares, L/T shapes)
3. **Placement**: Drag to empty cells, must fit within bounds
4. **Clearing**: Full rows/columns clear simultaneously
5. **Scoring**: 
   - 1 point per placed cell
   - 10 points per cleared line
   - Bonus: 5×L×(L-1) for clearing L lines at once
6. **Game Over**: No valid moves remaining

## Building the Project

### Prerequisites
- Android Studio Electric Eel or newer
- JDK 17
- Gradle 8.x

### Steps
```bash
# Sync dependencies
./gradlew sync

# Build project
./gradlew build

# Run tests
./gradlew test

# Generate APK for testing
./gradlew assembleDebug

# Generate release AAB
./gradlew bundleRelease
```

## Running Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Key Files

- **Game Engine**: [domain/GameEngine.kt](app/src/main/java/com/hb/puzz/domain/GameEngine.kt)
- **Data Model**: [domain/model/Piece.kt](app/src/main/java/com/hb/puzz/domain/model/Piece.kt)
- **Persistence**: [data/GameDatastore.kt](app/src/main/java/com/hb/puzz/data/GameDatastore.kt)
- **Tests**: 
  - [test/GameEngineTest.kt](app/src/test/java/com/hb/puzz/GameEngineTest.kt)
  - [test/GameBoardTest.kt](app/src/test/java/com/hb/puzz/GameBoardTest.kt)

## Scoring Examples

| Scenario | Calculation | Score |
|----------|-------------|-------|
| Place 4 cells, clear 1 line | 4 + (1×10) + 0 | 14 |
| Place 6 cells, clear 2 lines | 6 + (2×10) + 10 | 36 |
| Place 8 cells, clear 5 lines | 8 + (5×10) + 100 | 158 |

## Controls

- **Tap/Click**: Place piece at grid position
- **Drag**: Move piece and preview placement
- **Pause**: Temporary game pause with resume option
- **Restart**: Start new game from current screen

## Theme Colors

| Color | Hex |
|-------|-----|
| Background | #FAF7ED (Ivory) |
| Primary Text | #1A2B4C (Navy) |
| Block Coral | #FFB39A |
| Block Teal | #7DE3E8 |
| Block Blue | #9BC5F2 |
| Block Gold | #FFD166 |

## License

MIT License - free to use and modify for personal and commercial projects.
