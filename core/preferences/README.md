# Preferences

What the app persists between launches, and the store it persists into.

- `FavoritesRepository` and its DataStore-backed implementation;
- the `DataStore<Preferences>` itself, created at the platform's own path — `filesDir` on Android,
  the documents directory on iOS.

Favorites are the only preference today. A second one belongs here beside it rather than in a
module of its own: they share the store, and splitting per preference would mean one module has to
hand the store to the others. That is also why this module is named for the capability and not for
DataStore — the store is how, not what.
