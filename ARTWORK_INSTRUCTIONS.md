# Adding Your Own Artwork to Cozy Picture Blocks

## Current State

The game currently has:
- **Complete Game Engine**: Fully functional with shuffling, swapping, and validation
- **UI Framework**: Complete UI with puzzle board and tile interaction
- **Placeholder Images**: Simple vector patterns for 20 levels (replaceable)

## How to Add Your Own Artwork

### Option 1: Use Existing Vector Placeholders (Quick Start)

The game includes simple vector patterns that will work immediately. No additional artwork needed!

### Option 2: Replace with Custom Images

**Step 1**: Prepare your images
- Format: PNG or JPG
- Size: Square (e.g., 400x400, 600x600)
- Grid compatible: Dimensions should be divisible by grid size (3, 4, or 5)
- Background: Transparent background works best

**Step 2**: Add images to project
```
app/src/main/res/drawable/
├── level_1.png          ← Replace existing level_1.xml with your image
├── level_2.png
├── ...
└── level_20.png
```

**Step 3**: Update ImageAssets.kt

In `app/src/main/java/com/hb/puzz/ui/images/ImageAssets.kt`:

```kotlin
private val LEVEL_IMAGES = mapOf(
    1 to R.drawable.level_1,   // Change from XML to PNG if using images
    2 to R.drawable.level_2,
    // ...
)
```

**Step 4**: Update image loader

In `ImageAssets.kt`, modify the `loadBitmap` function:

```kotlin
fun loadBitmap(context: Context, levelId: Int): android.graphics.Bitmap? {
    val resourceId = getLevelImage(levelId)
    return try {
        if (resourceId.toString().endsWith(".png") || 
            resourceId.toString().endsWith(".jpg")) {
            // Load from file path
            BitmapFactory.decodeFile(resourcePath)
        } else {
            // Load from resources
            ContextCompat.getBitmap(context.resources, resourceId)
        }
    } catch (e: Exception) {
        DefaultLevelArtwork.generatePlaceholderBitmap(400, 400)
    }
}
```

### Option 3: Procedural Generation (Advanced)

Create your own procedural artwork generation in `DefaultLevelArtwork.kt`:

```kotlin
fun generateCustomArtwork(levelId: Int, width: Int, height: Int): android.graphics.Bitmap {
    return android.graphics.Bitmap.createBitmap(width, height, 
        android.graphics.Bitmap.Config.ARGB_8888).apply {
        val canvas = android.graphics.Canvas(this)
        
        // Your custom drawing logic here
        // Examples:
        // - Geometric patterns
        // - Nature scenes (mountains, trees, flowers)
        // - Abstract art
        // - Character designs
        
        // Use canvas.drawCircle(), drawRect(), drawPath() etc.
    }
}
```

## Artwork Requirements

### For Best Results:

1. **Resolution**: 
   - Minimum: 300x300 pixels
   - Recommended: 600-800 pixels for crisp display

2. **Format**: 
   - PNG (with transparency support)
   - JPG (for photos)

3. **Grid Compatibility**:
   - For 3×3 grid: width divisible by 3, height divisible by 3
   - For 4×4 grid: width divisible by 4, height divisible by 4  
   - For 5×5 grid: width divisible by 5, height divisible by 5

4. **Colors**:
   - High contrast works best
   - Avoid large uniform areas (hard to see tile boundaries)
   - Include distinct details in each section

### Example Artwork Themes:

```
Level 1-3 (Easy): Simple geometric patterns
Level 4-7 (Medium): Nature scenes (mountains, flowers)
Level 8-12 (Hard): Detailed landscapes, cityscapes
Level 13-20 (Expert): Complex abstract designs
```

## Testing Your Artwork

1. Replace the placeholder resources
2. Run the game: `./gradlew assembleDebug`
3. Test each level to ensure:
   - Images display correctly
   - Tile boundaries are visible
   - Merging animations work properly

## Troubleshooting

**Artwork not displaying**: Check resource IDs match in ImageAssets.kt

**Blurry images**: Use higher resolution artwork (minimum 300px per dimension)

**Tile boundaries invisible**: Ensure your artwork has sufficient contrast between sections

---

**Note**: The game will work perfectly with the built-in placeholder art. Custom artwork is optional for added visual appeal.
