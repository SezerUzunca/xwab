package com.xwab.convention

/** Pure architecture rules, separated from Gradle plumbing so every rule is unit-testable. */
internal object FeatureFirstRules {
    const val CORE_PREFIX = ":core:"
    const val FEATURE_PREFIX = ":feature:"

    val USE_CASE_DECLARATION =
        Regex("""^\s*(?:internal\s+|public\s+)?class\s+(\w+UseCase)\b""", RegexOption.MULTILINE)

    private val PACKAGE = Regex(
        """^\s*package\s+([A-Za-z0-9_.]+)\s*$""",
        RegexOption.MULTILINE,
    )

    private val IMPORT = Regex(
        """^\s*import\s+([A-Za-z0-9_.*]+)(?:\s+as\s+\w+)?\s*$""",
        RegexOption.MULTILINE,
    )

    private val QUALIFIED_CORE_REFERENCE =
        Regex("""\bcom\.xwab\.app\.core(?:\.[A-Za-z_][A-Za-z0-9_]*)+""")

    private val QUALIFIED_FEATURE_REFERENCE =
        Regex("""\bcom\.xwab\.app\.feature(?:\.[A-Za-z_][A-Za-z0-9_]*)*""")

    private val FEATURE_NAVIGATION_PACKAGE =
        Regex("""com\.xwab\.app\.feature\.[A-Za-z0-9_]+\.navigation""")

    private val FEATURE_DI_PACKAGE =
        Regex("""com\.xwab\.app\.feature\.[A-Za-z0-9_]+\.di""")

    private val CORE_PORT_PACKAGE =
        Regex("""com\.xwab\.app\.core\.[a-z][A-Za-z0-9]*\.port""")

    private val KOIN_CODE_REFERENCE = Regex(
        """^\s*import\s+org\.koin\.|libs(?:\.plugins)?\.koin\b""",
        RegexOption.MULTILINE,
    )

    private val KOIN_COORDINATE = Regex("""io\.insert-koin""")

    private val DECLARATION = Regex(
        """^\s*(?:(?:@[A-Za-z_][A-Za-z0-9_.:]*(?:\([^()\r\n]*\))?)\s+)*((?:(?:public|internal|private|protected|expect|actual|open|abstract|final|override|inner|companion|suspend|inline|tailrec|operator|infix|external|lateinit|const|data|sealed|enum|value|annotation|fun)\s+)*)(class|interface|object|fun|const\s+val|val|var|typealias)(?:\s+(?:<[^>]+>\s+)?(`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_.]*))?""",
    )

    private data class SourceDeclaration(
        val lineNumber: Int,
        val match: MatchResult,
    )

    val MODULES_OFF_LIMITS_TO_FEATURES = mapOf(
        ":shared" to
            "the app shell owns navigation state and destination policy",
        ":core:network" to
            "HTTP is an adapter detail; screens read content through public ports",
        ":core:delivery" to
            "source resolution and caching belong behind PlaybackPort",
        ":core:playback" to
            "the platform engine is hidden behind PlaybackPort",
    )

    fun staleRuleViolations(modules: Set<String>): List<String> =
        (MODULES_OFF_LIMITS_TO_FEATURES.keys - modules).sorted().map { missing ->
            "The adapter-boundary rule names $missing, which is not a module in this build. Update " +
                "MODULES_OFF_LIMITS_TO_FEATURES, or the rule protects nothing."
        }

    /** Sound and story each expose one cohesive port. */
    fun contentPortViolations(sources: List<CoreSource>): List<String> =
        mapOf(":core:sound" to "SoundPort", ":core:story" to "StoryPort").flatMap { (module, expected) ->
            val moduleSources = sources.filter { it.module == module }
            if (moduleSources.isEmpty()) return@flatMap emptyList()
            val interfaces = moduleSources.flatMap { source ->
                declarations(source.source, includeNested = true).mapNotNull { parsed ->
                    val declaration = parsed.match
                    if (declaration.groupValues[2] != "interface") return@mapNotNull null
                    if (!CORE_PORT_PACKAGE.matches(source.packageName)) return@mapNotNull null
                    declaration.groupValues[3]
                }
            }
            if (interfaces == listOf(expected)) emptyList()
            else listOf("$module must expose exactly one port interface: $expected.")
        }

