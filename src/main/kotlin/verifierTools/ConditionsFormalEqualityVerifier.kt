package org.example.verifierTools

import org.example.languages.Language
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ConditionsFormalEqualityVerifier(
    private val verifier: Verifier,
    private val promptDir: Path,
    private val language: Language,
    private val originalProgram: String,
    private val removeHelpers: Boolean,
    override val innerChecker: ResponseChecker?
) : ResponseChecker() {
    override fun checkResponse(filePath: Path): Pair<Boolean, String> {
        val validators = language.generateValidators(originalProgram, !removeHelpers)
        val currentCode = filePath.readText()
        filePath.appendText("\n" + language.simpleComment + " ==== verifiers ==== \n" + validators)
        val res = verifier.verify(filePath)
        filePath.writeText(currentCode)
        return res ?: Pair(false, (promptDir / "verifierTools" / "timeoutFormalEquality.txt").readText())
    }
}