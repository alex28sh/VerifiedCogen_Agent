package org.example.agenticTools.common

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import org.example.environment.ExperimentEnvironment
import kotlin.io.path.div

class CommonGenerationToolSet(
    env: ExperimentEnvironment,
) : CommonToolSet(env, env.promptDir / "CommonToolSet") {

    @Tool
    @LLMDescription("""
        Tool that modifies annotation parts of proof code, sucha as invariants or formal specifications, and actual implementation code. 
        Aimed to generate code from scratch or fix it, basing on the given verification error.
        
        It can have verification error set to null (if the agent just started solving task) or set to some error, obtained from verifier.
        If verification error is not null and from this error you can conclude, for example, that some postcondition cannot be proven - 
        then the point of this tool is to fix this error by adding helping preconditions or postconditions, modifying invariants or parts of executable code.
    """)
    suspend fun addCommonCode() : String =
        commonToolCall("commonModification.txt", "commonSystem.txt", "common modification prompt")

}