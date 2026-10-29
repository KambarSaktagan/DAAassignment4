package edu.textanalysis.matching;

import java.util.List;

/** Instrumentation is part of the search operation and included in benchmark timings. */
public record MatchResult(
        List<Integer> indices,
        long preprocessingComparisons,
        long searchComparisons,
        long hashHits,
        long hashCollisions,
        long windowsChecked) {

    public MatchResult {
        indices = List.copyOf(indices);
    }

    /** Empty pattern matches each boundary, including the end (n + 1 matches). */
    public static MatchResult emptyPattern(int textLength) {
        java.util.ArrayList<Integer> positions = new java.util.ArrayList<>();
        for (int i = 0; i <= textLength; i++) positions.add(i);
        return new MatchResult(positions, 0, 0, 0, 0, 0);
    }

    public static MatchResult noMatches() {
        return new MatchResult(List.of(), 0, 0, 0, 0, 0);
    }
}
