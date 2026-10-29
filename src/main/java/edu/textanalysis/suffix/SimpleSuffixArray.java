package edu.textanalysis.suffix;

import java.util.Arrays;

/** Benchmark setup only: slow/simple suffix-array generation; never included in Kasai timing. */
public final class SimpleSuffixArray {
    private SimpleSuffixArray() { }

    public static int[] build(String text) {
        if (text == null) throw new IllegalArgumentException("Text must not be null");
        int n = text.length();
        Integer[] starts = new Integer[n];
        for (int i = 0; i < n; i++) starts[i] = i;
        Arrays.sort(starts, (a, b) -> {
            int i = a;
            int j = b;
            while (i < n && j < n) {
                int difference = Character.compare(text.charAt(i), text.charAt(j));
                if (difference != 0) return difference;
                i++;
                j++;
            }
            // If one suffix is a prefix of the other, the shorter suffix sorts first.
            return Integer.compare(n - a, n - b);
        });
        int[] result = new int[n];
        for (int i = 0; i < n; i++) result[i] = starts[i];
        return result;
    }
}
