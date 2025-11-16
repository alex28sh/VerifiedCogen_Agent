package org.example.commonTools

import ai.koog.prompt.message.Message
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun fetchTokens(response: Message.Response): Double? {
    val jsonMap =
        response.metaInfo.additionalInfo["GRAZIE_SPENT_CREDIT"]?.let {
            val jsonElement = Json.parseToJsonElement(it)
            jsonElement.jsonObject.mapValues { (_, v) ->
                when (v) {
                    is JsonPrimitive -> v.jsonPrimitive.content
                    else -> v.toString()
                }
            }
        }

    return jsonMap?.get("amount")?.toDouble()
}