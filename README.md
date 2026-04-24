

# KeyTag

**Batch keyword tagging for Android — this does not exist anywhere else.**

Every desktop DAM has it. Lightroom has it. Capture One has it. No Android gallery app has it. KeyTag fills that gap.

Select any number of images in your native gallery. Tap Share → KeyTag. Type a keyword. Tap Apply. Every selected image is tagged instantly — one action, any number of photos.

Keywords are stored locally for instant search and written as industry-standard XMP sidecar files alongside your images. Move a photo to your desktop, open it in Lightroom or any XMP-aware application — the keywords are already there. Tag on your phone, find anywhere.

---

## Who is it for?

Anyone who shoots more photos than they can find later. Photographers who use a mobile workflow. Anyone frustrated by the impossibility of searching their phone library for anything more specific than a date or album name. If you have ever scrolled through hundreds of images looking for "that shot of the market in Bangkok" — KeyTag is for you.

---

## Overview

KeyTag is not a gallery app. It does not replace the gallery app you already use. It sits alongside it as a metadata layer — reading the same albums, leaving your files untouched, adding the one capability every gallery app omits: the ability to describe your images in words and find them again.

It is free, open source, and fully offline. No accounts, no cloud, no tracking.

---

## Core Workflow

KeyTag is built around three complementary tools. Use one, two, or all three depending on your workflow:

### 1. Keywording — the primary feature

Select any images in your native gallery. Tap Share → KeyTag. A batch tag dialog opens immediately. Type a keyword, tap Apply. Done.

Keywords are stored locally in KeyTag's database for instant search. They are also written as industry-standard XMP sidecar files alongside the original image files. This means your keywords are not locked inside the app — move an image to another device, open it in Lightroom, Capture One, or any XMP-aware application, and the keywords are already there. Tag once, search anywhere.

### 2. Categorization — albums as workflow

KeyTag reads all your native gallery albums automatically. No setup, no import. Albums you create in your native gallery appear in KeyTag instantly.

ShortList is a special folder KeyTag creates in your Pictures directory. Use it as a quick first-pass inbox — move images there from your native gallery Favorites or any other source, then open ShortList in KeyTag to keyword and categorise them in batch before moving them to their final album.

### 3. Starring — quality rating

Star images 1–3 from the viewer or in batch via the TAG dialog. Search by star rating across your entire library. Useful for filtering your best shots from a shoot without having to browse album by album.

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
- Keywords written as XMP sidecar files alongside originals — portable to any platform

### Search
- Global keyword search across all albums
- Search by star rating (1, 2, or 3 stars)
- Search accessible from main screen and album toolbar
- Results display in standard grid with star and video badges

### Video Support
- Videos-fin folder shows curated video clips with duration labels and filename
- Tap to play in native video player
- Long press to tag with keywords and star rating
- Tagged videos appear in keyword and star search results

### Share Integration
- Select images in your native gallery, tap Share → KeyTag
- TAG dialog opens automatically for the entire selection
- Keywords written to XMP sidecar files — searchable on any device, any platform
- Works on Vivo, Xiaomi, and standard Android devices
- Resolves manufacturer-specific URI formats to stable MediaStore IDs

---

## Privacy

KeyTag is fully offline. No accounts, no cloud sync, no analytics, no tracking of any kind. All metadata is stored locally on your device. Nothing leaves your device unless you explicitly share or export a file yourself.

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

image_keywords
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
KeyTag automatically creates a ShortList folder inside your device's Pictures/ directory on first launch. Move images there from your native gallery as a quick first-pass selection, then open ShortList in KeyTag to keyword and rate them in batch.

### Videos-fin Folder
Create a folder called Videos-fin inside Pictures/ using your native gallery or a file manager. Move final edited video clips there. KeyTag will display them in the Videos-fin tile on the main screen.

### Adding Other Folders
Tap the menu on the main screen and select Manage Folders to add folders from outside the default DCIM/ and Pictures/ paths.

---

## Known Limitations

### Manufacturer-Specific Behaviour
Xiaomi (MIUI): The native gallery creates virtual albums stored internally. On first launch some system folders may appear in the album grid. Long press any unwanted folder to hide it — hidden folders stay hidden across sessions.

### Folder Names
ShortList and Videos-fin are currently hardcoded. A future release will make these configurable in Settings.

### Video Metadata
The EXIF panel does not yet display video-specific metadata (codec, framerate, resolution). Planned for a future release.

### Album Renames
Renaming an album in the native gallery may not reflect in KeyTag on some devices, particularly Xiaomi/MIUI. This is a MediaStore limitation — the bucket display name is not reliably updated by the OS after a folder rename. Workaround: move all images out of the folder, delete it, recreate it with the new name, and move images back in.

---

## Roadmap

- Configurable folder names in Settings
- Video metadata panel
- Pending moves view with auto-clear when MediaStore detects file moved
- Play Store / IzzyOnDroid release preparation
- UI polish pass

---

## Contributing

Open an issue before submitting a pull request. This is a focused tool — new features should serve the core keywording and categorisation workflow.

---

## License

GNU General Public License v3.0

KeyTag is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

This means anyone can use, study, share and improve this software — but any distributed modifications must remain open source under the same license. Commercial exploitation without releasing source code is not permitted.

Full license: https://www.gnu.org/licenses/gpl-3.0.html

---

## Author

TST (PIXEL-PEN)
Developed with Claude (Anthropic) as an AI-assisted Android project.
Repository: https://github.com/PIXEL-PEN/KeyTag