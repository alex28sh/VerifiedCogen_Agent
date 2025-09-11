package org.example.verifierTools

import java.nio.file.Path

abstract class ResponseChecker {

    protected abstract val innerChecker: ResponseChecker?

    protected abstract fun checkResponse(filePath: Path): Pair<Boolean, String>

    fun checkResponseFolded(filePath: Path): Pair<Boolean, String> {
        innerChecker?.checkResponseFolded(filePath)
            ?.takeIf { it.first }?.let { return it }

        return checkResponse(filePath)
    }
}