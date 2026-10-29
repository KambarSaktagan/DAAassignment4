package edu.textanalysis.matching;

import java.util.ArrayList;
import java.util.List;

/** Rolling polynomial hash with exact character verification after every hash hit. */
public final class RabinKarpMatcher implements StringMatcher {
    private static final long DEFAULT_BASE = 256;
    private static final long DEFAULT_MODULUS = 1_000_000_007L;
    private final long base;
    private final long modulus;

    public RabinKarpMatcher() {
        this(DEFAULT_BASE, DEFAULT_MODULUS);
    }

    // Public constructor allows intentional, deterministic collision tests.
    public RabinKarpMatcher(long base, long modulus) {
        if (base <= 1 || modulus <= 1 || base > 1_000_000 || modulus > 1_000_000_007L) {
            throw new IllegalArgumentException("Invalid hash parameters or risk of long overflow");
        }
        this.base = base;
        this.modulus = modulus;
    }

    @Override
    public String name() { return "Rabin-Karp"; }

    @Override
    public MatchResult findAll(String text, String pattern) {
        StringMatcher.requireInputs(text, pattern);
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return MatchResult.emptyPattern(n);
        if (m > n) return MatchResult.noMatches();

        long highestPower = 1;
        for (int i = 1; i < m; i++) highestPower = highestPower * base % modulus;

        long patternHash = 0;
        long windowHash = 0;
        for (int j = 0; j < m; j++) {
            patternHash = (patternHash * base + pattern.charAt(j)) % modulus;
            windowHash = (windowHash * base + text.charAt(j)) % modulus;
        }

        List<Integer> matches = new ArrayList<>();
        long comparisons = 0;
        long hashHits = 0;
        long collisions = 0;
        long windows = 0;
        for (int start = 0; start <= n - m; start++) {
            windows++;
            if (windowHash == patternHash) {
                hashHits++;
                boolean equal = true;
                for (int j = 0; j < m; j++) {
                    comparisons++;
                    if (text.charAt(start + j) != pattern.charAt(j)) {
                        equal = false;
                        break;
                    }
                }
                if (equal) matches.add(start);
                else collisions++;
            }

            if (start < n - m) {
                long withoutOld = (windowHash - text.charAt(start) * highestPower % modulus + modulus) % modulus;
                windowHash = (withoutOld * base + text.charAt(start + m)) % modulus;
            }
        }
        return new MatchResult(matches, 0, comparisons, hashHits, collisions, windows);
    }
}
