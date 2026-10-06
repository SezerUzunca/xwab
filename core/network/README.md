# Network

`NetworkPort` streams HTTPS responses through bounded chunks. Its internal Ktor adapter owns
redirects, connection/socket timeouts and transport errors. `core:delivery` owns response policy
and destination writes. Android uses OkHttp; iOS uses Darwin. Neither engine leaks into the port.

The connection timeout is 10 seconds and the socket timeout is 30 seconds between chunks. There
is no whole-download deadline, so a slow transfer can complete while it continues making progress.

Connection and response-stream failures surface as `NetworkTransportException`, with their cause
retained for diagnostics. Callback failures and caller cancellation propagate unchanged. Invalid
initial URLs and non-HTTPS schemes are rejected before the request. Ktor refuses HTTPS downgrades;
the adapter also validates the final requested URL.

Catalogs ship with the app, so this port exposes only the download operation currently consumed.
Each platform has its own graph (`AndroidNetworkGraph`, `IosNetworkGraph`), which provides the
engine, OkHttp or Darwin. A shared binding container builds one `HttpClient` from that engine, and
with it one connection pool, for the application. Only `NetworkPort` reaches the application
graph.

Tests run against the production graph with only the engine replaced by a `MockEngine`, so the
client's own timeouts, redirects and status handling are what they exercise. They cover response
metadata, bytes, headers, URL/redirect validation, transport failures, callback
errors and cancellation. A real-engine test for a response interrupted mid-body is still needed;
the MockEngine harness does not reliably reproduce that case.
