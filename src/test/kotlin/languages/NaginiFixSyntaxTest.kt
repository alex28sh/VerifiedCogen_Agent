package languages

import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.languages.*

class NaginiFixSyntaxTest {

    private val naginiLanguage = NaginiLanguage(emptyList())

    @Test
    fun testFixForallSimple() {
        val code = "Forall(int, lambda i: i > 0)"
        val expected = "Forall(int, lambda i: i > 0)"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testFixForallWithTrigger() {
        val code = "Forall(int, lambda i: i > 0, [[i]])"
        val expected = "Forall(int, lambda i: (i > 0, [[i]]))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testFixForallAlreadyWrapped() {
        val code = "Forall(int, lambda i: (i > 0))"
        val expected = "Forall(int, lambda i: i > 0)"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testFixForallNested() {
        val code = "Forall(int, lambda i: Forall(int, lambda j: i > j))"
        val expected = "Forall(int, lambda i: Forall(int, lambda j: i > j))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testFixForallInLargerCode() {
        val code = """
            Ensures(Forall(int, lambda d_0_i_: Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (len(s))), lower((s)[d_0_i_]) == upper((Result())[d_0_i_]))))
        """.trimIndent()
        val expected = """
            Ensures(Forall(int, lambda d_0_i_: Implies(0 <= d_0_i_ and d_0_i_ < len(s), lower(s[d_0_i_]) == upper(Result()[d_0_i_]))))
        """.trimIndent()
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testFixExists() {
        val code = "Exists(int, lambda i: i > 0)"
        val expected = "Exists(int, lambda i: i > 0)"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testFixForallMultipleTriggers() {
        val code = "Forall(int, lambda i: i > 0, [[i], [i+1]])"
        val expected = "Forall(int, lambda i: (i > 0, [[i], [i + 1]]))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testDeeplyNestedMixedQuantifiers() {
        val code = "Forall(int, lambda i: Exists(int, lambda j: Forall(int, lambda k: i + j > k, [[k]]), [[j]]))"
        // Expected: Inner Forall should wrap (i+j > k, [[k]])
        // Then middle Exists should wrap (Forall(...), [[j]])
        // Outer Forall remains as is (no triggers)
        val expected = "Forall(int, lambda i: Exists(int, lambda j: (Forall(int, lambda k: (i + j > k, [[k]])), [[j]])))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testMultipleQuantifiersInEnsures() {
        val code = "Ensures(Forall(int, lambda i: i > 0, [[i]]) and Exists(int, lambda j: j < 0, [[j]]))"
        val expected = "Ensures(Forall(int, lambda i: (i > 0, [[i]])) and Exists(int, lambda j: (j < 0, [[j]])))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testComplexLambdaBodyWithTriggers() {
        val code = "Forall(List[int], lambda l: Implies(len(l) > 0, (l[0] == 1 or l[0] == 2)), [[l], [len(l)]])"
        val expected = "Forall(List[int], lambda l: (Implies(len(l) > 0, l[0] == 1 or l[0] == 2), [[l], [len(l)]]))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testQuantifierInWhileLoopInvariant() {
        val code = """
            while i < n:
                Invariant(Forall(int, lambda k: k < i, [[a[k] == 0]]))
                i += 1
        """.trimIndent()
        val expected = """
            while i < n:
                Invariant(Forall(int, lambda k: (k < i, [[a[k] == 0]])))
                i += 1
        """.trimIndent()
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testTriggersWithComplexExpressions() {
        val code = "Forall(int, lambda i: f(i) > g(i), [[f(i), g(i)]])"
        val expected = "Forall(int, lambda i: (f(i) > g(i), [[f(i), g(i)]]))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }

    @Test
    fun testSeveralTriggers() {
        val code = "Forall(int, lambda i: f(i) > g(i), [[f(i)]], [[g(i)]])"
        val expected = "Forall(int, lambda i: (f(i) > g(i), [[f(i)]], [[g(i)]]))"
        assertEquals(expected, naginiLanguage.fixSyntaxErrors(code))
    }
}
