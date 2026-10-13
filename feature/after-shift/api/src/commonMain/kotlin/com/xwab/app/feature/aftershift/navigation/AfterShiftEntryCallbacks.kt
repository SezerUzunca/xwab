package com.xwab.app.feature.aftershift.navigation

/** Actions supplied by the application's composition root. */
class AfterShiftEntryCallbacks(
    /** The listener wants to rewrite a recurring nightmare; where that leads is the shell's to decide. */
    val onRewriteNightmare: () -> Unit,
)