    /** A feature is one Gradle module; nested `api` / `impl` projects are not part of the model. */
    fun featureModuleShapeViolations(modules: Set<String>): List<String> =
        modules.filter { module ->
            module.startsWith(FEATURE_PREFIX) &&
                module.removePrefix(FEATURE_PREFIX).contains(':')
        }.sorted().map { module ->
            "$module is a nested feature project. Each feature must be exactly one " +
                ":feature:<name> module; keep Navigation 3 contracts and implementation together."
        }

    /** Neither modules nor source/package directories may recreate the old `api` / `impl` split. */
    fun legacySplitDirectoryViolations(paths: List<String>): List<String> =
        paths.map { it.replace('\\', '/') }
            .filter { path -> path.split('/').any { it == "api" || it == "impl" } }
            .distinct()
            .sorted()
            .map { path ->
                "$path recreates an api/impl split. Keep each feature cohesive and expose core " +
                    "contracts from its port package."
            }

    /** Metro is the only DI runtime in this project. */
    fun koinUsageViolations(sources: Map<String, String>): List<String> =
        sources.filter { (path, source) ->
            KOIN_CODE_REFERENCE.containsMatchIn(codeOnly(source)) ||
                (path.endsWith(".kts") || path.endsWith(".toml")) &&
                KOIN_COORDINATE.containsMatchIn(commentsRemoved(source))
        }.keys.sorted().map { path ->
            "$path references Koin. Use Metro contributions and the platform application graph."
        }

    fun dependencyViolations(
        graph: Map<String, List<String>>,
        apiEdges: Map<String, List<String>> = emptyMap(),
    ): List<String> {
        val violations = mutableListOf<String>()

        graph.forEach { (module, dependencies) ->
            dependencies.forEach { dependency ->
                if (module.startsWith(CORE_PREFIX) && dependency.startsWith(FEATURE_PREFIX)) {
                    violations += "$module depends on $dependency. A core module may not depend on a feature."
                }

                if (
                    module.startsWith(FEATURE_PREFIX) &&
                    dependency.startsWith(FEATURE_PREFIX) &&
                    featureOf(module) != featureOf(dependency)
                ) {
                    violations += "$module depends on $dependency. Feature modules must not depend " +
                        "on another feature; connect destination intents in :shared."
                }
            }

            if (module.startsWith(FEATURE_PREFIX)) {
                violations += offLimitsReachableFrom(module, dependencies, apiEdges)
            }
        }

        return violations.distinct().sorted()
    }

    private fun offLimitsReachableFrom(
        feature: String,
        directDependencies: List<String>,
        apiEdges: Map<String, List<String>>,
    ): List<String> {
        val violations = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val paths = ArrayDeque(directDependencies.map(::listOf))

        while (paths.isNotEmpty()) {
            val path = paths.removeFirst()
            val reached = path.last()
            if (!visited.add(reached)) continue

            MODULES_OFF_LIMITS_TO_FEATURES[reached]?.let { reason ->
                violations += if (path.size == 1) {
                    "$feature depends on $reached. A feature may not: $reason."
                } else {
                    "$feature reaches $reached through ${path.dropLast(1).joinToString(" -> ")}, " +
                        "whose api dependencies travel onto this feature's compile classpath. " +
                        "A feature may not: $reason."
                }
            }

            apiEdges[reached].orEmpty().forEach { paths.addLast(path + it) }
        }

        return violations
    }

    fun isApiConfiguration(configurationName: String): Boolean =
        configurationName == "api" || configurationName.endsWith("Api")

