# Local level artwork architecture (001-999)

## Resource naming

Android file-based resources cannot contain uppercase letters. Artwork placed under
`app/src/main/res/drawable-nodpi` must therefore be named:

- `level_001.jpeg`
- `level_002.jpeg`
- ...
- `level_999.jpeg`

Run `app/normalize_level_artwork.cmd` from the project root if the source files currently use
`Level_001.jpeg` style names. The command also verifies that all 999 files exist.

## Runtime mapping

`ImageAssets.resourceName(levelId)` maps level 1 to `level_001`, level 42 to `level_042`, and
level 999 to `level_999`. Resource IDs are resolved lazily and cached, so the app does not keep
a 999-entry hand-written `R.drawable` table.

## Bitmap memory strategy

The loader performs a bounds-only decode first, then uses `inSampleSize` to decode near the
actual screen requirement. Gameplay uses a device-aware target edge capped at 2048 px, while
list/history thumbnails request a 320 px cache bucket. A byte-bounded `LruCache` uses at most
one eighth of the VM heap and never more than 96 MiB. Only the current/recent images stay in
memory; the 999 JPEGs are not decoded eagerly.

The next bundled level is prefetched on an IO dispatcher after the current board finishes loading.
This improves next-level latency without blocking rendering or retaining the entire catalog.

## Catalog scaling

`PuzzleLevel` now generates levels 1 through 999 programmatically and performs O(1) lookup by
index. The original metadata/grid behavior for levels 1-20 is preserved for save compatibility.
Generated levels after 20 use bounded 4-8 grid-size bands. Level selection remains a
`LazyVerticalGrid`, and History is a `LazyColumn`, so hundreds of off-screen rows are not composed.
