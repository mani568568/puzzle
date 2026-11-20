UI architecture update

Gameplay top status
- removed the merged notification/HUD bar
- removed Moves from the top status area
- timer is now a standalone MM:SS display with no timer icon
- Crystal and Coin are individual resource icons with notification-style count badges above them
- coin badge is constrained for up to 6 digits

Gameplay controls
- removed the ocean/glass tray from behind the four bottom actions
- Pause, Grid, Hint and Restart are normal vibrant action buttons again
- Pause uses a depth-press interaction
- Grid uses a lift/bounce interaction
- Hint uses a pulse + sparkle interaction
- Restart uses a shake interaction

Shared interaction architecture
- introduced ActionMotion in VibrantUi.kt
- supported motions: SHRINK, BOUNCE_UP, DEPTH_PRESS, TILT, PULSE, SHAKE
- all motions keep the existing custom ripple/highlight behavior
- primary actions can sparkle while destructive/reset actions shake instead
- reused the system across Home, Settings, History, navigation, replay/start, and gameplay

Home
- Level button uses bounce/lift behavior
- Settings uses tilt behavior
- History uses bounce/lift behavior
- Crystal pill pulses
- Coin pill bounces
- Reset Progress shakes
- back/navigation buttons shrink cleanly
