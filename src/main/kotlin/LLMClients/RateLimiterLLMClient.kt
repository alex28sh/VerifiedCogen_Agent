package org.example.LLMClients

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlinx.coroutines.flow.Flow

import com.neutrine.krate.rateLimiter
import kotlinx.coroutines.runBlocking
import java.time.temporal.ChronoUnit

class RateLimiterLLMClient(
    private val delegate: LLMClient,
    model: LLModel,
) : LLMClient {

    private val eps = 100
    private val rateLimiter = rateLimiter(maxRate = (LLMBandwidths.user[model] ?: 1_000) - eps) {
        maxRateTimeUnit = ChronoUnit.HOURS
    }

    init {
        println("Rate limit for model ${model.id}: ${LLMBandwidths.user[model]}")
    }


//    val rateLimiterConfig = RateLimiterConfig.custom()
//        .limitRefreshPeriod(Duration.ofHours(1))
//        .limitForPeriod(100)
//        .timeoutDuration(Duration.ofSeconds(5))
//        .build()
//
//    val rateLimiter = RateLimiter.of("verified-cogen", rateLimiterConfig)

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): List<Message.Response> {
        rateLimiter.awaitUntilTake()
        return delegate.execute(prompt, model, tools)
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> {
        runBlocking {
            rateLimiter.awaitUntilTake()
        }
        return delegate.executeStreaming(prompt, model)
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult {
        return delegate.moderate(prompt, model)
    }
}