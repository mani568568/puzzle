# Build and Run Mosaic Blocks

## Prerequisites

- Android Studio Electric Eel or newer
- JDK 17 ( bundled with modern Android Studio)
- Gradle 8.x (bundled with Android Studio)

## Project Structure

```
app/
├── src/main/java/com/hb/puzz/
│   ├── MainActivity.kt           # App entry point
│   ├── GameActivity.kt          # Full game implementation
│   └── domain/                  # Core game logic
│       ├── GameEngine.kt        # Game state management
│       └── model/               # Data models
│           ├── Piece.kt
│           ├── Cell.kt
│           ├── GameBoard.kt
│           ├── GameState.kt
│           ├── PieceColor.kt
│           └── PieceGenerator.kt
├── src/test/java/com/hb/puzz/
│   ├── GameEngineTest.kt        # Unit tests for game logic
│   ├── GameBoardTest.kt         # Board validation tests
│   ├── PieceGeneratorTest.kt    # Piece generation tests
│   └── GameDatastoreTest.kt     # Persistence tests
└── src/main/res/                # Resources
```

## Build Steps

### 1. Open Project in Android Studio
- Launch Android Studio
- Select "Open" and choose the MPB project folder
- Wait for Gradle sync to complete (fix any dependency issues)

### 2. Sync Dependencies
```bash
# In Android Studio, use: File > Sync Project with Gradle Files
```

### 3. Build the Project

**Option A - Through Android Studio UI:**
- Click "Build" → "Make Project"
- Or click "Build" → "Rebuild Project"

**Option B - Command Line:**
```bash
./gradlew build
```

### 4. Run Unit Tests
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests GameEngineTest
```

## Running on Device/Emulator

### Through Android Studio:
1. Connect a device or start an emulator
2. Click the "Run" button (green play icon)
3. Select target device
4. App will install and launch automatically

### Command Line:
```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.hb.puzz/.MainActivity
```

## Testing the Game Features

### Manual Tests:
1. **Board Display**: Verify 8×8 grid appears with correct styling
2. **Piece Placement**: Try placing pieces on empty cells
3. **Line Clearing**: Complete rows/columns to see them clear
4. **Scoring**: Verify score updates correctly after placement
5. **Pause/Resume**: Test pause functionality works

### Automated Tests:
```bash
# Run all tests with coverage report
./gradlew testDebugUnitTest connectedAndroidTest --coverage

# View test results in HTML format
open app/build/reports/tests/testDebugUnitTest/index.html
```

## Debugging

### Common Issues:

**Gradle sync failures:**
```bash
./gradlew clean --refresh-dependencies
./gradlew build
```

**Build errors after code changes:**
- In Android Studio: Build → Clean Project, then Build → Rebuild Project

### Logs:
```bash
# View app logs
adb logcat -s com.hb.puzz:D AndroidRuntime:E
```

## APK Output Locations

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Release AAB**: `app/build/outputs/bundle/release/app-release.aab`

## Signing for Release

Generate a keystore:
```bash
keytool -genkey -v -keystore mosaic-release.keystore \
  -alias mosaic-key -keyalg RSA -keysize 2048 -validity 10000
```

Build signed release AAB:
```bash
./gradlew bundleRelease
```

The signed AAB will be at `app/build/outputs/bundle/release/app-release.aab`.

## Distribution

### Google Play Store:
1. Upload the AAB file to Google Play Console
2. Fill in store listing details
3. Submit for review

### Direct APK Distribution:
```bash
./gradlew assembleRelease
```

The signed release APK is at `app/build/outputs/apk/release/app-release.apk`.

## Performance Optimization

### Enable R8 ProGuard:
The project has minify enabled for release builds which will reduce APK size.

### Build Optimization Flags:
Add to `gradle.properties`:
```properties
android.enableR8=true
android.useAndroidX=true
android.nonTransitiveRClass=true
```

## Testing Checklist

- [ ] App compiles without errors
- [ ] Unit tests pass (run `./gradlew test`)
- [ ] App launches on device/emulator
- [ ] Board displays correctly (8×8 grid)
- [ ] Pieces appear in tray with correct colors
- [ ] Clicking cells places blocks
- [ ] Score updates when placing blocks
- [ ] Pause button works
- [ ] Dark/light theme support verified

## Future Enhancements

1. **Add Drag-to-Place**: Implement touch gesture for piece dragging
2. **Animations**: Add smooth placement and clearing animations  
3. **Save/Restore**: Persist game state between sessions
4. **Sound Effects**: Add audio feedback for actions
5. **Haptics**: Enable vibration on interactions
