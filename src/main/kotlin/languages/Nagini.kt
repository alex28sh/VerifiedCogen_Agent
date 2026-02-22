package org.example.languages

import org.example.languages.naginiHelpers.detectAndReplacePureCallsNagini
import org.example.languages.naginiHelpers.fixSyntaxErrorsNagini

class NaginiLanguage(removeAnnotations: List<AnnotationTypes>) : GenericLanguage(
    methodRegex = methodRegex,
    pureRegex = pureRegex,
    voidRegex = voidRegex,
    validatorTemplate = NAGINI_VALIDATOR_TEMPLATE,
    validatorTemplatePure = NAGINI_VALIDATOR_TEMPLATE_PURE,
    validatorTemplateVoid = NAGINI_VALIDATOR_TEMPLATE_VOID,
    validatorTemplatePureCopy = NAGINI_VALIDATOR_TEMPLATE_PURE_COPY,
    removePure = AnnotationTypes.PURE in removeAnnotations,
    checkPatterns = removeAnnotations.mapNotNull { annotationByType[it] },
    inlineAssertComment = "# assert-line",
    simpleComment = "#",
) {
    companion object {
        private val NAGINI_VALIDATOR_TEMPLATE = """
            def {method_name}_valid({parameters}) -> {returns}:{specs}
                ret = {method_name}({param_names})
                return ret
        """.trimIndent()

        private val NAGINI_VALIDATOR_TEMPLATE_VOID = """
            def {method_name}_valid({parameters}):{specs}
                {method_name}({param_names})
        """.trimIndent()

        private val NAGINI_VALIDATOR_TEMPLATE_PURE_COPY = """
            @Pure
            def {method_name}_copy_pure({parameters}) -> {returns}:{specs}
                {body}
        """.trimIndent()

        private val NAGINI_VALIDATOR_TEMPLATE_PURE = """
            @Pure
            def {method_name}_valid_pure({parameters}) -> {returns}:{specs}
                ret = {method_name}({param_names})
                return ret
        """.trimIndent()

        val annotationByType: Map<AnnotationTypes, String> = mapOf(
            AnnotationTypes.INVARIANTS to " *# invariants-start.*?# invariants-end\n?",
            AnnotationTypes.ASSERTIONS to " *# assert-start.*?# assert-end\n?",
            AnnotationTypes.PRE_CONDITIONS to " *# pre-conditions-start.*?# pre-conditions-end\n?",
            AnnotationTypes.POST_CONDITIONS to " *# post-conditions-start.*?# post-conditions-end\n?",
            AnnotationTypes.IMPLS to " *# impl-start.*?# impl-end\n?",
            AnnotationTypes.PURE to "@Pure\\ndef.*?# pure-end\n?",
        )

        val methodRegex = Regex(
            pattern = "def\\s+(\\w+)\\s*\\((.*?)\\)\\s*->\\s*(.*?):(.*?)\\s+# (impl-start|pure-start)",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val pureRegex = Regex(
            pattern = "@Pure\\s+def\\s+(\\w+)\\s*\\((.*?)\\)\\s*->\\s*(.*?):(.*?)\\s+# pure-start(.*?)\\s+# pure-end",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val voidRegex = Regex(
            pattern = "def\\s+(\\w+)\\s*\\((.*?)\\)\\s*:(.*?)\\s+# (impl-start|pure-start)",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
    }

    override fun separateValidatorErrors(errors: String): Pair<String, String> {
        val lines = errors.split("\n").filter {
            !it.contains("Verification successful") && !it.contains("Verification took")
        }
        return lines.joinToString("\n") to ""
    }

    override fun checkHelpers(code: String, pureNonHelpers: List<String>): Pair<List<String>, String> {
        return detectAndReplacePureCallsNagini(code, pureNonHelpers)
    }

    override fun findPureNonHelpers(code: String): List<String> {
        val pattern = Regex(
            pattern = "#use-as-unpure\\s+@Pure\\s+def\\s+(\\w+)\\s*\\((.*?)\\)\\s*->\\s*(.*?):",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
        return pattern.findAll(code).map { it.groupValues.getOrNull(1) ?: "" }.filter { it.isNotEmpty() }.toList()
    }

    override fun fixSyntaxErrors(code: String): String {
//        return try {
        return fixSyntaxErrorsNagini(code)
//        } catch (e: Throwable) {
//            // Fallback to manual character-by-character iteration if JEP is not available or fails
//            fixSyntaxErrorsManual(code)
//        }
    }

    private fun fixSyntaxErrorsManual(code: String): String {
        val quantifierRegex = Regex("""(Forall|Exists)\s*\(\s*\w+\s*,\s*lambda\s+[^:]+:\s*""")
        var result = code

        val matches = quantifierRegex.findAll(result).toList()
        if (matches.isEmpty()) return result

        // Process from end to start to avoid offset management with simple indexing
        for (match in matches.reversed()) {
            val startOfInner = match.range.last + 1
            if (startOfInner >= result.length) continue

            // Find the balanced closing parenthesis for Forall
            var balance = 1
            var i = startOfInner
            var endOfInner = -1
            while (i < result.length) {
                if (result[i] == '(') balance++
                else if (result[i] == ')') {
                    balance--
                    if (balance == 0) {
                        endOfInner = i
                        break
                    }
                }
                i++
            }

            if (endOfInner != -1) {
                val innerExpression = result.substring(startOfInner, endOfInner)
                if (!isBalanced(innerExpression.trim())) {
                    val wrapped = "($innerExpression)"
                    result = result.substring(0, startOfInner) + wrapped + result.substring(endOfInner)
                }
            }
        }
        
        // Handle nested Forall by recursion or repeating until stable
        if (result != code) {
            return fixSyntaxErrorsManual(result)
        }

        return result
    }

    private fun isBalanced(expression: String): Boolean {
        if (!expression.startsWith("(") || !expression.endsWith(")")) return false
        var balance = 0
        for (i in expression.indices) {
            if (expression[i] == '(') balance++
            else if (expression[i] == ')') balance--
            if (balance == 0 && i < expression.length - 1) return false
        }
        return balance == 0
    }
}
