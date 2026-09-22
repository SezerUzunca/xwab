plugins {
    // Contracts only: no adapter here, so nothing to wire and nothing to depend on.
    id("xwab.kmp.library")
}

kotlin {
    android { namespace = "com.xwab.app.core.resolution" }
}
