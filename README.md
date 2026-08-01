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
- **Downloads Organizer** — scans a user-granted folder via the Storage Access Framework,
  classifies files by extension (Images/Videos/Audio/Documents/Archives/APKs/Others), and
  moves them into matching subfolders using `DocumentsContract.moveDocument`.
- **APK Manager** — finds `.apk` files in a granted folder, reads version/label metadata
  (via a temporary private-cache copy, since `PackageManager` needs a real file path),
  shows size and modified date, and supports multi-select delete.

Not yet built: Duplicate Scanner, Zero-byte Scanner, Empty Folder Cleaner, Large File
Scanner, Storage Analyzer charts, Search, Recycle Bin, Scan History UI, Settings. The
`ScanHistoryRepository`/Room database and clean-architecture folders for these already
exist so they can be added without restructuring.

## Architecture

```
ui/            Compose screens, grouped by feature
navigation/     NavHost + route definitions
scanner/        Pure scanning logic (SAF-based, cancellable, Dispatchers.IO)
data/           Room database + DataStore preferences
repository/     Mediates between scanners/database and ViewModels
models/         Data classes shared across layers
utils/          FileUtils, StorageStatsUtil, SafManager
viewmodel/      MVVM state holders, StateFlow-based
```

## Permissions

Deliberately minimal: no `READ_EXTERNAL_STORAGE`, no `MANAGE_EXTERNAL_STORAGE`. Folder
access is granted per-session by the user via `ACTION_OPEN_DOCUMENT_TREE` (Storage Access
Framework) and persisted with a permanent URI permission — the approach Google recommends
for scoped storage on Android 11+.

## Build

Auto-builds via GitHub Actions on every push to `main`. Check the **Actions** tab →
latest run → **Artifacts** → `storage-toolkit-debug-apk`.

## Author
**Ainebyoona Godfrey**
Computer Science Student, Metropolitan International University, Mbarara
