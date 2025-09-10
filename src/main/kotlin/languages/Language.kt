package org.example.languages

enum class AnnotationTypes(val strRepl: String) {
    INVARIANTS("invariants"),
    ASSERTIONS("asserts"),
    PRE_CONDITIONS("pre-conditions"),
    POST_CONDITIONS("post-conditions"),
    IMPLS("impls"),
    PURE("pure"),
}

interface Language {
    val simpleComment: String

    fun generateValidators(code: String, validateHelpers: Boolean): String

    fun removeConditions(code: String): String

    fun separateValidatorErrors(errors: String): Pair<String, String>

    fun checkHelpers(code: String, pureNonHelpers: List<String>): Pair<List<String>, String>

    fun findPureNonHelpers(code: String): List<String>
}

open class GenericLanguage(
    private val methodRegex: Regex,
    private val pureRegex: Regex,
    private val voidRegex: Regex,
    private val validatorTemplate: String,
    private val validatorTemplatePure: String,
    private val validatorTemplatePureCopy: String,
    private val validatorTemplateVoid: String,
    private val removePure: Boolean,
    private val checkPatterns: List<String>,
    private val inlineAssertComment: String?,
    override val simpleComment: String,
) : Language {

    protected open fun splitParams(parameters: String): String {
        if (parameters.isBlank()) return ""
        return parameters.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { param ->
                val idx = param.indexOf(":")
                if (idx >= 0) param.substring(0, idx).trim() else param
            }
            .joinToString(", ")
    }

    protected open fun validatorsFrom(
        methodName: String,
        parameters: String,
        returns: String,
        specs: String,
    ): String {
        return validatorTemplate
            .replace("{method_name}", methodName)
            .replace("{parameters}", parameters.ifBlank { "" })
            .replace("{returns}", returns.ifBlank { "" })
            .replace("{specs}", specs.ifBlank { "\n" })
            .replace("{param_names}", splitParams(parameters))
    }

    protected open fun validatorsFromVoid(
        methodName: String,
        parameters: String,
        specs: String,
    ): String {
        return validatorTemplateVoid
            .replace("{method_name}", methodName)
            .replace("{parameters}", parameters.ifBlank { "" })
            .replace("{specs}", specs.ifBlank { "\n" })
            .replace("{param_names}", splitParams(parameters))
    }

    protected open fun validatorsFromPureCopy(
        methodName: String,
        parameters: String,
        returns: String,
        specs: String,
        body: String,
    ): String {
        return validatorTemplatePureCopy
            .replace("{method_name}", methodName)
            .replace("{parameters}", parameters.ifBlank { "" })
            .replace("{returns}", returns.ifBlank { "" })
            .replace("{specs}", specs.ifBlank { "\n" })
            .replace("{param_names}", splitParams(parameters))
            .replace("{body}", body)
    }

    protected open fun validatorsFromPure(
        methodName: String,
        parameters: String,
        returns: String,
        specs: String,
        body: String,
    ): String {
        return validatorTemplatePure
            .replace("{method_name}", methodName)
            .replace("{parameters}", parameters.ifBlank { "" })
            .replace("{returns}", returns.ifBlank { "" })
            .replace("{specs}", specs.ifBlank { "\n" })
            .replace("{param_names}", splitParams(parameters))
            .replace("{body}", body)
    }

    protected open fun replacePure(code: String, pureNames: List<String>): String {
        var result = code
        for (pureName in pureNames) {
            result = result.replace("$pureName(", "${pureName}_copy_pure(")
        }
        return result
    }

    override fun generateValidators(code: String, validateHelpers: Boolean): String {
        val pureMethods = pureRegex.findAll(code).toList()
        val methods = methodRegex.findAll(code).toList()
        val voidMethods = voidRegex.findAll(code).toList()

        val validators = mutableListOf<String>()
        val pureNames = mutableListOf<String>()

        for (pureMatch in pureMethods) {
            val methodName = pureMatch.groupValues.getOrNull(1) ?: ""
            if (methodName.isNotEmpty()) pureNames.add(methodName)
        }

        if (removePure) {
            for (pureMatch in pureMethods) {
                val methodName = pureMatch.groupValues.getOrNull(1) ?: continue
                val parameters = pureMatch.groupValues.getOrNull(2) ?: ""
                val returns = pureMatch.groupValues.getOrNull(3) ?: ""
                var specs = pureMatch.groupValues.getOrNull(4) ?: ""
                var body = pureMatch.groupValues.getOrNull(5) ?: ""

                specs = replacePure(specs, pureNames)
                body = replacePure(body, pureNames)

                validators.add(
                    validatorsFromPureCopy(methodName, parameters, returns, specs, body)
                )
            }
        }

        if (validateHelpers) {
            for (pureMatch in pureMethods) {
                val methodName = pureMatch.groupValues.getOrNull(1) ?: continue
                val parameters = pureMatch.groupValues.getOrNull(2) ?: ""
                val returns = pureMatch.groupValues.getOrNull(3) ?: ""
                val specs = pureMatch.groupValues.getOrNull(4) ?: ""
                val body = pureMatch.groupValues.getOrNull(5) ?: ""
                validators.add(
                    validatorsFromPure(methodName, parameters, returns, specs, body)
                )
            }
        }

        for (match in methods) {
            val methodName = match.groupValues.getOrNull(1) ?: ""
            if (methodName in pureNames) continue

            val parameters = match.groupValues.getOrNull(2) ?: ""
            val returns = match.groupValues.getOrNull(3) ?: ""
            var specs = match.groupValues.getOrNull(4) ?: ""

            if (removePure) {
                specs = replacePure(specs, pureNames)
            }

            validators.add(
                validatorsFrom(methodName, parameters, returns, specs)
            )
        }

        for (match in voidMethods) {
            val methodName = match.groupValues.getOrNull(1) ?: ""
            if (methodName.isEmpty()) continue
            val parameters = match.groupValues.getOrNull(2) ?: ""
            var specs = match.groupValues.getOrNull(3) ?: ""

            val methodNamesSet = methods.mapNotNull { it.groupValues.getOrNull(1) }.toSet()
            if (methodName in pureNames || methodName in methodNamesSet || methodName == "main") continue

            if (removePure) {
                specs = replacePure(specs, pureNames)
            }

            validators.add(
                validatorsFromVoid(methodName, parameters, specs)
            )
        }

        return validators.joinToString("\n")
    }

    override fun removeConditions(code: String): String {
        var cleaned = code
        for (pattern in checkPatterns) {
            val regex = Regex(pattern, setOf(RegexOption.DOT_MATCHES_ALL))
            cleaned = cleaned.replace(regex, "")
        }
        // Collapse multiple blank lines
        cleaned = cleaned.replace(Regex("\\n\\s*\\n"), "\n")
        // Remove lines containing inline assert comment if provided
        val lines = cleaned.lines().filter { line ->
            inlineAssertComment == null || !line.contains(inlineAssertComment)
        }
        return lines.joinToString("\n").trim()
    }

    override fun separateValidatorErrors(errors: String): Pair<String, String> {
        // Default neutral behavior: return all errors as one chunk, no separation
        return errors to ""
    }

    override fun checkHelpers(code: String, pureNonHelpers: List<String>): Pair<List<String>, String> {
        return emptyList<String>() to code
    }

    override fun findPureNonHelpers(code: String): List<String> {
        return emptyList()
    }
}

