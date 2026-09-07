# Network

One capability: **performing shared HTTP operations**.

`NetworkPort` is the public application-facing port. Its internal Ktor adapter owns HTTPS enforcement,
redirects, connection/socket timeouts, response metadata, text bodies and streamed response bytes.
Android contributes Ktor's OkHttp engine and iOS contributes Darwin; callers never select an
engine and never depend on a Ktor type.

The client deliberately has two operations:

- `getText` keeps a small UTF-8 document in memory;
- `download` exposes bounded chunks so media is never accumulated as one `ByteArray`.

Only `download` has a caller today — `core:sound:delivery`, filling the audio cache. Both catalogs
ship with the build, so `getText` is the half of this port that a content feed would use, kept with
its tests rather than deleted and re-added unchanged. If a feed is ruled out, `getText`,
`NetworkHttpException` and `NetworkTimeoutException` go with it; `NetworkTransportException` also
serves downloads.

They are timed differently:

| | connect | between chunks | whole call |
|---|---|---|---|
| `getText` | 10s | 30s | **15s** |
| `download` | 10s | 30s | **none** |

A total download timeout would reject a healthy 25 MB transfer on a slow connection. The socket
timeout instead stops a connection that has stopped making progress. `getText` converts only its
own 15-second limit to `NetworkTimeoutException`; cancellation imposed by its caller remains
cancellation.

Connection and response-stream failures, including engine connection/socket timeouts, surface as
`NetworkTransportException` from either operation. Its cause is retained for diagnostics; callers
handle the port exception instead of matching Ktor types. Exceptions from `download` callbacks are
propagated unchanged because response policy and destination writes belong to the caller. Initial
URLs rejected by the URL parser or using a non-HTTPS scheme produce `IllegalArgumentException`
without invoking the engine.

This module contains no catalog refresh, fallback, JSON mapping, or content policy. The current
Sound and Story manifests are local application data. Sound delivery decides acceptable media
types and sizes and writes downloaded bytes through Okio; Story streams go directly from their
manifest source to the platform player and do not use this client.

Metro contributes the internal adapter as `NetworkPort` in `AppScope`, so there is one Ktor client
and one connection pool for the application lifetime. The implementation stays internal; Metro's
generated public contribution provider returns only `NetworkPort`. Screens do not declare this
module; they read content through capability ports.
