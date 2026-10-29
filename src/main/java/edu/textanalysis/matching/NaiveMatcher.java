package edu.textanalysis.matching;

import java.util.ArrayList;
import java.util.List;

/** Try each possible start position and compare characters left-to-right. */
public final class NaiveMatcher implements StringMatcher {
    @Override
    public String name() { return "Naive"; }

    @Override
    public MatchResult findAll(String text, String pattern) {
        StringMatcher.requireInputs(text, pattern);
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return MatchResult.emptyPattern(n);
        if (m > n) return MatchResult.noMatches();

        List<Integer> matches = new ArrayList<>();
        long comparisons = 0;
        long windows = 0;
        for (int start = 0; start <= n - m; start++) {
            windows++;
            int j = 0;
            while (j < m) {
                comparisons++;
                if (text.charAt(start + j) != pattern.charAt(j)) break;
                j++;
            }
            if (j == m) matches.add(start);
        }
        return new MatchResult(matches, 0, comparisons, 0, 0, windows);
    }
}
