package com.xwab.convention

/** Pure architecture rules, separated from Gradle plumbing so every rule is unit-testable. */
internal object FeatureFirstRules {
    const val CORE_PREFIX = ":core:"
    const val FEATURE_PREFIX = ":feature:"
    const val SHELL_MODULE = ":shared"

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

    /** `data object BrowseRoute : NavKey`, `data class SoundRoute(val trackId: String) : NavKey`. */
    private val ROUTE_DECLARATION = Regex(
        """^\s*(?:(?:public|internal|data|value)\s+)*(?:object|class)\s+(\w+)\b[^\r\n]*:\s*NavKey\s*\{?\s*$""",
    )

    private val EXPLICIT_SERIAL_NAME = Regex("""@SerialName\s*\(""")

    /** `@StringKey(SOUND_PLAYBACK_KIND)` — the kind a content module registers its resolver under. */
    private val CONTRIBUTED_PLAYBACK_KIND =
        Regex("""@StringKey\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)""")

    private val DECLARATION = Regex(
        """^\s*(?:(?:@[A-Za-z_][A-Za-z0-9_.:]*(?:\([^()\r\n]*\))?)\s+)*((?:(?:public|internal|private|protected|expect|actual|open|abstract|final|override|inner|companion|suspend|inline|tailrec|operator|infix|external|lateinit|const|data|sealed|enum|value|annotation|fun)\s+)*)(class|interface|object|fun|const\s+val|val|var|typealias)(?:\s+(?:<[^>]+>\s+)?(`[^`\r\n]+`|[A-Za-z_][A-Za-z0-9_.]*))?""",
    )

    private data class SourceDeclaration(
        val lineNumber: Int,
        val match: MatchResult,
    )

    /** UI primitives and presentation models have no application project dependencies. */
    val INDEPENDENT_SUPPORT_MODULES = setOf(":designsystem")

    /** Only fixed application structure lives here; capability rules travel with their module. */
    fun staleRuleViolations(modules: Set<String>): List<String> =
        (INDEPENDENT_SUPPORT_MODULES - modules).sorted().map {
            "The independent-support rule names $it, which is not a module in this build. " +
                "Update INDEPENDENT_SUPPORT_MODULES."
        } + if (SHELL_MODULE !in modules) listOf(
            "The shell-wiring rule names $SHELL_MODULE, which is not a module in this build. Update SHELL_MODULE.",
        ) else emptyList()

    fun corePolicyViolations(
        modules: Set<String>,
        policies: Map<String, CoreModulePolicy>,
    ): List<String> = policies.flatMap { (module, policy) ->
        (policy.dependencies - modules).sorted().map { dependency ->
            "$module architecture.properties still names absent dependency $dependency. " +
                "Update this module's boundary when replacing or removing that capability."
        }
    }

    /** Each module explicitly owns its complete set of callable public contracts. */
    fun corePortViolations(
        sources: List<CoreSource>,
        policies: Map<String, CoreModulePolicy>,
    ): List<String> = policies.flatMap { (module, policy) ->
        val interfaces = sources.filter { it.module == module && CORE_PORT_PACKAGE.matches(it.packageName) }
            .flatMap { source ->
                declarations(source.source, includeNested = true).mapNotNull { parsed ->
                    val declaration = parsed.match
                    val modifiers = declaration.groupValues[1].split(Regex("""\s+"""))
                    if (declaration.groupValues[2] != "interface" || "sealed" in modifiers) null
                    else declaration.groupValues[3]
                }
            }.toSet()
        if (interfaces == policy.publicInterfaces) emptyList()
        else listOf(
            "$module must expose exactly the callable port interfaces declared in architecture.properties: " +
                "${policy.publicInterfaces.sorted()}; found ${interfaces.sorted()}.",
        )
    }

