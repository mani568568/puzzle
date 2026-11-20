# Phase 1–5 implementation status

## Phase 1 — conflicts and architecture

The current cumulative source tree already contains the modern puzzle engine and does not contain the requested legacy files (`domain/GameEngine.kt`, `domain/model/*`, `ui/GameScreen.kt`, `ui/GameViewModel.kt`, `data/GameDatastore.kt`, `data/GameStateCodec.kt`). A PowerShell cleanup script is included at `app/cleanup_legacy_files.ps1` so those files can also be removed from an older local checkout before copying this app patch.

`PuzzleEngine.kt`, `PuzzleLevel.kt`, and `PuzzleBoard.kt` remain intact. The app-only cumulative archive does not contain the project-level `GameActivity.kt`, `App.kt`, or `app/build.gradle.kts`, so this patch does not overwrite them. `GRADLE_MATERIAL3_PATCH.md` contains the exact Material 3 / material-icons-extended dependency additions for the existing Gradle file.

## Phase 2 — gameplay

Already implemented in the current engine/UI:

- drag/drop board interaction through gesture detection in `PuzzleBoard.kt`;
- source-region image rendering using grid-relative source rectangles;
- connected-group rendering as one image fragment so internal seams disappear;
- merge pulse / contour / sparkle celebration when new connections form.

The existing project uses portrait rectangular grids (`N × (N+2)`), not 3×3/4×4/5×5. The image crop/rendering code is grid-generic, so no square-grid-only rewrite was introduced.

## Phase 3 — flow

Added a 20-level `LevelSelectionScreen` and a `levels` navigation route. The home screen now exposes a continue action when a saved session exists and a separate New Game action that opens level selection. Selecting a level clears the active session before starting a fresh puzzle. Existing pause/resume and Settings flows are retained.

## Phase 4 — persistence

The current `GameSettings` already uses Preferences DataStore to persist completed levels, highest unlocked level, active puzzle positions/session, coins/crystals/hints, history, image source, block movement, sound, haptics, and dark theme. No duplicate Room layer was added.

## Phase 5 — polish

The current game already has sound/haptic feedback hooks and solved/merge celebration animation. Interactive controls touched by this patch were normalized to at least 48dp.

The 20 custom PNG/JPG artworks are not included because the cumulative source archive contains no `res/` artwork folder or original placeholder vectors to replace. Those assets should be added to the full Android project rather than fabricated into an app-source-only patch.
