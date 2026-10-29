package edu.textanalysis.matching;

public interface StringMatcher {
    String name();
    MatchResult findAll(String text, String pattern);

    static void requireInputs(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("Text and pattern must not be null");
        }
    }
}
