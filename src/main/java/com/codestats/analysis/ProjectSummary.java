package com.codestats.analysis;

import java.time.LocalDateTime;

/** Executive summary of project development metrics. */
public record ProjectSummary(
    String projectName,
    int totalCommits,
    int activeContributors,
    int totalLinesChanged,
    String analysisPeriod,
    LocalDateTime oldestCommit,
    LocalDateTime newestCommit,
    String topPerformerName,
    int topPerformerCommits,
    double topPerformerPercentage) {

  public String getFormattedLinesChanged() {
    if (totalLinesChanged >= 1000) {
      return String.format("%.1fK", totalLinesChanged / 1000.0);
    }
    return String.valueOf(totalLinesChanged);
  }

  public String getVelocityIndicator() {
    if (totalCommits > 100) return "🚀 High";
    if (totalCommits > 50) return "📈 Medium";
    return "📊 Steady";
  }
}
