package org.example

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableClientWithBackoff
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GrazieLLMClient
//import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.params.LLMParams
import org.example.config.BaseLLMClient
import org.example.config.CliConfig

//fun simpleGrazieClient(
//    token: String,
//    environment: GrazieEnvironment = GrazieEnvironment.Staging,
//    grazieAgent: GrazieAgent = GrazieAgent("koog-agents-workshop", "dev"),
//    baseUrl: String
//): GrazieLLMClient {
//    val apiClient = SuspendableAPIGatewayClient(
//        baseUrl,
//        SuspendableHTTPClient.WithV5(
//            SuspendableClientWithBackoff(
//                GrazieKtorHTTPClient.Client.Default,
//            ), AuthData(
//                token,
//                grazieAgent = grazieAgent
//            )
//        )
//    )
//    return GrazieLLMClient(apiClient)
//}

fun getBaseLLMClient(config: CliConfig): LLMClient {
    return when(config.llmProfile.baseLLMClient) {
        BaseLLMClient.GrazieClient -> {
            GrazieLLMClient(
                client = SuspendableAPIGatewayClient(
                    serverUrl = "https://api.app.stgn.grazie.aws.intellij.net/",
                    httpClient = SuspendableHTTPClient.WithV5(
                        SuspendableClientWithBackoff(
                            GrazieKtorHTTPClient.Client.WithExtendedTimeout,
                        ), AuthData(
                            token = config.token,
                            grazieAgent = GrazieAgent("verified-cogen-agent", "dev")
                        )
                    ),
                    authType = if (config.isApplication) AuthType.Application else AuthType.User,
                ),
                default = LLMParams(
                    temperature = config.temperature.takeIf { !config.llmProfile.model.id.contains("o1") && !config.llmProfile.model.id.contains("o3") && !config.llmProfile.model.id.contains("o4") },
                    /// TODO: some things like thinking budget, maxTokens...
                )
            )
        }
        BaseLLMClient.OpenAIClient -> {
            OpenAILLMClient(apiKey = config.token)
        }
    }
}