package com.xwab.convention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The format each module reports its own dependencies in.
 *
 * `checkArchitecture` reads the whole graph through this, so a report it misreads is every rule
 * that looks at that module silently skipped. Anything unrecognised fails instead.
 */
class ModuleDependencyReportTest {

    @Test
    fun aReportReadsBackAsWritten() {
        val report = ModuleDependencyReport(
            module = ":feature:sound",
            dependencies = listOf(":core:favorites", ":core:session", ":core:sound"),
            apiDependencies = listOf(":core:sound"),
        )

        assertEquals(report, ModuleDependencyReport.parse(report.format()))
    }

    @Test
    fun aModuleWithNoDependenciesStillNamesItself() {
        val report = ModuleDependencyReport(":core:network", emptyList(), emptyList())

        assertEquals(report, ModuleDependencyReport.parse(report.format()))
    }

    @Test
    fun aReportThatDoesNotSayWhoWroteItIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            ModuleDependencyReport.parse("dependency :core:sound\n")
        }
    }

    @Test
    fun aReportNamingTwoModulesIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            ModuleDependencyReport.parse("module :core:sound\nmodule :core:story\n")
        }
    }

    @Test
    fun anUnknownLineIsRefusedRatherThanSkipped() {
        assertFailsWith<IllegalArgumentException> {
            ModuleDependencyReport.parse("module :core:sound\nimplementation :core:session\n")
        }
    }

    @Test
    fun aLineWithoutAProjectPathIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            ModuleDependencyReport.parse("module :core:sound\ndependency core-session\n")
        }
    }

    @Test
    fun theGraphsKeepDeclaredAndReExportedEdgesApart() {
        val (graph, apiGraph) = ModuleDependencyReport.graphsOf(
            listOf(
                ModuleDependencyReport(":feature:story", listOf(":core:story", ":core:session"), emptyList()),
                ModuleDependencyReport(":core:story", listOf(":core:session"), emptyList()),
                ModuleDependencyReport(":testing:sound", listOf(":core:sound"), listOf(":core:sound")),
            ),
        )

        assertEquals(
            mapOf(
                ":feature:story" to listOf(":core:session", ":core:story"),
                ":core:story" to listOf(":core:session"),
                ":testing:sound" to listOf(":core:sound"),
            ),
            graph,
        )
        assertEquals(
            mapOf(
                ":feature:story" to emptyList(),
                ":core:story" to emptyList(),
                ":testing:sound" to listOf(":core:sound"),
            ),
            apiGraph,
        )
    }

    @Test
    fun aModuleReportingTwiceIsRefusedRatherThanMerged() {
        assertFailsWith<IllegalArgumentException> {
            ModuleDependencyReport.graphsOf(
                listOf(
                    ModuleDependencyReport(":core:sound", listOf(":core:session"), emptyList()),
                    ModuleDependencyReport(":core:sound", listOf(":core:delivery"), emptyList()),
                ),
            )
        }
    }
}
