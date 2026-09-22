package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdapterOnlyTypesTest {
    private val policy = CoreModulePolicy(
        responsibility = "Coordinate playback through contributed resolvers.",
        featureAccessible = true,
        dependencies = emptySet(),
        publicInterfaces = setOf("PlaybackPort", "PlaybackItemResolver"),
        adapterOnlyTypes = setOf("PlaybackItemResolver", "ItemResolution", "PlaybackPolicy"),
    )
    private val policies = mapOf(":core:session" to policy)
    private val reserved = source("session", ".port", """
        fun interface PlaybackItemResolver {
            suspend fun resolve(value: String): ItemResolution
        }
        data class PlaybackPolicy(val defaultLooping: Boolean)
        sealed interface ItemResolution {
            data class Resolved(val uri: String, val policy: PlaybackPolicy) : ItemResolution
        }
    """.trimIndent())

    @Test
    fun featuresCannotImportAliasQualifyOrWildcardAdapterOnlyTypes() {
        val references = listOf(
            "import com.xwab.app.core.session.port.PlaybackItemResolver",
            "import com.xwab.app.core.session.port.PlaybackItemResolver as Resolver",
            "import com.xwab.app.core.session.port.ItemResolution.Resolved",
            "import com.xwab.app.core.session.port.ItemResolution.Resolved as Result",
            "import com.xwab.app.core.session.port.ItemResolution.*",
            "import com.xwab.app.core.session.port.*",
            "internal fun draw(value: com.xwab.app.core.session.port.ItemResolution.Resolved) = Unit",
            "internal val policy = com.xwab.app.core.session.port.PlaybackPolicy(true)",
        )
        references.forEach { reference ->
            val violations = check(feature = "package com.xwab.app.feature.sample\n$reference")
            assertEquals(1, violations.size, reference)
            assertTrue(violations.single().contains("adapter-only port types"), reference)
        }
    }

    @Test
    fun consumerPortsAndInternalContributorsRemainAccessible() {
        val contributor = source("sound", "", """
            import com.xwab.app.core.session.port.PlaybackItemResolver
            import com.xwab.app.core.session.port.ItemResolution
            internal class SoundPlaybackResolver : PlaybackItemResolver {
                override suspend fun resolve(value: String): ItemResolution = TODO()
            }
        """.trimIndent())
        assertEquals(emptyList(), check(
            extra = listOf(contributor),
            feature = """
                package com.xwab.app.feature.sample
                import com.xwab.app.core.session.port.PlaybackPort
                import com.xwab.app.core.session.port.PlaybackSummary
                internal class Screen(private val playback: PlaybackPort)
            """.trimIndent(),
        ))
        assertEquals(emptyList(), check())
    }

    @Test
    fun otherCorePortsCannotReexportAdapterOnlyTypes() {
        val attempts = listOf(
            """
                import com.xwab.app.core.session.port.ItemResolution
                typealias PublicResult = ItemResolution
            """.trimIndent(),
            """
                import com.xwab.app.core.session.port.ItemResolution as Result
                interface SoundPort { fun result(): Result }
            """.trimIndent(),
            "interface SoundPort { fun result(): com.xwab.app.core.session.port.ItemResolution.Resolved }",
            """
                import com.xwab.app.core.session.port.*
                interface SoundPort { fun result(): ItemResolution }
            """.trimIndent(),
        )
        attempts.forEach { attempt ->
            assertEquals(1, check(extra = listOf(source("sound", ".port", attempt))).size, attempt)
        }
    }

    @Test
    fun ownersOrdinaryPortsCannotLeakMarkedTypesThroughSamePackageReferences() {
        listOf(
            "typealias PublicResult = ItemResolution",
            "interface PlaybackPort { fun result(): ItemResolution.Resolved }",
            "data class PlaybackSummary(val policy: PlaybackPolicy)",
        ).forEach { attempt ->
            assertEquals(1, check(extra = listOf(source("session", ".port", attempt))).size, attempt)
        }
        val mixed = reserved.copy(source = reserved.source + "\ntypealias PublicResult = ItemResolution")
        val violations = FeatureFirstRules.adapterOnlyTypeViolations(listOf(mixed), emptyMap(), policies)
        assertTrue(violations.single().contains("mixes adapter-only types"))
    }

    @Test
    fun missingOrMovedPolicyTypesFailInsteadOfLeavingAnInactiveRestriction() {
        val missing = policy.copy(adapterOnlyTypes = policy.adapterOnlyTypes + "RemovedType")
        assertTrue(FeatureFirstRules.adapterOnlyTypeViolations(
            listOf(reserved), emptyMap(), mapOf(":core:session" to missing),
        ).single().contains("no owned top-level port type"))
        val implementation = source("session", "", "internal class RemovedType")
        assertEquals(1, FeatureFirstRules.adapterOnlyTypeViolations(
            listOf(reserved, implementation), emptyMap(), mapOf(":core:session" to missing),
        ).size)
    }

    @Test
    fun commentsAndStringLiteralsAreNotReferencesButPackageSpoofingIsRejected() {
        assertEquals(emptyList(), check(feature = """
            package com.xwab.app.feature.sample
            // import com.xwab.app.core.session.port.ItemResolution
            internal val help = "com.xwab.app.core.session.port.PlaybackItemResolver"
            internal val docs = """ + "\"\"\"com.xwab.app.core.session.port.PlaybackPolicy\"\"\"",
        ))
        assertEquals(1, check(feature = """
            package com.xwab.app.core.session.port
            internal val bypass: ItemResolution? = null
        """.trimIndent()).size)
    }

    private fun check(
        extra: List<FeatureFirstRules.CoreSource> = emptyList(),
        feature: String? = null,
    ) = FeatureFirstRules.adapterOnlyTypeViolations(
        coreSources = listOf(reserved) + extra,
        featureSources = feature?.let { mapOf("feature/sample/src/commonMain/kotlin/Screen.kt" to it) }.orEmpty(),
        policies = policies,
    )

    private fun source(module: String, suffix: String, body: String) = FeatureFirstRules.CoreSource(
        path = "core/$module/src/commonMain/kotlin/${if (suffix.isEmpty()) "Adapter" else "Contract"}.kt",
        module = ":core:$module",
        packageName = "com.xwab.app.core.$module$suffix",
        source = "package com.xwab.app.core.$module$suffix\n$body",
    )
}
