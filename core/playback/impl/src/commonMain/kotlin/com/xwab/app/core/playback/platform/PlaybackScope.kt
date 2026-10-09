package com.xwab.app.core.playback.platform

/**
 * The lifetime of this module's own graph: one per application, owned by the platform's graph
 * adapter. Each platform declares its graph next to its engine, which is what differs between them.
 */
internal object PlaybackScope
