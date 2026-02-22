package org.example.languages

class VerusLanguage(removeAnnotations: List<AnnotationTypes>) : GenericLanguage(
    methodRegex = methodRegex,
    pureRegex = pureRegex,
    voidRegex = voidRegex,
    validatorTemplate = VERUS_VALIDATOR_TEMPLATE,
    validatorTemplatePure = VERUS_VALIDATOR_TEMPLATE_PURE,
    validatorTemplateVoid = VERUS_VALIDATOR_TEMPLATE_VOID,
    validatorTemplatePureCopy = VERUS_VALIDATOR_TEMPLATE_PURE_COPY,
    removePure = AnnotationTypes.PURE in removeAnnotations,
    checkPatterns = removeAnnotations.mapNotNull { annotationByType[it] },
    inlineAssertComment = "// assert-line",
    simpleComment = "//",
) {

    companion object {
        private val VERUS_VALIDATOR_TEMPLATE = """
            fn {method_name}_valid({parameters}) -> ({returns}){specs}
            { let ret = {method_name}({param_names}); ret }
        """.trimIndent()

        private val VERUS_VALIDATOR_TEMPLATE_VOID = """
            fn {method_name}_valid({parameters}){specs}
            { {method_name}({param_names}); }
        """.trimIndent()

        private val VERUS_VALIDATOR_TEMPLATE_PURE_COPY = """
            spec fn {method_name}_copy_pure({parameters}) -> ({returns}){specs}
            {{body}}
        """.trimIndent()

        private val VERUS_VALIDATOR_TEMPLATE_PURE = """
            spec fn {method_name}_valid_pure({parameters}) -> ({returns}){specs}
            { let ret = {method_name}({param_names}); ret }
        """.trimIndent()

        val annotationByType: Map<AnnotationTypes, String> = mapOf(
            AnnotationTypes.INVARIANTS to " *// invariants-start.*?// invariants-end\n?",
            AnnotationTypes.ASSERTIONS to " *// assert-start.*?// assert-end\n?",
            AnnotationTypes.PRE_CONDITIONS to " *// pre-conditions-start.*?// pre-conditions-end\n?",
            AnnotationTypes.POST_CONDITIONS to " *// post-conditions-start.*?// post-conditions-end\n?",
            AnnotationTypes.IMPLS to " *// impl-start.*?// impl-end\n?",
            AnnotationTypes.PURE to " *(spec fn|proof fn).*?// pure-end\n?",
        )

        val methodRegex = Regex(
            pattern = "^\\s*fn\\s+(\\w+)\\s*\\((.*?)\\)\\s*->\\s*\\((.*?)\\)(.*?)\\{",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )

        val pureRegex = Regex(
            pattern = "^\\s*spec fn\\s+(\\w+)\\s*\\((.*?)\\)\\s*->\\s*\\((.*?)\\)(.*?)\\{(.*?)}\\s*\\n\\s*//\\s*pure-end",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )

        val voidRegex = Regex(
            pattern = "^\\s*fn\\s+(\\w+)\\s*\\((.*?)\\)(.*?)\\{",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )
    }

    override fun splitParams(parameters: String): String {
        if (parameters.isBlank()) return ""
        // "(\w+)\s*:\s*((?:\[[^\[\]]*\]|\([^()]*\)|[^\[\],])+)"
//        val regex = Regex("(\\w+)\\s*:\\s*((?:\\[[^\\[\\]]*\\]|\\([^()]*\\)|[^\\[\\],])+)")
        val regex = Regex("""(\w+)\s*:\s*((?:\[[^\[\]]*\]|\([^()]*\)|[^\[\],])+)""")
        val matches = regex.findAll(parameters)
        return matches.map { it.groupValues[1].trim() }.joinToString(", ")
    }

    override fun generateValidators(code: String, validateHelpers: Boolean): String {
        val result = super.generateValidators(code, validateHelpers)
        return if (result.isBlank()) result else "verus!{\n$result\n}"
    }

    override fun separateValidatorErrors(errors: String): Pair<String, String> {
        val lines = errors.split("\n").filter { !it.contains("verification results") }
        return lines.joinToString("\n") to ""
    }

    override fun fixSyntaxErrors(code: String): String {
        return code
    }
}
