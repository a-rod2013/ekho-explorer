# Local journal backend

Stores owner-scoped journal drafts in Room (`journal.db`) and copies imported
photos into private app storage. Drafts and photos remain available offline.

## Usage

Access `journalRepository` and `journalPhotoStorage` through `AppContainer`.

- `observeDrafts(ownerId)`: observe visible drafts, newest first.
- `getDraft(ownerId, draftId)`: retrieve a visible draft or null.
- `saveDraft(draft)`: create or update a draft.
- `deleteDraft(ownerId, draftId)`: hide a draft and record its deletion.
- `importPhoto(ownerId, uri)`: copy a photo; store the returned path in
  `JournalDraft.photoPaths`.

Call suspend functions from a coroutine. Callers supply owner IDs and unique
draft IDs; this layer does not authenticate users.

## Tracking

Each save increments the record's revision. Deletions retain a hidden
tombstone for future sync; deleted IDs cannot be reused. Upload acknowledgment
only succeeds when its revision matches the current local revision.

## Limits

UI integration and cloud sync are separate work. Photos remain local and are
removed when app data is cleared or the app is uninstalled. Deleting a draft
does not yet clean up its photo files.

## Validation

`JournalStorageTest` and `JournalSyncTrackingTest` passed on the Pixel 8
emulator, covering persistence, owner isolation, photo retention, revisions,
and deletion tracking.

## AI Assistance
Claude assisted with formatting of journal-backend.md
