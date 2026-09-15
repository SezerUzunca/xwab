# Sources

`port/SourcePort.kt` is this module's only public port. It resolves caller-owned namespaces and
plain item IDs to physical HTTPS addresses, and reports the cache files still referenced by a
namespace. Screens cannot depend on this module; playback is the only production consumer.

`SOUND_NAMESPACE` and `STORY_NAMESPACE`, published beside the port, are the two namespaces this
module's manifests populate. `:core:session` reads sources under these names and pairs the sound
one with `core:delivery`'s `CacheKey`, so both sides import the same constant rather than each
typing the string.

`ContentSource` is content-neutral: a non-null cache filename means the source participates in
delivery caching, a null filename means it streams directly, and its filename validation accepts
any extension `core:delivery`'s own `CacheKey` would — this module knows its manifests point at
audio today, not that every source ever will. `headers` carries what a source's host requires to
serve it (Wikimedia refuses a request with no client identity, for instance) on the type that owns
the URL, so the module that later fetches it does not have to know the host by name.

Sound and story addresses live in separate internal manifests, while `ManifestSourceAdapter` is
the module's single Metro binding. This module is a dependency-free leaf: it knows no sound, story,
delivery, or playback types. Source rows validate HTTPS, safe cache filenames, non-blank headers,
unique item IDs, unique cache names, and unique URLs in tests. Metadata/source completeness is
checked from `:core:session`, the module that makes the same lookup at runtime and already depends
on all three ports.