    /** Every shared source set observes the same composition, navigation and DI boundaries. */
    fun sharedFeatureReferenceViolations(sources: Map<String, String>): List<String> =
        sources.flatMap { (path, source) ->
            val code = codeOnly(source)
            val packageName = PACKAGE.find(code)?.groupValues?.get(1).orEmpty()
            val boundary = when {
                packageName.isWithin("com.xwab.app.navigation") ||
                    packageName.isWithin("com.xwab.app.composition") -> "navigation"
                packageName.isWithin("com.xwab.app.di") -> "di"
                else -> null
            }

            references(code, QUALIFIED_FEATURE_REFERENCE).mapNotNull { reference ->
                if (!reference.isWithin("com.xwab.app.feature")) return@mapNotNull null

                val target = reference.removePrefix("com.xwab.app.feature.").split('.')
                val allowed = when (boundary) {
                    "navigation" -> target.size >= 3 && target[1] == "navigation"
                    "di" -> target.size == 3 && target[1] == "di" &&
                        target[2].endsWith("Dependencies")
                    else -> false
                }
                if (allowed) return@mapNotNull null

                "$path references $reference. Shared navigation/composition may reference only " +
                    "feature navigation contracts; shared DI may reference only feature DI " +
                    "Dependencies classes. Other shared packages may not reference features."
            }.toList()
        }.sorted()

    /** Feature screens, state, ViewModels and use cases stay inside their own module. */
    fun featureVisibilityViolations(sources: Map<String, String>): List<String> =
        sources.flatMap { (path, source) ->
            val packageName = PACKAGE.find(codeOnly(source))?.groupValues?.get(1).orEmpty()
            val isNavigationPackage = FEATURE_NAVIGATION_PACKAGE.matches(packageName)
            val isDiPackage = FEATURE_DI_PACKAGE.matches(packageName)

            declarations(source, includeNested = false).mapNotNull { parsed ->
                val declaration = parsed.match
                val modifiers = declaration.groupValues[1].trim().split(Regex("""\s+"""))
                if (modifiers.any { it == "internal" || it == "private" }) return@mapNotNull null

                val kind = declaration.groupValues[2]
                val name = declaration.groupValues[3].removeSurrounding("`").substringAfterLast('.')
                if (isNavigationPackage || isDiPackage && kind == "class" && name.endsWith("Dependencies")) {
                    return@mapNotNull null
                }

                "$path:${parsed.lineNumber} exposes $name outside feature navigation contracts " +
                    "or a DI Dependencies class. Feature implementations must be internal or private."
            }
        }.sorted()

    fun leakedUseCaseViolations(
        useCases: List<Pair<String, String>>,
        sourcesByFeature: Map<String, List<String>>,
    ): List<String> = useCases.mapNotNull { (useCase, module) ->
        val users = sourcesByFeature.filterValues { sources -> sources.any { it.contains(useCase) } }.keys
        if (users.size != 1) return@mapNotNull null

        "$module declares $useCase, but only feature:${users.single()} uses it. " +
            "Move it into that feature's domain package, or leave it here once a second feature needs it."
    }.sorted()

    /** A production source from a logical core module. */
    data class CoreSource(
        val path: String,
        val module: String,
        val packageName: String,
        val source: String,
    )

    /**
     * Keeps the public ABI of every core capability to port contracts and their data.
     * Implementations, Metro contributors and helpers must be internal or private.
     */
    fun coreVisibilityViolations(sources: List<CoreSource>): List<String> =
        sources.flatMap { source ->
            val isPortPackage = CORE_PORT_PACKAGE.matches(source.packageName)
            declarations(source.source, includeNested = isPortPackage).mapNotNull { parsed ->
                val declaration = parsed.match
                val modifiers = declaration.groupValues[1]
                    .trim()
                    .split(Regex("""\s+"""))
                    .filter(String::isNotBlank)
                val visibility = modifiers
                    .firstOrNull { it in setOf("public", "internal", "private", "protected") }
                val kind = declaration.groupValues[2]
                val name = declaration.groupValues[3]
                    .removeSurrounding("`")
                    .substringAfterLast('.')
                    .ifBlank { "<anonymous $kind>" }
                // A port is public by definition, so Kotlin's own default visibility already says
                // so without a keyword. Only a keyword that says otherwise is a violation.
                val isPublic = visibility == null || visibility == "public"

                when {
                    isPortPackage && !isPublic ->
                        "${source.path}:${parsed.lineNumber} declares $name as $visibility. " +
                            "Every declaration in a port package must be public."

                    isPortPackage && isPublic &&
                        kind == "interface" && "sealed" !in modifiers && !name.endsWith("Port") ->
                        "${source.path}:${parsed.lineNumber} exposes interface $name. " +
                            "Public port interfaces must end in Port."

                    !isPortPackage && visibility !in setOf("internal", "private") ->
                        "${source.path}:${parsed.lineNumber} exposes $name outside a port package. " +
                            "Core implementations must be internal or private."

                    else -> null
                }
            }.toList()
        }.sorted()

