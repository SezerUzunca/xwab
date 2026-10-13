plugins {
    id("xwab.kmp.feature.api")
}

kotlin {
    android { namespace = "com.xwab.app.feature.aftershift.api" }

    // The callback contract is one plain action, opening the nightmare-rewriting screen, so this
    // module names no core type.
}
