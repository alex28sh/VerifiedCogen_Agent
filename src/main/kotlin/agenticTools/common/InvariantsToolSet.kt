package org.example.tools.common

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

@LLMDescription("""
    A set of tools for adding/rewriting/removing invariants in a verified code.
    Tools here, either add new invariants to help in proving more postconditions.
    They can add invariants to prove other more complex invariants.
    Or they rewrite current invariants in a more provable way - for example, split complex invariants in a easier ones or fixing errors in them (for example, bounds of indices)
    They can remove useless or/and incorrect invariants (also, they can remove invariant, if it's rather simpler to remove it and write a newer one than fixing the current invariant
    """)
class InvariantsToolSet(
    val promptExecutor: PromptExecutor,
    val model: LLModel,
    val promptsPath: Path,
    val taskDescription: String?,
) : ToolSet {

    @Tool
    @LLMDescription(
        """
            Tool that adds invariants either to prove postconditions or other invariants.
            It can have previousError set to null (if the agent just started solving task) or set to some error, obtained from verifier.
            If previousError is not null and from this error you can conclude, for example, that some invariant or postcondition cannot be proven - 
            then the point of this tool is to fix this error by adding helping invariants.
            Please, don't use this tool in case of syntactic errors or in case some other parts of code should be rewritten (without adding invariants - for example, when we need to rewrite postconditions)
        """
    )
    fun addInvariants(
        @LLMDescription("previousError is either null (if agent just got the task to fill in the gaps) or some error from prover (that agent got when sending code to the prover)")
        previousError: String?,
        @LLMDescription("code for the task that agent has by this time (and it need to be fixed)")
        code: String,
    ): String = runBlocking {
        val pathFile = promptsPath / "addInvariants.txt"
        var promptText = Files.readString(pathFile)
            .replace("{ previousError }", previousError?.let {
                   "The code above gets the following verification error:\n" +
                           previousError
                } ?: run {
                    "The code above is a code, received by an agent when starting the task.\n" +
                    "It hasn't yet been tested against verifier.\n" +
                    "You should come up with plausible invariants that will help to prove conditions and avoid verification errors."
                }
            )
            .replace("{ code }", code)
        if (taskDescription != null) {
            promptText = promptText.replace("{ taskDescription }", taskDescription)
        }
        promptExecutor.execute(
            prompt = prompt("adding invariants prompt") {
                system(Files.readString(promptsPath / "invariantsSystem.txt"))
                user(promptText)
            }, model = model, tools = emptyList()
        )[0].content
    }

    @Tool
    @LLMDescription(
        """
            Tool that removes invariants that are either incorrect or very hard to prove and not useful for proving postconditions.
            It can have previousError set to null (if the agent just started solving task) or set to some error, obtained from verifier.
            If previousError is not null and from this error you can conclude, for example, that some invariant cannot be proven, 
            moreover, it's too hard to be proven or/and is unneeded to prove any useful postconditions - then you can decide just to remove an invariant.
            Please, don't use this tool in case of syntactic errors or in case some other parts of code should be rewritten (without adding invariants - for example, when we need to rewrite postconditions) 
        """
    )
    fun removeInvariants(
        @LLMDescription("some error from prover (that agent got when sending code to the prover)")
        previousError: String,
        @LLMDescription("code for the task that agent has by this time (and it need to be fixed)")
        code: String,
    ): String = runBlocking {
        val pathFile = promptsPath / "removeInvariants.txt"
        var promptText = Files.readString(pathFile)
            .replace("{ previousError }",
                "The code above gets the following verification error.\n" +
                    previousError
            )
            .replace("{ code }", code)
        if (taskDescription != null) {
            promptText = promptText.replace("{ taskDescription }", taskDescription)
        }
        promptExecutor.execute(
            prompt = prompt("removing invariants prompt") {
                system(Files.readString(promptsPath / "invariantsSystem.txt"))
                user(promptText)
            }, model = model, tools = emptyList()
        )[0].content
    }

    @Tool
    @LLMDescription(
        """
            Tool that rewrites invariants that are either incorrect or very hard to prove, but still they can be useful for proving other invariants or postcondintions.
            Typically, when you can not prove some invariant, you can think about rewriting it in another terms 
            (that may be, for example, more plausible for the style of proofs for this particular verification framework. 
            For example, when having an invariant with some example of `forall` statement (it can be written in another way - depending on a framework) verifier could require additional triggers (expressions aimed to optimize usage/proofs of forall statements).
            Sometimes, it's hard to choose the right trigger - therefore, verifier can stall or give up. Then, you would have to choose another trigger.
            Another example - you have a range invariant and you confused range limits. That could rise 'out of bounds' errors or trigger errors in other invariants. So, you would have to rewrite this invariant with correct bounds.
            There are more other examples, when you would need to fix error in invariant/assertion or just rewrite it in a way, that will simplify proof.
            Please, don't use this tool in case of syntactic errors or in case some other parts of code should be rewritten (without adding invariants - for example, when we need to rewrite postconditions) 
        """
    )
    fun rewriteInvariants(
        @LLMDescription("some error from prover (that agent got when sending code to the prover)")
        previousError: String,
        @LLMDescription("code for the task that agent has by this time (and it need to be fixed)")
        code: String,
    ): String = runBlocking {
        val pathFile = promptsPath / "rewriteInvariants.txt"
        var promptText = Files.readString(pathFile)
            .replace("{ previousError }",
                "The code above gets the following verification error.\n" +
                        previousError
            )
            .replace("{ code }", code)
        if (taskDescription != null) {
            promptText = promptText.replace("{ taskDescription }", taskDescription)
        }
        promptExecutor.execute(
            prompt = prompt("rewriting invariants prompt") {
                system(Files.readString(promptsPath / "invariantsSystem.txt"))
                user(promptText)
            }, model = model, tools = emptyList()
        )[0].content
    }
}
