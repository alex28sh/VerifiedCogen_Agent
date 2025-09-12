package checkers

import org.example.languages.AnnotationTypes
import org.example.languages.NaginiLanguage
import org.example.verifierTools.ConditionsFormalEqualityVerifier
import org.example.verifierTools.ProofSufficiencyChecker
import org.example.verifierTools.Verifier
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckerTests {

    val verifier = Verifier("nagini")

    @Nested
    inner class ProofSufficiencyTests {

        val checker = ProofSufficiencyChecker(verifier, Path("prompts/nagini/mode1/mode1-1"), null)

        @Test
        fun testNaginiVerifies() {
            val code = """
                from typing import cast, List, Dict, Set, Optional, Union
                from nagini_contracts.contracts import *
        
                @Pure
                def lower(c : int) -> bool :
                    # pure-start
                    return ((0) <= (c)) and ((c) <= (25))
                    # pure-end
        
                @Pure
                def upper(c : int) -> bool :
                    # pure-start
                    return ((26) <= (c)) and ((c) <= (51))
                    # pure-end
        
                @Pure
                def alpha(c : int) -> bool :
                    # pure-start
                    return (lower(c)) or (upper(c))
                    # pure-end
        
                @Pure
                def flip__char(c : int) -> int :
                    # pre-conditions-start
                    Ensures(lower(c) == upper(Result()))
                    Ensures(upper(c) == lower(Result()))
                    # pre-conditions-end
        
                    # pure-start
                    if lower(c):
                        return ((c) - (0)) + (26)
                    elif upper(c):
                        return ((c) + (0)) - (26)
                    elif True:
                        return c
                    # pure-end
        
                def flip__case(s : List[int]) -> List[int] :
                    # pre-conditions-start
                    Requires(Acc(list_pred(s)))
                    # pre-conditions-end
                    # post-conditions-start
                    Ensures(Acc(list_pred(s)))
                    Ensures(Acc(list_pred(Result())))
                    Ensures((len(Result())) == (len(s)))
                    Ensures(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (len(s))), lower((s)[d_0_i_]) == upper((Result())[d_0_i_])))))
                    Ensures(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (len(s))), upper((s)[d_0_i_]) == lower((Result())[d_0_i_])))))
                    # post-conditions-end
        
                    # impl-start
                    res = list([int(0)] * len(s)) # type : List[int]
                    i = int(0) # type : int
                    while i < len(s):
                        # invariants-start
                        Invariant(Acc(list_pred(s)))
                        Invariant(Acc(list_pred(res)))
                        Invariant(((0) <= (i)) and ((i) <= (len(s))))
                        Invariant((len(res)) == (len(s)))
                        Invariant(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (i)), lower((s)[d_0_i_]) == upper((res)[d_0_i_])))))
                        Invariant(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (i)), upper((s)[d_0_i_]) == lower((res)[d_0_i_])))))
                        # invariants-end
                        res[i] = flip__char(s[i])
                        i = i + 1
                    return res
                    # impl-end
            """.trimIndent()

            val tempFile: Path = Files.createTempFile("myTest", ".py")
            tempFile.toFile().writeText(code)

            val (verified, _) = checker.checkResponseFolded(tempFile)
            assertTrue(verified)
        }

        @Test
        fun testNaginiNotVerifies() {
            val code = """
                from typing import cast, List, Dict, Set, Optional, Union
                from nagini_contracts.contracts import *
        
                @Pure
                def lower(c : int) -> bool :
                    # pure-start
                    return ((0) <= (c)) and ((c) <= (25))
                    # pure-end
        
                @Pure
                def upper(c : int) -> bool :
                    # pure-start
                    return ((26) <= (c)) and ((c) <= (51))
                    # pure-end
        
                @Pure
                def alpha(c : int) -> bool :
                    # pure-start
                    return (lower(c)) or (upper(c))
                    # pure-end
        
                @Pure
                def flip__char(c : int) -> int :
                    # pre-conditions-start
                    Ensures(lower(c) == upper(Result()))
                    Ensures(upper(c) == lower(Result()))
                    # pre-conditions-end
        
                    # pure-start
                    if lower(c):
                        return ((c) - (0)) + (26)
                    elif upper(c):
                        return ((c) + (0)) - (26)
                    elif True:
                        return c
                    # pure-end
        
                def flip__case(s : List[int]) -> List[int] :
                    # pre-conditions-start
                    Requires(Acc(list_pred(s)))
                    # pre-conditions-end
                    # post-conditions-start
                    Ensures(Acc(list_pred(s)))
                    Ensures(Acc(list_pred(Result())))
                    Ensures((len(Result())) == (len(s)))
                    Ensures(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (len(s))), lower((s)[d_0_i_]) == upper((Result())[d_0_i_])))))
                    Ensures(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (len(s))), upper((s)[d_0_i_]) == lower((Result())[d_0_i_])))))
                    # post-conditions-end
        
                    # impl-start
                    res = list([int(0)] * len(s)) # type : List[int]
                    i = int(0) # type : int
                    while i < len(s):
                        # invariants-start
                        Invariant(Acc(list_pred(s)))
                        Invariant(Acc(list_pred(res)))
                        Invariant(((0) <= (i)) and ((i) <= (len(s))))
                        res[i] = flip__char(s[i])
                        i = i + 1
                    return res
                    # impl-end
            """.trimIndent()

            val tempFile: Path = Files.createTempFile("myTest", ".py")
            tempFile.toFile().writeText(code)

            val (verified, text) = checker.checkResponseFolded(tempFile)
            assertFalse(verified)
            assertTrue("Verification failed" in text)
        }
    }

    @Nested
    inner class ConditionsFormalEqualityTests {

        val naginiLanguage = NaginiLanguage(
            listOf(
                AnnotationTypes.INVARIANTS,
                AnnotationTypes.ASSERTIONS,
                AnnotationTypes.PRE_CONDITIONS,
                AnnotationTypes.POST_CONDITIONS,
            )
        )

        val originalProgram = """
                from typing import cast, List, Dict, Set, Optional, Union
                from nagini_contracts.contracts import *
        
                @Pure
                def lower(c : int) -> bool :
                    # pure-start
                    return ((0) <= (c)) and ((c) <= (25))
                    # pure-end
        
                @Pure
                def upper(c : int) -> bool :
                    # pure-start
                    return ((26) <= (c)) and ((c) <= (51))
                    # pure-end
        
                @Pure
                def alpha(c : int) -> bool :
                    # pure-start
                    return (lower(c)) or (upper(c))
                    # pure-end
        
                @Pure
                def flip__char(c : int) -> int :
                    # pre-conditions-start
                    Ensures(lower(c) == upper(Result()))
                    Ensures(upper(c) == lower(Result()))
                    # pre-conditions-end
        
                    # pure-start
                    if lower(c):
                        return ((c) - (0)) + (26)
                    elif upper(c):
                        return ((c) + (0)) - (26)
                    elif True:
                        return c
                    # pure-end
        
                def flip__case(s : List[int]) -> List[int] :
                    # pre-conditions-start
                    Requires(Acc(list_pred(s)))
                    # pre-conditions-end
                    # post-conditions-start
                    Ensures(Acc(list_pred(s)))
                    Ensures(Acc(list_pred(Result())))
                    Ensures((len(Result())) == (len(s)))
                    Ensures(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (len(s))), lower((s)[d_0_i_]) == upper((Result())[d_0_i_])))))
                    Ensures(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (len(s))), upper((s)[d_0_i_]) == lower((Result())[d_0_i_])))))
                    # post-conditions-end
        
                    # impl-start
                    res = list([int(0)] * len(s)) # type : List[int]
                    i = int(0) # type : int
                    while i < len(s):
                        # invariants-start
                        Invariant(Acc(list_pred(s)))
                        Invariant(Acc(list_pred(res)))
                        Invariant(((0) <= (i)) and ((i) <= (len(s))))
                        Invariant((len(res)) == (len(s)))
                        Invariant(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (i)), lower((s)[d_0_i_]) == upper((res)[d_0_i_])))))
                        Invariant(Forall(int, lambda d_0_i_: (Implies(((0) <= (d_0_i_)) and ((d_0_i_) < (i)), upper((s)[d_0_i_]) == lower((res)[d_0_i_])))))
                        # invariants-end
                        res[i] = flip__char(s[i])
                        i = i + 1
                    return res
                    # impl-end
            """.trimIndent()


        val checker = ConditionsFormalEqualityVerifier(
            verifier = verifier,
            promptDir = Path("prompts/nagini/mode1/mode1-1"),
            language = naginiLanguage,
            originalProgram = originalProgram,
            removeHelpers = false,
            innerChecker = null
        )

        @Test
        fun testNaginiSameConditions() {

            val tempFile: Path = Files.createTempFile("myTest", ".py")
            tempFile.toFile().writeText(originalProgram)

            val (verified, _) = checker.checkResponseFolded(tempFile)
            assertTrue(verified)
        }

        @Test
        fun testNaginiWithoutConditions() {

            val removedConditions = naginiLanguage.removeMarkup(originalProgram)

            val tempFile: Path = Files.createTempFile("myTest", ".py")
            tempFile.toFile().writeText(removedConditions)

            val (verified, text) = checker.checkResponseFolded(tempFile)
            assertFalse(verified)
            assertTrue("Verification failed" in text)
        }
    }
}