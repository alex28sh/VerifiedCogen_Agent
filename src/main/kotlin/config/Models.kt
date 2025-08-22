package org.example.config

import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel

enum class Model(val model: LLModel) {
    GPT4_1(OpenAIModels.Chat.GPT4_1),
    GPT4o(OpenAIModels.Chat.GPT4o),
    GPT4oMini(OpenAIModels.Reasoning.GPT4oMini),
    O1Mini(OpenAIModels.Reasoning.O1Mini),
    O3Mini(OpenAIModels.Reasoning.O3Mini),
    O1(OpenAIModels.Reasoning.O1),
    O3(OpenAIModels.Reasoning.O3),
    O4Mini(OpenAIModels.CostOptimized.O4Mini),


}