package com.codestats.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable record representing statistics for a single contributor. Using Java 17 records for
 * immutability and functional style.
 */
public record ContributorStats(
    String name,
    String primaryEmail,
    List<String> allEmails,
    int commitCount,
    int filesChanged,
    int insertions,
    int deletions,
    Map<String, LanguageStats> languageStats,
    Map<String, Integer> productionLines,
    Map<String, Integer> testLines,
    Map<String, Integer> otherLines) {

  /** Calculate total lines changed (insertions + deletions) */
  public int totalLinesChanged() {
    return insertions + deletions;
  }

  /** Calculate net lines (insertions - deletions) */
  public int netLines() {
    return insertions - deletions;
  }

  /** Get contribution percentage relative to total commits */
  public double getCommitPercentage(int totalCommits) {
    return totalCommits == 0 ? 0.0 : (double) commitCount / totalCommits * 100.0;
  }

  /** Builder for functional construction */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String primaryEmail;
    private List<String> allEmails = List.of();
    private int commitCount;
    private int filesChanged;
    private int insertions;
    private int deletions;
    private Map<String, LanguageStats> languageStats = Map.of();
    private Map<String, Integer> productionLines = Map.of();
    private Map<String, Integer> testLines = Map.of();
    private Map<String, Integer> otherLines = Map.of();

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder primaryEmail(String primaryEmail) {
      this.primaryEmail = primaryEmail;
      return this;
    }

    public Builder allEmails(List<String> allEmails) {
      this.allEmails = List.copyOf(allEmails);
      return this;
    }

    public Builder commitCount(int commitCount) {
      this.commitCount = commitCount;
      return this;
    }

    public Builder filesChanged(int filesChanged) {
      this.filesChanged = filesChanged;
      return this;
    }

    public Builder insertions(int insertions) {
      this.insertions = insertions;
      return this;
    }

    public Builder deletions(int deletions) {
      this.deletions = deletions;
      return this;
    }

    public Builder languageStats(Map<String, LanguageStats> languageStats) {
      this.languageStats = Map.copyOf(languageStats);
      return this;
    }

    public Builder productionLines(Map<String, Integer> productionLines) {
      this.productionLines = Map.copyOf(productionLines);
      return this;
    }

    public Builder testLines(Map<String, Integer> testLines) {
      this.testLines = Map.copyOf(testLines);
      return this;
    }

    public Builder otherLines(Map<String, Integer> otherLines) {
      this.otherLines = Map.copyOf(otherLines);
      return this;
    }

    public ContributorStats build() {
      return new ContributorStats(
          name,
          primaryEmail,
          allEmails,
          commitCount,
          filesChanged,
          insertions,
          deletions,
          languageStats,
          productionLines,
          testLines,
          otherLines);
    }
  }
}
