package languages

import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.languages.*
import org.junit.jupiter.api.Nested

class LanguageTests {

    @Nested
    inner class NaginiTests {

        @Nested
        inner class GenerationTests {

            val naginiLanguage = NaginiLanguage(listOf(AnnotationTypes.INVARIANTS, AnnotationTypes.ASSERTIONS))

            @Test
            fun testNaginiGenerate() {
                val code = """
                    def main(value: int) -> int:
                        Requires(value >= 10)
                        Ensures(Result() >= 20)
                        # impl-start
                        Assert(value * 2 >= 20) # assert-line
                        return value * 2
                        # impl-end
                """.trimIndent()
                val validator = """
                    def main_valid(value: int) -> int:
                        Requires(value >= 10)
                        Ensures(Result() >= 20)
                        ret = main(value)
                        return ret
                """.trimIndent()
                assertEquals(validator, naginiLanguage.generateValidators(code, true))
            }

            @Test
            fun testNaginiWithComments() {
                val code = """
                    def main(value: int) -> int:
                        # pre-conditions-start
                        Requires(value >= 10)
                        # pre-conditions-end
                        # post-conditions-start
                        Ensures(Result() >= 20)
                        # post-conditions-end
                        # impl-start
                        Assert(value * 2 >= 20) # assert-line
                        return value * 2
                        # impl-end
                """.trimIndent()
                val validator = """
                    def main_valid(value: int) -> int:
                        # pre-conditions-start
                        Requires(value >= 10)
                        # pre-conditions-end
                        # post-conditions-start
                        Ensures(Result() >= 20)
                        # post-conditions-end
                        ret = main(value)
                        return ret
                """.trimIndent()
                assertEquals(validator, naginiLanguage.generateValidators(code, true))
            }

            @Test
            fun testNaginiLarge() {
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
                val validator = """
                    @Pure
                    def lower_valid_pure(c : int) -> bool :
            
                        ret = lower(c)
                        return ret
                    
                    @Pure
                    def upper_valid_pure(c : int) -> bool :
            
                        ret = upper(c)
                        return ret
                    
                    @Pure
                    def alpha_valid_pure(c : int) -> bool :
            
                        ret = alpha(c)
                        return ret
                    
                    @Pure
                    def flip__char_valid_pure(c : int) -> int :
                        # pre-conditions-start
                        Ensures(lower(c) == upper(Result()))
                        Ensures(upper(c) == lower(Result()))
                        # pre-conditions-end
                        ret = flip__char(c)
                        return ret
                    
                    def flip__case_valid(s : List[int]) -> List[int] :
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
                        ret = flip__case(s)
                        return ret
                """.trimIndent()
                assertEquals(validator, naginiLanguage.generateValidators(code, true))
            }

            @Test
            fun testNaginiSmall() {
                val code = """
                    @Pure
                    def flip__char(c : int) -> int :
                        # pre-conditions-start
                        Ensures(lower(c) == upper(Result()))
                        Ensures(upper(c) == lower(Result()))
                        # pre-conditions-end
            
                        # impl-start
                        if lower(c):
                            return ((c) - (0)) + (26)
                        elif upper(c):
                            return ((c) + (0)) - (26)
                        elif True:
                            return c
                        # impl-end
                """.trimIndent()
                val validator = """
                    def flip__char_valid(c : int) -> int :
                        # pre-conditions-start
                        Ensures(lower(c) == upper(Result()))
                        Ensures(upper(c) == lower(Result()))
                        # pre-conditions-end
                        ret = flip__char(c)
                        return ret
                """.trimIndent()
                assertEquals(validator, naginiLanguage.generateValidators(code, true))
            }
        }

        @Nested
        inner class RemovingInvariantsTests {

            val naginiLanguage = NaginiLanguage(listOf(AnnotationTypes.INVARIANTS, AnnotationTypes.ASSERTIONS))

            @Test
            fun testRemoveLine() {
                val code = """
                    def main():
                        Assert(a == 1) # assert-line
                """.trimIndent()
                val validator = """
                    def main():
                """.trimIndent()
                assertEquals(validator, naginiLanguage.removeMarkup(code))
            }

            @Test
            fun testRemoveMultilineAssert() {
                val code = """
                    def main():
                        # assert-start
                        Assert(
                            a == 1
                        )
                        # assert-end
                """.trimIndent()
                val validator = """
                    def main():
                """.trimIndent()
                assertEquals(validator, naginiLanguage.removeMarkup(code))
            }

            @Test
            fun testRemoveInvariants() {
                val code = """
                    def main():
                        while True:
                            # invariants-start
                            Invariant(false)
                            Invariant(true)
                            # invariants-end
                """.trimIndent()
                val validator = """
                    def main():
                        while True:
                """.trimIndent()
                assertEquals(validator, naginiLanguage.removeMarkup(code))
            }

            @Test
            fun testRemoveAll() {
                val code = """
                    def is_prime(k : int) -> bool:
                        # pre-conditions-start
                        Requires((k) >= (2))
                        # pre-conditions-end
                        # post-conditions-start
                        Ensures(not (Result()) or (Forall(int, lambda d_0_i_:
                            not (((2) <= (d_0_i_)) and ((d_0_i_) < (k))) or ((k % d_0_i_) != (0)))))
                        Ensures(not (not(Result())) or (Exists(int, lambda d_1_j_:
                            (((2) <= (d_1_j_)) and ((d_1_j_) < (k))) and (((k % d_1_j_)) == (0)))))
                        # post-conditions-end
                        result = False # type : bool
                        d_2_i_ = int(0) # type : int
                        d_2_i_ = 2
                        result = True
                        while (d_2_i_) < (k):
                            # invariants-start
                            Invariant(((2) <= (d_2_i_)) and ((d_2_i_) <= (k)))
                            Invariant(not (not(result)) or (Exists(int, lambda d_3_j_:
                                (((2) <= (d_3_j_)) and ((d_3_j_) < (d_2_i_))) and (((k % d_3_j_)) == (0)))))
                            Invariant(not (result) or (Forall(int, lambda d_4_j_:
                                not (((2) <= (d_4_j_)) and ((d_4_j_) < (d_2_i_))) or (((k % d_4_j_)) != (0)))))
                            # invariants-end
                            if ((k % d_2_i_)) == (0):
                                result = False
                            Assert((not result) or Forall(int, lambda j : 2 <= j < i ==> k % j != 0)) # assert-line
                            # assert-start
                            Assert(result
                                or Exists(int,
                                    lamdbda j : 2 <= j < i && k % j == 0)
                            # assert-end
                            d_2_i_ = (d_2_i_) + (1)
                        return result
                """.trimIndent()
                val validator = """
                    def is_prime(k : int) -> bool:
                        # pre-conditions-start
                        Requires((k) >= (2))
                        # pre-conditions-end
                        # post-conditions-start
                        Ensures(not (Result()) or (Forall(int, lambda d_0_i_:
                            not (((2) <= (d_0_i_)) and ((d_0_i_) < (k))) or ((k % d_0_i_) != (0)))))
                        Ensures(not (not(Result())) or (Exists(int, lambda d_1_j_:
                            (((2) <= (d_1_j_)) and ((d_1_j_) < (k))) and (((k % d_1_j_)) == (0)))))
                        # post-conditions-end
                        result = False # type : bool
                        d_2_i_ = int(0) # type : int
                        d_2_i_ = 2
                        result = True
                        while (d_2_i_) < (k):
                            if ((k % d_2_i_)) == (0):
                                result = False
                            d_2_i_ = (d_2_i_) + (1)
                        return result
                """.trimIndent()
                assertEquals(validator, naginiLanguage.removeMarkup(code))
            }
        }

        @Nested
        inner class RemovingConditionsTests {
            val naginiLanguage = NaginiLanguage(listOf(AnnotationTypes.INVARIANTS, AnnotationTypes.ASSERTIONS, AnnotationTypes.PRE_CONDITIONS, AnnotationTypes.POST_CONDITIONS))

            @Test
            fun testRemoveAll() {
                val code = """
                    def is_prime(k : int) -> bool:
                        # pre-conditions-start
                        Requires((k) >= (2))
                        # pre-conditions-end
                        # post-conditions-start
                        Ensures(not (Result()) or (Forall(int, lambda d_0_i_:
                            not (((2) <= (d_0_i_)) and ((d_0_i_) < (k))) or ((k % d_0_i_) != (0)))))
                        Ensures(not (not(Result())) or (Exists(int, lambda d_1_j_:
                            (((2) <= (d_1_j_)) and ((d_1_j_) < (k))) and (((k % d_1_j_)) == (0)))))
                        # post-conditions-end
                        result = False # type : bool
                        d_2_i_ = int(0) # type : int
                        d_2_i_ = 2
                        result = True
                        while (d_2_i_) < (k):
                            # invariants-start
                            Invariant(((2) <= (d_2_i_)) and ((d_2_i_) <= (k)))
                            Invariant(not (not(result)) or (Exists(int, lambda d_3_j_:
                                (((2) <= (d_3_j_)) and ((d_3_j_) < (d_2_i_))) and (((k % d_3_j_)) == (0)))))
                            Invariant(not (result) or (Forall(int, lambda d_4_j_:
                                not (((2) <= (d_4_j_)) and ((d_4_j_) < (d_2_i_))) or (((k % d_4_j_)) != (0)))))
                            # invariants-end
                            if ((k % d_2_i_)) == (0):
                                result = False
                            Assert((not result) or Forall(int, lambda j : 2 <= j < i ==> k % j != 0)) # assert-line
                            # assert-start
                            Assert(result
                                or Exists(int,
                                    lamdbda j : 2 <= j < i && k % j == 0)
                            # assert-end
                            d_2_i_ = (d_2_i_) + (1)
                        return result
                """.trimIndent()
                val validator = """
                    def is_prime(k : int) -> bool:
                        result = False # type : bool
                        d_2_i_ = int(0) # type : int
                        d_2_i_ = 2
                        result = True
                        while (d_2_i_) < (k):
                            if ((k % d_2_i_)) == (0):
                                result = False
                            d_2_i_ = (d_2_i_) + (1)
                        return result
                """.trimIndent()
                assertEquals(validator, naginiLanguage.removeMarkup(code))
            }
        }

        @Nested
        inner class RemovingConditionsAndCodeTests {
            val naginiLanguage = NaginiLanguage(
                listOf(
                    AnnotationTypes.INVARIANTS,
                    AnnotationTypes.ASSERTIONS,
                    AnnotationTypes.PRE_CONDITIONS,
                    AnnotationTypes.POST_CONDITIONS,
                    AnnotationTypes.IMPLS
                )
            )

            @Test
            fun testRemoveAll() {
                val code = """
                    def is_prime(k : int) -> bool:
                        # pre-conditions-start
                        Requires((k) >= (2))
                        # pre-conditions-end
                        # post-conditions-start
                        Ensures(not (Result()) or (Forall(int, lambda d_0_i_:
                            not (((2) <= (d_0_i_)) and ((d_0_i_) < (k))) or ((k % d_0_i_) != (0)))))
                        Ensures(not (not(Result())) or (Exists(int, lambda d_1_j_:
                            (((2) <= (d_1_j_)) and ((d_1_j_) < (k))) and (((k % d_1_j_)) == (0)))))
                        # post-conditions-end
                        # impl-start
                        result = False # type : bool
                        d_2_i_ = int(0) # type : int
                        d_2_i_ = 2
                        result = True
                        while (d_2_i_) < (k):
                            # invariants-start
                            Invariant(((2) <= (d_2_i_)) and ((d_2_i_) <= (k)))
                            Invariant(not (not(result)) or (Exists(int, lambda d_3_j_:
                                (((2) <= (d_3_j_)) and ((d_3_j_) < (d_2_i_))) and (((k % d_3_j_)) == (0)))))
                            Invariant(not (result) or (Forall(int, lambda d_4_j_:
                                not (((2) <= (d_4_j_)) and ((d_4_j_) < (d_2_i_))) or (((k % d_4_j_)) != (0)))))
                            # invariants-end
                            if ((k % d_2_i_)) == (0):
                                result = False
                            Assert((not result) or Forall(int, lambda j : 2 <= j < i ==> k % j != 0)) # assert-line
                            # assert-start
                            Assert(result
                                or Exists(int,
                                    lamdbda j : 2 <= j < i && k % j == 0)
                            # assert-end
                            d_2_i_ = (d_2_i_) + (1)
                        return result
                        # impl-end
                """.trimIndent()
                val validator = """
                    def is_prime(k : int) -> bool:
                """.trimIndent()
                assertEquals(validator, naginiLanguage.removeMarkup(code))
            }
        }
    }


