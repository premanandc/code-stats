package com.codestats.stats;

import java.util.List;
import java.util.Map;

import com.codestats.model.ContributorIdentity;
import com.codestats.model.ContributorStats;
import com.codestats.model.GitCommit;

/**
 * Functional interface for aggregating git commit data into contributor statistics. Combines commit
 * data with contributor identities to produce final analytics.
 */
public interface StatisticsAggregator {

  /**
   * Aggregate git commits into contributor statistics.
   *
   * @param commits List of git commits to analyze
   * @param identities Map of canonical email to contributor identity
   * @param languageMapping Map of file extension to language name
   * @param productionPatterns Patterns to identify production code directories
   * @param testPatterns Patterns to identify test code directories
   * @return List of contributor statistics sorted by contribution size
   */
  List<ContributorStats> aggregateStatistics(
      List<GitCommit> commits,
      Map<String, ContributorIdentity> identities,
      Map<String, String> languageMapping,
      List<String> productionPatterns,
      List<String> testPatterns);

  /**
   * Calculate total commit count across all contributors. Used for percentage calculations.
   *
   * @param commits List of commits
   * @return Total number of commits
   */
  default int calculateTotalCommits(List<GitCommit> commits) {
    return commits.size();
  }

  /**
   * Determine if a file path represents production code.
   *
   * @param filePath Path to check
   * @param productionPatterns List of patterns that indicate production code
   * @param testPatterns List of patterns that indicate test code
   * @return true if this is production code, false if test code
   */
  default boolean isProductionCode(
      String filePath, List<String> productionPatterns, List<String> testPatterns) {
    String lowerPath = filePath.toLowerCase();

    // Check test patterns first (more specific)
    for (String testPattern : testPatterns) {
      if (lowerPath.contains(testPattern.toLowerCase())) {
        return false;
      }
    }

    // Check production patterns
    for (String prodPattern : productionPatterns) {
      if (lowerPath.contains(prodPattern.toLowerCase())) {
        return true;
      }
    }

    // Default to production if no pattern matches
    return true;
  }

  /**
   * Get the language for a file based on its extension.
   *
   * @param filePath File path
   * @param languageMapping Map of extension to language
   * @return Language name or "Unknown"
   */
  default String getLanguageForFile(String filePath, Map<String, String> languageMapping) {
    int lastDot = filePath.lastIndexOf('.');
    if (lastDot == -1) return "Unknown";

    String extension = filePath.substring(lastDot + 1).toLowerCase();
    return languageMapping.getOrDefault(extension, "Unknown");
  }
}
