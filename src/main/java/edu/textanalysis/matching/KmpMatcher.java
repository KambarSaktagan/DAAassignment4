package edu.textanalysis.matching;

import java.util.ArrayList;
import java.util.List;

/** Knuth-Morris-Pratt: use the longest proper prefix/suffix (LPS) to skip rechecks. */
public final class KmpMatcher implements StringMatcher {
    @Override
    public String name() { return "KMP"; }

    @Override
    public MatchResult findAll(String text, String pattern) {
        StringMatcher.requireInputs(text, pattern);
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return MatchResult.emptyPattern(n);
        if (m > n) return MatchResult.noMatches();

        int[] lps = new int[m];
        long preprocessComparisons = 0;
        for (int i = 1, len = 0; i < m;) {
            preprocessComparisons++;
            if (pattern.charAt(i) == pattern.charAt(len)) {
                lps[i++] = ++len;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i++] = 0;
            }
        }

        List<Integer> matches = new ArrayList<>();
        long searchComparisons = 0;
        int i = 0; // current text index
        int j = 0; // current pattern index
        while (i < n) {
            searchComparisons++;
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    matches.add(i - m);
                    j = lps[j - 1]; // permits overlapping matches
                }
            } else if (j > 0) {
                j = lps[j - 1]; // do not increment i: no need to recheck prior matches
            } else {
                i++;
            }
        }
        return new MatchResult(matches, preprocessComparisons, searchComparisons, 0, 0, 0);
    }
}
