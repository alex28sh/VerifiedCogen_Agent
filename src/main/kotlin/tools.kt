package org.example

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableClientWithBackoff
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GrazieLLMClient
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor

fun simpleGrazieClient(
    token: String,
    environment: GrazieEnvironment = GrazieEnvironment.Staging,
    grazieAgent: GrazieAgent = GrazieAgent("koog-agents-workshop", "dev"),
    baseUrl: String
): GrazieLLMClient {
    val apiClient = SuspendableAPIGatewayClient(
        baseUrl,
        SuspendableHTTPClient.WithV5(
            SuspendableClientWithBackoff(
                GrazieKtorHTTPClient.Client.Default,
            ), AuthData(
                token,
                grazieAgent = grazieAgent
            )
        )
    )
    return GrazieLLMClient(apiClient)
}