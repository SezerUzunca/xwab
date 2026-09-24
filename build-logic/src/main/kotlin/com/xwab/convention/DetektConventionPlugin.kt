package com.xwab.convention

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskCollection

/**
 * `xwab.detekt` — Kotlin static analysis for one module, and the two tasks CI calls it through.
 *
 * Applied by `xwab.kmp.library`, and by id in `:shared` and `:androidApp`, which configure
 * themselves. detekt registers a task per Android compilation — `main` reads `commonMain` and
 * `androidMain` together with the compile classpath, so rules that need types see them — and one
 * per source set without types. A typed run applies every rule an untyped one would, so a
 * source-set task whose files a typed task already reads only repeats that analysis: it used to be
 * a third of detekt's time here, and every finding it matched was baselined twice. Those are left
 * out; one that reads something no compilation compiles — `iosMain`, where no Android compilation
 * reaches — still runs. Its plain `detekt` task reads `src/main/kotlin`, which no module here has,
 * so it is left out rather than reporting an empty success.
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
                task.dependsOn(analysisTasks(Detekt::class.java, PLAIN_DETEKT_TASK))
            }
            tasks.register(STATIC_ANALYSIS_BASELINE_TASK) { task ->
                task.group = "verification"
                task.description = "Records this module's current detekt findings as its baseline."
                task.dependsOn(analysisTasks(DetektCreateBaselineTask::class.java, PLAIN_DETEKT_BASELINE_TASK))
            }
        }
    }

    /**
     * The [type] tasks worth running: every one except [plainTask], and except a source-set task
     * whose every file some compilation task already reads. Decided by the files themselves rather
     * than by matching names, so it holds for the Android app's variants and for Kotlin
     * Multiplatform's source sets alike, and a source set only iOS compiles is never dropped by a
     * build that has no iOS compilation to cover it.
     */
    private fun <T : SourceTask> Project.analysisTasks(type: Class<T>, plainTask: String): TaskCollection<T> {
        val candidates = tasks.withType(type).matching { it.name != plainTask }
        val readWithTypes by lazy {
            candidates.filterNot { it.name.endsWith(SOURCE_SET_SUFFIX) }.flatMapTo(HashSet()) { it.source.files }
        }
        return candidates.matching { task ->
            !task.name.endsWith(SOURCE_SET_SUFFIX) || !readWithTypes.containsAll(task.source.files)
        }
    }

    private companion object {
        const val STATIC_ANALYSIS_TASK = "staticAnalysis"
        const val STATIC_ANALYSIS_BASELINE_TASK = "staticAnalysisBaseline"
        const val PLAIN_DETEKT_TASK = "detekt"
        const val PLAIN_DETEKT_BASELINE_TASK = "detektBaseline"

        /** detekt's name for the tasks it registers per source set, which run without types. */
        const val SOURCE_SET_SUFFIX = "SourceSet"
    }
}
