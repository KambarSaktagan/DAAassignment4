package edu.textanalysis.benchmark;

import edu.textanalysis.dp.EditDistance;
import edu.textanalysis.dp.LongestCommonSubsequence;
import edu.textanalysis.matching.KmpMatcher;
import edu.textanalysis.matching.MatchResult;
import edu.textanalysis.matching.NaiveMatcher;
import edu.textanalysis.matching.RabinKarpMatcher;
import edu.textanalysis.matching.StringMatcher;
import edu.textanalysis.suffix.KasaiLcp;
import edu.textanalysis.suffix.SimpleSuffixArray;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** One command reproduces every fixed-size workload and writes results/benchmarks.csv. */
public final class BenchmarkRunner {
    private static final int TIMED_RUNS = 5;
    private static final int WARMUP_RUNS = 30;
    private static final long SEED = 20261029L;
    private static final String ALPHABET = "abcd";
    private static final int PATTERN_LENGTH = 24;
    private static final List<StringMatcher> MATCHERS = List.of(
            new NaiveMatcher(), new KmpMatcher(), new RabinKarpMatcher());

    // Volatile consumers stop the JIT compiler treating an unused computed result as dead code.
    private static volatile int intSink;
    private static volatile MatchResult matchSink;
    private static volatile int[] arraySink;

    private BenchmarkRunner() { }

    public static void main(String[] args) throws IOException {
        Random random = new Random(SEED);
        List<Row> results = new ArrayList<>();
        int[] dpSizes = {100, 500, 1000};
        int[] matchSizes = {10000, 50000, 100000};
        int[] kasaiSizes = {1000, 5000, 10000};

        System.out.println("Benchmark seed: " + SEED + "; alphabet: " + ALPHABET);
        System.out.println("Warmups: " + WARMUP_RUNS + "; timed repetitions: " + TIMED_RUNS);

        for (int size : dpSizes) {
            final String a = randomString(random, size);
            final String b = randomString(random, size);
            int lcs = LongestCommonSubsequence.length(a, b);
            Timing lcsTime = measure(() -> intSink = LongestCommonSubsequence.length(a, b));
            results.add(new Row("LCS", "random", size, 0, lcsTime,
                    (long) size * size, 0, 0, 0, 0, 0, lcs));

            int distance = EditDistance.distance(a, b);
            Timing editTime = measure(() -> intSink = EditDistance.distance(a, b));
            results.add(new Row("Edit Distance", "random", size, 0, editTime,
                    (long) size * size, 0, 0, 0, 0, 0, distance));
        }

        for (int size : matchSizes) {
            final String text = randomString(random, size);
            final String present = text.substring(size / 2, size / 2 + PATTERN_LENGTH);
            // An absent pattern is created with the same alphabet and independently verified.
            String absent;
            do {
                absent = randomString(random, PATTERN_LENGTH);
            } while (!new NaiveMatcher().findAll(text, absent).indices().isEmpty());

            for (String scenario : List.of("present", "absent")) {
                final String pattern = scenario.equals("present") ? present : absent;
                List<Integer> expected = MATCHERS.get(0).findAll(text, pattern).indices();
                if (scenario.equals("present") && expected.isEmpty()) {
                    throw new IllegalStateException("Present pattern unexpectedly absent");
                }
                if (scenario.equals("absent") && !expected.isEmpty()) {
                    throw new IllegalStateException("Absent pattern unexpectedly present");
                }
                for (StringMatcher matcher : MATCHERS) {
                    MatchResult observed = matcher.findAll(text, pattern);
                    if (!observed.indices().equals(expected)) {
                        throw new AssertionError("Mismatch: " + matcher.name() + " / " + scenario);
                    }
                    Timing time = measure(() -> matchSink = matcher.findAll(text, pattern));
                    results.add(new Row(matcher.name(), scenario, size, PATTERN_LENGTH, time,
                            0, observed.preprocessingComparisons(), observed.searchComparisons(),
                            observed.hashHits(), observed.hashCollisions(), observed.windowsChecked(),
                            observed.indices().size()));
                }
            }
        }

        for (int size : kasaiSizes) {
            final String text = randomString(random, size);
            // This slow, comparator-based construction is intentionally OUTSIDE the timed region.
            final int[] suffixArray = SimpleSuffixArray.build(text);
            final int[] expected = KasaiLcp.build(text, suffixArray);
            Timing time = measure(() -> arraySink = KasaiLcp.build(text, suffixArray));
            results.add(new Row("Kasai", "random", size, 0, time,
                    0, 0, 0, 0, 0, 0, checksum(expected)));
        }

        Path output = Path.of("results", "benchmarks.csv");
        Files.createDirectories(output.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("algorithm,scenario,input_size,pattern_length,average_ms,min_ms,max_ms,dp_cells,"
                + "preprocess_char_comparisons,search_char_comparisons,hash_hits,hash_collisions,"
                + "windows_checked,result_value");
        for (Row row : results) {
            lines.add(row.toCsv());
            System.out.printf(Locale.US, "%-14s %-7s n=%-6d avg=%9.5f ms result=%d%n",
                    row.algorithm(), row.scenario(), row.inputSize(), row.timing().averageMs(),
                    row.resultValue());
        }
        Files.write(output, lines, StandardCharsets.UTF_8);
        System.out.println("Saved " + output + " (" + results.size() + " measurements).");
    }

    private static Timing measure(Runnable algorithmCall) {
        for (int r = 0; r < WARMUP_RUNS; r++) algorithmCall.run();
        long sumNanos = 0;
        long minNanos = Long.MAX_VALUE;
        long maxNanos = 0;
        for (int r = 0; r < TIMED_RUNS; r++) {
            long before = System.nanoTime();
            algorithmCall.run();
            long after = System.nanoTime();
            long elapsed = after - before;
            sumNanos += elapsed;
            minNanos = Math.min(minNanos, elapsed);
            maxNanos = Math.max(maxNanos, elapsed);
        }
        return new Timing(sumNanos / (double) TIMED_RUNS / 1_000_000.0,
                minNanos / 1_000_000.0, maxNanos / 1_000_000.0);
    }

    private static String randomString(Random r, int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
        }
        return result.toString();
    }

    private static int checksum(int[] values) {
        int sum = 0;
        for (int v : values) sum += v;
        return sum;
    }

    private record Timing(double averageMs, double minMs, double maxMs) { }

    private record Row(String algorithm, String scenario, int inputSize, int patternLength,
                       Timing timing, long dpCells, long preprocessComparisons,
                       long searchComparisons, long hashHits, long hashCollisions,
                       long windowsChecked, int resultValue) {
        String toCsv() {
            return String.format(Locale.US,
                    "%s,%s,%d,%d,%.8f,%.8f,%.8f,%d,%d,%d,%d,%d,%d,%d",
                    algorithm, scenario, inputSize, patternLength,
                    timing.averageMs(), timing.minMs(), timing.maxMs(),
                    dpCells, preprocessComparisons, searchComparisons,
                    hashHits, hashCollisions, windowsChecked, resultValue);
        }
    }
}
