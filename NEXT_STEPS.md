# Cozy Picture Blocks - Next Steps

## Current Status

✅ **Core Game Engine Complete:**
- `domain/PuzzleEngine.kt` - Full tile shuffling, validation, and swap logic
- `domain/PuzzleLevel.kt` - 20 levels with metadata (3×3, 4×4, 5×5)

❌ **Compilation Issues:**
- Multiple domain classes have naming conflicts (`GameState`, `GameBoard`, `Piece`)
- UI files reference missing types from legacy code

## To Complete the Implementation:

### Immediate Fix Required:
1. Remove duplicate/conflicting domain classes
2. Resolve type conflicts between old Mosaic Blocks code and new puzzle engine
3. Add missing imports for Material Icons Extended

### Files to Delete (Conflicting Legacy Code):
```
domain/GameEngine.kt                    # Conflicts with PuzzleEngine
domain/model/*.kt                       # Duplicate model definitions  
ui/GameScreen.kt                        # Old implementation
ui/GameViewModel.kt                     # References missing types
data/GameDatastore.kt                   # Unresolved references
data/GameStateCodec.kt                  # Unresolved references
```

### Files to Keep:
```
domain/PuzzleEngine.kt                  # Core game logic
domain/PuzzleLevel.kt                   # Level definitions  
ui/PuzzleBoard.kt                       # Board UI (once dependencies fixed)
GameActivity.kt                         # Main game screen
App.kt                                  # Application class
```

### Remaining Implementation Steps:

**Phase 1: Fix Compilation**
- Remove conflicting files above
- Update `build.gradle.kts` to use correct theme
- Add missing Material Icons Extended dependency

**Phase 2: Complete UI**
- Implement drag-to-swap (gesture detection)
- Add visual merging animations
- Add tile source image rendering
- Create placeholder artwork in `res/drawable/`

**Phase 3: Navigation Flow**
- Home screen with continue button
- Level select gallery  
- Settings screen
- Pause/resume flow

**Phase 4: Persistence**
- DataStore for settings (sound, theme)
- Save/load game state

**Phase 5: Polish**
- Completion celebration animation
- Sound effects (optional)
- Accessibility improvements

## Quick Fix Commands:

```bash
# Remove conflicting files
rm app/src/main/java/com/hb/puzz/domain/GameEngine.kt
rm -rf app/src/main/java/com/hb/puzz/domain/model/
rm app/src/main/java/com/hb/puzz/ui/GameScreen.kt
rm app/src/main/java/com/hb/puzz/ui/GameViewModel.kt

# Then rebuild
./gradlew clean && ./gradlew assembleDebug
```

## Testing Plan:

Once compilation succeeds:
1. Test basic gameplay (tile selection, swapping)
2. Verify solved state detection
3. Test level progression
4. Validate visual merging animations

---

**Status**: Core logic complete, need to resolve file conflicts to compile.
