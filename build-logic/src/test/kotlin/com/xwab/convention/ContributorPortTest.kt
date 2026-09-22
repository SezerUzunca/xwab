package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Answering a capability's contract and calling it are different roles.
 *
 * The session publishes both: `PlaybackItemResolver` for the content modules that implement it, and
 * `PlaybackPort` for the screens that drive it. A content module holding both would ask the session
 * to play the very thing it was asked to resolve — acyclic in Gradle, backwards in meaning, and
 * invisible to every other rule here.
 */
class ContributorPortTest {

    private val policies = mapOf(
        ":core:session" to CoreModulePolicy(
            responsibility = "Coordinate playback through contributed resolvers.",
            featureAccessible = true,
            dependencies = emptySet(),
            publicInterfaces = setOf("PlaybackPort", "PlaybackItemResolver"),
            adapterOnlyTypes = setOf("PlaybackItemResolver", "ItemResolution", "PlaybackPolicy"),
        ),
    )

    private val sessionPort = source("session", ".port", """
        fun interface PlaybackItemResolver {
            suspend fun resolve(value: String): ItemResolution
        }
        data class PlaybackPolicy(val defaultLooping: Boolean)
        sealed interface ItemResolution {
            data class Resolved(val uri: String, val policy: PlaybackPolicy) : ItemResolution
        }
        interface PlaybackPort
    """.trimIndent())

    @Test
    fun aContributorThatAlsoCallsTheCapabilityIsReported() {
        val violations = FeatureFirstRules.contributorPortViolations(
            listOf(
                sessionPort,
                source("sound", "", """
                    import com.xwab.app.core.session.port.ItemResolution
                    import com.xwab.app.core.session.port.PlaybackItemResolver
                    import com.xwab.app.core.session.port.PlaybackPort

                    internal class SoundPlaybackResolver(private val playback: PlaybackPort) :
                        PlaybackItemResolver
                """.trimIndent()),
            ),
            policies,
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains(":core:sound"))
        assertTrue(violations.single().contains("PlaybackPort"))
    }

    /** The whole arrangement today: contributors reach the contract and nothing else. */
    @Test
    fun aContributorThatOnlyImplementsTheContractIsAccepted() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.contributorPortViolations(
                listOf(
                    sessionPort,
                    source("sound", "", """
                        import com.xwab.app.core.session.port.ItemResolution
                        import com.xwab.app.core.session.port.PlaybackItemResolver
                        import com.xwab.app.core.session.port.PlaybackPolicy

                        internal class SoundPlaybackResolver : PlaybackItemResolver
                    """.trimIndent()),
                ),
                policies,
            ),
        )
    }

    /**
     * A module that never implements the contract is an ordinary consumer, and `PlaybackPort` is
     * exactly what it is published for.
     */
    @Test
    fun aPlainConsumerMayCallTheCapability() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.contributorPortViolations(
                listOf(
                    sessionPort,
                    source("widget", "", """
                        import com.xwab.app.core.session.port.PlaybackPort

                        internal class Widget(private val playback: PlaybackPort)
                    """.trimIndent()),
                ),
                policies,
            ),
        )
    }

    /** The owner declares both roles, so it is the one module allowed to hold both. */
    @Test
    fun theOwningModuleIsNotItsOwnContributor() {
        assertEquals(
            emptyList(),
            FeatureFirstRules.contributorPortViolations(
                listOf(
                    sessionPort,
                    source("session", "", """
                        import com.xwab.app.core.session.port.PlaybackItemResolver
                        import com.xwab.app.core.session.port.PlaybackPort

                        internal class DefaultPlaybackAdapter(
                            private val resolvers: Map<String, PlaybackItemResolver>,
                        ) : PlaybackPort
                    """.trimIndent()),
                ),
                policies,
            ),
        )
    }

    /** A wildcard import reaches the same types, so it is the same violation. */
    @Test
    fun aWildcardImportDoesNotEscapeTheRule() {
        val violations = FeatureFirstRules.contributorPortViolations(
            listOf(
                sessionPort,
                source("sound", "", """
                    import com.xwab.app.core.session.port.*

                    internal class SoundPlaybackResolver : PlaybackItemResolver
                """.trimIndent()),
            ),
            policies,
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("PlaybackPort"))
    }

    /**
     * Nothing to separate when a module publishes one role only. Without this the rule would have
     * to be told which capabilities it applies to, which is the list it exists to avoid.
     */
    @Test
    fun aCapabilityWithoutAdapterOnlyTypesIsUntouched() {
        val plain = mapOf(
            ":core:favorites" to CoreModulePolicy(
                responsibility = "Persist namespaced favorite identifiers.",
                featureAccessible = true,
                dependencies = emptySet(),
                publicInterfaces = setOf("FavoritesPort"),
            ),
        )

        assertEquals(
            emptyList(),
            FeatureFirstRules.contributorPortViolations(
                listOf(
                    source("favorites", ".port", "interface FavoritesPort"),
                    source("sound", "", """
                        import com.xwab.app.core.favorites.port.FavoritesPort

                        internal class SoundFavorites(private val favorites: FavoritesPort)
                    """.trimIndent()),
                ),
                plain,
            ),
        )
    }

    private fun source(module: String, suffix: String, body: String) = FeatureFirstRules.CoreSource(
        path = "core/$module/src/commonMain/kotlin/${if (suffix.isEmpty()) "Adapter" else "Contract"}.kt",
        module = ":core:$module",
        packageName = "com.xwab.app.core.$module$suffix",
        source = "package com.xwab.app.core.$module$suffix\n$body",
    )
}
