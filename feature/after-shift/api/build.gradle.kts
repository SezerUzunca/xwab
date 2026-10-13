plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.aftershift.api" }

    // No callback contract: the screen plays its guides and keeps its note in place and sends the
    // shell no intents, so this module publishes the route alone and names no core type.
}
