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

    /** A single-line `key = { ... }` lambda, which is how every list in this app spells one. */
    private val LAZY_LIST_KEY = Regex("""\bkey\s*=\s*\{([^{}\n]*)\}""")

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
        ":core:sources" to
            "physical content addresses are adapter details hidden from screens",
    )

    /** The one port interface each content module is allowed to publish. */
    val CONTENT_MODULE_PORTS = mapOf(
        ":core:sound" to "SoundPort",
        ":core:story" to "StoryPort",
    )

    /**
     * Modules reusable outside this app, and the only project dependencies each may declare.
     * Favorites stores ids it never interprets; delivery moves bytes and needs a transport.
     */
    val REUSABLE_MODULE_DEPENDENCIES = mapOf(
        ":core:favorites" to emptySet<String>(),
        ":core:delivery" to setOf(":core:network"),
    )

    /** UI primitives and presentation models have no application project dependencies. */
    val INDEPENDENT_SUPPORT_MODULES = setOf(":designsystem")

    /**
     * Every rule that names a module by path, and the constant holding those names.
     *
     * A rule keyed on a module path stops matching anything the moment that module is renamed —
     * it does not fail, it just quietly protects nothing, which is the one failure mode an
     * architecture guard cannot afford. Registering each list here makes the rename itself the
     * thing that breaks the build, rather than the next mistake the rule was meant to catch.
     */
    private val RULE_MODULE_REFERENCES: List<Triple<String, String, Set<String>>> = listOf(
        Triple("independent-support rule", "INDEPENDENT_SUPPORT_MODULES", INDEPENDENT_SUPPORT_MODULES),
        Triple(
            "adapter-boundary rule",
            "MODULES_OFF_LIMITS_TO_FEATURES",
            MODULES_OFF_LIMITS_TO_FEATURES.keys,
        ),
        Triple(
            "one-port-per-content rule",
            "CONTENT_MODULE_PORTS",
            CONTENT_MODULE_PORTS.keys,
        ),
        Triple(
            "reusable-capability rule",
            "REUSABLE_MODULE_DEPENDENCIES",
            REUSABLE_MODULE_DEPENDENCIES.keys + REUSABLE_MODULE_DEPENDENCIES.values.flatten(),
        ),
    )

    fun staleRuleViolations(modules: Set<String>): List<String> =
        RULE_MODULE_REFERENCES.flatMap { (rule, constant, referenced) ->
            (referenced - modules).sorted().map { missing ->
                "The $rule names $missing, which is not a module in this build. Update " +
                    "$constant, or the rule protects nothing."
            }
        }

    /** Sound and story each expose one cohesive port. */
    fun contentPortViolations(sources: List<CoreSource>): List<String> =
        CONTENT_MODULE_PORTS.flatMap { (module, expected) ->
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
            val reusableDependencies = REUSABLE_MODULE_DEPENDENCIES[module]
            dependencies.forEach { dependency ->
                // KMP host-test configurations include a dependency on their own main module.
                if (reusableDependencies != null && dependency != module && dependency !in reusableDependencies) {
                    violations += "$module depends on $dependency. Reusable favorites and delivery " +
                        "must not depend on app content; only delivery may use core:network."
                }
                if (module.startsWith(CORE_PREFIX) && dependency.startsWith(FEATURE_PREFIX)) {
                    violations += "$module depends on $dependency. A core module may not depend on a feature."
                }

                if (module in INDEPENDENT_SUPPORT_MODULES && dependency != module) {
                    violations += "$module depends on $dependency. Support modules must remain independent of application projects."
                }
                if (module.startsWith(CORE_PREFIX) &&
                    (dependency == ":shared" || dependency in INDEPENDENT_SUPPORT_MODULES)
                ) {
                    violations += "$module depends on $dependency. Core capabilities may not depend on UI or the app shell."
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

    private val DECLARED_TYPE_KINDS = setOf("class", "interface", "object", "typealias")

    /** How a two-state presentation wrapper spells itself, whatever the enclosing type is named. */
    private val PRESENTATION_STATE_NAMES = setOf("Loading", "Ready", "Loadable")

    /**
     * Whether a screen has anything to draw yet is that screen's own state.
     *
     * `Loadable<T>` used to live in `:designsystem` and all five features imported it. That put a
     * presentation type — and one all-or-nothing answer to "is there content yet?" — in a module
     * that owns no screen, so changing how one screen waits meant changing how all five did. Each
     * feature states it in its own module now, and they are free to disagree: a screen whose only
     * source is an in-memory list has no wait to describe, while one joining a disk read does.
     *
     * Deliberately narrow, for the same reason [lazyListKeyViolations] is: it flags a `Loading`,
     * `Ready` or `Loadable` *type* declared outside `feature/`, which is how this shape spells
     * itself whatever the enclosing type is called. Enum entries are untouched — `PlaybackPhase`
     * has `Loading` and `Ready` entries, and an engine phase is a real capability state that
     * belongs in core.
     *
     * This rule is one half of a pair and only guards the half that has no other guard. A feature's
     * own state type is already required to be internal by [featureVisibilityViolations]. Nothing
     * requires a feature to declare one at all: a screen with nothing to wait for should give its
     * state a default value and drop the wrapper entirely.
     */
    fun featureStateViolations(sources: Map<String, String>): List<String> =
        sources.flatMap { (path, source) ->
            declarations(source, includeNested = true).mapNotNull { parsed ->
                val declaration = parsed.match
                if (declaration.groupValues[2] !in DECLARED_TYPE_KINDS) return@mapNotNull null
                val name = declaration.groupValues[3].removeSurrounding("`").substringAfterLast('.')
                if (name !in PRESENTATION_STATE_NAMES) return@mapNotNull null

                "$path:${parsed.lineNumber} declares $name outside a feature module. Whether a " +
                    "screen has content yet is that screen's own state: declare it in the feature " +
                    "that asks, not in a module every feature has to share."
            }
        }.sorted()

    /**
     * A lazy list key is an identity, not a value class.
     *
     * Compose holds a key as `Any`, so a `@JvmInline value class` boxes back into an object, and on
     * Android the saveable state holder behind a navigation entry writes those keys into a
     * `Bundle`, which cannot hold one. The screen crashes rather than degrades, and only on
     * Android.
     *
     * Nothing else in this build sees it. The compiler cannot: a key accepts every type. The
     * screen tests cannot: they run on a simulator with no `Bundle`. `BrowseScreen` carried the
     * mistake from the first commit until it crashed a device, while three sibling lists unwrapped
     * correctly the whole time.
     *
     * Deliberately narrow: it flags a key lambda whose body ends in `.id`, which in this codebase
     * is always one of the value-class ids. A key built from anything else — a plain `String`
     * field, an index, a already-unwrapped `.value` — is left alone. A method reference or a key
     * computed over several lines is out of its reach; the rule is a cheap net over the shape that
     * actually went wrong, not a type checker.
     */
    fun lazyListKeyViolations(sources: Map<String, String>): List<String> =
        sources.flatMap { (path, source) ->
            val code = codeOnly(source)
            LAZY_LIST_KEY.findAll(code).mapNotNull { match ->
                val body = match.groupValues[1].trim()
                if (!body.endsWith(".id")) return@mapNotNull null
                val lineNumber = code.take(match.range.first).count { it == '\n' } + 1
                "$path:$lineNumber uses `$body` as a lazy list key. A key is stored as `Any`, so a " +
                    "value class boxes into an object Android cannot put in a Bundle: unwrap it " +
                    "with `.value`."
            }.toList()
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
                val isLegacyAbstraction = "Repository" in name || "Provider" in name
                val hasImplementationSuffix = name.endsWith("Impl")
                if (kind !in setOf("class", "interface", "object", "typealias") ||
                    (!isLegacyAbstraction && !hasImplementationSuffix)
                ) {
                    return@mapNotNull null
                }

                if (isLegacyAbstraction) {
                    "${source.path}:${parsed.lineNumber} declares $name. Core contracts must be ports and " +
                        "implementations must be adapters; repositories/providers may only be feature-local."
                } else {
                    "${source.path}:${parsed.lineNumber} declares $name. Concrete core implementations " +
                        "must use an Adapter name, not an Impl suffix."
                }
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
