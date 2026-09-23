package com.xwab.convention

/** Owned by the module, so installing/removing a capability never edits a central allowlist. */
internal data class CoreModulePolicy(
    val responsibility: String,
    val featureAccessible: Boolean,
    val dependencies: Set<String>,
    val publicInterfaces: Set<String>,
    val adapterOnlyTypes: Set<String> = emptySet(),
    val wireFormat: Map<String, String> = emptyMap(),
)

internal data class CoreModulePolicyResult(
    val policy: CoreModulePolicy?,
    val violations: List<String>,
)

/**
 * A deliberately small properties format: one key=value per line, with # comments.
 * Missing, misspelled and duplicate fields fail closed instead of weakening a new module's rules.
 * adapterOnlyTypes is optional; it reserves selected public port types for core implementations.
 * wireFormat is optional; it pins the values this capability has written onto devices.
 */
internal fun parseCoreModulePolicy(module: String, source: String?): CoreModulePolicyResult {
    val location = "${module.removePrefix(":").replace(':', '/')}/architecture.properties"
    if (source == null) {
        return CoreModulePolicyResult(null, listOf("$module must declare its responsibility and boundaries in $location."))
    }
    val required = setOf("responsibility", "featureAccessible", "dependencies", "publicInterfaces")
    val allowed = required + setOf("adapterOnlyTypes", "wireFormat")
    val fields = linkedMapOf<String, String>()
    val violations = mutableListOf<String>()
    source.lineSequence().forEachIndexed { index, raw ->
        val line = raw.trim()
        if (line.isBlank() || line.startsWith('#')) return@forEachIndexed
        val key = line.substringBefore('=', missingDelimiterValue = "").trim()
        when {
            key !in allowed -> violations += "$location:${index + 1} has an unknown or malformed policy field: $line."
            key in fields -> violations += "$location:${index + 1} repeats policy field $key."
            else -> fields[key] = line.substringAfter('=').trim()
        }
    }
    (required - fields.keys).sorted().forEach { key -> violations += "$location must declare $key." }
    if (fields["responsibility"]?.isBlank() == true) violations += "$location must state a non-empty responsibility."
    if (fields["featureAccessible"] != null && fields["featureAccessible"] !in setOf("true", "false")) {
        violations += "$location featureAccessible must be true or false."
    }
    fun entries(key: String): Set<String> {
        val value = fields[key].orEmpty()
        if (value.isBlank()) return emptySet()
        val entries = value.split(',').map(String::trim)
        if (entries.any(String::isBlank) || entries.distinct().size != entries.size) {
            violations += "$location $key must contain distinct, non-empty comma-separated entries."
        }
        return entries.toSet()
    }
    val dependencies = entries("dependencies")
    dependencies.filterNot { Regex(":core:[a-z][a-z0-9]*(?:-[a-z0-9]+)*").matches(it) }
        .forEach { violations += "$location dependency $it must name a flat :core:<name> module." }
    if (module in dependencies) violations += "$location must not list its own module as a dependency."
    val interfaces = entries("publicInterfaces")
    if (interfaces.isEmpty()) violations += "$location must name at least one public capability or contribution interface."
    interfaces.filterNot { Regex("[A-Z][A-Za-z0-9_]*").matches(it) }
        .forEach { violations += "$location public interface $it is not a valid interface name." }
    val adapterOnlyTypes = entries("adapterOnlyTypes")
    val wireFormat = linkedMapOf<String, String>()
    entries("wireFormat").forEach { entry ->
        val name = entry.substringBefore('=', missingDelimiterValue = "").trim()
        val value = entry.substringAfter('=', missingDelimiterValue = "").trim()
        when {
            !Regex("[A-Z][A-Z0-9_]*").matches(name) ->
                violations += "$location wire format entry $entry must read NAME=value."
            value.isEmpty() ->
                violations += "$location wire format $name must state the value that is stored on devices."
            name in wireFormat ->
                violations += "$location repeats wire format $name."
            else -> wireFormat[name] = value
        }
    }
    adapterOnlyTypes.filterNot { Regex("[A-Z][A-Za-z0-9_]*").matches(it) }
        .forEach { violations += "$location adapter-only type $it is not a valid top-level type name." }
    return CoreModulePolicyResult(
        policy = if (violations.isEmpty()) CoreModulePolicy(
            responsibility = fields.getValue("responsibility"),
            featureAccessible = fields.getValue("featureAccessible").toBoolean(),
            dependencies = dependencies,
            publicInterfaces = interfaces,
            adapterOnlyTypes = adapterOnlyTypes,
            wireFormat = wireFormat,
        ) else null,
        violations = violations.sorted(),
    )
}
