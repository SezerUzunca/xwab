plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.shuffle.api" }

    // No callback contract: the shuffle screen plays its decks in place and sends the shell no
    // intents, so this module publishes the route alone and names no core type.
}
