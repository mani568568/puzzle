# Source review issue log

Status describes the implementation, not a claim of device verification. See UPDATE_NOTES.md.

| ID | Priority | Finding | Status and action |
|---|---|---|---|
| 01 | High | Sparse chapter 11–20 vectors create identical blank pieces; exact tile IDs still determine completion. | Mitigated: numbered guides default on for these chapters; reference preview added. Original illustrations retained. Replace with detailed original full-frame art before a polished commercial release. |
| 02 | High | Countdown has no foreground/background lifecycle handling. | Fixed in code: activity pause/stop freezes monotonic active time and hides the board; explicit Resume required. |
| 03 | High | Back navigation can restore stale timer state; screen-scoped save jobs may be cancelled. | Fixed in code: serialized writer outlives screen disposal until queued writes finish; explicit exit waits for save; periodic checkpoint remains five seconds. Abrupt termination can lose the last uncommitted state. |
| 04 | High | Android 12+ backup includes SharedPreferences but game uses DataStore. | Fixed: include file/datastore in cloud, transfer and legacy backup rules. Backup transport still requires device validation. |
| 05 | Medium | Never starting/pausing/adding time can inflate the time bonus. | Fixed by replacing adjustable countdown with active-play clock and fixed difficulty reward targets. Paused board cannot be played. Preview time counts. |
| 06 | Medium | Timer labels wrap and control strip exceeds available width. | Fixed by removing the +/- strip and using responsive stat cards plus separate 48dp action targets. |
| 07 | Medium | Fixed header/board minimums overflow short or landscape screens. | Fixed in code: bounded square board and scrollable layouts. Landscape is functional via scrolling, not a separate two-pane layout. Large-font tests still required. |
| 08 | Medium | Gameplay stretches square art, completion crops it differently. | Fixed: square play surface and consistent Fit completion/preview. |
| 09 | Medium | Global source preference/fallback can change the image behind a saved permutation. | Fixed: save actual source and image identity with the session. If original online art cannot load, show Retry and preserve the session. |
| 10 | Medium | Online refresh deletes current photo before replacement succeeds. | Fixed: validate/cache replacement first; retain old cache on failure; commit metadata after download; atomic temporary-file rename. UI no longer refreshes art in mid-attempt. |
| 11 | Medium | Dark theme is only partly applied; white status icons on cream screenshot. | Fixed in code: theme tokens throughout new game layout, gold coin colors in both modes, explicit system-bar icon appearance and safe insets. |
| 12 | Medium | Root Gradle settings and wrapper are missing from supplied ZIP. | App-only delivery requested: use the existing project root and wrapper. No root file changes are required by this update. |
| 13 | Medium | Instrumentation dependencies missing. | Fixed: AndroidX JUnit, Espresso, runner and Compose test dependencies added. Device tests still need an emulator/phone. |
| 14 | Medium | Several engine tests assert nothing useful about their named behavior. | Fixed: real row-boundary/copy-isolation assertions; randomized group-move invariants. New clock/coin tests and persistence integration test added. |
| 15 | Design | Dropping onto a merged destination can split that destination group. | Retained intentionally: moving group stays rigid; displaced destination tiles reflow. How-to now explains it. Preserving every destination group would require a different move rule. |
| 16 | Medium | API key compiled into BuildConfig can be extracted from distributed APK. | Remaining if online photos are enabled. Offline default avoids requiring a key. Keep Pexels key out of source control; a confidential API credential needs a server-side service or a provider-approved public-client approach. |
| 17 | Medium | Full-resolution downloaded bitmaps can consume excessive memory. | Improved: cap compressed response at 15MiB and sample decode to a maximum dimension near 2048px; correct temporary bitmap recycling. Real low-memory device profiling remains. |
| 18 | Low | Generic catch intercepts coroutine cancellation during image loads. | Fixed: rethrow CancellationException. HttpURLConnection itself still uses blocking I/O with timeouts. |
| 19 | Low | Visual drag resistance differs from drop target movement. | Fixed: drag artwork follows the same delta used for target calculation. |
| 20 | Low | Destructive reshuffle occurs with one tap. | Fixed: pause and ask before resetting an attempt. Existing earned coins are retained. |
| 21 | Low | Developer setup/API-key messaging appears in the gameplay footer. | Fixed: removed from gameplay; settings uses player-facing availability copy. |
| 22 | Low | No full-picture reference or help for uniform sky/blank tiles. | Added live preview and optional numbered guides. Guides reveal solution ordering and remain an assistance feature. |
| 23 | High | New coin completion could be credited multiple times or lose progress on interruption. | Implemented atomic wallet/best/chapter/session update and durable per-chapter completion receipt; solved-session checkpoint permits recovery after interruption. Duplicate completion is covered by an authored device test. |
| 24 | Medium | Old save format does not contain reliable active duration. | Migration preserves board/moves, marks speed reward ineligible, explains this in results. New attempts measure active time. |
| 25 | Low | Home/settings/how-to use tall non-scrolling layouts. | Updated: safe insets and vertical scrolling; new home artwork and wallet. |
| 26 | Low | Old documentation makes conflicting claims about timer, protection of groups and level selection. | Historical markdown archived; UPGRADE_README.md is the current specification. |

| 27 | Build | `maxWidth` read inside nested Column was rejected by Compose DSL scope resolution. | Fixed: compute `useCompactActions` inside BoxWithConstraints and use the captured Boolean inside Column. Reported by the supplied build log. |

## Still required before release

- Replace sparse artwork (and verify rights to all bundled photos); guides are a functional mitigation.
- Run the device checklist, including TalkBack, large fonts, landscape and low-memory phones.
- Review whether optional online Pexels access belongs in the intended offline product.
- The game still requires drag input; numbered guides do not constitute full TalkBack gameplay support.
- Local wallets cannot provide server-verified anti-cheat or synchronization across devices.
- No purchases, coin spending, advertisements or account backend were introduced.

## Technical references checked

- Compose side-effect lifetime: https://developer.android.com/develop/ui/compose/side-effects
- Lifecycle integration: https://developer.android.com/jetpack/androidx/releases/lifecycle
- Android backup inclusion: https://developer.android.com/identity/data/autobackup
- Monotonic elapsed time: https://developer.android.com/reference/android/os/SystemClock
- Instrumentation dependencies: https://developer.android.com/training/testing/instrumented-tests
