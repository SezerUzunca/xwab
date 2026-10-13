plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.rehearsal.api" }

    // The callback contract is a Back action alone, so this module names no core type.
}
