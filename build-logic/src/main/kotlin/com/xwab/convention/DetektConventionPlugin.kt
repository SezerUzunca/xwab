package com.xwab.convention

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `xwab.detekt` — Kotlin static analysis for one module, and the two tasks CI calls it through.
 *
 * Applied by `xwab.kmp.library`, and by id in `:shared` and `:androidApp`, which configure
 * themselves. detekt registers a task per Android compilation — `main` reads `commonMain` and
 * `androidMain` together with the compile classpath, so rules that need types see them — and one
 * per source set without types. Both kinds run. Its plain `detekt` task reads `src/main/kotlin`,
 * which no module here has, so it is left out rather than reporting an empty success.
 *
 * Findings that predate this plugin live in each module's `detekt-baseline-<task>.xml`, recorded
 * by CI with `staticAnalysisBaseline`. A new finding fails the build; an old one fails only once
 * the code it points at changes enough to stop matching, which is when it is cheapest to fix.
 */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")
            extensions.configure(DetektExtension::class.java) { detekt ->
                // detekt's defaults, adjusted only where `config/detekt/detekt.yml` says so. Read
                // through the isolated root, which is all a project may see of another.
                detekt.buildUponDefaultConfig.set(true)
                detekt.config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml"))
                detekt.parallel.set(true)
                // Each compilation reads `detekt-baseline-<compilation>.xml` beside this.
                detekt.baseline.set(layout.projectDirectory.file("detekt-baseline.xml"))
            }

            // A compilation's sources include what plugins generate into `build/` — Compose's
            // resource accessors among them. Nobody writes that code, so nobody can fix a finding
            // in it; analysing it only teaches the baseline to hide generator output.
            tasks.withType(Detekt::class.java).configureEach { task ->
                task.exclude { element -> "/build/" in element.file.invariantSeparatorsPath }
            }
            tasks.withType(DetektCreateBaselineTask::class.java).configureEach { task ->
                task.exclude { element -> "/build/" in element.file.invariantSeparatorsPath }
            }

            tasks.register(STATIC_ANALYSIS_TASK) { task ->
                task.group = "verification"
                task.description = "Runs detekt over every Kotlin compilation of this module."
                task.dependsOn(tasks.withType(Detekt::class.java).matching { it.name != PLAIN_DETEKT_TASK })
            }
            tasks.register(STATIC_ANALYSIS_BASELINE_TASK) { task ->
                task.group = "verification"
                task.description = "Records this module's current detekt findings as its baseline."
                task.dependsOn(
                    tasks.withType(DetektCreateBaselineTask::class.java)
                        .matching { it.name != PLAIN_DETEKT_BASELINE_TASK },
                )
            }
        }
    }

    private companion object {
        const val STATIC_ANALYSIS_TASK = "staticAnalysis"
        const val STATIC_ANALYSIS_BASELINE_TASK = "staticAnalysisBaseline"
        const val PLAIN_DETEKT_TASK = "detekt"
        const val PLAIN_DETEKT_BASELINE_TASK = "detektBaseline"
    }
}
