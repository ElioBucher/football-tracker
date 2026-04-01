package ch.elio.football;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FuzzySearchService {

    public double similarity(String a, String b) {
        a = a.toLowerCase().trim();
        b = b.toLowerCase().trim();

        if (a.equals(b)) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        if (b.contains(a) || a.contains(b)) return 0.9;

        int distance = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return 1.0 - ((double) distance / maxLen);
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i-1][j-1],
                            Math.min(dp[i-1][j], dp[i][j-1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }
}