    /** Selected port types serve core contributors, never feature code or other public APIs. */
    fun adapterOnlyTypeViolations(
        coreSources: List<CoreSource>,
        featureSources: Map<String, String>,
        policies: Map<String, CoreModulePolicy>,
    ): List<String> {
        data class RestrictedType(val module: String, val packageName: String, val name: String) {
            val qualifiedName: String get() = "$packageName.$name"
        }
        val portSources = coreSources.filter { CORE_PORT_PACKAGE.matches(it.packageName) }
        val declarationsBySource = portSources.associateWith {
            declarations(it.source, includeNested = false).map { parsed ->
                parsed.match.groupValues[2] to parsed.match.groupValues[3].removeSurrounding("`")
            }
        }
        val portTypes = declarationsBySource.flatMap { (source, declarations) ->
            declarations.filter { (kind, _) -> kind in setOf("class", "interface", "object", "typealias") }
                .map { (_, name) -> RestrictedType(source.module, source.packageName, name) }
        }.distinct()
        val restricted = portTypes.filter { it.name in policies[it.module]?.adapterOnlyTypes.orEmpty() }
        val violations = mutableListOf<String>()
        policies.forEach { (module, policy) ->
            val declared = portTypes.filter { it.module == module }.map { it.name }.toSet()
            (policy.adapterOnlyTypes - declared).sorted().forEach { name ->
                violations += "$module architecture.properties marks $name adapter-only, but no owned " +
                    "top-level port type has that name. Update adapterOnlyTypes when moving or removing a type."
            }
        }

        fun checkReferences(path: String, source: String, allowedOwner: String? = null) {
            val code = codeOnly(source)
            val packageName = PACKAGE.find(code)?.groupValues?.get(1)
            val references = references(code, QUALIFIED_CORE_REFERENCE).toList()
            val exposed = restricted.filter { type ->
                type.module != allowedOwner && (
                    references.any { reference ->
                        reference.removeSuffix(".*").isWithin(type.qualifiedName) ||
                            reference == "${type.packageName}.*"
                    } || packageName == type.packageName && Regex("""\b${type.name}\b""").containsMatchIn(code)
                )
            }
            if (exposed.isNotEmpty()) {
                violations += "$path references adapter-only port types: " +
                    exposed.map { it.qualifiedName }.sorted().joinToString() +
                    ". Features and public consumer ports must not expose these core implementation contracts."
            }
        }
        featureSources.forEach { (path, source) -> checkReferences(path, source) }
        portSources.forEach { source ->
            val declaredNames = declarationsBySource.getValue(source).map { it.second }.toSet()
            val ownRestricted = policies[source.module]?.adapterOnlyTypes.orEmpty()
            val definesRestrictedType = declaredNames.any { it in ownRestricted }
            if (definesRestrictedType) {
                // Keeping ordinary contracts in a separate file lets same-package references be
                // checked without a second package hierarchy or a Kotlin compiler dependency.
                val mixed = declaredNames - ownRestricted
                if (mixed.isNotEmpty()) {
                    violations += "${source.path} mixes adapter-only types with unmarked public declarations " +
                        "${mixed.sorted()}. Keep consumer contracts in a separate file in the same port package."
                }
            }
            checkReferences(source.path, source.source, allowedOwner = source.module.takeIf { definesRestrictedType })
        }
        return violations.distinct().sorted()
    }

