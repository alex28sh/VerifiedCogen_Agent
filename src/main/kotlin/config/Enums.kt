package org.example.config

import agenticTools.common.CodeToolSet
import agenticTools.common.ConditionsToolSet
import ai.jetbrains.code.prompt.llm.JetBrainsAIModels
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.llm.LLModel
import org.example.environment.ExperimentEnvironment
import org.example.languages.AnnotationTypes
import org.example.languages.AnnotationTypes.*
import org.example.languages.DafnyLanguage
import org.example.languages.Language
import org.example.languages.NaginiLanguage
import org.example.languages.VerusLanguage
import agenticTools.common.ErrorsToolSet
import agenticTools.common.InvariantsToolSet
import agenticTools.nagini.NaginiErrorsToolSet

enum class Model(val model: LLModel, val strRepl: String) {
    GPT4_1(JetBrainsAIModels.OpenAI_GPT4_1_via_JBAI, "gpt4.1"),
    GPT4o(JetBrainsAIModels.OpenAI_GPT4o_via_JBAI, "gpt4o"),
    GPT4oMini(JetBrainsAIModels.OpenAI_GPT4oMini_via_JBAI, "gpt4o-mini"),
    O3Mini(JetBrainsAIModels.OpenAI_O3Mini_via_JBAI, "o3-mini"),
    O3(JetBrainsAIModels.OpenAI_O3_via_JBAI, "o3"),
    O4Mini(JetBrainsAIModels.OpenAI_O4Mini_via_JBAI, "o4-mini"),

    AnthropicSonnet3_7(JetBrainsAIModels.Anthropic_Sonnet_3_7_via_JBAI, "sonnet-3.7"),
    AnthropicSonnet4(JetBrainsAIModels.Anthropic_Sonnet_4_via_JBAI, "sonnet-4"),
    AnthropicOpus4(JetBrainsAIModels.Anthropic_Opus_4_via_JBAI, "opus-4"),

    GoogleFlash2_5(JetBrainsAIModels.Google_Flash2_5_via_JBAI, "google-flash-2.5"),
    GooglePro2_5(JetBrainsAIModels.Google_Pro2_5_via_JBAI, "google-pro-2.5"),
}

enum class Modes(
    val strRepl: String,
    val removeAnnotations: List<AnnotationTypes>,
    val textDescription: Boolean,
) {
    Mode1("mode1", listOf(INVARIANTS, ASSERTIONS), false),
    Mode2("mode2", listOf(INVARIANTS, ASSERTIONS, PRE_CONDITIONS, POST_CONDITIONS), false),
    Mode3("mode3", listOf(INVARIANTS, ASSERTIONS, IMPLS), false),
    Mode4("mode4", listOf(INVARIANTS, ASSERTIONS, IMPLS), true),
    Mode5("mode5", listOf(INVARIANTS, ASSERTIONS, IMPLS, PRE_CONDITIONS, POST_CONDITIONS), true),
    Mode6("mode6", AnnotationTypes.entries, true),
}

enum class Extensions(
    val strRepl: String,
    val ctor: (List<AnnotationTypes>) -> Language,
) {
    Nagini("py", ::NaginiLanguage),
    Dafny("dfy", ::DafnyLanguage),
    Verus("rs", ::VerusLanguage),
}

val modesConditionsGenerators = Modes.entries.filter { PRE_CONDITIONS in it.removeAnnotations }
val modesCodeGenerators = Modes.entries.filter { IMPLS in it.removeAnnotations }

enum class AgenticTools(
    val strRepl: String,
    val modeCompatibility: List<Modes>,
    val langCompatibility: List<Extensions>,
    val default: Boolean,
    val ctor: ((ExperimentEnvironment) -> ToolSet)?,
) {
    InequalitiesReplacer("InequalitiesReplacer", Modes.entries, listOf(Extensions.Nagini), false, null),
    ImplicationReplacer("ImplicationReplacer", Modes.entries, listOf(Extensions.Nagini), false, null),

    InvariantsInserter("InvariantsInserter", Modes.entries, Extensions.entries, true, ::InvariantsToolSet),
    InvariantsRewriter("InvariantsRewriter", Modes.entries, Extensions.entries, true, ::InvariantsToolSet),
    InvariantsRemover("InvariantsRemover", Modes.entries, Extensions.entries, true, ::InvariantsToolSet),

    ConditionsInserter("ConditionsInserter", modesConditionsGenerators, Extensions.entries, true, ::ConditionsToolSet),
    ConditionsRewriter("ConditionsRewriter", modesConditionsGenerators, Extensions.entries, true, ::ConditionsToolSet),
    ConditionsRemover("ConditionsRemover", modesConditionsGenerators, Extensions.entries, true, ::ConditionsToolSet),

    CodeInserter("CodeInserter", modesCodeGenerators, Extensions.entries, true, ::CodeToolSet),
    CodeRewriter("CodeRewriter", modesCodeGenerators, Extensions.entries, true, ::CodeToolSet),

    ErrorExplainer("ErrorExplainer", Modes.entries, Extensions.entries, false, ::ErrorsToolSet),
    CodeSnippetExtractor("CodeSnippetExtractor", Modes.entries, listOf(Extensions.Nagini), false, ::NaginiErrorsToolSet),
}

enum class CheckerArt {
    ProofSufficiency,
    ConditionsFormalEquality,
}