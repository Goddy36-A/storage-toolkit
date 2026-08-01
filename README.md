# Storage Toolkit

A privacy-focused Android storage manager built with Kotlin, Jetpack Compose, Material 3,
MVVM, Coroutines/Flow, and Room. No fake RAM boosters, CPU coolers, or battery "optimizers" —
just real file management.

## Status: in progress

This is being built incrementally, one fully-working feature at a time, rather than
scaffolded all at once. Currently implemented and functional:

- **Home Dashboard** — total/used/free storage (via `StatFs`), circular usage chart,
  quick-action grid. Cards for features not yet built are clearly marked "Coming soon"
  rather than showing fake data.
- **Downloads Organizer** — scans the Downloads folder, classifies files by extension
  (Images/Videos/Audio/Documents/Archives/APKs/Others), and moves them into matching
  subfolders via a plain file move.
- **APK Manager** — recursively finds `.apk` files across the whole accessible storage
  volume, reads version/label metadata (via a temporary private-cache copy, since
  `PackageManager` needs a real file path), shows size and modified date, and supports
  multi-select delete.
- **Zero-byte File Scanner** — recursively scans the whole accessible storage volume for
  files exactly 0 bytes, skipping hidden/`Android` folders. Shows filename, location,
  modified date. Select All / multi-select delete. Feeds the Dashboard's "last scan" time.
- **Empty Folder Cleaner** — recursively finds directories with no real files anywhere
  in their subtree (a folder full of only empty folders still counts). Only reports the
  outermost empty folder in a branch, since deleting it removes everything nested inside.
  Same skip-list and multi-select delete pattern as the other scanners.
- **Duplicate File Scanner** — finds files with identical content (SHA-256 hash, not
  just name/size) across the whole storage volume. Two-pass for performance: files are
  first grouped by size (free), and only files sharing a size with another file get
  actually hashed. Each group's oldest file is the suggested keeper; "Select duplicates
  (keep 1 each)" pre-selects everything else for deletion. Sortable by wasted space,
  name, or date.
- **Large File Scanner** — recursively finds files at or above 100 MB (scanned once at
  the lowest tier so switching the 100MB/500MB/1GB filter chip doesn't need a rescan).
  Sortable by size, name, or date. Tapping "Preview" opens a dialog with size/location/
  modified date before deleting -- and a downsampled thumbnail for images, decoded at a
  bounded resolution so a multi-hundred-MB photo doesn't get fully loaded into memory
  just to preview it.

Not yet built: Storage Analyzer charts, Search, Recycle Bin, Scan History UI, Settings.
The `ScanHistoryRepository`/Room database and clean-architecture folders for these
already exist so they can be added without restructuring.

## Architecture

```
ui/            Compose screens, grouped by feature
navigation/     NavHost + route definitions
scanner/        Pure scanning logic (cancellable, Dispatchers.IO)
data/           Room database
repository/     Mediates between scanners/database and ViewModels
models/         Data classes shared across layers
utils/          FileUtils, StorageAccessManager, StorageRoots
viewmodel/      MVVM state holders, StateFlow-based
```

## Permissions

Uses `MANAGE_EXTERNAL_STORAGE` ("All Files Access", Android 11+), with legacy
`READ/WRITE_EXTERNAL_STORAGE` for Android 10 and below. **This replaced an earlier
per-feature Storage Access Framework design** — the SAF version required granting a
folder separately for each feature, and whichever folder got granted first (or last,
from any screen) silently became the shared root for every scanner, with no way to see
or fix that from within the app. All Files Access is a deliberate scope decision, not a
shortcut: Storage Toolkit's entire purpose is managing files across shared storage,
which is exactly the "core functionality" carve-out Play Store policy allows for this
permission — a generic utility app bolting it on for a minor feature would not qualify.

One grant now covers the whole app. The grant happens in system Settings (there's no
in-app callback for it), so each screen re-checks permission state via `OnResumeEffect`
when you back out of Settings.

- `utils/StorageAccessManager.kt` — checks/requests the permission
- `utils/StorageRoots.kt` — fixed roots (`primary()` = whole storage, `downloads()` =
  just the Downloads folder) that scanners read from; no per-feature folder picking
- `utils/FileUtils.resolveDocumentFile()` — reconstructs a file-backed `DocumentFile` for
  delete/move ops; the old `DocumentFile.fromSingleUri` only works for `content://` SAF
  tree URIs, not the `file://` paths All Files Access gives us

## Build

Auto-builds via GitHub Actions on every push to `main`. Check the **Actions** tab →
latest run → **Artifacts** → `storage-toolkit-debug-apk`.

## Author
**Ainebyoona Godfrey**
Computer Science Student, Metropolitan International University, Mbarara
