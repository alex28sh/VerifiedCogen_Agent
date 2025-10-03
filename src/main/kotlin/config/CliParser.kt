package org.example.config

import kotlinx.cli.*

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.div

private data class ToolOverride(
    val tool: AgenticTools,
    val enabled: Boolean
)

private typealias ToolOverrideGroup = List<ToolOverride>

fun cliParse(args: Array<String>) : CliConfig {
    val parser = ArgParser("verified-cogen")

    val grazieToken by parser.option(
        ArgType.String,
        fullName = "grazie-token",
        description = "Grazie API token"
    ).required()

    val llmProfile by parser.option(
        ArgType.Choice(
            choices = Model.entries,
            { value -> Model.entries.firstOrNull { it.strRepl == value }
                ?: throw IllegalArgumentException("Unknown model: $value. Expected one of: ${Model.entries.joinToString { it.strRepl }}") },
            { it.strRepl }
        ),
        fullName = "llm-profile",
        description = "LLM profile identifier (e.g., anthropic-claude-4-sonnet)"
    ).required()

    val tries by parser.option(
        ArgType.Int,
        fullName = "tries",
        description = "Number of tries per task"
    ).default(1)

    val runs by parser.option(
        ArgType.Int,
        fullName = "runs",
        description = "Number of runs"
    ).default(1)

    val filterByExt by parser.option(
        ArgType.Choice(
            choices = Extensions.entries,
            { value -> Extensions.entries.firstOrNull { it.strRepl == value }
                ?: throw IllegalArgumentException("Unknown extension: $value. Expected one of: ${Extensions.entries.joinToString { it.strRepl }}") },
            { it.strRepl }
        ),
        fullName = "filter-by-ext"
    ).required()

    val outputLogging by parser.option(
        ArgType.Boolean,
        fullName = "output-logging"
    ).default(false)

    val pathArgType = object : ArgType<Path>(true) {
        override val description: kotlin.String
            get() = "{ path }"

        override fun convert(value: kotlin.String, name: kotlin.String): Path =
            Paths.get(value)
    }

    val dir by parser.option(
        pathArgType,
        fullName = "dir",
        description = "path to benchmarks"
    ).required()

    val modes by parser.option(
        ArgType.Choice(
            choices = Modes.entries,
            { value -> Modes.entries.firstOrNull { it.strRepl == value }
                ?: throw IllegalArgumentException("Unknown mode: $value. Expected one of: ${Modes.entries.joinToString { it.strRepl }}") },
            { it.strRepl }
        ),
        fullName = "modes",
        description = "Execution mode"
    ).delimiter(",") //.default(emptyList())

    val promptDirs by parser.option(
        pathArgType,
        fullName = "prompts",
        description = "paths to prompts"
    ).delimiter(",") // .default(emptyList())

    val temperature by parser.option(
        ArgType.Double,
        fullName = "temperature",
        description = "agent temperature"
    ).default(0.0)

    val maxJobs by parser.option(
        ArgType.Int,
        fullName = "max-jobs",
        description = "parallel jobs limit"
    ).default(5)

    val verifierCommand by parser.option(
        ArgType.String,
        fullName = "verifier-command",
        description = "command to run (cmd [file_path]) to verify a file"
    ).required()

    val agenticToolGroupType = object : ArgType<List<ToolOverrideGroup>>(true) {
        override val description: kotlin.String
            get() = "[ +Tool, -Tool, ... ], [ ... ], ..."

        override fun convert(value: kotlin.String, name: kotlin.String): List<ToolOverrideGroup> {
            return parseToolGroups(value)
        }
    }

    val agenticToolGroups by parser.option(
        agenticToolGroupType,
        fullName = "agentic-tools",
        description = "Per-mode tool overrides using bracketed groups: [ +ToolA, -ToolB ], [ +ToolC ]"
    )

    var resultPath by parser.option(
        pathArgType,
        fullName = "results-path",
        description = "Paths to write results"
    )

    val checkers by parser.option(
        ArgType.Choice(
            choices = CheckerArt.entries,
            { value -> CheckerArt.entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown mode: $value. Expected one of: ${CheckerArt.entries.joinToString { it.name }}") },
            { it.name }
        ),
        fullName = "checkers",
        description = "Checker types in a nesting order"
    ).delimiter(",")

    val maxIterations by parser.option(
        ArgType.Int,
        fullName = "maxIterations",
        description = "Agent maxIterations"
    ).default(100)

    parser.parse(args)

    require(tries > 0) { "Number of tries must be positive, but got $tries" }
    require(runs > 0) { "Number of runs must be positive, but got $runs" }

//    println(agenticToolGroups)
    val resolvedTools = resolveToolsPerMode(modes, filterByExt, agenticToolGroups.orEmpty())
//    require(agenticToolGroups.size == modes.size) { "Lists of tool groups and modes should be of equal size" }
//    require(agenticTools.all {  } )

    if (resultPath == null) {
        resultPath = dir.parent / "results"
    }

    return CliConfig(
        grazieToken,
        llmProfile,
        tries,
        runs,
        filterByExt,
        outputLogging,
        dir,
        modes,
        promptDirs,
        temperature,
        maxJobs,
        verifierCommand,
        resolvedTools,
        resultPath!!,
        checkers,
        maxIterations,
    )
}

private fun parseToolGroups(input: String): List<ToolOverrideGroup> {
    val toolByName = AgenticTools.entries.associateBy { it.strRepl }

    val groups = mutableListOf<ToolOverrideGroup>()

    // Find all bracketed groups: [ ... ]
    val regex = Regex("\\[(.*?)\\]")
    val matches = regex.findAll(input)

//    println(input)
//    println("Matches")
//    matches.forEach { println(it.value) }

    for (m in matches) {
        val content = m.groupValues[1] // inside brackets
        println(content)
        if (content.isBlank()) {
            groups.add(emptyList())
            continue
        }

        val overrides = content.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { token ->
                val sign = token.firstOrNull()
                    ?: throw IllegalArgumentException("Empty token in agentic-tools group: '$token'")
                val plus = sign == '+'
                val minus = sign == '-'
                require(plus || minus) {
                    "Each tool override must start with '+' or '-': got '$token'"
                }
                val name = token.drop(1).trim()
                val tool = toolByName[name] ?: throw IllegalArgumentException(
                    "Unknown tool: $name. Expected one of: ${AgenticTools.entries.joinToString { it.strRepl }}"
                )
                ToolOverride(tool, enabled = plus)
            }

        groups.add(overrides)
    }

    if (groups.isEmpty()) {
        // Also support a degenerate case with a single flat list w/o [] (fallback)
        val trimmed = input.trim()
        if (trimmed.isNotEmpty()) {
            // Try to parse as if surrounded by []
            return parseToolGroups("[$trimmed]")
        }
    }

    return groups
}

private fun resolveToolsPerMode(
    modes: List<Modes>,
    lang: Extensions,
    groups: List<ToolOverrideGroup>
): Map<Modes, Set<AgenticTools>> {
    if (modes.isEmpty()) {
        throw IllegalArgumentException("You must provide at least one mode via --modes when using --agentic-tools")
    }

    val appliedGroups: List<ToolOverrideGroup> = when {
        groups.isEmpty() -> List(modes.size) { emptyList() } // nothing overridden
        groups.size == 1 -> List(modes.size) { groups[0] }   // one group applies to all
        groups.size == modes.size -> groups
        else -> throw IllegalArgumentException(
            "Number of tool groups (${groups.size}) must be 1 or equal to the number of modes (${modes.size})."
        )
    }

    val result = linkedMapOf<Modes, Set<AgenticTools>>()

    modes.forEachIndexed { idx, mode ->
        // Start from defaults compatible with this mode and language
        val active = AgenticTools.entries
            .asSequence()
            .filter { it.default }
            .filter { mode in it.modeCompatibility }
            .filter { lang in it.langCompatibility }
            .toMutableSet()

        // Apply overrides for this mode
        for (ov in appliedGroups[idx]) {
            val tool = ov.tool
            if (ov.enabled) {
                require(mode in tool.modeCompatibility) {
                    "Tool ${tool.strRepl} is not compatible with mode ${mode.strRepl}"
                }
                require(lang in tool.langCompatibility) {
                    "Tool ${tool.strRepl} is not compatible with language ${lang.strRepl}"
                }
                active.add(tool)
            } else {
                active.remove(tool)
            }
        }

        result[mode] = active
    }

    return result
}