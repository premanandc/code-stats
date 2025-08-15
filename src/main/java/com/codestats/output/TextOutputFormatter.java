package com.codestats.output;

import com.codestats.model.ContributorStats;
import com.codestats.model.LanguageStats;
import com.codestats.service.CodeStatsService;

import java.util.Map;

/**
 * Text-based output formatter with colors and clear formatting.
 */
public class TextOutputFormatter implements OutputFormatter {
    
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    
    private final boolean useColors;
    
    public TextOutputFormatter(boolean useColors) {
        this.useColors = useColors;
    }
    
    public TextOutputFormatter() {
        this(true);
    }
    
    @Override
    public String format(CodeStatsService.CodeStatsResult result) {
        if (!result.isSuccess()) {
            return color(RED, "❌ Error: " + result.errorMessage());
        }
        
        if (result.contributorStats().isEmpty()) {
            return color(YELLOW, "📊 No commits found in the specified repository or time range.");
        }
        
        StringBuilder output = new StringBuilder();
        
        // Header
        output.append(color(BOLD + BLUE, "📊 Code Statistics Report")).append("\n");
        output.append("━".repeat(50)).append("\n");
        output.append(color(CYAN, "Repository: ")).append(result.repositoryPath().getAbsolutePath()).append("\n");
        output.append(color(CYAN, "Period: ")).append(result.getAnalysisPeriod()).append("\n");
        output.append(color(CYAN, "Total commits: ")).append(result.totalCommits()).append("\n\n");
        
        // Contributor statistics
        output.append(color(BOLD, "Contributors:")).append("\n");
        for (int i = 0; i < result.contributorStats().size(); i++) {
            ContributorStats stats = result.contributorStats().get(i);
            output.append(formatContributor(stats, result.totalCommits(), i + 1));
            output.append("\n");
        }
        
        return output.toString();
    }
    
    private String formatContributor(ContributorStats stats, int totalCommits, int rank) {
        StringBuilder sb = new StringBuilder();
        
        // Header with rank and name
        sb.append(color(BOLD + GREEN, String.format("%d. %s", rank, stats.name()))).append("\n");
        sb.append("   ").append(color(CYAN, "Email: ")).append(stats.primaryEmail()).append("\n");
        
        // Show aliases if multiple emails
        if (stats.allEmails().size() > 1) {
            sb.append("   ").append(color(YELLOW, "Also commits as: "));
            stats.allEmails().stream()
                .filter(email -> !email.equals(stats.primaryEmail()))
                .forEach(email -> sb.append(email).append(" "));
            sb.append("\n");
        }
        
        // Basic statistics
        double percentage = stats.getCommitPercentage(totalCommits);
        sb.append("   ").append(color(CYAN, "Commits: ")).append(stats.commitCount())
          .append(" (").append(String.format("%.1f%%", percentage)).append(")\n");
        
        sb.append("   ").append(color(CYAN, "Files changed: ")).append(stats.filesChanged()).append("\n");
        
        // Lines with colors based on net change
        int netLines = stats.netLines();
        String linesColor = netLines > 0 ? GREEN : (netLines < 0 ? RED : YELLOW);
        sb.append("   ").append(color(CYAN, "Lines: "))
          .append(color(GREEN, "+" + stats.insertions()))
          .append(" ")
          .append(color(RED, "-" + stats.deletions()))
          .append(" (net: ").append(color(linesColor, formatNetChange(netLines))).append(")\n");
        
        // Language breakdown
        if (!stats.languageStats().isEmpty()) {
            sb.append("   ").append(color(CYAN, "Languages: "));
            stats.languageStats().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().linesChanged(), a.getValue().linesChanged()))
                .limit(3) // Show top 3 languages
                .forEach(entry -> {
                    String lang = entry.getKey();
                    LanguageStats langStats = entry.getValue();
                    sb.append(lang).append(" (").append(langStats.linesChanged()).append(") ");
                });
            sb.append("\n");
        }
        
        // Production vs Test lines
        if (!stats.productionLines().isEmpty() || !stats.testLines().isEmpty()) {
            int totalProd = stats.productionLines().values().stream().mapToInt(Integer::intValue).sum();
            int totalTest = stats.testLines().values().stream().mapToInt(Integer::intValue).sum();
            
            if (totalProd > 0 || totalTest > 0) {
                sb.append("   ").append(color(CYAN, "Code distribution: "));
                if (totalProd > 0) {
                    sb.append(color(GREEN, "Production: " + totalProd));
                }
                if (totalTest > 0) {
                    if (totalProd > 0) sb.append(", ");
                    sb.append(color(YELLOW, "Test: " + totalTest));
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String formatNetChange(int netLines) {
        if (netLines > 0) {
            return "+" + netLines;
        } else {
            return String.valueOf(netLines);
        }
    }
    
    private String color(String colorCode, String text) {
        if (!useColors) {
            return text;
        }
        return colorCode + text + RESET;
    }
}