package com.xwab.convention

/**
 * One module's own project dependencies, as that module reports them to `checkArchitecture`.
 *
 * The check used to collect these itself: once every project was evaluated, the root walked each
 * subproject's configurations. That is one project reading another's mutable state, which Gradle's
 * isolated projects mode forbids. Each module now writes its own edges to a file and the root
 * resolves those files as ordinary dependencies, so nothing crosses a project boundary except a
 * task output.
 *
 * Plain lines rather than a serialization format: `build-logic` has no serialization library, and
 * three kinds of line need none.
 *
 * ```
 * module :feature:sound
 * dependency :core:sound
 * api :core:sound
 * ```
 *
 * @param dependencies every project this module declares in a production configuration.
 * @param apiDependencies the subset declared in `api` configurations, which travel onward to
 *   whatever depends on this module.
 */
internal data class ModuleDependencyReport(
    val module: String,
    val dependencies: List<String>,
    val apiDependencies: List<String>,
) {
    fun format(): String = buildString {
        appendLine("$MODULE $module")
        dependencies.forEach { appendLine("$DEPENDENCY $it") }
        apiDependencies.forEach { appendLine("$API $it") }
    }

    companion object {
        private const val MODULE = "module"
        private const val DEPENDENCY = "dependency"
        private const val API = "api"

        /** Fails on anything it does not recognise: a report the check misreads is a rule skipped. */
        fun parse(text: String): ModuleDependencyReport {
            var module: String? = null
            val dependencies = mutableListOf<String>()
            val apiDependencies = mutableListOf<String>()

            text.lineSequence().map(String::trim).filter(String::isNotEmpty).forEach { line ->
                val kind = line.substringBefore(' ')
                val path = line.substringAfter(' ', missingDelimiterValue = "").trim()
                require(path.startsWith(":")) { "Malformed architecture report line: $line" }
                when (kind) {
                    MODULE -> {
                        require(module == null) { "An architecture report names more than one module: $line" }
                        module = path
                    }
                    DEPENDENCY -> dependencies += path
                    API -> apiDependencies += path
                    else -> throw IllegalArgumentException("Unknown architecture report line: $line")
                }
            }

            return ModuleDependencyReport(
                module = requireNotNull(module) { "An architecture report does not say which module wrote it." },
                dependencies = dependencies,
                apiDependencies = apiDependencies,
            )
        }

        /**
         * The two graphs `checkArchitecture` reads: what each module declares, and what it
         * re-exports. A module reporting twice is refused rather than merged, since one of the two
         * reports would otherwise be silently ignored.
         */
        fun graphsOf(
            reports: List<ModuleDependencyReport>,
        ): Pair<Map<String, List<String>>, Map<String, List<String>>> {
            val repeated = reports.groupBy { it.module }.filterValues { it.size > 1 }.keys
            require(repeated.isEmpty()) { "Modules reported their dependencies more than once: ${repeated.sorted()}" }
            return reports.associate { it.module to it.dependencies.distinct().sorted() } to
                reports.associate { it.module to it.apiDependencies.distinct().sorted() }
        }
    }
}
