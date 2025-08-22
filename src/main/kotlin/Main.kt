package org.example

import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels

import ai.jetbrains.code.prompt.llm.JetBrainsAIModels
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment.Staging
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import kotlinx.coroutines.runBlocking

fun main() {

////    println(GrazieEnvironment.Production.toString())
//    val token = "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJHcmF6aWUgQXV0aGVudGljYXRpb24iLCJ1aWQiOiJmNWFkMzk3MS00MTc0LTRhMzUtYTJmZS04ZDZiODg5ODQxOTEiLCJ1c2VyX3N0YXRlIjoiSU5URVJOQUwiLCJyZWdpc3RyYXRpb25fZGF0ZSI6MTcxNzY3MDY0NTU2NCwidG9rZW5fdHlwZSI6InVzZXIiLCJsaWNlbnNlIjoiNUgwUk1MMUQ3RSIsImxpY2Vuc2VfdHlwZSI6ImdyYXppZS5pbmRpdmlkdWFsLmxpdGUiLCJjdXN0b21lcl9jb2RlIjoxODcyMzk3MiwiZXhwIjoxNzU1ODkwMTA0fQ.BxmIYu8GZZ4SEYgLWX0ir6MaArMvBbDruVxTi0MfIqSjLPBBT4u5RaHJrO-5QFJHGTOKG7yaZq49NL6m_XMPfjgydM3qlo4zTw6LM97fHeLaMSOUe1gphEma9eiFi1vPtZxSt_40jqBcrB2PwR8IAu0j-LvLRSBo5zX25h4lI-ePIlL9CKHousI1VmzcFBJ2YjS8fp4mURJxACfQ7HLEVSZ1oyT8hfaMIi2869jRUbWaruQSQ5LNUeaEXnih1yRL162uZgUE8pmPvpqtignRXjy1QwzI5d1hXG327FOLOjrIfUP1TCI7EakJ-MV800_ZLtbOt_84sJTqUJ8hp8w_yw"
////    val token = "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJHcmF6aWUgQXV0aGVudGljYXRpb24iLCJ1aWQiOiJkNTkyOTFhNS00NzZjLTQyYjctYjcxNC0yNzFkZjg1ODY2NDIiLCJ1c2VyX3N0YXRlIjoiSU5URVJOQUwiLCJyZWdpc3RyYXRpb25fZGF0ZSI6MTcxNzY2NzkyMTI2NCwidG9rZW5fdHlwZSI6InVzZXIiLCJsaWNlbnNlIjoiNUgwUk1MMUQ3RSIsImxpY2Vuc2VfdHlwZSI6ImdyYXppZS5pbmRpdmlkdWFsLmxpdGUiLCJjdXN0b21lcl9jb2RlIjoxODcyMzk3MiwiZXhwIjoxNzU1ODkyMjk1fQ.gdJ30nAu6KRvd8oFFR6JU4EnygHdVksueDjlFc0Bbu2FGZ-Mt4r8KnBMaHYBZWXFXnms_f_k9h4ysj2jMNtTzfzNI62zQewKH4B7R95aukT-r-pnbtYOznap36d3a9vdO45ik4NkRCzycS803kTKharU09P1vwCN2BKg4kx5IaU8DtK8OWXNgaoE5DamVS2LOWmlB3bLI9XxvZZseGV_Srf-t-qMCdo_V6qM2nZz-pbVNyAvYvSpLWLhulhnf5w8ml5v6kx-FfxSNHNtUfD4mfSnFToQQrVNt8SjdsATtloXHjnCQAXFKVu4LXG1LTZ2YL0W2TjQxl-YpMGKmKHDSw"
//    val executor = simpleGrazieExecutor(
//        token = token,
////        environment = GrazieEnvironment.Staging,
//        baseUrl = "https://api.stgn.jetbrains.ai/" // null // or provide explicit URL like "https://platform.jetbrains.ai/"
////        baseUrl = "https://api.jetbrains.ai/" // null // or provide explicit URL like "https://platform.jetbrains.ai/"
//    )
    val token = "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"
    val executor = SingleLLMPromptExecutor(OpenAILLMClient(token))
    val res = runBlocking {
        executor.execute(
            prompt("addition prompt") {
                system("you are a calculator")
                user("add 2 and 3")
            },
//            model = JetBrainsAIModels.OpenAI_GPT4o,
            model = OpenAIModels.Reasoning.O3,
            tools = emptyList()
        )
    }
    println(res[0].content)
}