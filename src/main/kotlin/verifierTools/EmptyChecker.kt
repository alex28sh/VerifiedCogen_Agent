package org.example.verifierTools

import java.nio.file.Path

class EmptyChecker() : ResponseChecker() {
    override val innerChecker: ResponseChecker?
        get() = null

    override fun checkResponse(filePath: Path): Pair<Boolean, String> {
        return Pair(true, "")
    }
}