    @Nested
    inner class DafnyTests {

        @Nested
        inner class GenerationTests {
            private val dafny = DafnyLanguage(listOf(AnnotationTypes.INVARIANTS, AnnotationTypes.ASSERTIONS))

            @Test
            fun testDafnyGenerate() {
                val code = """
                    method main(value: int) returns (result: int)
                        requires value >= 10
                        ensures result >= 20
                    {
                        assert value * 2 >= 20; // assert-line
                        result := value * 2;
                    }
                """.trimIndent()
                val expected = """
                    method main_valid(value: int) returns (result: int)
                        requires value >= 10
                        ensures result >= 20
                    
                        { var ret0 := main(value); return ret0; }
                """.trimIndent()
                assertEquals(expected, dafny.generateValidators(code, true))
            }

            @Test
            fun testDafnyGenerateVoid() {
                val code = """
                    method BubbleSort(a: array<int>)
                      modifies a
                      // post-conditions-start
                      ensures forall i,j::0<= i < j < a.Length ==> a[i] <= a[j]
                      ensures multiset(a[..])==multiset(old(a[..]))
                      // post-conditions-end
                    {
                      // impl-start
                      var i := a.Length - 1;
                      while (i > 0)
                        // invariants-start
                        invariant i < 0 ==> a.Length == 0
                        invariant -1 <= i < a.Length
                        invariant forall ii,jj::i <= ii< jj <a.Length ==> a[ii] <= a[jj]
                        invariant forall k,k'::0<=k<=i<k'<a.Length==>a[k]<=a[k']
                        invariant multiset(a[..])==multiset(old(a[..]))
                        // invariants-end
                      {
                        var j := 0;
                        while (j < i)
                          // invariants-start
                          invariant 0 < i < a.Length && 0 <= j <= i
                          invariant forall ii,jj::i<= ii <= jj <a.Length ==> a[ii] <= a[jj]
                          invariant forall k, k'::0<=k<=i<k'<a.Length==>a[k]<=a[k']
                          invariant forall k :: 0 <= k <= j ==> a[k] <= a[j]
                          invariant multiset(a[..])==multiset(old(a[..]))
                          // invariants-end
                        {
                          if (a[j] > a[j + 1])
                          {
                            a[j], a[j + 1] := a[j + 1], a[j];
                          }
                          j := j + 1;
                        }
                        i := i - 1;
                      }
                      // impl-end
                    }
                """.trimIndent()
                val expected = """
                    method BubbleSort_valid(a: array<int>)
                      modifies a
                      // post-conditions-start
                      ensures forall i,j::0<= i < j < a.Length ==> a[i] <= a[j]
                      ensures multiset(a[..])==multiset(old(a[..]))
                      // post-conditions-end
                    
                        { BubbleSort(a); }
                """.trimIndent()
                assertEquals(expected, dafny.generateValidators(code, true))
            }

            @Test
            fun testDafnyGenerateWithHelper() {
                val code = """
                    function abs(n: int) : nat { if n > 0 then n else -n }
            
                    method main(value: int) returns (result: int)
                        requires value >= 10
                        ensures result >= 20
                    {
                        assert value * 2 >= 20; // assert-line
                        result := value * 2;
                    }""".trimIndent()
                            val expected = """
                    function abs_valid_pure(n: int): nat 
                        { abs(n) }
            
                    method main_valid(value: int) returns (result: int)
                        requires value >= 10
                        ensures result >= 20
                    
                        { var ret0 := main(value); return ret0; }
                """.trimIndent()
                assertEquals(expected, dafny.generateValidators(code, true))
            }

            @Test
            fun testDafnyGenerateMultipleReturns() {
                val code = """
                    method main(value: int) returns (result: int, result2: int)
                        requires value >= 10
                        ensures result >= 20
                        ensures result2 >= 30
                    {
                        assert value * 2 >= 20; // assert-line
                        result := value * 2;
                        result2 := value * 3;
                    }""".trimIndent()
                            val expected = """
                    method main_valid(value: int) returns (result: int, result2: int)
                        requires value >= 10
                        ensures result >= 20
                        ensures result2 >= 30
                    
                        { var ret0, ret1 := main(value); return ret0, ret1; }
                """.trimIndent()
                assertEquals(expected, dafny.generateValidators(code, true))
            }
        }

        @Nested
        inner class RemovingConditionsTests {
            private val dafny = DafnyLanguage(listOf(AnnotationTypes.INVARIANTS, AnnotationTypes.ASSERTIONS))

            @Test
            fun testRemoveLine() {
                val code = """
                    method main() {
                        assert a == 1; // assert-line
                    }
                """.trimIndent()
                val expected = """
                    method main() {
                    }
                """.trimIndent()
                assertEquals(expected, dafny.removeMarkup(code))
            }

            @Test
            fun testRemoveMultilineAssert() {
                val code = """
                    method main() {
                        // assert-start
                        assert a == 1 by {
            
                        }
                        // assert-end
                    }
                """.trimIndent()
                val expected = """
                    method main() {
                    }
                """.trimIndent()
                assertEquals(expected, dafny.removeMarkup(code))
            }

            @Test
            fun testRemoveInvariants() {
                val code = """
                    method main() {
                        while true
                            // invariants-start
                            invariant false
                            invariant true
                            // invariants-end
                        {
                        }
                    }
                """.trimIndent()
                val expected = """
                    method main() {
                        while true
                        {
                        }
                    }
                """.trimIndent()
                assertEquals(expected, dafny.removeMarkup(code))
            }

            @Test
            fun testRemoveAll() {
                val code = """
                    method is_prime(k: int) returns (result: bool)
                      requires k >= 2
                      ensures result ==> forall i :: 2 <= i < k ==> k % i != 0
                      ensures !result ==> exists j :: 2 <= j < k && k % j == 0
                    {
                      var i := 2;
                      result := true;
                      while i < k
                        // invariants-start
                        invariant 2 <= i <= k
                        invariant !result ==> exists j :: 2 <= j < i && k % j == 0
                        invariant result ==> forall j :: 2 <= j < i ==> k % j != 0
                        // invariants-end
                      {
                        if k % i == 0 {
                          result := false;
                        }
                        assert result ==> forall j :: 2 <= j < i ==> k % j != 0; // assert-line
                        // assert-start
                        assert !result ==> exists j :: 2 <= j < i && k % j == 0 by {
                            assert true;
                        }
                        // assert-end
                        i := i + 1;
                      }
                    }
                """.trimIndent()
                val expected = """
                    method is_prime(k: int) returns (result: bool)
                      requires k >= 2
                      ensures result ==> forall i :: 2 <= i < k ==> k % i != 0
                      ensures !result ==> exists j :: 2 <= j < k && k % j == 0
                    {
                      var i := 2;
                      result := true;
                      while i < k
                      {
                        if k % i == 0 {
                          result := false;
                        }
                        i := i + 1;
                      }
                    }
                """.trimIndent()
                assertEquals(expected, dafny.removeMarkup(code))
            }
        }
    }

