package org.example.verifierTools

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readText

class ProofSufficiencyChecker(
    private val verifier: Verifier,
    private val promptDir: Path,
    override val innerChecker: ResponseChecker?
) : ResponseChecker() {

    override fun checkResponse(filePath: Path): Pair<Boolean, String> =
        verifier.verify(filePath) ?: Pair(false, (promptDir / "verifierTools" / "timeout.txt").readText())
}