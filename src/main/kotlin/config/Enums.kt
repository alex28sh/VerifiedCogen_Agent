package org.example.config

import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel

enum class Model(val model: LLModel, val strRepl: String) {
    GPT4_1(OpenAIModels.Chat.GPT4_1, "gpt4.1"),
    GPT4o(OpenAIModels.Chat.GPT4o, "gpt4o"),
    GPT4oMini(OpenAIModels.Reasoning.GPT4oMini, "gpt4o-mini"),
    O1Mini(OpenAIModels.Reasoning.O1Mini, "o1-mini"),
    O3Mini(OpenAIModels.Reasoning.O3Mini, "o3-mini"),
    O1(OpenAIModels.Reasoning.O1, "o1"),
    O3(OpenAIModels.Reasoning.O3, "o3"),
    O4Mini(OpenAIModels.CostOptimized.O4Mini, "o4-mini"),
}

enum class Modes(val strRepl: String, val code: Boolean, val conditions: Boolean, val helpers: Boolean) {
    Mode1("mode1", false, false, false),
    Mode2("mode2", false, true, false),
    Mode3("mode3", true, false, false),
    Mode4("mode4", true, false, false),
    Mode5("mode5", true, true, false),
    Mode6("mode6", true, true, true),
}

enum class Extensions(val strRepl: String) {
    Nagini("py"),
    Dafny("dfy"),
    Verus("rs"),
}

val modesConditionsGenerators = Modes.entries.filter { it.conditions }
val modesCodeGenerators = Modes.entries.filter { it.code }

enum class AgenticTools(val strRepl: String, val modeCompatibility: List<Modes>, val langCompatibility: List<Extensions>, val default: Boolean) {
    InequalitiesReplacer("InequalitiesReplacer", Modes.entries, listOf(Extensions.Nagini), false),
    ImplicationReplacer("ImplicationReplacer", Modes.entries, listOf(Extensions.Nagini), false),

    InvariantsInserter("InvariantsInserter", Modes.entries, Extensions.entries, true),
    InvariantsRewriter("InvariantsRewriter", Modes.entries, Extensions.entries, true),
    InvariantsRemover("InvariantsRemover", Modes.entries, Extensions.entries, true),

    ConditionsInserter("ConditionsInserter", modesConditionsGenerators, Extensions.entries, true),
    ConditionsRewriter("ConditionsRewriter", modesConditionsGenerators, Extensions.entries, true),
    ConditionsRemover("ConditionsRemover", modesConditionsGenerators, Extensions.entries, true),

    CodeInserter("CodeInserter", modesCodeGenerators, Extensions.entries, true),
    CodeRewriter("CodeRewriter", modesCodeGenerators, Extensions.entries, true),
}