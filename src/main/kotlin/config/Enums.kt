package org.example.config

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
import org.example.tools.common.ErrorsToolSet
import org.example.tools.common.InvariantsToolSet
import org.example.tools.nagini.NaginiErrorsToolSet

enum class Model(val model: LLModel, val strRepl: String) {
    GPT4_1(JetBrainsAIModels.OpenAI_GPT4_1, "gpt4.1"),
    GPT4o(JetBrainsAIModels.OpenAI_GPT4o, "gpt4o"),
    GPT4oMini(JetBrainsAIModels.OpenAI_GPT4oMini, "gpt4o-mini"),
//    O1Mini(JetBrainsAIModels.OPENAI_O, "o1-mini"),
    O3Mini(JetBrainsAIModels.OpenAI_O3Mini, "o3-mini"),
//    O1(JetBrainsAIModels., "o1"),
    O3(JetBrainsAIModels.OpenAI_O3, "o3"),
    O4Mini(JetBrainsAIModels.OpenAI_O4Mini, "o4-mini"),

//    GPT4_1(OpenAIModels.Chat.GPT4_1, "gpt4.1"),
//    GPT4o(OpenAIModels.Chat.GPT4o, "gpt4o"),
//    GPT4oMini(OpenAIModels.Reasoning.GPT4oMini, "gpt4o-mini"),
//    O1Mini(OpenAIModels.Reasoning.O1Mini, "o1-mini"),
//    O3Mini(OpenAIModels.Reasoning.O3Mini, "o3-mini"),
//    O1(OpenAIModels.Reasoning.O1, "o1"),
//    O3(OpenAIModels.Reasoning.O3, "o3"),
//    O4Mini(OpenAIModels.CostOptimized.O4Mini, "o4-mini"),
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

    ConditionsInserter("ConditionsInserter", modesConditionsGenerators, Extensions.entries, true, null),
    ConditionsRewriter("ConditionsRewriter", modesConditionsGenerators, Extensions.entries, true, null),
    ConditionsRemover("ConditionsRemover", modesConditionsGenerators, Extensions.entries, true, null),

    CodeInserter("CodeInserter", modesCodeGenerators, Extensions.entries, true, null),
    CodeRewriter("CodeRewriter", modesCodeGenerators, Extensions.entries, true, null),

    ErrorExplainer("ErrorExplainer", Modes.entries, Extensions.entries, false, ::ErrorsToolSet),
    CodeSnippetExtractor("CodeSnippetExtractor", Modes.entries, listOf(Extensions.Nagini), false, ::NaginiErrorsToolSet),
}

enum class CheckerArt {
    ProofSufficiency,
    ConditionsFormalEquality,
}