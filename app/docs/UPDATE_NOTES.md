# App-only update — Compose scope compile fix

## Apply

Back up your existing app folder. Extract this archive into your existing MPB
project and replace matching app files. The archive contains only app/.
Use your existing root build.gradle.kts, settings.gradle.kts, Gradle wrapper,
gradle.properties, gradle/libs.versions.toml and local.properties.
No files outside app/ need changing: the version catalog supplied in the original
source and the previous upgrade is byte-for-byte identical.

The app/build.gradle.kts file already contains the dependencies used by the UI
and coin upgrade. Preserve any signing configuration you added separately.

## Fix from the supplied build log

PicturePuzzleGameScreen.kt read BoxWithConstraints.maxWidth inside a nested
Column lambda. Compose DSL scopes prevent that implicit outer receiver access.
The width decision is now computed in BoxWithConstraints as useCompactActions,
and the Column reads that local Boolean. The responsive behavior is unchanged.

The only existing app file changed from the last delivered upgrade is:
src/main/java/com/hb/puzz/ui/PicturePuzzleGameScreen.kt
If you already copied that upgrade, replacing just this file addresses the
reported error. The full app folder is included for convenience.

## Validation

- Confirmed no maxWidth/maxHeight access remains inside the nested Column.
- Confirmed only the intended existing source file differs from the prior app.
- Parsed all Android XML files and verified ZIP integrity and app-only contents.
- The previous 32 passing JVM tests cover unchanged domain logic; they were not
  rerun for this scope-only UI fix.
- A complete Android build was not rerun here. From the existing project root:

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace
```

The UI, coin rewards, save handling and other earlier improvements are retained.
See ISSUE_LOG.md for the full findings and remaining device checks.
