# Board rendering alignment fix

- Removed the 4dp inset between the outer puzzle Surface and PuzzleBoard.
- Replaced the old 18dp outer / 6dp inner double-radius layout with one 12dp board geometry.
- The parent Surface now owns the single outer stroke and shadow.
- PuzzleBoard clips its artwork to the exact same 12dp footprint.
- Completed artwork fills that exact footprint too.

This keeps the source image, tile grid, merged groups and outer frame on the same rectangle.
