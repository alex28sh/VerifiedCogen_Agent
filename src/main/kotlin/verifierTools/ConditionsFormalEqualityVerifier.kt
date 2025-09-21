package org.example.verifierTools

import org.example.languages.Language
import java.nio.file.Path
import kotlin.io.path.*

class ConditionsFormalEqualityVerifier(
    private val verifier: Verifier,
    private val promptDir: Path,
    private val language: Language,
    private val originalProgram: String,
    private val removeHelpers: Boolean,
    override val innerChecker: ResponseChecker?
) : ResponseChecker() {
    override fun checkResponse(filePath: Path): Pair<Boolean, String> {
        println(originalProgram)
        val validators = language.generateValidators(originalProgram, !removeHelpers)
        val currentCode = filePath.readText()
        val validationCode = currentCode + "\n" + language.simpleComment + " ==== verifiers ==== \n" + validators
//        filePath.appendText("\n" + language.simpleComment + " ==== verifiers ==== \n" + validators)

//        {
            val name = filePath.nameWithoutExtension
            val ext = filePath.extension
            val parent = filePath.parent

            val newName =
                "${name}ConditionsFormalEqualityVerifier.$ext"
            val validationPath = parent / newName
            validationPath.writeText(validationCode)
//        }
        val res = verifier.verify(validationPath)
//        filePath.writeText(currentCode)
        return res ?: Pair(false, (promptDir / "verifierTools" / "timeoutFormalEquality.txt").readText())
    }
}