package org.example.LLMClients

import ai.jetbrains.code.prompt.llm.JetBrainsAIModels
import ai.koog.prompt.llm.LLModel

object LLMBandwidths {

    val user = mapOf<LLModel, Pair<Long, Long>>(
        JetBrainsAIModels.Anthropic_Sonnet_3_7_via_JBAI to Pair(2_000, 200),
        JetBrainsAIModels.Anthropic_Sonnet_4_via_JBAI to Pair(2_000, 200),
//        JetBrainsAIModels.Anthropic_Sonnet_4_5_via_JBAI to Pair(1_000, 100),
        JetBrainsAIModels.Anthropic_Opus_4_via_JBAI to Pair(2_000, 200),
        JetBrainsAIModels.Google_Flash2_5_via_JBAI to Pair(1_000, 100),
        JetBrainsAIModels.Google_Pro2_5_via_JBAI to Pair(100, 50),
        JetBrainsAIModels.OpenAI_GPT5_via_JBAI to Pair(500, 50), /// TODO: look into throttling limits https://github.com/JetBrains/jetbrains-ai-platform
        JetBrainsAIModels.OpenAI_GPT5_Mini_via_JBAI to Pair(1000, 100),
        JetBrainsAIModels.OpenAI_O3_via_JBAI to Pair(500, 50),
        JetBrainsAIModels.OpenAI_O3Mini_via_JBAI to Pair(1000, 100),
        JetBrainsAIModels.OpenAI_O4Mini_via_JBAI to Pair(1000, 100),
    )
//    val user = mapOf(
//        GoogleProfileIDs.Chat.GeminiPro1_5 to setOf(Bandwidth.count(500, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash1_5 to setOf(Bandwidth.count(500, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash2_0 to setOf(Bandwidth.count(1000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlashLite2_0 to setOf(Bandwidth.count(1000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiPro2_5 to setOf(Bandwidth.count(100, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash2_5 to setOf(Bandwidth.count(1_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlashLite2_5 to setOf(Bandwidth.count(1_000, Duration.hours(1))),
//
//        AnthropicProfileIDs.Claude_3_5_Sonnet to setOf(Bandwidth.count(500, Duration.hours(1))),
//        AnthropicProfileIDs.Claude_3_5_Haiku to setOf(Bandwidth.count(500, Duration.hours(1))),
//        AnthropicProfileIDs.Claude_3_7_Sonnet to setOf(Bandwidth.count(2000, Duration.hours(1))),
//        AnthropicProfileIDs.Claude_4_Sonnet to setOf(Bandwidth.count(2000, Duration.hours(1))),
//        AnthropicProfileIDs.Claude_4_Opus to setOf(Bandwidth.count(2000, Duration.hours(1))),
//        AnthropicProfileIDs.Claude_4_1_Opus to setOf(Bandwidth.count(2000, Duration.hours(1))),
//    )
//
//    val service = mapOf(
//        GoogleProfileIDs.Chat.GeminiPro1_5 to setOf(Bandwidth.count(5000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash1_5 to setOf(Bandwidth.count(5000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash2_0 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlashLite2_0 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiPro2_5 to setOf(Bandwidth.count(1_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash2_5 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlashLite2_5 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//
//        AnthropicProfileIDs.Claude_3_5_Sonnet to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_3_5_Haiku to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_3_7_Sonnet to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_4_Sonnet to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_4_Opus to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_4_1_Opus to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//    )
//
//    val application = mapOf(
//        GoogleProfileIDs.Chat.GeminiPro1_5 to setOf(Bandwidth.count(5000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash1_5 to setOf(Bandwidth.count(5000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash2_0 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlashLite2_0 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiPro2_5 to setOf(Bandwidth.count(1_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlash2_5 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//        GoogleProfileIDs.Chat.GeminiFlashLite2_5 to setOf(Bandwidth.count(10_000, Duration.hours(1))),
//
//        AnthropicProfileIDs.Claude_3_5_Sonnet to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_3_5_Haiku to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_3_7_Sonnet to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_4_Sonnet to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_4_Opus to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//        AnthropicProfileIDs.Claude_4_1_Opus to setOf(
//            Bandwidth.count(200, Duration.minutes(1)),
//            Bandwidth.count(5000, Duration.hours(1))
//        ),
//    )
}