    @Nested
    inner class VerusLanguageTests {

        @Nested
        inner class GenerationTests {
            private val verus = VerusLanguage(listOf(AnnotationTypes.INVARIANTS, AnnotationTypes.ASSERTIONS))

            @Test
            fun testVerusValidateVoid() {
                val code = """
                    fn myfun4(x: &Vec<u64>, y: &mut Vec<u64>)
                        // pre-conditions-start
                        requires 
                            old(y).len() == 0,
                        // pre-conditions-end
                        // post-conditions-start
                        ensures 
                            y@ == x@.filter(|k:u64| k%3 == 0),
                        // post-conditions-end
                    {
                        // impl-start
                        let mut i: usize = 0;
                        let xlen = x.len();
                        
                        // assert-start
                        assert(y@ == x@.take(0).filter(|k:u64| k%3 ==0)); 
                        // assert-end
                        while (i < xlen) 
                            // invariants-start
                            invariant 
                                0 <= i <= xlen,
                                x@.len() == xlen,  
                                y@ == x@.take(i as int).filter(|k:u64| k%3 == 0),
                            // invariants-end
                        { 
                            if (x[i] % 3 == 0) {
                                y.push(x[i]);
                            }
                            // assert-start
                            assert(x@.take((i + 1) as int).drop_last() == x@.take(i as int));
                            reveal(Seq::filter);
                            // assert-end
                            i = i + 1;
                        }
                        // assert-start
                        assert(x@ == x@.take(x.len() as int)); 
                        // assert-end
                        // impl-end
                    }
                    
                    fn main() {}
                """.trimIndent()

                val expected = """
                    verus!{
                    fn myfun4_valid(x: &Vec<u64>, y: &mut Vec<u64>)
                        // pre-conditions-start
                        requires 
                            old(y).len() == 0,
                        // pre-conditions-end
                        // post-conditions-start
                        ensures 
                            y@ == x@.filter(|k:u64| k%3 == 0),
                        // post-conditions-end
                    
                    { myfun4(x, y); }
                    }
                """.trimIndent()

                assertEquals(expected, verus.generateValidators(code, true))
            }

            @Test
            fun testVerusGenerate() {
                val code = """
                    fn main(value: i32) -> (result: i32)
                        requires
                            value >= 10,
                        ensures
                            result >= 20,
                    {
                        assert(value * 2 >= 20); // assert-line
                        value * 2
                    }
    
                    spec fn test(val: i32) -> (result: i32)
                    {
                        val
                    }
                    // pure-end
    
                    fn is_prime(num: u32) -> (result: bool)
                        requires
                            num >= 2,
                        ensures
                            result <==> spec_prime(num as int),
                    {
                        let mut i = 2;
                        let mut result = true;
                        while i < num
                            // invariants-start
                            invariant
                                2 <= i <= num,
                                result <==> spec_prime_helper(num as int, i as int),
                            // invariants-end
                        {
                            if num % i == 0 {
                                result = false;
                                assert(result <==> spec_prime_helper(num as int, i as int)); // assert-line
                            }
                            // assert-start
                            assert(result <==> spec_prime_helper(num as int, i as int)) by {
                                assert(true);
                            }
                            // assert-end
                            i += 1;
                        }
                        result
                    }
                """.trimIndent()

                val expected = """
                    verus!{
                    spec fn test_valid_pure(val: i32) -> (result: i32)
                    
                    { let ret = test(val); ret }
    
                    fn main_valid(value: i32) -> (result: i32)
                        requires
                            value >= 10,
                        ensures
                            result >= 20,
                    
                    { let ret = main(value); ret }
    
                    fn is_prime_valid(num: u32) -> (result: bool)
                        requires
                            num >= 2,
                        ensures
                            result <==> spec_prime(num as int),
                    
                    { let ret = is_prime(num); ret }
                    }
                """.trimIndent()

                assertEquals(expected, verus.generateValidators(code, true))
            }

            @Test
            fun testVerusGenerate2() {
                val code = """
                    spec fn expr_inner_divide_i32_by_usize(qr : (i32, usize), x: i32, d: usize) -> (result:bool)
                    {
                        let (q, r) = qr;
                        q == x as int / d as int && r == x as int % d as int
                    }
                    // pure-end
                """.trimIndent()

                val expected = """
                    verus!{
                    spec fn expr_inner_divide_i32_by_usize_valid_pure(qr : (i32, usize), x: i32, d: usize) -> (result:bool)
                    
                    { let ret = expr_inner_divide_i32_by_usize(qr, x, d); ret }
                    }
                """.trimIndent()

                assertEquals(expected, verus.generateValidators(code, true))
            }

            @Test
            fun testVerusGenerate1Step0() {
                val code = """
                    use vstd::assert_seqs_equal;
                    use vstd::prelude::*;
    
                    verus! {
                    spec fn intersperse_spec(numbers: Seq<u64>, delimiter: u64) -> (result:Seq<u64>)
                        decreases numbers.len(),
                    {
                        if numbers.len() <= 1 {
                            numbers
                        } else {
                            intersperse_spec(numbers.drop_last(), delimiter) + seq![delimiter, numbers.last()]
                        }
                    }
                    // pure-end
    
                    spec fn even(i: int) -> (result:int) {
                        2 * i
                    }
                    // pure-end
    
                    } // verus!
                """.trimIndent()

                val expected = """
                    verus!{
                    spec fn intersperse_spec_valid_pure(numbers: Seq<u64>, delimiter: u64) -> (result:Seq<u64>)
                        decreases numbers.len(),
                    
                    { let ret = intersperse_spec(numbers, delimiter); ret }
    
                    spec fn even_valid_pure(i: int) -> (result:int)
                    
                    { let ret = even(i); ret }
                    }
                """.trimIndent()

                assertEquals(expected, verus.generateValidators(code, true))
            }

            @Test
            fun testVerusGenerate1Step1() {
                val code = """
                    use vstd::assert_seqs_equal;
                    use vstd::prelude::*;
    
                    verus! {
                    proof fn intersperse_spec_len(numbers: Seq<u64>, delimiter: u64)
                        // post-conditions-start
                        ensures
                            numbers.len() > 0 ==> intersperse_spec(numbers, delimiter).len() == 2 * numbers.len() - 1,
                        decreases numbers.len(),
                        // post-conditions-end
                    {
                        // impl-start
                        if numbers.len() > 0 {
                            intersperse_spec_len(numbers.drop_last(), delimiter);
                        }
                        // impl-end
                    }
                    // pure-end
    
                    proof fn intersperse_quantified_is_spec(numbers: Seq<u64>, delimiter: u64, interspersed: Seq<u64>)
                        // pre-conditions-start
                        requires
                            intersperse_quantified(numbers, delimiter, interspersed),
                        // pre-conditions-end
                        // post-conditions-start
                        ensures
                            interspersed == intersperse_spec(numbers, delimiter),
                        decreases numbers.len(),
                        // post-conditions-end
                    {
                        // impl-start
                        let is = intersperse_spec(numbers, delimiter);
                        if numbers.len() == 0 {
                        } else if numbers.len() == 1 {
                            assert(interspersed.len() == 1); // assert-line
                            assert(interspersed[even(0)] == numbers[0]); // assert-line
                        } else {
                            intersperse_quantified_is_spec(
                                numbers.drop_last(),
                                delimiter,
                                interspersed.take(interspersed.len() - 2),
                            );
                            intersperse_spec_len(numbers, delimiter);
                            // assert-start
                            assert_seqs_equal!(is == interspersed, i => {
                                if i < is.len() - 2 {
                                } else {
                                    if i % 2 == 0 {
                                        assert(is[i] == numbers.last());
                                        assert(interspersed[even(i/2)] == numbers[i / 2]);
                                        assert(i / 2 == numbers.len() - 1);
                                    } else {
                                        assert(is[i] == delimiter);
                                        assert(interspersed[odd((i-1)/2)] == delimiter);
                                    }
                                }
                            });
                            // assert-end
                        }
                        assert(interspersed =~= intersperse_spec(numbers, delimiter)); // assert-line
                        // impl-end
                    }
                    // pure-end
    
                    } // verus!
                """.trimIndent()

                val expected = """
                """.trimIndent()

                assertEquals(expected, verus.generateValidators(code, true))
            }

            @Test
            fun testVerusGenerate1Step2() {
                val code = """
                    use vstd::assert_seqs_equal;
                    use vstd::prelude::*;
    
                    verus! {
                    fn intersperse(numbers: Vec<u64>, delimiter: u64) -> (result: Vec<u64>)
                        // post-conditions-start
                        ensures
                            result@ == intersperse_spec(numbers@, delimiter),
                        // post-conditions-end
                    {
                        // impl-start
                        if numbers.len() <= 1 {
                            numbers
                        } else {
                            let mut result = Vec::new();
                            let mut index = 0;
                            while index < numbers.len() - 1
                                // invariants-start
                                invariant
                                    numbers.len() > 1,
                                    0 <= index < numbers.len(),
                                    result.len() == 2 * index,
                                    forall|i: int| 0 <= i < index ==> #[trigger] result[even(i)] == numbers[i],
                                    forall|i: int| 0 <= i < index ==> #[trigger] result[odd(i)] == delimiter,
                                // invariants-end
                            {
                                result.push(numbers[index]);
                                result.push(delimiter);
                                index += 1;
                            }
                            result.push(numbers[numbers.len() - 1]);
                            // assert-start
                            proof {
                                intersperse_quantified_is_spec(numbers@, delimiter, result@);
                            }
                            // assert-end
                            result
                        }
                        // impl-end
                    }
    
                    } // verus!
                """.trimIndent()

                val expected = """
                    verus!{
                    fn intersperse_valid(numbers: Vec<u64>, delimiter: u64) -> (result: Vec<u64>)
                        // post-conditions-start
                        ensures
                            result@ == intersperse_spec(numbers@, delimiter),
                        // post-conditions-end
                    
                    { let ret = intersperse(numbers, delimiter); ret }
                    }
                """.trimIndent()

                assertEquals(expected, verus.generateValidators(code, true))
            }

            @Test
            fun testVerusGenerate1() {
                val code = """
                    use vstd::assert_seqs_equal;
                    use vstd::prelude::*;
                    
                    verus! {
                    spec fn intersperse_spec(numbers: Seq<u64>, delimiter: u64) -> (result:Seq<u64>)
                        decreases numbers.len(),
                    {
                        if numbers.len() <= 1 {
                            numbers
                        } else {
                            intersperse_spec(numbers.drop_last(), delimiter) + seq![delimiter, numbers.last()]
                        }
                    }
                    // pure-end
                    
                    spec fn even(i: int) -> (result:int) {
                        2 * i
                    }
                    // pure-end
                    
                    spec fn odd(i: int) -> (result:int) {
                        2 * i + 1
                    }
                    // pure-end
                    
                    spec fn intersperse_quantified(numbers: Seq<u64>, delimiter: u64, interspersed: Seq<u64>) -> (result:bool) {
                        (if numbers.len() == 0 {
                            interspersed.len() == 0
                        } else {
                            interspersed.len() == 2 * numbers.len() - 1
                        }) && (forall|i: int| 0 <= i < numbers.len() ==> #[trigger] interspersed[even(i)] == numbers[i])
                            && (forall|i: int|
                            0 <= i < numbers.len() - 1 ==> #[trigger] interspersed[odd(i)] == delimiter)
                    }
                    // pure-end
                    
                    proof fn intersperse_spec_len(numbers: Seq<u64>, delimiter: u64)
                        // post-conditions-start
                        ensures
                            numbers.len() > 0 ==> intersperse_spec(numbers, delimiter).len() == 2 * numbers.len() - 1,
                        decreases numbers.len(),
                        // post-conditions-end
                    {
                        // impl-start
                        if numbers.len() > 0 {
                            intersperse_spec_len(numbers.drop_last(), delimiter);
                        }
                        // impl-end
                    }
                    // pure-end
                    
                    proof fn intersperse_quantified_is_spec(numbers: Seq<u64>, delimiter: u64, interspersed: Seq<u64>)
                        // pre-conditions-start
                        requires
                            intersperse_quantified(numbers, delimiter, interspersed),
                        // pre-conditions-end
                        // post-conditions-start
                        ensures
                            interspersed == intersperse_spec(numbers, delimiter),
                        decreases numbers.len(),
                        // post-conditions-end
                    {
                        // impl-start
                        let is = intersperse_spec(numbers, delimiter);
                        if numbers.len() == 0 {
                        } else if numbers.len() == 1 {
                            assert(interspersed.len() == 1); // assert-line
                            assert(interspersed[even(0)] == numbers[0]); // assert-line
                        } else {
                            intersperse_quantified_is_spec(
                                numbers.drop_last(),
                                delimiter,
                                interspersed.take(interspersed.len() - 2),
                            );
                            intersperse_spec_len(numbers, delimiter);
                            // assert-start
                            assert_seqs_equal!(is == interspersed, i => {
                                if i < is.len() - 2 {
                                } else {
                                    if i % 2 == 0 {
                                        assert(is[i] == numbers.last());
                                        assert(interspersed[even(i/2)] == numbers[i / 2]);
                                        assert(i / 2 == numbers.len() - 1);
                                    } else {
                                        assert(is[i] == delimiter);
                                        assert(interspersed[odd((i-1)/2)] == delimiter);
                                    }
                                }
                            });
                            // assert-end
                        }
                        assert(interspersed =~= intersperse_spec(numbers, delimiter)); // assert-line
                        // impl-end
                    }
                    // pure-end
                    
                    fn intersperse(numbers: Vec<u64>, delimiter: u64) -> (result: Vec<u64>)
                        // post-conditions-start
                        ensures
                            result@ == intersperse_spec(numbers@, delimiter),
                        // post-conditions-end
                    {
                        // impl-start
                        if numbers.len() <= 1 {
                            numbers
                        } else {
                            let mut result = Vec::new();
                            let mut index = 0;
                            while index < numbers.len() - 1
                                // invariants-start
                                invariant
                                    numbers.len() > 1,
                                    0 <= index < numbers.len(),
                                    result.len() == 2 * index,
                                    forall|i: int| 0 <= i < index ==> #[trigger] result[even(i)] == numbers[i],
                                    forall|i: int| 0 <= i < index ==> #[trigger] result[odd(i)] == delimiter,
                                // invariants-end
                            {
                                result.push(numbers[index]);
                                result.push(delimiter);
                                index += 1;
                            }
                            result.push(numbers[numbers.len() - 1]);
                            // assert-start
                            proof {
                                intersperse_quantified_is_spec(numbers@, delimiter, result@);
                            }
                            // assert-end
                            result
                        }
                        // impl-end
                    }
                    
                    } // verus!
                """.trimIndent()

                val expected = """
                    verus!{
                    spec fn intersperse_spec_valid_pure(numbers: Seq<u64>, delimiter: u64) -> (result:Seq<u64>)
                        decreases numbers.len(),
                    
                    { let ret = intersperse_spec(numbers, delimiter); ret }
    
                    spec fn even_valid_pure(i: int) -> (result:int)
                    
                    { let ret = even(i); ret }
    
                    spec fn odd_valid_pure(i: int) -> (result:int)
                    
                    { let ret = odd(i); ret }
    
                    spec fn intersperse_quantified_valid_pure(numbers: Seq<u64>, delimiter: u64, interspersed: Seq<u64>) -> (result:bool)
                    
                    { let ret = intersperse_quantified(numbers, delimiter, interspersed); ret }
    
                    fn intersperse_valid(numbers: Vec<u64>, delimiter: u64) -> (result: Vec<u64>)
                        // post-conditions-start
                        ensures
                            result@ == intersperse_spec(numbers@, delimiter),
                        // post-conditions-end
                    
                    { let ret = intersperse(numbers, delimiter); ret }
                    }
                """.trimIndent()

                assertEquals(expected, verus.generateValidators(code, true))
            }
        }

        @Nested
        inner class RemovingInvariantsTests {
            private val verus = VerusLanguage(listOf(AnnotationTypes.INVARIANTS, AnnotationTypes.ASSERTIONS))

            @Test
            fun testRemoveLine() {
                val code = """
                    fn main() {
                        assert a == 1; // assert-line
                    }
                """.trimIndent()

                val expected = """
                    fn main() {
                    }
                """.trimIndent()

                assertEquals(expected, verus.removeMarkup(code))
            }

            @Test
            fun testRemoveMultilineAssert() {
                val code = """
                    fn main() {
                        // assert-start
                        assert a == 1 by {
    
                        }
                        // assert-end
                    }
                """.trimIndent()

                val expected = """
                    fn main() {
                    }
                """.trimIndent()

                assertEquals(expected, verus.removeMarkup(code))
            }

            @Test
            fun testRemoveInvariants() {
                val code = """
                    fn main() {
                        while true
                            // invariants-start
                            invariant false
                            invariant true
                            // invariants-end
                        {
                        }
                    }
                """.trimIndent()

                val expected = """
                    fn main() {
                        while true
                        {
                        }
                    }
                """.trimIndent()

                assertEquals(expected, verus.removeMarkup(code))
            }

            @Test
            fun testRemoveAll() {
                val code = """
                    fn is_prime(num: u32) -> (result: bool)
                        requires
                            num >= 2,
                        ensures
                            result <==> spec_prime(num as int),
                    {
                        let mut i = 2;
                        let mut result = true;
                        while i < num
                            // invariants-start
                            invariant
                                2 <= i <= num,
                                result <==> spec_prime_helper(num as int, i as int),
                            // invariants-end
                        {
                            if num % i == 0 {
                                result = false;
                                assert(result <==> spec_prime_helper(num as int, i as int)); // assert-line
                            }
                            // assert-start
                            assert(result <==> spec_prime_helper(num as int, i as int)) by {
                                assert(true);
                            }
                            // assert-end
                            i += 1;
                        }
                        result
                    }
                """.trimIndent()

                val expected = """
                    fn is_prime(num: u32) -> (result: bool)
                        requires
                            num >= 2,
                        ensures
                            result <==> spec_prime(num as int),
                    {
                        let mut i = 2;
                        let mut result = true;
                        while i < num
                        {
                            if num % i == 0 {
                                result = false;
                            }
                            i += 1;
                        }
                        result
                    }
                """.trimIndent()

                assertEquals(expected, verus.removeMarkup(code))
            }
        }
    }

}
