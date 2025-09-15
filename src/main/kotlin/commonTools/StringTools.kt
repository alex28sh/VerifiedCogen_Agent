package org.example.commonTools

import org.example.environment.ExperimentEnvironment

fun String.insertAt(index: Int, insert: String): String {
    require(index in 0..length) { "Index $index out of bounds for length $length" }
    return substring(0, index) + insert + substring(index)
}

fun String.previousErrorPrompt(env: ExperimentEnvironment) =
    this.replace("{ previousError }", env.lastTestResult.error?.let {
           "The code above gets the following verification error:\n" +
                   it
        } ?: run {
            "The code above is a code, received by an agent when starting the task.\n" +
                    "It hasn't yet been tested against verifier.\n" +
                    "You should come up with plausible invariants that will help to prove conditions and avoid verification errors."
        }
    )

fun String.codePrompt(env: ExperimentEnvironment) =
    this.replace("{ code }", env.lastTestResult.generatedCode)