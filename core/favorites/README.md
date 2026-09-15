# Favorites

`:core:favorites` stores arbitrary item IDs separately for each caller-owned namespace. It has no
project dependency on sound, story or another content module. Its only public interface is
`com.xwab.app.core.favorites.port.FavoritesPort`; DataStore and platform adapters are internal.

```kotlin
val storyIds = favoritesPort.observe("story") // Flow<FavoritesSnapshot>
favoritesPort.toggle(namespace = "story", itemId = "night-came-slowly")
```

The same ID may be favorited in multiple namespaces independently. IDs must be nonblank;
namespaces use lowercase letters, digits, underscores or hyphens, with a maximum of 64 characters.
The caller maps these strings to its own domain types. Features and core modules may consume this
port through Metro without accessing persistence implementation details.

The file remains `xwab.preferences_pb`, in Android filesDir or the iOS documents directory.
Each namespace uses `favorite_<namespace>_ids`. Sound features pass `sound`, the same name
`:core:sources` gives that content kind; the namespace is stated once, by `:core:sound`.
Tests cover records written straight into the store, persistence, namespace isolation and invalid
inputs.

`FavoritesSnapshot` distinguishes a successful read from unavailable storage. During a read
failure it retains the last known IDs (empty before the first read), sets `isAvailable` to false,
and retries with exponential delay capped at 4.8 seconds while the flow is collected. Successful
reads restore availability; cancelling the collector stops retries.

`toggle` returns `FavoriteToggleResult.Updated` or `Unavailable`. Features display read/write
failures and decide which controls remain enabled. Cancellation propagates unchanged.
