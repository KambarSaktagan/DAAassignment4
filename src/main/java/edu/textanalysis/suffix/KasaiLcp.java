package edu.textanalysis.suffix;

/** Linear-time LCP construction from a GIVEN sorted suffix array. */
public final class KasaiLcp {
    private KasaiLcp() { }

    /** lcp[i] = common-prefix length of suffixes at suffixArray[i-1] and suffixArray[i]. */
    public static int[] build(String text, int[] suffixArray) {
        if (text == null || suffixArray == null) {
            throw new IllegalArgumentException("Text and suffix array must not be null");
        }
        int n = text.length();
        if (suffixArray.length != n) {
            throw new IllegalArgumentException("Suffix array length must equal text length");
        }

        int[] rank = new int[n];
        boolean[] seen = new boolean[n];
        for (int i = 0; i < n; i++) {
            int pos = suffixArray[i];
            if (pos < 0 || pos >= n || seen[pos]) {
                throw new IllegalArgumentException("Suffix array must be a permutation of [0,n)");
            }
            seen[pos] = true;
            rank[pos] = i;
        }

        int[] lcp = new int[n];
        int h = 0;
        for (int i = 0; i < n; i++) {
            int sortedPosition = rank[i];
            if (sortedPosition == 0) {
                h = 0;
                continue; // first sorted suffix has no predecessor
            }
            int j = suffixArray[sortedPosition - 1];
            while (i + h < n && j + h < n && text.charAt(i + h) == text.charAt(j + h)) {
                h++;
            }
            lcp[sortedPosition] = h;
            if (h > 0) h--; // neighboring suffixes share at least h-1 characters
        }
        return lcp;
    }
}