    /**
     * A module that answers a capability's contract may not also call it.
     *
     * `:core:sound` implements `PlaybackItemResolver` so the session can play a sound without
     * naming sounds. Nothing stopped it importing `PlaybackPort` as well and asking the session to
     * play one — which would turn a contributor into a caller and put the coordination back inside
     * the module it was moved out of. Acyclic in Gradle, backwards in meaning.
     *
     * Derived rather than declared, so there is no list to maintain: a module's own policy already
     * separates the two roles. `adapterOnlyTypes` are what contributors implement, and whatever
     * remains in `publicInterfaces` is what consumers call. Any core module referencing the first
     * set is a contributor and may not touch the second.
     *
     * Kotlin has no way to say this in the language. Java does — a module system `exports ... to`
     * shares a package with named modules without making it generally available — and Kotlin is
     * working on the equivalent in KEEP-0451 (`shared internal`). Until then the build says it.
     *
     * Deliberately not keyed on module names. Naming the permitted modules is how the Java module
     * system spells this, and its own documentation warns what that costs: the owning module has to
     * be edited whenever a new module needs access. That is the coupling this project removed when
     * each capability started declaring its own boundary, and it is not worth reintroducing for a
     * rule that can read the roles off declarations already there.
     */
    fun contributorPortViolations(
        coreSources: List<CoreSource>,
        policies: Map<String, CoreModulePolicy>,
    ): List<String> {
        data class OwnedType(val module: String, val packageName: String, val name: String) {
            val qualifiedName: String get() = "$packageName.$name"
        }

        val portTypes = coreSources
            .filter { CORE_PORT_PACKAGE.matches(it.packageName) }
            .flatMap { source ->
                declarations(source.source, includeNested = false).mapNotNull { parsed ->
                    if (parsed.match.groupValues[2] !in DECLARED_TYPE_KINDS) return@mapNotNull null
                    val name = parsed.match.groupValues[3].removeSurrounding("`")
                    OwnedType(source.module, source.packageName, name).takeIf { name.isNotBlank() }
                }
            }
            .distinct()

        val violations = mutableListOf<String>()

        policies.forEach { (owner, policy) ->
            val consumerNames = policy.publicInterfaces - policy.adapterOnlyTypes
            val ownerTypes = portTypes.filter { it.module == owner }
            val contract = ownerTypes.filter { it.name in policy.adapterOnlyTypes }
            val consumer = ownerTypes.filter { it.name in consumerNames }
            if (contract.isEmpty() || consumer.isEmpty()) return@forEach

            coreSources.filter { it.module != owner }
                .groupBy { it.module }
                .forEach { (module, sources) ->
                    val code = sources.joinToString("\n") { codeOnly(it.source) }
                    val referenced = references(code, QUALIFIED_CORE_REFERENCE).toList()
                    fun refersTo(type: OwnedType) = referenced.any { reference ->
                        reference.removeSuffix(".*").isWithin(type.qualifiedName) ||
                            reference == "${type.packageName}.*"
                    }

                    if (contract.none(::refersTo)) return@forEach
                    val called = consumer.filter(::refersTo)
                    if (called.isNotEmpty()) {
                        violations += "$module implements $owner's adapter contract and also references " +
                            called.map { it.qualifiedName }.sorted().joinToString() +
                            ". A module that answers a capability's contract must not also call it: " +
                            "reach the capability through the contract it contributed, or stop " +
                            "contributing and become an ordinary consumer."
                    }
                }
        }

        return violations.distinct().sorted()
    }

