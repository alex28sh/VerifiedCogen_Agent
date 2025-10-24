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

    private val epsHour = 100
    private val hourLimit = (LLMBandwidths.user[model]?.first ?: 1_000) - epsHour
    private val hourRateLimiter = rateLimiter(maxRate = hourLimit) {
        maxRateTimeUnit = ChronoUnit.HOURS
    }.also {
        println("Rate limit in hour for model ${model.id}: $hourLimit")
    }

    private val epsMin = 10
    private val minuteLimit = (LLMBandwidths.user[model]?.second ?: 100) - epsMin
    private val minuteRateLimiter = rateLimiter(maxRate = minuteLimit) {
        maxRateTimeUnit = ChronoUnit.MINUTES
    }.also {
        println("Rate limit for model ${model.id}: $minuteLimit")
    }

    private suspend fun awaitRateLimiters() {
        // Order: check the smaller window first for quicker feedback in bursts
        minuteRateLimiter.awaitUntilTake()
        hourRateLimiter.awaitUntilTake()
    }

//    val rateLimiterConfig = RateLimiterConfig.custom()
//        .limitRefreshPeriod(Duration.ofHours(1))
//        .limitForPeriod(100)
//        .timeoutDuration(Duration.ofSeconds(5))
//        .build()
//
//    val rateLimiter = RateLimiter.of("verified-cogen", rateLimiterConfig)

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): List<Message.Response> {
        awaitRateLimiters()
        return delegate.execute(prompt, model, tools)
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> {
        runBlocking {
            awaitRateLimiters()
        }
        return delegate.executeStreaming(prompt, model)
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult {
        return delegate.moderate(prompt, model)
    }
}