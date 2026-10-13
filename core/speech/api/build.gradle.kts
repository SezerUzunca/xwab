/**
 * The speech capability's contract: its port package, and nothing that implements it. Content
 * capabilities compile against this module; only the composition root installs the implementation.
 */
plugins {
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.speech.api" }
}
