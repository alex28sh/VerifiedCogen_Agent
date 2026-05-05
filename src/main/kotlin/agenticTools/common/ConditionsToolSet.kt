package agenticTools.common

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import org.example.agenticTools.common.CommonToolSet
import org.example.environment.ExperimentEnvironment
import kotlin.io.path.div

@LLMDescription(
    """
    A set of tools for adding/rewriting/removing conditions in a verified code.
    Tools here can add new pre/postconditions, so that they will represent the actual method behavior and/or help to prove other postconditions.
    Or they rewrite current conditions in a more provable way - for example, split complex conditions in a easier ones or fixing errors in them (for example, bounds of indices)
    They can remove useless or/and incorrect conditions (also, they can remove postcondition, if it's rather simpler to remove it and write a newer one than fixing the current postcondition)
    """
)
class ConditionsToolSet(
    env: ExperimentEnvironment,
) : CommonToolSet(env, env.promptDir / "ConditionsToolSet") {

    @Tool
    @LLMDescription("""
        Tool that adds preconditions/postconditions that reflect the actual behavior of method.
        These conditions can help to prove other postconditions. 
        Added preconditions can eliminate proof inconsistencies inside method body (such as error, happening in accessing an array by index). 
        
        It can have verification error set to null (if the agent just started solving task) or set to some error, obtained from verifier.
        If verification error is not null and from this error you can conclude, for example, that some postcondition cannot be proven - 
        then the point of this tool is to fix this error by adding helping preconditions or postconditions.
        Please, don't use this tool in case of syntactic errors or in case some other parts of code should be rewritten (without adding pre/postconditions - for example, when we need to rewrite invariants)
    """)
    suspend fun addConditions() : String =
        commonToolCall("addConditions.txt", "conditionsSystem.txt", "adding conditions prompt")

    @Tool
    @LLMDescription("""
        Tool that removes preconditions/postconditions that don't reflect the actual behavior of method or are being hard to prove.
        
        It can have verification error set to null (if the agent just started solving task) or set to some error, obtained from verifier.
        If verification error is not null and from this error you can conclude, for example, that some postcondition cannot be proven - 
        then the point of this tool is to fix this error by adding helping preconditions or postconditions.
        Please, don't use this tool in case of syntactic errors or in case some other parts of code should be rewritten (without adding pre/postconditions - for example, when we need to rewrite invariants)
    """)
    suspend fun removeConditions() : String =
        commonToolCall("removeConditions.txt", "conditionsSystem.txt", "removing conditions prompt")

    @Tool
    @LLMDescription("""
        Tool that rewrites preconditions/postconditions that either contain minor errors or are being hard to prove, so that rewriting/splitting them can simplify verification.

        It can have verification error set to null (if the agent just started solving task) or set to some error, obtained from verifier.
        If verification error is not null and from this error you can conclude, for example, that some postcondition cannot be proven - 
        then the point of this tool is to fix this error by adding helping preconditions or postconditions.
        Please, don't use this tool in case of syntactic errors or in case some other parts of code should be rewritten (without adding pre/postconditions - for example, when we need to rewrite invariants)
    """)
    suspend fun rewriteConditions() : String =
        commonToolCall("rewriteConditions.txt", "conditionsSystem.txt", "rewriting conditions prompt")
}