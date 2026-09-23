package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The values a capability has already written onto devices.
 *
 * Every other rule here guards something the compiler could have guarded. This one guards what it
 * cannot see: a playback kind is the prefix of an engine source id held by a media service that
 * outlives the app, and a namespace is a key on disk. Rename one and everything stays green while
 * installed copies lose their place.
 */
class WireFormatTest {

    private fun policy(vararg pins: Pair<String, String>) = CoreModulePolicy(
        responsibility = "Own sound metadata and the sound playback policy.",
        featureAccessible = true,
        dependencies = emptySet(),
        publicInterfaces = setOf("SoundPort"),
        wireFormat = pins.toMap(),
    )

    @Test
    fun aPinnedValueThatNoLongerMatchesIsReported() {
        val violations = FeatureFirstRules.wireFormatViolations(
            listOf(source("""const val SOUND_PLAYBACK_KIND: String = "sounds"""")),
            mapOf(":core:sound" to policy("SOUND_PLAYBACK_KIND" to "sound")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("already stored on devices"))
        assertTrue(violations.single().contains("sounds"))
    }

    @Test
    fun aValueThatStillMatchesItsPinIsAccepted() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.wireFormatViolations(
                listOf(source("""const val SOUND_PLAYBACK_KIND: String = "sound"""")),
                mapOf(":core:sound" to policy("SOUND_PLAYBACK_KIND" to "sound")),
            ),
        )
    }

    /** Deleting the constant and leaving the pin behind is the quiet half of the same mistake. */
    @Test
    fun aPinWithNoConstantIsReported() {
        val violations = FeatureFirstRules.wireFormatViolations(
            listOf(source("""const val SOMETHING_ELSE: String = "x"""")),
            mapOf(":core:sound" to policy("SOUND_PLAYBACK_KIND" to "sound")),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("declares no such constant"))
    }

    /**
     * A new content type's kind joins by being named, so nobody has to remember a registry. This is
     * what makes the rule survive a module that did not exist when it was written.
     */
    @Test
    fun anUnpinnedNamespaceOrKindConstantIsReported() {
        val violations = FeatureFirstRules.wireFormatViolations(
            listOf(
                source(
                    """
                    const val MEDITATION_PLAYBACK_KIND: String = "meditation"
                    const val MEDITATION_CACHE_NAMESPACE: String = "meditation"
                    """.trimIndent(),
                ),
            ),
            mapOf(":core:sound" to policy()),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.any { it.contains("MEDITATION_PLAYBACK_KIND") })
        assertTrue(violations.any { it.contains("MEDITATION_CACHE_NAMESPACE") })
    }

    /** Ordinary constants are not storage; only the two suffixes claim to leave the process. */
    @Test
    fun anOrdinaryConstantNeedsNoPin() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.wireFormatViolations(
                listOf(source("""const val DEFAULT_TITLE: String = "Sleep Sounds"""")),
                mapOf(":core:sound" to policy()),
            ),
        )
    }

    /** A commented-out constant is not a constant; `commentsRemoved` is what sees that. */
    @Test
    fun aCommentedOutConstantIsIgnored() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.wireFormatViolations(
                listOf(source("""// const val GONE_KIND: String = "gone"""")),
                mapOf(":core:sound" to policy()),
            ),
        )
    }

    /**
     * The parser refuses a malformed pin rather than reading it as an unpinned value, so a typo in
     * the policy cannot silently switch the guard off.
     */
    @Test
    fun aMalformedPinFailsTheParse() {
        val result = parseCoreModulePolicy(
            ":core:sound",
            """
            responsibility=Own sound metadata.
            featureAccessible=true
            dependencies=
            publicInterfaces=SoundPort
            wireFormat=SOUND_PLAYBACK_KIND
            """.trimIndent(),
        )

        assertEquals(null, result.policy)
        assertTrue(result.violations.single().contains("must read NAME=value"))
    }

    @Test
    fun aPinWithNoValueFailsTheParse() {
        val result = parseCoreModulePolicy(
            ":core:sound",
            """
            responsibility=Own sound metadata.
            featureAccessible=true
            dependencies=
            publicInterfaces=SoundPort
            wireFormat=SOUND_PLAYBACK_KIND=
            """.trimIndent(),
        )

        assertEquals(null, result.policy)
        assertTrue(result.violations.single().contains("stored on devices"))
    }

    private fun source(body: String) = FeatureFirstRules.CoreSource(
        path = "core/sound/src/commonMain/kotlin/SoundPlayback.kt",
        module = ":core:sound",
        packageName = "com.xwab.app.core.sound.port",
        source = "package com.xwab.app.core.sound.port\n$body",
    )
}
