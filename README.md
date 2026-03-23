# KeyTag

**A lightweight Digital Asset Manager (DAM) for Android**

KeyTag works alongside your native gallery app — adding keywords, star ratings, and metadata to your photos and videos without replacing the gallery experience you already use.

---

## Overview

Most Android gallery apps are great at displaying photos but poor at organising them with searchable metadata. KeyTag fills that gap. It reads the same albums and folders your native gallery uses, lets you tag and rate images in batch, and makes your entire photo library searchable by keyword and star rating.

KeyTag is a free, open source. It is not intended to replace your native gallery — it is designed to complement it.

---

## Features

### Album Grid
- Displays all photo albums from DCIM/ and Pictures/ on your device
- ShortList — a pinned special folder at the top of the grid for first-pass selections
- Videos-fin — a dedicated folder for curated final video clips
- Alphabetical or date-added sort order
- Pinch-to-zoom grid density
- Long press any album to hide it from KeyTag (non-destructive)
- Manage hidden albums and restore individually
- Add external folders via the Manage Folders menu
- Direct link to native gallery from toolbar

### Image Viewer
- Full-screen swipe viewer with immersive mode
- Star rating (1-3 stars) via top bar
- Keyword chips with individual remove option
- EXIF panel showing date, device, lens specs, dimensions and keywords
- Share and Open With buttons
- Direct link to native gallery

### TAG Dialog (Batch Tagging)
- Select multiple images then tap TAG
- Assign star rating (0-3) to all selected images
- Add keyword with autocomplete from existing keywords
- Assign a category from existing Pictures albums
- Keywords written as XMP sidecar files for portability

### Search
- Global keyword search across all albums
- Search by star rating (1, 2, or 3 stars)
- Search accessible from main screen and album toolbar
- Results display in standard grid with star and play badges

### Video Support
- Videos-fin folder shows curated video clips
- Video thumbnails with duration overlay
- Tap to play in native video player
- Long press to tag with keywords and star rating
- Tagged videos appear in keyword and star search results

### Share Integration
- Share images directly from any app to KeyTag
- TAG dialog opens automatically for shared images
- Works on Vivo, Xiaomi, and standard Android devices
- Resolves manufacturer-specific URI formats to stable MediaStore IDs

---

## Architecture

### Technology Stack
- Language: Java
- Database: Room (SQLite) for keywords, star ratings and image metadata
- Image Loading: Glide
- UI: Material Design 3 components
- Minimum SDK: Android 10 (API 29)

### Database Schema

images
    id              long (primary key)
    uri             text
    mediaStoreId    long (stable cross-manufacturer identifier)
    qualityLevel    int (0-3 star rating)
    dateAdded       long

keywords
    id              long (primary key)
    name            text
    usageCount      int

image_keywords      (cross-reference table)
    imageId         long
    keywordId       long

### Key Design Decision — MediaStore ID

Early versions used URI strings as the primary key for images. This caused failures on Xiaomi devices where the native gallery shares images via com.miui.gallery.open URIs that do not match the MediaStore URIs used elsewhere in the app. The fix was to resolve all incoming URIs to their stable MediaStore _ID at write time, with URI string as fallback. This is handled in MediaStoreUtil.getMediaStoreId() which also falls back to querying by DISPLAY_NAME for exotic manufacturer URIs.

---

## Installation

### Requirements
- Android Studio Hedgehog or later
- Android SDK 34
- Java 17

### Build

git clone https://github.com/PIXEL-PEN/KeyTag.git
cd KeyTag
./gradlew assembleDebug

### Permissions Required

READ_MEDIA_IMAGES
READ_MEDIA_VIDEO
READ_EXTERNAL_STORAGE (API 32 and below)
WRITE_MEDIA_IMAGES

---

## Setup

### ShortList Folder
KeyTag automatically creates a ShortList folder inside your device's Pictures/ directory on first launch. Use your native gallery to move images there as a first-pass selection, then open ShortList in KeyTag to tag and rate them.

### Videos-fin Folder
Create a folder called Videos-fin inside Pictures/ using your native gallery or a file manager. Move final edited video clips there. KeyTag will display them in the Videos-fin tile on the main screen.

### Adding Other Folders
Tap the menu on the main screen and select Manage Folders to add folders from outside the default DCIM/ and Pictures/ paths.

---

## Known Limitations

### Manufacturer-Specific Behaviour
Xiaomi (MIUI): The native gallery creates virtual albums stored internally rather than as filesystem folders. Albums created through Xiaomi's gallery UI appear under Pictures/gallery/owner/ in MediaStore. KeyTag filters this path — albums intended for KeyTag should be created as real filesystem folders using a file manager.

Folder names: Some folder and album names are currently hardcoded (ShortList, Videos-fin). A future release will make these configurable in Settings.

### Video Metadata
The EXIF panel does not yet display video-specific metadata (codec, framerate, resolution). Planned for a future release.

### Category / Pending Moves
The TAG dialog includes a Category spinner that stores an intended album assignment. This is a planning feature only — KeyTag does not move files. The actual file move must be done in the native gallery.

---

## Workflow

1. Shoot photos and video
2. Browse in native gallery, create albums, move images into them
3. Open KeyTag — albums appear automatically
4. Select images, tap TAG, assign keywords and star rating
5. Search by keyword or star rating to find images across all albums
6. Move images to final album in native gallery guided by category tag

---

## Roadmap

- Configurable folder names in Settings
- Video metadata panel
- Pending moves view
- Auto-clear pending status when file is moved
- Remove debug logs before production release
- Play Store release preparation
- UI polish pass

---

## Contributing

This is a hobby project and contributions are welcome. Please open an issue before submitting a pull request.

---

## License

MIT License

---

## Author

Developed with Claude (Anthropic) as an AI-assisted Android project.
Repository: https://github.com/PIXEL-PEN/KeyTag