Timer + HUD update
- Fixed timer getting stuck after Android lifecycle pause/stop events.
- Manual Pause is now tracked separately from lifecycle pause.
- Returning to the foreground automatically resumes only timers paused by the lifecycle.
- A player who intentionally tapped Pause remains paused.
- Timer HUD now displays separate minute and second units (for example 01m : 12s).
- Timer receives more horizontal room while Moves, Crystals, and Coins remain aligned at the same height.
- Restart and Grid Shift reset/resume the timer cleanly and clear stale pause state.