    /** `const val SOUND_PLAYBACK_KIND: String = "sound"` — name and the literal it is pinned to. */
    private val WIRE_FORMAT_CONSTANT =
        Regex("""\bconst\s+val\s+([A-Z][A-Z0-9_]*)\s*(?::\s*String\s*)?=\s*"([^"]*)"""")

    /** A constant whose name says it names something outside this process. */
    private val WIRE_FORMAT_NAME = Regex("""[A-Z][A-Z0-9_]*_(?:NAMESPACE|KIND)""")

    /**
     * Values this capability has already written onto devices cannot be renamed by editing them.
     *
     * A playback kind is the prefix of an engine source id, and on Android the media service
     * outlives the app, so a reconnect reads back what an earlier build wrote. A favorites
     * namespace and a cache namespace are keys on disk. Every one of them is a plain string a
     * refactor would happily rename: nothing fails to compile, no test notices, CI stays green, and
     * the damage lands on people who already have the app — detached playback, lost favourites, a
     * cache nothing will ever sweep.
     *
     * So the value is pinned beside the capability that owns it. Changing the constant now means
     * changing `architecture.properties` too, and that second edit is the moment to ask whether a
     * migration is owed. The build cannot decide that; it can only refuse to let it happen quietly.
     *
     * Constants join by being named, not by being listed: anything ending in `_NAMESPACE` or
     * `_KIND` must be pinned. A new content type's kind cannot be added without declaring it, which
     * is the same trick the user-agent rule uses and the reason neither needs a registry.
     */
    fun wireFormatViolations(
        coreSources: List<CoreSource>,
        policies: Map<String, CoreModulePolicy>,
    ): List<String> {
        val violations = mutableListOf<String>()

        val declaredByModule = coreSources.groupBy { it.module }.mapValues { (_, sources) ->
            sources.flatMap { source ->
                WIRE_FORMAT_CONSTANT.findAll(commentsRemoved(source.source)).map { match ->
                    Triple(match.groupValues[1], match.groupValues[2], source.path)
                }
            }
        }

        policies.forEach { (module, policy) ->
            val constants = declaredByModule[module].orEmpty()

            policy.wireFormat.forEach { (name, pinned) ->
                val found = constants.filter { it.first == name }
                when {
                    found.isEmpty() ->
                        violations += "$module architecture.properties pins wire format $name, but the " +
                            "module declares no such constant. Update wireFormat when moving or removing one."

                    found.none { it.second == pinned } ->
                        violations += "$module declares $name as \"${found.first().second}\" in " +
                            "${found.first().third}, but architecture.properties pins it to \"$pinned\". " +
                            "This value is already stored on devices: installed copies hold it in an engine " +
                            "source id, a favourites key or a cache path. Changing it detaches what is " +
                            "there, so pin the new value only together with a migration for the old one."
                }
            }

            constants.filter { WIRE_FORMAT_NAME.matches(it.first) && it.first !in policy.wireFormat }
                .forEach { (name, value, path) ->
                    violations += "$path declares $name = \"$value\", which names something outside this " +
                        "process, but $module architecture.properties does not pin it. Add it to " +
                        "wireFormat so renaming it cannot pass unnoticed."
                }
        }

        return violations.distinct().sorted()
    }

    fun coreModuleShapeViolations(modules: Set<String>): List<String> =
        modules.filter { it.startsWith(CORE_PREFIX) && !Regex(":core:[a-z][a-z0-9]*(?:-[a-z0-9]+)*").matches(it) }
            .sorted().map {
                "$it is not a flat core capability. Each core must be exactly one :core:<name> module."
            }

    /** A module cannot disguise its own implementation as somebody else's public contract. */
    fun corePackageOwnershipViolations(sources: List<CoreSource>): List<String> =
        sources.mapNotNull { source ->
            val packageSegment = source.module.removePrefix(CORE_PREFIX).replace("-", "")
            val expected = "com.xwab.app.core.$packageSegment"
            if (source.packageName.isWithin(expected)) null
            else "${source.path} uses ${source.packageName}; ${source.module} owns only $expected and its subpackages."
        }.sorted()

    /** A feature is one Gradle module; nested `api` / `impl` projects are not part of the model. */
    fun featureModuleShapeViolations(modules: Set<String>): List<String> =
        modules.filter { module ->
            module.startsWith(FEATURE_PREFIX) &&
                module.removePrefix(FEATURE_PREFIX).contains(':')
        }.sorted().map { module ->
            "$module is a nested feature project. Each feature must be exactly one " +
                ":feature:<name> module; keep Navigation 3 contracts and implementation together."
        }

    /**
     * A module directory that no `include` names builds nothing and fails nothing.
     *
     * Flat capabilities and features are discovered under `core/` and `feature/`. A directory in
     * another location or outside that shape still needs explicit inclusion; otherwise it compiles
     * nowhere and runs no tests. Every graph rule would silently skip it without this check.
     *
     * Plugging a module in is supposed to be dropping a directory and wiring it. This is the half
     * of that nothing else reports.
     */
    fun unregisteredModuleViolations(
        moduleDirectories: Collection<String>,
        modules: Set<String>,
    ): List<String> {
        val registered = modules.map(::directoryOf).toSet()
        return (moduleDirectories - registered).sorted().map { directory ->
            "$directory holds a build script but is not a module in this build. Add it to " +
                "settings.gradle.kts or delete it: an unregistered directory compiles nothing, " +
                "runs no tests, and no rule here can see it."
        }
    }

    /**
     * A capability or a screen the shell never declares is in no application.
     *
     * Metro aggregates a scope's contributions from the compile classpath. The shell discovers
     * core dependencies automatically; feature wiring remains explicit application policy. This
     * check guards both paths, so a broken discovery loop or forgotten feature dependency cannot
     * leave a registered, tested module absent from the application graph.
     */
    fun unwiredModuleViolations(graph: Map<String, List<String>>): List<String> {
        val shell = graph[SHELL_MODULE] ?: return emptyList()
        return graph.keys
            .filter { module ->
                (module.startsWith(CORE_PREFIX) || module.startsWith(FEATURE_PREFIX)) &&
                    module !in shell
            }
            .sorted()
            .map { module ->
                "$module is in the build but $SHELL_MODULE does not depend on it. Check core discovery " +
                    "or declare the feature in shared/build.gradle.kts: a core capability the shell " +
                    "does not see contributes nothing to the application graph, and a feature it " +
                    "does not see is in no app."
            }
    }

    /**
     * Every content kind the app can play has somewhere to open.
     *
     * This replaces a guarantee the compiler used to give. While a playback kind was a closed enum,
     * the composition root's `when` over it was exhaustive: adding a content type would not build
     * until it had a route. Kinds are open strings now — which is what lets a content module be
     * added or removed without editing `:core:session` — and an open `when` needs an `else`, so the
     * compiler has nothing left to say.
     *
     * So the build says it instead. A module that registers a resolver under a kind has declared
     * that the app can play that kind; if the composition root never mentions the same constant,
     * something is playable with no screen to open from the now-playing bar.
     *
     * Keyed on the constant's *name* rather than its value, which is the whole point of each
     * content module publishing one: a rule that compared string literals would be satisfied by a
     * matching typo.
     */
    fun unroutedPlaybackKindViolations(
        coreSources: Map<String, String>,
        compositionSources: Map<String, String>,
    ): List<String> {
        val contributed = coreSources
            .filterValues { it.contains("PlaybackItemResolver") }
            .flatMap { (path, source) ->
                CONTRIBUTED_PLAYBACK_KIND.findAll(codeOnly(source))
                    .map { path to it.groupValues[1] }
                    .toList()
            }
        if (contributed.isEmpty()) return emptyList()

        val composition = compositionSources.values.joinToString("\n", transform = ::codeOnly)
        return contributed
            .filterNot { (_, constant) -> Regex("""\b$constant\b""").containsMatchIn(composition) }
            .map { (path, constant) ->
                "$path registers a playback resolver under $constant, but nothing in the app shell " +
                    "names that kind. Something the app can play has no screen to open: give it a " +
                    "route where the now-playing bar turns an item into one."
            }
            .sorted()
    }

    /**
     * A route's serial name is a wire format, so it is stated rather than inferred.
     *
     * Left implicit, a `@Serializable` route is named after its package. A saved back stack holds
     * that name, and the build that reads one back is never the build that wrote it — so moving the
     * file, renaming the package or repackaging the feature silently invalidates the navigation
     * every installed copy restores. Written out, the name survives all three.
     *
     * This is what makes a feature removable at all: [com.xwab.app.navigation.RetiredRoute] can
     * only recognise a name it no longer has if that name was stable in the first place.
     *
     * Scoped to `feature/`, which is where routes live. The shell's own fallback names itself in a
     * hand-written descriptor instead, and is not a route anything navigates to.
     */
    fun routeSerialNameViolations(sources: Map<String, String>): List<String> =
        sources.flatMap { (path, source) ->
            val lines = codeOnly(source).lines()
            lines.mapIndexedNotNull { index, line ->
                val route = ROUTE_DECLARATION.find(line)?.groupValues?.get(1)
                    ?: return@mapIndexedNotNull null

                val annotations = lines.take(index).asReversed()
                    .takeWhile { it.isBlank() || it.trimStart().startsWith("@") }
                if (annotations.any(EXPLICIT_SERIAL_NAME::containsMatchIn)) {
                    return@mapIndexedNotNull null
                }

                "$path:${index + 1} declares route $route without an explicit @SerialName. The " +
                    "name a saved back stack holds would then follow the package, and moving the " +
                    "file would break the navigation every installed copy restores."
            }
        }.sorted()

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

    /**
     * A constant whose name *ends* in `USER_AGENT`, which is what a user agent is called, rather
     * than one that merely contains it. `USER_AGENT_METADATA_KEY` holds the name of the manifest
     * entry a user agent is read from — the label on the box, not what is in it — and the first
     * version of this rule reported it as a third, disagreeing declaration.
     */
    private val KOTLIN_USER_AGENT =
        Regex("""\bconst\s+val\s+\w*USER_AGENT\s*(?::\s*\w+\s*)?=\s*"([^"]*)"""")

    /** `/src/test/`, `/src/commonTest/`, `/src/iosTest/` — every source set that is not shipped. */
    private val TEST_SOURCE_SET = Regex("""/src/[^/]*[Tt]est[^/]*/""")

    private val META_DATA_ELEMENT = Regex("""<meta-data\b[^>]*>""")

    private val META_DATA_ATTRIBUTE = Regex("""android:(name|value)\s*=\s*"([^"]*)"""")

    private val PLIST_STRING = Regex("""<key>\s*([^<]+?)\s*</key>\s*<string>([^<]*)</string>""")

    /**
     * Everywhere this app says who it is, saying the same thing.
     *
     * Download and platform playback paths reach the same host, and they are fed from
     * different places: a sound downloads through common Kotlin, and streams — before it has
     * downloaded — through a service Android constructs, which can only be given a value through
     * manifest metadata. iOS reads its application bundle's Info.plist. These platform entry
     * points cannot read the internal source manifest, so every declaration is checked here.
     *
     * A divergence fails in the worst available way: one path keeps working. A listener would hear
     * sounds that stream and never cache, or cache and never stream, and nothing would say why. So
     * the build refuses it instead.
     *
     * Deliberately not keyed on file paths. Anything that declares a user agent joins the check by
     * declaring one, rather than by being added to a list someone has to remember.
     */
    fun userAgentAgreementViolations(sources: Map<String, String>): List<String> {
        val declarations = sources.entries
            .sortedBy { it.key }
            .flatMap { (path, source) -> userAgentsIn(path, source) }
        if (declarations.map { it.second }.distinct().size < 2) return emptyList()

        return listOf(
            "This app states its user agent in more than one place and they disagree: " +
                declarations.joinToString { (path, value) -> "$path says \"$value\"" } +
                ". The download and the playback paths identify the same client to the same host; " +
                "when they drift, one of them keeps working and the failure is invisible.",
        )
    }

    private fun userAgentsIn(path: String, source: String): List<Pair<String, String>> = when {
        // A rule that reads its own test data reports it. The first version of this one found the
        // three fixtures below in `FeatureFirstRulesTest` and called them a disagreement.
        TEST_SOURCE_SET.containsMatchIn(path) -> emptyList()

        path.endsWith(".kt") ->
            KOTLIN_USER_AGENT.findAll(commentsRemoved(source))
                .map { path to it.groupValues[1] }
                .toList()

        path.endsWith(".plist") ->
            PLIST_STRING.findAll(source.replace(Regex("<!--[\\s\\S]*?-->"), ""))
                .filter { it.groupValues[1].endsWith("USER_AGENT") }
                .map { path to it.groupValues[2] }
                .toList()

        path.endsWith(".xml") ->
            META_DATA_ELEMENT.findAll(source)
                .mapNotNull { element ->
                    val attributes = META_DATA_ATTRIBUTE.findAll(element.value)
                        .associate { it.groupValues[1] to it.groupValues[2] }
                    val value = attributes["value"]
                    if (attributes["name"].orEmpty().contains("USER_AGENT") && value != null) {
                        path to value
                    } else {
                        null
                    }
                }
                .toList()

        else -> emptyList()
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
        policies: Map<String, CoreModulePolicy>,
    ): List<String> {
        val violations = mutableListOf<String>()

        graph.forEach { (module, dependencies) ->
            val allowedDependencies = policies[module]?.dependencies
            dependencies.forEach { dependency ->
                if (module.startsWith(CORE_PREFIX) && dependency.startsWith(FEATURE_PREFIX)) {
                    violations += "$module depends on $dependency. A core module may not depend on a feature."
                } else if (module.startsWith(CORE_PREFIX) &&
                    (dependency == ":shared" || dependency in INDEPENDENT_SUPPORT_MODULES)
                ) {
                    violations += "$module depends on $dependency. Core capabilities may not depend on UI or the app shell."
                } else if (module.startsWith(CORE_PREFIX) && dependency !in allowedDependencies.orEmpty()) {
                    violations += "$module depends on $dependency. This module states its project " +
                        "dependencies exhaustively in architecture.properties and $dependency is not among them: " +
                        "${allowedDependencies.orEmpty().sorted()}."
                }

                if (module in INDEPENDENT_SUPPORT_MODULES && dependency != module) {
                    violations += "$module depends on $dependency. Support modules must remain independent of application projects."
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
                violations += offLimitsReachableFrom(module, dependencies, apiEdges, policies)
            }
        }

        return (violations + coreDependencyCycleViolations(graph)).distinct().sorted()
    }

    private fun offLimitsReachableFrom(
        feature: String,
        directDependencies: List<String>,
        apiEdges: Map<String, List<String>>,
        policies: Map<String, CoreModulePolicy>,
    ): List<String> {
        val violations = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val paths = ArrayDeque(directDependencies.map(::listOf))

        while (paths.isNotEmpty()) {
            val path = paths.removeFirst()
            val reached = path.last()
            if (!visited.add(reached)) continue

            val policy = policies[reached]
            val reason = when {
                reached == SHELL_MODULE -> "the app shell owns navigation state and destination policy"
                reached.startsWith(CORE_PREFIX) && policy?.featureAccessible != true ->
                    policy?.let { "this is an adapter-only capability: ${it.responsibility}" }
                        ?: "this core has no valid architecture.properties declaring feature access"
                else -> null
            }
            reason?.let {
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

    /** Test fixtures do not change a production capability's dependency boundary. */
    fun isProductionConfiguration(configurationName: String): Boolean =
        !configurationName.startsWith("test", ignoreCase = true) && !configurationName.contains("Test")

    fun coreDependencyCycleViolations(graph: Map<String, List<String>>): List<String> {
        val complete = mutableSetOf<String>()
        val active = mutableListOf<String>()
        val cycles = sortedSetOf<String>()
        fun visit(module: String) {
            val cycleStart = active.indexOf(module)
            if (cycleStart >= 0) {
                val cycle = active.drop(cycleStart)
                val first = cycle.indices.minBy { cycle[it] }
                val canonical = cycle.drop(first) + cycle.take(first)
                cycles += (canonical + canonical.first()).joinToString(" -> ")
                return
            }
            if (!complete.add(module)) return
            active += module
            graph[module].orEmpty().filter { it.startsWith(CORE_PREFIX) }
                .sorted().forEach(::visit)
            active.removeAt(active.lastIndex)
        }
        graph.keys.filter { it.startsWith(CORE_PREFIX) }.sorted().forEach(::visit)
        return cycles.map { "Core capability dependencies must be acyclic: $it." }
    }

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
    fun coreVisibilityViolations(
        sources: List<CoreSource>,
        policies: Map<String, CoreModulePolicy> = emptyMap(),
    ): List<String> =
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
                        kind == "interface" && "sealed" !in modifiers && !name.endsWith("Port") &&
                        name !in policies[source.module]?.publicInterfaces.orEmpty() ->
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
                val sourceIsPort = CORE_PORT_PACKAGE.matches(source.packageName)
                val targetIsPort = target?.packageName?.let(CORE_PORT_PACKAGE::matches) == true
                if (targetIsPort || target?.module == source.module && !sourceIsPort) {
                    return@mapNotNull null
                }

                val owner = target?.module ?: "an unresolved core package"
                "${source.path} references $reference from $owner. " +
                    if (sourceIsPort) "Public port contracts must not reference implementation packages, including their own."
                    else "Core modules may communicate only through port packages."
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
