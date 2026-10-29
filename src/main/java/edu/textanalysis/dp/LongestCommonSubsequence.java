package edu.textanalysis.dp;

/** Finds the LENGTH of the longest common subsequence (not necessarily contiguous). */
public final class LongestCommonSubsequence {
    private LongestCommonSubsequence() { }

    public static int length(String first, String second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Input strings must not be null");
        }

        int m = first.length();
        int n = second.length();
        int[][] dp = new int[m + 1][n + 1];
        // Base case: dp[0][j] and dp[i][0] are zero (Java's default initialization).
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (first.charAt(i - 1) == second.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }
}
