package com.xwab.app.core.delivery.port

/** Namespaces isolate files, cleanup and in-flight transfers belonging to different consumers. */
data class CacheKey(public val namespace: String, public val fileName: String) {
    init {
        require(namespace.matches(Regex("[a-z0-9][a-z0-9_-]{0,63}"))) { "Invalid cache namespace." }
        require(fileName.length <= 128 && fileName.matches(Regex("[a-z0-9][a-z0-9_-]*(?:\\.[a-z0-9]+)?"))) {
            "Unsafe cache file name."
        }
    }
}

/**
 * The caller owns source identity and versions: change the file name when its content changes.
 * An empty acceptedContentTypes set accepts any media type. retainedFileNames, when supplied,
 * must be the complete current inventory for this namespace; null disables inventory cleanup.
 *
 * [headers] carries request headers the source requires — a `User-Agent` identifying the client is
 * the usual one, and some hosts refuse a request without it. They belong to the caller because this
 * module knows nothing about the host it is fetching from. `Accept` is derived from
 * [acceptedContentTypes] and is rejected here, so the two cannot describe different things.
 */
data class DeliveryRequest(
    val key: CacheKey,
    val httpsUrl: String,
    val acceptedContentTypes: Set<String> = emptySet(),
    val maxBytes: Long = 25L * 1024L * 1024L,
    val retainedFileNames: Set<String>? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    init {
        require(httpsUrl.startsWith("https://")) { "Content must use HTTPS." }
        require(maxBytes > 0) { "Download size limit must be positive." }
        require(acceptedContentTypes.all { it.isNotBlank() }) { "Content types must not be blank." }
        require(headers.all { (name, value) -> name.isNotBlank() && value.isNotBlank() }) {
            "Request headers must not be blank."
        }
        require(headers.keys.none { it.equals("Accept", ignoreCase = true) }) {
            "Accept is derived from acceptedContentTypes."
        }
        retainedFileNames?.let { names ->
            require(key.fileName in names) { "The requested file must be retained." }
            names.forEach { CacheKey(key.namespace, it) }
        }
    }
}
