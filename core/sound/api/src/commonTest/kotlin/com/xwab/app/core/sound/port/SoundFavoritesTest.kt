package com.xwab.app.core.sound.port

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundFavoritesTest {
    /**
     * Pins the stored name rather than the constant.
     *
     * Every caller reads [SOUND_FAVORITES_NAMESPACE], so nothing else in the build would notice its
     * value changing, and changing it moves where favorites are kept on disk. That is free until
     * the app ships; after that this assertion is the one that says a change here needs a migration
     * for the favorites already saved under the old name.
     */
    @Test
    fun theNamespaceIsTheNameTheStoreUses() {
        assertEquals("sound", SOUND_FAVORITES_NAMESPACE)
    }
}
