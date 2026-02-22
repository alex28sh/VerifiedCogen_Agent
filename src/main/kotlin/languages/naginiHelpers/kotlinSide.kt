package org.example.languages.naginiHelpers

import jep.SharedInterpreter
import jep.MainInterpreter
import jep.JepConfig
import kotlin.io.path.Path
import kotlin.io.path.readText
import java.io.File

private var jepInitialized = false

private fun setupJep() {
    if (jepInitialized) return

    val venvPath = File("venv").absolutePath
    val jepLibPath = File("venv/lib/python3.12/site-packages/jep").absolutePath
    val pythonPath = File("venv/lib/python3.12/site-packages").absolutePath

    if (File(jepLibPath).exists()) {
        val currentLibPath = System.getProperty("java.library.path") ?: ""
        if (!currentLibPath.contains(jepLibPath)) {
            val newLibPath = if (currentLibPath.isEmpty()) jepLibPath else "$jepLibPath${File.pathSeparator}$currentLibPath"
            System.setProperty("java.library.path", newLibPath)
            
            // Critical: Force refresh of library path
            try {
                val field = ClassLoader::class.java.getDeclaredField("sys_paths")
                field.isAccessible = true
                field.set(null, null)
            } catch (e: Exception) {
                // Ignore failure on newer JDKs where this might not work
                // But it's worth trying for older ones or specific setups
            }
        }

        MainInterpreter.setJepLibraryPath("$jepLibPath/libjep.so")
        
        val config = JepConfig()
        config.addIncludePaths(pythonPath)
        SharedInterpreter.setConfig(config)

        // Set environment variables for the current process
        val processBuilder = ProcessBuilder()
        val env = processBuilder.environment()
        env["PYTHONHOME"] = venvPath
        env["PYTHONPATH"] = pythonPath
    }

    jepInitialized = true
}

fun detectAndReplacePureCallsNagini(code: String, pureNonHelpers: List<String>): Pair<List<String>, String> {
    setupJep()
    val pythonSide = try {
        Path("src/main/kotlin/languages/naginiHelpers/pureCallDetectors.py").readText()
    } catch (e: Exception) {
        Path("languages/naginiHelpers/pureCallDetectors.py").readText()
    }
    val detectedCalls: List<String>
    val newCode: String
    SharedInterpreter().use { jep ->
        jep.exec(pythonSide)
        jep.set("code", code)
        jep.set("pure_non_helpers", pureNonHelpers)
        jep.eval("(detected_calls, new_code) = detect_and_replace_pure_calls_nagini(code, pure_non_helpers)")
        detectedCalls = jep.getValue("detected_calls", List::class.java) as List<String>
        newCode = jep.getValue("new_code", String::class.java)
    }
    return Pair(detectedCalls, newCode)
}

fun fixSyntaxErrorsNagini(code: String): String {
    setupJep()
    val pythonSide = try {
        Path("src/main/kotlin/languages/naginiHelpers/syntaxFixer.py").readText()
    } catch (e: Exception) {
        Path("languages/naginiHelpers/syntaxFixer.py").readText()
    }
    val newCode: String
    SharedInterpreter().use { jep ->
        jep.exec(pythonSide)
        jep.set("code", code)
        jep.eval("new_code = fix_syntax_errors_nagini(code)")
        newCode = jep.getValue("new_code", String::class.java)
    }
    return newCode
}
