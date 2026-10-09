package com.xwab.app.core.story.port

/**
 * What this content type is called when something is played.
 *
 * A playback item id is a kind and a raw value, and each content module names its own kind so that
 * `:core:session` never has to hold a list of them.
 *
 * It leaves the process as the engine source id's prefix, so from the first release on it is a
 * stored name rather than a constant.
 */
public const val STORY_PLAYBACK_KIND: String = "story"
