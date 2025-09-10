package org.example.languages

class DafnyLanguage(removeAnnotations: List<AnnotationTypes>) : GenericLanguage(
    methodRegex = methodRegex,
    pureRegex = pureRegex,
    voidRegex = voidRegex,
    validatorTemplate = DAFNY_VALIDATOR_TEMPLATE,
    validatorTemplatePure = DAFNY_VALIDATOR_TEMPLATE_PURE,
    validatorTemplateVoid = DAFNY_VALIDATOR_TEMPLATE_VOID,
    validatorTemplatePureCopy = DAFNY_VALIDATOR_TEMPLATE_PURE_COPY,
    removePure = AnnotationTypes.PURE in removeAnnotations,
    checkPatterns = removeAnnotations.mapNotNull { annotationByType[it] },
    inlineAssertComment = "// assert-line",
    simpleComment = "//",
) {

    companion object {
        private val DAFNY_VALIDATOR_TEMPLATE = """
        method {method_name}_valid({parameters}) returns ({returns}){specs}
            { var {return_values} := {method_name}({param_names}); return {return_values}; }
        """.trimIndent()

        private val DAFNY_VALIDATOR_TEMPLATE_VOID = """
        method {method_name}_valid({parameters}){specs}
            { {method_name}({param_names}); }
        """.trimIndent()

        private val DAFNY_VALIDATOR_TEMPLATE_PURE_COPY = """
        function {method_name}_copy_pure({parameters}):{returns} {specs}
        { 
            {body} 
        }
        """.trimIndent()

        private val DAFNY_VALIDATOR_TEMPLATE_PURE = """
        function {method_name}_valid_pure({parameters}):{returns} {specs}
            { {method_name}({param_names}) }
        """.trimIndent()

        val annotationByType: Map<AnnotationTypes, String> = mapOf(
            AnnotationTypes.INVARIANTS to " *// invariants-start.*?// invariants-end\n",
            AnnotationTypes.ASSERTIONS to " *// assert-start.*?// assert-end\n",
            AnnotationTypes.PRE_CONDITIONS to " *// pre-conditions-start.*?// pre-conditions-end\n",
            AnnotationTypes.POST_CONDITIONS to " *// post-conditions-start.*?// post-conditions-end\n",
            AnnotationTypes.IMPLS to " *// impl-start.*?// impl-end\n",
            AnnotationTypes.PURE to "(function|lemma|predicate|class).*?// pure-end\n",
        )

        val methodRegex = Regex(
            pattern = "method\\s+(\\w+)\\s*\\((.*?)\\)\\s*returns\\s*\\((.*?)\\)(.*?)\\{",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val pureRegex = Regex(
            pattern = "function\\s+(\\w+)\\s*\\((.*?)\\) *: *(.*?)\\s*(.*?)\\{(.*?)}",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val voidRegex = Regex(
            pattern = "method\\s+(\\w+)\\s*\\((.*?)\\)(.*?)\\{",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
    }

    override fun validatorsFrom(methodName: String, parameters: String, returns: String, specs: String): String {
        val base = super.validatorsFrom(methodName, parameters, returns, specs)
        val count = returns.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
        val returnValues = if (count == 0) "" else (0 until count).joinToString(", ") { "ret$it" }
        return base.replace("{return_values}", returnValues)
    }

    override fun separateValidatorErrors(errors: String): Pair<String, String> {
        val lines = errors.split("\n").filter { !it.contains("Dafny program verifier finished") }
        val idx = lines.indexOfFirst { it.contains("ret0") }
        return if (idx == -1) {
            lines.joinToString("\n") to ""
        } else {
            val nonVerifier = lines.take((idx - 2).coerceAtLeast(0)).joinToString("\n").trim()
            val verifier = lines.drop((idx - 2).coerceAtLeast(0)).joinToString("\n").trim()
            nonVerifier to verifier
        }
    }
}
