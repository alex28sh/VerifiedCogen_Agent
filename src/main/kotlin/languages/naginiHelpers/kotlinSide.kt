package org.example.languages.naginiHelpers

import jep.SharedInterpreter
import kotlin.io.path.Path
import kotlin.io.path.readText

fun detectAndReplacePureCallsNagini(code: String, pureNonHelpers: List<String>): Pair<List<String>, String> {
    val pythonSide = Path("pureCallDetectors.py").readText()
    val detectedCalls: List<String>
    val newCode: String
    SharedInterpreter().use { jep ->
        jep.eval(pythonSide)
        jep.set("code", code)
        jep.set("pure_non_helpers", pureNonHelpers)
        jep.eval("(detected_calls, new_code) = detect_and_replace_pure_calls_nagini(code, pure_non_helpers)")
        detectedCalls = jep.getValue("detected_calls", List::class.java) as List<String>
        newCode = jep.getValue("new_code", String::class.java)
    }
    return Pair(detectedCalls, newCode)
}