# Mosaic Blocks Game Design Document

## Overview

**Mosaic Blocks** is a relaxing, offline block puzzle game built with Kotlin and Jetpack Compose. Players place blocks on an 8×8 grid to complete rows and columns for points.

## Core Gameplay Mechanics

### Board
- 8×8 grid of cells
- Cells can be empty or filled with colored blocks
- Rows and columns clear when completely filled

### Pieces
Three random pieces appear at the bottom of the screen:
- Single blocks (1 cell)
- Lines: lengths 2–5 (horizontal/vertical)
- Squares: 2×2, 3×3
- L shapes, T shapes, zigzag patterns

### Actions
1. **Placement**: Drag or tap to place a piece on empty cells
2. **Validation**: Check if piece fits entirely within bounds and doesn't overlap
3. **Clearing**: After placement, clear any completed rows/columns
4. **Refill**: Generate 3 new pieces after using all current ones

### Game Over Condition
When none of the remaining pieces can fit on the board.

## Scoring System

- **Placement Points**: 1 point per placed cell
- **Line Clearing**: 10 points per completed row/column
- **Bonus**: For clearing L lines simultaneously: `5 × L × (L - 1)` points

**Examples**:
- Clear 1 line: 10 + 0 = 10 points
- Clear 2 lines: 20 + 10 = 30 points
- Clear 5 lines: 50 + 100 = 150 points

## Design System

### Color Palette
- Background: Warm ivory (#FAF7ED)
- Typography: Dark navy (#1A2B4C)
- Blocks: Pastel coral, teal, blue, gold, pink, lavender

### UI Components
- Rounded corners (6–12 dp)
- Subtle shadows and borders
- Smooth animations for placement/clearing
- Responsive layouts for all phone sizes

## Architecture

```
app/
├── domain/          # Game logic & models
│   ├── model/       # Data classes
│   │   ├── Piece.kt
│   │   ├── Cell.kt  
│   │   └── GameBoard.kt
│   └── GameEngine.kt
├── data/            # Persistence layer
│   └── GameDatastore.kt
├── ui/              # UI components
│   ├── theme/       # Colors, themes
│   ├── components/  # Reusable UI
│   └── screens/     # Screen composables
├── MainActivity.kt  # Entry point
└── GameActivity.kt  # Game screen
```

## Implementation Features

### Core Engine
- Piece validation (bounds checking)
- Overlap detection
- Line clearing logic
- Score calculation
- Game over detection

### Persistence
- DataStore for settings (sound, haptics, theme)
- Saved game state restoration
- Best score persistence

### UI/UX
- Touch targets minimum 48dp
- Drag-to-place with visual feedback
- Animation states: placing, clearing, scoring
- Dark/light mode support

## Testing Strategy

1. **Unit Tests**
   - Board boundary checks
   - Overlap detection
   - Line completion logic
   - Scoring calculations
   - Piece generation variety

2. **Integration Tests**
   - State persistence/restore
   - Game flow (start → play → game over)
   - Theme switching

3. **Manual Testing**
   - Drag and drop smoothness
   - Pause/resume functionality
   - Screen rotation handling

## Build Instructions

1. Open project in Android Studio
2. Sync Gradle dependencies
3. Run `./gradlew build` to compile
4. Test on emulator or physical device

## Release Build

```bash
# Generate signed AAB
./gradlew bundleRelease

# Or generate APK for testing
./gradlew assembleRelease
```

Sign with keystore stored outside source control.

## Future Enhancements

- Power-ups and special blocks
- Multiple difficulty modes
- Daily challenges
- Local leaderboards
- Animations: piece slide, line clear effects
- Sound effects and music toggle