    /** Core uses capability ports and concrete adapters, never repository/provider seams. */
    fun legacyCoreAbstractionViolations(sources: List<CoreSource>): List<String> =
        sources.flatMap { source ->
            declarations(source.source, includeNested = true).mapNotNull { parsed ->
                val declaration = parsed.match
                val kind = declaration.groupValues[2]
                val name = declaration.groupValues[3].removeSurrounding("`").substringAfterLast('.')
                if (
                    kind !in setOf("class", "interface", "object", "typealias") ||
                    ("Repository" !in name && "Provider" !in name)
                ) {
                    return@mapNotNull null
                }

                "${source.path}:${parsed.lineNumber} declares $name. Core contracts must be ports and " +
                    "implementations must be adapters; repositories/providers may only be feature-local."
            }
        }.sorted()

    /** Every dependency crossing from one core module to another must target a port package. */
    fun coreImportViolations(sources: List<CoreSource>): List<String> {
        data class CoreOwner(val module: String, val packageName: String)

        val declarationOwners = buildMap<String, CoreOwner> {
            sources.forEach { source ->
                declarations(source.source, includeNested = false).forEach { parsed ->
                    val declaration = parsed.match
                    val name = declaration.groupValues[3].removeSurrounding("`").substringAfterLast('.')
                    if (name.isBlank()) return@forEach
                    put("${source.packageName}.$name", CoreOwner(source.module, source.packageName))
                }
            }
        }

        fun ownerOf(reference: String): CoreOwner? {
            val normalized = reference.removeSuffix(".*")
            declarationOwners.entries
                .filter { (declaration, _) ->
                    normalized == declaration || normalized.startsWith("$declaration.")
                }
                .maxByOrNull { it.key.length }
                ?.value?.let { return it }

            val candidates = sources.asSequence()
                .filter { candidate ->
                    if (reference.endsWith(".*")) {
                        normalized == candidate.packageName
                    } else {
                        normalized == candidate.packageName ||
                            normalized.startsWith("${candidate.packageName}.")
                    }
                }
                .map { CoreOwner(it.module, it.packageName) }
                .distinct()
                .toList()
            val longestPackage = candidates.maxOfOrNull { it.packageName.length } ?: return null
            return candidates.filter { it.packageName.length == longestPackage }.singleOrNull()
        }

        return sources.flatMap { source ->
            val code = codeOnly(source.source)
            references(code, QUALIFIED_CORE_REFERENCE).mapNotNull { reference ->
                if (!reference.startsWith("com.xwab.app.core.")) return@mapNotNull null

                val target = ownerOf(reference)
                if (target?.module == source.module || target?.packageName?.let(CORE_PORT_PACKAGE::matches) == true) {
                    return@mapNotNull null
                }

                val owner = target?.module ?: "an unresolved core package"
                "${source.path} references $reference from $owner. " +
                    "Core modules may communicate only through port packages."
            }
        }.sorted()
    }

    private fun String.isWithin(packageName: String): Boolean =
        this == packageName || startsWith("$packageName.")

