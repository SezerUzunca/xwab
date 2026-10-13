# Notes

`port/NotesPort.kt` is the module's only public port interface. It keeps the listener's own short
text notes on this device and nowhere else, each under a key its caller owns, as favourites live
under a caller's namespace. This module knows nothing of what a note is for: the After Shift screen
keeps the errands a listener parks for after they wake under `after-shift`, and
[rehearsal](../rehearsal/README.md) keeps the listener's rewritten nightmare under `rehearsal`; each
caller pins its own key.

`observe(key)` emits the stored text, or an empty one when there is none; `save(key, text)` replaces
it, and a blank text removes it. A file that cannot be read shows as an empty note and is logged; a
write the device refuses answers `Unavailable` and is logged, and the screen keeps what was typed.
Keys are lower-case letters, digits and dashes: they are names on disk, and renaming one strands
what was written under it.

## Wiring

Notes live in their own preferences file, `notes.preferences_pb`, because DataStore refuses two
instances over one file. `NotesBindings` holds the adapter, the store and its IO scope;
`AndroidNotesGraph` (from the `Context`) and `IosNotesGraph` bind the file, which DataStore resolves
on first access, off the main thread. One bridge per platform hands `NotesPort` to the application
graph.

## Tests

Host tests cover saving, replacing, clearing, a refused write and an unreadable file over in-memory
stores. `AndroidNotesGraphTest` and `IosNotesGraphTest` build the unmodified platform graphs, write a
note to disk, read it back and remove it.
