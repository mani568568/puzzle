# Grid Shift update

- Adds a `Grid Shift` button directly below the puzzle board.
- First press keeps the same Adventure/image and randomly reduces the grid by one or two sizes.
- Entering a reduced Grid Shift costs **1 Crystal**.
- Every player starts the full Journey with **2 free Crystals**; when the balance reaches 0, reduced Grid Shift is locked until more are purchased.
- The shift never exceeds the game's existing 8x8 maximum.
- While shifted, the button changes to `Restore Grid`; pressing it returns to the Adventure's original grid size **for free**.
- Grid-size changes intentionally restart the current puzzle arrangement, moves, and active timer because pieces cannot be safely mapped between different grid dimensions.
- The original/base grid is persisted with the saved session so `Restore Grid` still works after leaving and continuing later.
- Move and coin targets update to the active grid size.
- Existing Adventure history, milestones, drag behavior, magical merge effects, and Journey Journal remain unchanged.
