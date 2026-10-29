package edu.textanalysis.dp;

/** Unit-cost Levenshtein distance: insert, delete or substitute one UTF-16 code unit. */
public final class EditDistance {
    private EditDistance() { }

    public static int distance(String first, String second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Input strings must not be null");
        }

        int m = first.length();
        int n = second.length();
        int[][] dp = new int[m + 1][n + 1];
        // Transform a prefix into (or from) an empty prefix.
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int substitutionCost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                int delete = dp[i - 1][j] + 1;
                int insert = dp[i][j - 1] + 1;
                int substitute = dp[i - 1][j - 1] + substitutionCost;
                dp[i][j] = Math.min(delete, Math.min(insert, substitute));
            }
        }
        return dp[m][n];
    }
}
