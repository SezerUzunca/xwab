# Favorites

One capability: **the tracks the listener marked**, kept across launches.

- `FavoritesRepository` and its DataStore-backed implementation;
- the `DataStore<Preferences>` itself, created at the platform's own path — `filesDir` on Android,
  the documents directory on iOS.

Named for the capability rather than for DataStore, and rather than for "preferences": the store is
*how*, not *what*. A module called `preferences` would have collected the next persisted thing, and
the one after that, until it was a layer named for a mechanism — which is exactly what `core:data`
was before it was dissolved.

So a second persisted capability gets its own module beside this one rather than a second key in
here. The cost is that the two would need a shared `DataStore` binding; at that point the store
moves to a small module both depend on, and each capability still owns its own port. That trade is
worth making at the second capability, not before it.

The on-disk file name is `xwab.preferences_pb` and stays that way regardless of what this module is
called — renaming it would silently drop every favorite an installed copy of the app has saved.
