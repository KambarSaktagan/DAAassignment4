package edu.textanalysis;

import edu.textanalysis.dp.EditDistance;
import edu.textanalysis.dp.LongestCommonSubsequence;
import edu.textanalysis.matching.KmpMatcher;
import edu.textanalysis.matching.MatchResult;
import edu.textanalysis.matching.NaiveMatcher;
import edu.textanalysis.matching.RabinKarpMatcher;
import edu.textanalysis.matching.StringMatcher;
import edu.textanalysis.suffix.KasaiLcp;
import edu.textanalysis.suffix.SimpleSuffixArray;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Self-contained tests; no JUnit/dependency/download is required. */
public final class AlgorithmTests {
    private static int assertions = 0;
    private static final List<StringMatcher> MATCHERS = List.of(
            new NaiveMatcher(), new KmpMatcher(), new RabinKarpMatcher());

    public static void main(String[] args) {
        testLcs();
        testEditDistance();
        testMatching();
        testKasai();
        randomizedCrossChecks();
        System.out.printf("PASS: %,d assertions (all six algorithms and edge cases).%n", assertions);
    }

    private static void testLcs() {
        check("LCS identical", 6, LongestCommonSubsequence.length("banana", "banana"));
        check("LCS disjoint", 0, LongestCommonSubsequence.length("abc", "XYZ"));
        check("LCS first empty", 0, LongestCommonSubsequence.length("", "abc"));
        check("LCS second empty", 0, LongestCommonSubsequence.length("abc", ""));
        check("LCS both empty", 0, LongestCommonSubsequence.length("", ""));
        check("LCS classic", 4, LongestCommonSubsequence.length("AGGTAB", "GXTXAYB"));
        check("LCS not substring", 3, LongestCommonSubsequence.length("abcde", "ace"));
        check("LCS case sensitive", 0, LongestCommonSubsequence.length("abc", "ABC"));
    }

    private static void testEditDistance() {
        check("Edit identical", 0, EditDistance.distance("abc", "abc"));
        check("Edit first empty", 3, EditDistance.distance("", "abc"));
        check("Edit second empty", 3, EditDistance.distance("abc", ""));
        check("Edit both empty", 0, EditDistance.distance("", ""));
        check("Edit substitution", 1, EditDistance.distance("cat", "cut"));
        check("Edit insertion", 1, EditDistance.distance("cat", "cart"));
        check("Edit deletion", 1, EditDistance.distance("cart", "cat"));
        check("Edit classic", 3, EditDistance.distance("kitten", "sitting"));
        check("Edit reverse symmetry", 3, EditDistance.distance("sitting", "kitten"));
    }

    private static void testMatching() {
        matchCase("beginning", "abcxxx", "abc", List.of(0));
        matchCase("middle", "xxabcxx", "abc", List.of(2));
        matchCase("end", "xxxabc", "abc", List.of(3));
        matchCase("separate repeats", "abcabcabc", "abc", List.of(0, 3, 6));
        matchCase("overlapping repeats", "aaaaa", "aa", List.of(0, 1, 2, 3));
        matchCase("absent", "hello world", "xyz", List.of());
        matchCase("longer than text", "ab", "abcd", List.of());
        matchCase("empty pattern", "abc", "", List.of(0, 1, 2, 3));
        matchCase("both empty", "", "", List.of(0));
        matchCase("empty text nonempty pattern", "", "a", List.of());
        matchCase("single-character", "ababa", "a", List.of(0, 2, 4));
        matchCase("fallback repeated prefix", "ababababac", "ababac", List.of(4));
        matchCase("UTF-16 BMP chars", "café café", "fé", List.of(2, 7));

        // Modulus 2 guarantees a hash match for 'ab' and 'cb' without an actual match.
        MatchResult collision = new RabinKarpMatcher(256, 2).findAll("ab", "cb");
        check("RK collision no false positive", List.of(), collision.indices());
        checkTrue("RK detects collision", collision.hashCollisions() >= 1);
    }

    private static void testKasai() {
        // banana suffix order: a, ana, anana, banana, na, nana
        int[] bananaSa = {5, 3, 1, 0, 4, 2};
        int[] bananaLcp = {0, 1, 3, 0, 0, 2};
        checkArray("Kasai banana manual", bananaLcp, KasaiLcp.build("banana", bananaSa));
        checkArray("Simple SA banana", bananaSa, SimpleSuffixArray.build("banana"));
        checkArray("Kasai empty", new int[]{}, KasaiLcp.build("", new int[]{}));
        checkArray("Kasai singleton", new int[]{0}, KasaiLcp.build("x", new int[]{0}));
        checkArray("Kasai all equal", new int[]{0, 1, 2, 3},
                KasaiLcp.build("aaaa", new int[]{3, 2, 1, 0}));
        checkArray("Kasai distinct", new int[]{0, 0, 0},
                KasaiLcp.build("abc", new int[]{0, 1, 2}));
        checkThrows("Kasai invalid permutation", () -> KasaiLcp.build("abc", new int[]{0, 1, 1}));
        checkThrows("Kasai wrong length", () -> KasaiLcp.build("abc", new int[]{0, 1}));
    }

    private static void randomizedCrossChecks() {
        Random random = new Random(42);
        for (int test = 0; test < 200; test++) {
            String text = randomString(random, random.nextInt(21));
            String pattern = randomString(random, random.nextInt(9));
            List<Integer> expected = bruteForceMatch(text, pattern);
            matchCase("random matching case #" + test, text, pattern, expected);
        }
        for (int test = 0; test < 100; test++) {
            String text = randomString(random, random.nextInt(13));
            int[] suffixArray = SimpleSuffixArray.build(text);
            int[] expectedLcp = bruteForceLcp(text, suffixArray);
            checkArray("random LCP case #" + test, expectedLcp, KasaiLcp.build(text, suffixArray));
        }
    }

    private static List<Integer> bruteForceMatch(String text, String pattern) {
        java.util.ArrayList<Integer> answer = new java.util.ArrayList<>();
        for (int i = 0; i + pattern.length() <= text.length(); i++) {
            boolean allEqual = true;
            for (int j = 0; j < pattern.length(); j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    allEqual = false;
                    break;
                }
            }
            if (allEqual) answer.add(i);
        }
        return answer;
    }

    private static int[] bruteForceLcp(String text, int[] suffixArray) {
        int[] expected = new int[text.length()];
        for (int k = 1; k < suffixArray.length; k++) {
            int i = suffixArray[k - 1];
            int j = suffixArray[k];
            while (i + expected[k] < text.length() && j + expected[k] < text.length()
                    && text.charAt(i + expected[k]) == text.charAt(j + expected[k])) {
                expected[k]++;
            }
        }
        return expected;
    }

    private static String randomString(Random r, int n) {
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) b.append("abcd".charAt(r.nextInt(4)));
        return b.toString();
    }

    private static void matchCase(String label, String text, String pattern, List<Integer> expected) {
        for (StringMatcher matcher : MATCHERS) {
            check(label + " / " + matcher.name(), expected, matcher.findAll(text, pattern).indices());
        }
    }

    private static void check(String name, Object expected, Object actual) {
        assertions++;
        if (!expected.equals(actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void checkTrue(String name, boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError(name);
    }

    private static void checkArray(String name, int[] expected, int[] actual) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(name + ": expected " + Arrays.toString(expected)
                    + ", got " + Arrays.toString(actual));
        }
    }

    private static void checkThrows(String name, Runnable runnable) {
        assertions++;
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(name + ": expected IllegalArgumentException");
    }
}
