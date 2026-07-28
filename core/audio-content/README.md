# Audio content

This capability owns the sound catalog and every way a catalog item becomes playable:

- bundled Compose Resource MP3 files;
- permanent remote HTTPS MP3 sources;
- app-owned local copies of completed remote downloads;
- the `MusicCatalogRepository` and `AudioContentResolver` adapters.

Resolution order is:

1. a completed app-owned local file;
2. a bundled Compose Resource;
3. the remote HTTPS stream.

On a remote cache miss, the HTTPS URI is returned immediately so playback can start while one
background, single-flight download writes to a `.part` file. Only a complete validated response is
renamed to the final MP3 name. Later playback therefore resolves to `file://`.

Android stores files below `noBackupFilesDir/audio-content`; iOS uses
`Application Support/audio-content`. The catalog is finite and currently adds roughly 16 MB at
most, so no eviction policy is needed yet. If the catalog becomes server-driven or unbounded,
quota and explicit download management belong in this capability rather than in the playback
engine.
