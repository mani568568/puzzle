# Required project-level Gradle patch

The cumulative app ZIP intentionally contains the `app/src` implementation and does not include the project's existing `app/build.gradle.kts`, so it is unsafe to replace that file blindly.

In the existing `dependencies {}` block, keep the project's Compose BOM/version strategy and make sure these two libraries are present:

```kotlin
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
```

If the project already uses the Compose BOM, do not add explicit versions to those two dependencies. If it does not use the BOM, use the same Compose version family already configured by the project.

Material Design 3 is selected in Kotlin by wrapping the app in a `MaterialTheme` from `androidx.compose.material3`; Gradle only supplies the Material 3 library. Do not mix the legacy `androidx.compose.material.MaterialTheme` with the Material 3 theme.
