# Mosaic Blocks - Architecture

## Overview
A complete, original offline Android block puzzle game using Jetpack Compose with clean separation of concerns.

## Architecture Layers

### 1. Game Engine (Pure Kotlin)
- Located in `common/` package (shared between main and test)
- **Board**: 8×8 grid management, cell states, placement validation
- **Pieces**: Shape definitions, relative coordinates, generation logic
- **Scoring**: Points calculation with multi-line bonuses
- **GameState**: Tracks board state, available pieces, score, game over status

### 2. Data Layer
- **DataStore**: Settings (theme, sound, haptics), best score, saved game state
- Uses `androidx.datastore:datastore-preferences`

### 3. UI Layer (Jetpack Compose)
- **MainActivity**: Navigation between screens, state observation
- **ViewModel**: UI state management with StateFlow
- **Composables**: All screens - Home, Game, Pause, GameOver, Settings, HowToPlay

### 4. Key Design Patterns
- MVVM pattern
- Unidirectional data flow (State → Event → Action)
- Immutable game state for predictability
- Canvas-based rendering for smooth performance

## File Structure
```
src/main/java/com/hb/puzz/
├── MainActivity.kt              # Navigation & screen management
├── GameViewModel.kt           # UI state and game logic bridge
├── game/                      # Game engine (Kotlin files, not Android)
│   ├── Board.kt               # 8×8 grid, cell states, placement
│   ├── Piece.kt               # Shape definitions, generation
│   ├── Scoring.kt             # Score calculation with bonuses
│   └── GameState.kt           # Complete game state, validation
├── data/                      # Persistence layer
│   ├── SettingsRepository.kt  # DataStore wrapper
│   └── SavedGameSerializer.kt # Gson/Manual serialization
└── ui/                        # Jetpack Compose UI
    ├── theme/
    │   ├── Color.kt           # Custom palette (ivory, navy, coral, teal, blue, gold)
    │   ├── Theme.kt           # Light/Dark theme setup
    │   └── Shape.kt           # Rounded corners, custom shapes
    ├── home/                  # Home screen components
    ├── game/
    │   ├── GameBoard.kt       # Canvas-based 8×8 board with drag
    │   ├── PieceTray.kt       # Three-piece tray
    │   └── BoardCell.kt       # Individual cell rendering
    ├── pause/                 # Pause menu
    ├── gameover/              # Game over screen
    └── settings/              # Settings screen

```

## Core Mechanics

### Board State
- 8×8 grid stored as `Array<Int>` (0 = empty, >0 = color ID)
- Placement validation: bounds check + overlap check
- Clearing: rows and columns simultaneously when full

### Piece System
- Shapes: single, lines(2-5), squares(2×2, 3×3), L-shapes, T-shape, zigzag
- Each shape defined by relative coordinates (e.g., `listOf(Pair(0,0), Pair(1,0))`)
- No rotation in v1

### Game Loop
1. Load saved game or start new (8×8 empty board)
2. Show 3 random pieces in tray
3. User drags piece onto board
4. Validate placement → update score → clear lines → check game over
5. Only generate new pieces when all 3 used
6. Save state after each move

### Scoring Formula
```
placementPoints = cellsInPiece (1 point per cell)
lineClears = rowsCleared + colsCleared
bonus = 5 × lineClears × (lineClears - 1) if lineClears > 0
totalScore += placementPoints + lineClears × 10 + bonus
```

### State Persistence
- Save after every successful move
- Board state, piece tray, score, best score, settings
- Restore on app start via Intent extras or DataStore

## Technical Decisions

1. **Canvas-based board**: More efficient than Grid for complex rendering and drag
2. **Immutable game state**: Easier testing, no race conditions
3. **Separate GameEngine class**: No Android imports in core logic
4. **ViewModel bridges UI ↔ Engine**: StateFlow for reactive updates
5. **Drag handling with offset**: Finger doesn't hide piece

## Testing Strategy
- Unit tests: Board validation, scoring, piece generation, game-over detection
- Instrumented tests: Drag simulation, pause/resume, state restoration