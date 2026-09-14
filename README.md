# Cozy Picture Blocks

A relaxing picture puzzle game where players rearrange shuffled rectangular picture tiles to reconstruct an illustration.

## Features

- **Offline Play**: Works without internet connection from first launch
- **Clean Architecture**: Separated game logic from UI code
- **Modern UI**: Jetpack Compose with Material Design 3
- **Persistence**: DataStore for settings and best scores
- **Multiple Difficulty Levels**: 3×3, 4×4, and 5×5 grids (20 levels)
- **Visual Merging**: Correctly adjacent tiles visually merge

## Game Rules

1. Puzzle divided into N×N grid (3×3, 4×4, or 5×5)
2. Each tile has a permanent ID representing its correct position
3. Initially shuffle all tiles among grid cells
4. All cells remain occupied; no empty sliding-puzzle cell
5. Drag any single tile onto any other cell to swap
6. Dropping outside board returns tile to original position
7. Dropping onto own cell makes no move
8. Tiles in correct positions are still movable
9. Count completed swaps as moves only
10. Puzzle solved when every tile is in its correct cell

## Scoring

- Move counter tracks puzzle attempts
- No time limit or lives system
- Solved state displayed with move count

## Architecture

```
app/
├── domain/              # Game logic & rules
│   ├── model/          # Data classes (PuzzleLevel, PuzzleGameState)
│   └── PuzzleEngine.kt    # Core puzzle mechanics
├── ui/                 # UI components
│   ├── theme/          # Colors and themes
│   ├── screens/        # Main screens
│   └── components/     # Reusable UI elements
├── data/               # Data persistence (DataStore, Room)
└── MainActivity.kt     # Entry point
```

## Building the Project

### Prerequisites
- Android Studio Electric Eel or newer
- JDK 17
- Gradle 8.x

### Steps
```bash
# Open project in Android Studio
# Sync dependencies (File > Sync Project with Gradle Files)

# Build project
./gradlew build

# Run unit tests
./gradlew testDebugUnitTest

# Generate debug APK
./gradlew assembleDebug
```

## Testing

Run unit tests with:
```bash
./gradlew testDebugUnitTest --tests "com.hb.puzz.PuzzleEngineTest"
```

## Key Files

- **Puzzle Engine**: `domain/PuzzleEngine.kt`
- **Level Data**: `domain/PuzzleLevel.kt`
- **Game State**: `ui/GameActivity.kt`

## Theme Colors

| Color | Hex |
|-------|-----|
| Background | #F5F1E8 (Cozy Cream) |
| Primary Text | #2D5F6D (Deep Teal) |
| Accent | #FFB7A3 (Peach) |

## Next Steps

1. Implement full drag-to-swap UI
2. Add visual merging animations for connected tiles
3. Implement level gallery screen
4. Add completion celebration animation
5. Implement persistence with DataStore/Room
6. Add sound effects and haptics

## License

MIT License - free to use and modify.
