# Puzzle frame / image overlap fix

- The outer frame is now a dedicated 1dp shell outside the puzzle artwork.
- The artwork keeps the exact source-image aspect ratio inside that shell.
- The puzzle corner radius was reduced to 6dp (7dp outer frame) to avoid an oval/card look.
- Tile images, merged-group outlines, and grid lines are clipped to the inner artwork region, so they cannot paint over the outer frame.
- The completed image uses the same inner footprint as the live puzzle.
