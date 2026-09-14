package com.xwab.app.core.sources.port

/**
 * A physical HTTPS address and, when cached, its stable filename.
 *
 * [cacheFileName] validates against the same safe-filename shape `core:delivery`'s own `CacheKey`
 * requires, not a fixed extension: this module knows only that its manifests point at audio today,
 * not that every source ever will.
 *
 * [headers] carries request headers a source's host requires — some hosts refuse a request that
 * does not identify its client. They are declared here, on the type that owns the URL, rather than
 * guessed by whichever module happens to fetch it.
 */
data class ContentSource(
    val httpsUrl: String,
    val cacheFileName: String? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    init {
        require(httpsUrl.startsWith("https://")) { "Content sources must use HTTPS." }
        require(headers.all { (name, value) -> name.isNotBlank() && value.isNotBlank() }) {
            "Content source headers must not be blank."
        }
        cacheFileName?.let { name ->
            require(name.length <= 128 && name.matches(Regex("[a-z0-9][a-z0-9_-]*(?:\\.[a-z0-9]+)?"))) {
                "Cached content must use a safe filename: $name"
            }
        }
    }
}
