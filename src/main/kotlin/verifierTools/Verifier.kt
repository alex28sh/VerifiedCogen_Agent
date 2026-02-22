package org.example.verifierTools

import java.time.Duration
import java.time.Instant
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.io.path.name

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
        val tempDir = Files.createTempDirectory("verifier_")
        val targetPath = tempDir.resolve(filePath.name)
        Files.copy(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING)

        val process = ProcessBuilder("bash", "-c", "$verifierCmd ${targetPath.name}")
            .directory(tempDir.toFile())
            .redirectErrorStream(false)
            .start()

        return try {
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                cleanupZ3Processes(timeoutSeconds)
                process.destroyForcibly()
                tempDir.toFile().deleteRecursively()
                return null
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            cleanupZ3Processes(timeoutSeconds)
            process.destroy()
            tempDir.toFile().deleteRecursively()

            Pair(process.exitValue() == 0, stdout + "\n" + stderr)
        } catch (e: Exception) {
            cleanupZ3Processes(timeoutSeconds)
            process.destroyForcibly()
            tempDir.toFile().deleteRecursively()
            Pair(false, e.message ?: "process crashed with unknown error")
        }
    }
}