    private fun references(code: String, qualifiedReference: Regex): Sequence<String> {
        val imports = IMPORT.findAll(code).map { it.groupValues[1] }
        val qualifiedReferences = code.lineSequence()
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("package ") || trimmed.startsWith("import ")
            }
            .flatMap { line -> qualifiedReference.findAll(line).map { it.value } }
        return (imports + qualifiedReferences).distinct()
    }

    /**
     * Finds declarations in actual code, independent of indentation. Non-port checks keep only
     * lexical top-level declarations; port checks include members and nested contract types too.
     */
    private fun declarations(source: String, includeNested: Boolean): List<SourceDeclaration> {
        var braceDepth = 0
        var parenthesisDepth = 0
        return codeOnly(source).lineSequence().mapIndexedNotNull { index, line ->
            val atTopLevel = braceDepth == 0 && parenthesisDepth == 0
            val declaration = if (includeNested || atTopLevel) DECLARATION.find(line) else null

            braceDepth += line.count { it == '{' } - line.count { it == '}' }
            parenthesisDepth += line.count { it == '(' } - line.count { it == ')' }

            declaration?.let { SourceDeclaration(lineNumber = index + 1, match = it) }
        }.toList()
    }

    /**
     * Keeps source positions while blanking comments and literals. This prevents URI/action strings
     * and documentation examples from looking like fully-qualified Kotlin symbol references.
     */
    private fun codeOnly(source: String): String = sanitize(source, blankLiterals = true)

    private fun commentsRemoved(source: String): String = sanitize(source, blankLiterals = false)

    private fun sanitize(source: String, blankLiterals: Boolean): String = buildString(source.length) {
        var index = 0
        var blockCommentDepth = 0
        var mode = LexicalMode.CODE

        fun blank(character: Char) {
            append(if (character == '\n' || character == '\r') character else ' ')
        }

        fun literal(character: Char) {
            if (blankLiterals) blank(character) else append(character)
        }

        fun literal(value: String) {
            if (blankLiterals) append(" ".repeat(value.length)) else append(value)
        }

        while (index < source.length) {
            val current = source[index]
            val next = source.getOrNull(index + 1)
            val tripleQuote = source.startsWith("\"\"\"", index)

            when (mode) {
                LexicalMode.CODE -> when {
                    current == '/' && next == '/' -> {
                        append("  ")
                        index += 2
                        mode = LexicalMode.LINE_COMMENT
                    }

                    current == '/' && next == '*' -> {
                        append("  ")
                        index += 2
                        blockCommentDepth = 1
                        mode = LexicalMode.BLOCK_COMMENT
                    }

                    tripleQuote -> {
                        literal("\"\"\"")
                        index += 3
                        mode = LexicalMode.RAW_STRING
                    }

                    current == '"' -> {
                        literal(current)
                        index++
                        mode = LexicalMode.STRING
                    }

                    current == '\'' -> {
                        literal(current)
                        index++
                        mode = LexicalMode.CHAR
                    }

                    else -> {
                        append(current)
                        index++
                    }
                }

                LexicalMode.LINE_COMMENT -> {
                    blank(current)
                    index++
                    if (current == '\n' || current == '\r') mode = LexicalMode.CODE
                }

                LexicalMode.BLOCK_COMMENT -> when {
                    current == '/' && next == '*' -> {
                        append("  ")
                        index += 2
                        blockCommentDepth++
                    }

                    current == '*' && next == '/' -> {
                        append("  ")
                        index += 2
                        blockCommentDepth--
                        if (blockCommentDepth == 0) mode = LexicalMode.CODE
                    }

                    else -> {
                        blank(current)
                        index++
                    }
                }

                LexicalMode.STRING, LexicalMode.CHAR -> when {
                    current == '\\' && next != null -> {
                        literal(current)
                        literal(next)
                        index += 2
                    }

                    mode == LexicalMode.STRING && current == '"' -> {
                        literal(current)
                        index++
                        mode = LexicalMode.CODE
                    }

                    mode == LexicalMode.CHAR && current == '\'' -> {
                        literal(current)
                        index++
                        mode = LexicalMode.CODE
                    }

                    else -> {
                        literal(current)
                        index++
                    }
                }

                LexicalMode.RAW_STRING -> if (tripleQuote) {
                    literal("\"\"\"")
                    index += 3
                    mode = LexicalMode.CODE
                } else {
                    literal(current)
                    index++
                }
            }
        }
    }

    private enum class LexicalMode {
        CODE,
        LINE_COMMENT,
        BLOCK_COMMENT,
        STRING,
        RAW_STRING,
        CHAR,
    }

    fun featureOf(modulePath: String): String =
        modulePath.removePrefix(FEATURE_PREFIX).substringBefore(':')

    fun owningModule(relativeSourcePath: String, modulePaths: Collection<String>): String? =
        modulePaths
            .filter { relativeSourcePath.startsWith("${directoryOf(it)}/") }
            .maxByOrNull { it.length }

    private fun directoryOf(modulePath: String): String =
        modulePath.removePrefix(":").replace(':', '/')
}
