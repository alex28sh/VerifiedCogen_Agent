package org.example.verifierTools

import java.time.Duration
import java.time.Instant
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolute

fun cleanupZ3Processes(timeoutSeconds: Long = 60) {
    val now = Instant.now()
    ProcessHandle.allProcesses().forEach { ph ->
        val info = ph.info()
        val cmd = info.command().orElse("")
        if (cmd.contains("z3")) {
            val start = info.startInstant().orElse(null)
            if (start != null) {
                val elapsed = Duration.between(start, now).seconds
                if (elapsed > timeoutSeconds + 10) {
                    try { ph.destroyForcibly() } catch (_: Exception) {}
                }
            }
        }
    }
}

class Verifier(private val verifierCmd: String, private val timeoutSeconds: Long = 60) {
    fun verify(filePath: Path): Pair<Boolean, String>? {

        val process = ProcessBuilder("bash", "-c", verifierCmd + " " + filePath.absolute().toString())
            .redirectErrorStream(false)
            .start()

        return try {
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                cleanupZ3Processes(timeoutSeconds)
                process.destroyForcibly()
                return null
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            cleanupZ3Processes(timeoutSeconds)
            process.destroy()

            Pair(process.exitValue() == 0, stdout + "\n" + stderr)
        } catch (e: Exception) {
            cleanupZ3Processes(timeoutSeconds)
            process.destroyForcibly()
            Pair(false, e.message ?: "process crashed with unknown error")
        }
    }
}