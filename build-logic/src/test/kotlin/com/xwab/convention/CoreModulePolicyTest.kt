package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreModulePolicyTest {
    private val policyText = """
        # The contract belongs to the installed module.
        responsibility=Own a sample capability.
        featureAccessible=true
        dependencies=
        publicInterfaces=SamplePort
    """.trimIndent()

    @Test
    fun everyNewCapabilityRequiresAnExplicitCompletePolicy() {
        val valid = parseCoreModulePolicy(":core:sample", policyText)
        assertEquals(emptyList(), valid.violations)
        val policy = assertNotNull(valid.policy)
        assertEquals(emptySet(), policy.dependencies)
        assertEquals(setOf("SamplePort"), policy.publicInterfaces)
        assertEquals(emptySet(), policy.adapterOnlyTypes)
        assertTrue(policy.featureAccessible)

        val missing = parseCoreModulePolicy(":core:sample", null)
        assertNull(missing.policy)
        assertTrue(missing.violations.single().contains("core/sample/architecture.properties"))
        listOf("responsibility", "featureAccessible", "dependencies", "publicInterfaces").forEach { field ->
            val incomplete = parseCoreModulePolicy(":core:sample", policyText.lines()
                .filterNot { it.startsWith("$field=") }.joinToString("\n"))
            assertNull(incomplete.policy, field)
            assertTrue(incomplete.violations.any { it.contains("must declare $field") }, field)
        }
    }

    @Test
    fun adapterOnlyTypesIsAnOptionalValidatedSetOfTopLevelTypeNames() {
        val declared = parseCoreModulePolicy(":core:sample", "$policyText\nadapterOnlyTypes=SamplePort,Result")
        assertEquals(setOf("SamplePort", "Result"), assertNotNull(declared.policy).adapterOnlyTypes)
        assertEquals(emptyList(), declared.violations)
        assertEquals(emptySet(), assertNotNull(parseCoreModulePolicy(
            ":core:sample", "$policyText\nadapterOnlyTypes=",
        ).policy).adapterOnlyTypes)
        listOf("sample name", "Result.Nested", "Result,", "Result,Result").forEach { value ->
            assertNull(parseCoreModulePolicy(":core:sample", "$policyText\nadapterOnlyTypes=$value").policy, value)
        }
        assertNull(parseCoreModulePolicy(":core:sample",
            "$policyText\nadapterOnlyTypes=Result\nadapterOnlyTypes=SamplePort").policy)
    }

    @Test
    fun malformedPoliciesNeverFallBackToPermissiveDefaults() {
        val invalid = listOf(
            "$policyText\nfeatureAcessible=true",
            "$policyText\nfeatureAccessible=false",
            policyText.replace("featureAccessible=true", "featureAccessible=yes"),
            policyText.replace("responsibility=Own a sample capability.", "responsibility="),
            policyText.replace("dependencies=", "dependencies=:feature:screen"),
            policyText.replace("dependencies=", "dependencies=:core:sample"),
            policyText.replace("dependencies=", "dependencies=:core:other,:core:other"),
            policyText.replace("dependencies=", "dependencies=:core:other,"),
            policyText.replace("publicInterfaces=SamplePort", "publicInterfaces="),
            policyText.replace("publicInterfaces=SamplePort", "publicInterfaces=sample port"),
        )
        invalid.forEach { source ->
            val result = parseCoreModulePolicy(":core:sample", source)
            assertNull(result.policy, source)
            assertTrue(result.violations.isNotEmpty(), source)
        }
    }

    @Test
    fun installingAndRemovingCapabilitiesDoesNotChangeCentralRules() {
        val module = ":core:meditation"
        val policy = assertNotNull(parseCoreModulePolicy(module,
            policyText.replace("SamplePort", "MeditationPort")).policy)
        val policies = mapOf(module to policy)
        assertEquals(emptyList(), FeatureFirstRules.corePolicyViolations(setOf(module), policies))
        assertEquals(emptyList(), FeatureFirstRules.dependencyViolations(
            mapOf(":feature:meditation" to listOf(module), module to emptyList()), policies = policies))
        assertEquals(emptyList(), FeatureFirstRules.corePolicyViolations(emptySet(), emptyMap()))

        val staleConsumer = policy.copy(dependencies = setOf(":core:retired"))
        val violations = FeatureFirstRules.corePolicyViolations(setOf(module), mapOf(module to staleConsumer))
        assertTrue(violations.single().contains("absent dependency :core:retired"))
    }

    @Test
    fun everyCorePolicyEnforcesItsEntireDependencyBoundary() {
        val module = ":core:sample"
        val independent = assertNotNull(parseCoreModulePolicy(module, policyText).policy)
        listOf(":core:other", ":testing", ":androidApp").forEach { dependency ->
            val violations = FeatureFirstRules.dependencyViolations(
                mapOf(module to listOf(dependency)), policies = mapOf(module to independent))
            assertTrue(violations.single().contains("is not among them"), dependency)
        }
        assertEquals(emptyList(), FeatureFirstRules.dependencyViolations(
            mapOf(module to listOf(":core:other")),
            policies = mapOf(module to independent.copy(dependencies = setOf(":core:other"))),
        ))
        assertTrue(FeatureFirstRules.dependencyViolations(
            mapOf(":feature:sample" to listOf(module)), policies = emptyMap(),
        ).single().contains("no valid architecture.properties"))
    }

    @Test
    fun dependencyCyclesAndSelfDependenciesAreRejectedButSharedInfrastructureIsAccepted() {
        val cycle = mapOf(
            ":core:a" to listOf(":core:b"),
            ":core:b" to listOf(":core:c"),
            ":core:c" to listOf(":core:a"),
        )
        assertEquals(listOf("Core capability dependencies must be acyclic: :core:a -> :core:b -> :core:c -> :core:a."),
            FeatureFirstRules.coreDependencyCycleViolations(cycle))
        assertEquals(listOf("Core capability dependencies must be acyclic: :core:a -> :core:a."),
            FeatureFirstRules.coreDependencyCycleViolations(mapOf(":core:a" to listOf(":core:a"))))
        assertEquals(emptyList(), FeatureFirstRules.coreDependencyCycleViolations(mapOf(
            ":core:a" to listOf(":core:transport"),
            ":core:b" to listOf(":core:transport"),
            ":core:transport" to emptyList(),
        )))
    }

    @Test
    fun productionGraphExcludesEveryPlatformTestConfiguration() {
        listOf("commonMainImplementation", "androidMainApi", "iosArm64MainImplementation", "implementation", "latestMainApi")
            .forEach { assertTrue(FeatureFirstRules.isProductionConfiguration(it), it) }
        listOf("commonTestImplementation", "androidHostTestApi", "androidDeviceTestImplementation", "iosTestApi", "testFixturesApi")
            .forEach { assertEquals(false, FeatureFirstRules.isProductionConfiguration(it), it) }
    }

    @Test
    fun modulesOwnOnlyTheirFlatCapabilityNamespace() {
        assertEquals(emptyList(), FeatureFirstRules.coreModuleShapeViolations(setOf(":core", ":core:sleep-timer")))
        assertEquals(1, FeatureFirstRules.coreModuleShapeViolations(setOf(":core:sample:api")).size)
        val source = source("", "internal class Hidden")
        assertEquals(emptyList(), FeatureFirstRules.corePackageOwnershipViolations(listOf(source)))
        assertEquals(1, FeatureFirstRules.corePackageOwnershipViolations(listOf(
            source.copy(packageName = "com.xwab.app.core.other.port"),
        )).size)
    }

    @Test
    fun publicPortsCannotExposeTheirOwnImplementationDetails() {
        val implementation = source("", "internal class Hidden")
        val contract = source(".port", """
            import com.xwab.app.core.sample.Hidden
            interface SamplePort {
                fun read(): Hidden
            }
        """.trimIndent())
        assertTrue(FeatureFirstRules.coreImportViolations(listOf(implementation, contract))
            .single().contains("including their own"))
        val qualified = source(".port", "interface SamplePort { fun read(): com.xwab.app.core.sample.Hidden }")
        assertEquals(1, FeatureFirstRules.coreImportViolations(listOf(implementation, qualified)).size)
        val helper = source(".adapter", """
            import com.xwab.app.core.sample.Hidden
            internal class Adapter(val hidden: Hidden)
        """.trimIndent())
        assertEquals(emptyList(), FeatureFirstRules.coreImportViolations(listOf(implementation, helper)))
    }

    @Test
    fun contentMayResolveItsPrivateSourceWithoutPublishingAnotherCapability() {
        val policy = assertNotNull(parseCoreModulePolicy(":core:sample", policyText).policy)
        val policies = mapOf(":core:sample" to policy)
        val metadataPort = source(".port", "interface SamplePort")
        val privateSource = source("", "internal data class SampleSource(val httpsUrl: String)")
        val resolver = source(".playback", """
            import com.xwab.app.core.sample.SampleSource
            internal class SamplePlaybackResolver(private val source: SampleSource)
        """.trimIndent())
        val sources = listOf(metadataPort, privateSource, resolver)

        assertEquals(emptyList(), FeatureFirstRules.coreImportViolations(sources))
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(sources, policies))
        assertEquals(emptyList(), FeatureFirstRules.corePortViolations(sources, policies))
        assertEquals(emptyList(), FeatureFirstRules.dependencyViolations(
            mapOf(":core:sample" to emptyList()), policies = policies,
        ))
        assertEquals(1, FeatureFirstRules.coreVisibilityViolations(listOf(
            privateSource.copy(source = privateSource.source.replace("internal data", "data")),
        ), policies).size)
    }

    @Test
    fun everyCapabilityDeclaresItsExactPublicInterfacesIncludingContributionContracts() {
        val policy = assertNotNull(parseCoreModulePolicy(":core:sample", policyText
            .replace("SamplePort", "SamplePort,SampleContribution")).policy)
        val policies = mapOf(":core:sample" to policy)
        val declarations = source(".port", """
            interface SamplePort
            fun interface SampleContribution
            sealed interface Result
        """.trimIndent())
        assertEquals(emptyList(), FeatureFirstRules.corePortViolations(listOf(declarations), policies))
        assertEquals(emptyList(), FeatureFirstRules.coreVisibilityViolations(listOf(declarations), policies))
        assertEquals(1, FeatureFirstRules.corePortViolations(emptyList(), policies).size)
        assertEquals(1, FeatureFirstRules.corePortViolations(listOf(declarations.copy(
            source = declarations.source + "\ninterface UnownedPort")), policies).size)
    }

    private fun source(suffix: String, body: String) = FeatureFirstRules.CoreSource(
        path = "core/sample/src/commonMain/kotlin/Test.kt",
        module = ":core:sample",
        packageName = "com.xwab.app.core.sample$suffix",
        source = "package com.xwab.app.core.sample$suffix\n$body",
    )
}
