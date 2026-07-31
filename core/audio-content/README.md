# Audio content

One capability: **the app's sound catalog** — what a listener can pick, and the audio behind it,
wherever that happens to come from.

This module owns:

- the catalog manifest — track metadata paired with its physical source;
- the bundled Compose Resource MP3 files;
- the permanent remote HTTPS MP3 sources;
- app-owned local copies of completed remote downloads;
- `MusicCatalogRepository`, the port screens read;
- `AudioContentResolver`, the port `core:playback` asks for a playable URI.

Only those two ports leave the module. Resource paths, remote URLs, cache file names and the file
stores stay internal, so a screen holding `MusicCatalogRepository` cannot reach the network or the
file system.

The two ports are not equals. Every feature has this module on its classpath for the catalog, so
`checkArchitecture` fails the build if a feature source so much as names `AudioContentResolver` or
`AudioFileStore` — resolving a track is playback's job.

Metadata, source and MP3 live together on purpose: adding a track is one edit in one module. The
former split — metadata in `core:data`, resource names in the domain model, MP3s in
`core:playback` — is what made that a three-module change.

Resolution order is:

1. a completed app-owned local file;
2. a bundled Compose Resource;
3. the remote HTTPS stream.

On a remote cache miss, the HTTPS URI is returned immediately so playback can start while one
background, single-flight download writes to a `.part` file. Only a complete validated response is
renamed to the final MP3 name. Later playback therefore resolves to `file://`.

Android stores files below `noBackupFilesDir/audio-content`; iOS uses
`Application Support/audio-content`. The catalog is finite and currently adds roughly 16 MB at
most, so no eviction policy is needed yet. If the catalog becomes server-driven or unbounded, quota
and explicit download management belong here rather than in the playback engine — and that is also
the point at which splitting delivery out from the catalog manifest starts to pay for itself.
