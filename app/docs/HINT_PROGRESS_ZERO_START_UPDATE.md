# Zero-start progress + Journey Hint wallet

## New Adventure progress
- Fresh puzzles are now generated with **zero correct neighbor connections**, so Completion always begins at **0%**.
- Restart and Grid Shift also use the same zero-start shuffle.
- Old pristine saved sessions that were created by the previous shuffle logic are automatically re-shuffled if they would open above 0% before the player has made a move.

## Hint wallet
- A Journey starts with **1 Hint**.
- The Hint button shows the live remaining count as a badge.
- Each Hint press consumes exactly **1 Hint** and performs one random assisted puzzle step.
- A failed/unavailable assisted step is refunded automatically.
- Completing an Adventure in **optimal/par moves or better** earns **+1 Hint**.
- The player can hold at most **3 Hints** at a time.
- The completion card shows when a Hint was earned.
- Resetting Journey progress restarts the Journey Hint wallet at 1. Coins and Crystals keep their previous reset behavior.
