Rectangular grid + vibrant interaction update

Puzzle geometry
- puzzle engine now uses a true portrait rectangular grid with more rows than columns
- size presets map as follows:
  2 -> 2x3
  3 -> 3x4
  4 -> 4x5
  5 -> 5x6
  6 -> 6x7
  7 -> 7x8
  8 -> 8x9
- board aspect ratio follows the rectangular geometry and occupies substantially more vertical screen space
- drag/drop, connected groups, merge contours, source-image slicing, hover targeting, hint logic, saved-session validation and progress connections were updated for rectangular geometry

Button/UI interaction
- added reusable VibrantAction / VibrantCircleAction components
- press feedback combines scale/bounce, slight shiver rotation, extra shadow/glow and sparkle flashes
- applied to Home Level button, Settings, History shortcut, navigation/back controls, gameplay controls, next-level button, History filters, replay/start/continue actions, Settings image/motion selectors and reset action

Visual styling
- Home, gameplay, History, Settings and How-to screens use coordinated pastel/vibrant gradient backgrounds
- main actions use orange/pink/blue/mint gradients with high-contrast content
- PLAY / IN / BLOCK sizing was tightened so BLOCK stays on one line
- locked History images remain fully blurred with lock treatment

Dependencies
- no new third-party animation library is required; all effects use Jetpack Compose APIs already used by the app.
