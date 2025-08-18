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
   * @param extensionMapping Map of file extension to language name
   * @param filenameMapping Map of filename to language name
   * @param productionPatterns Patterns to identify production code directories
   * @param testPatterns Patterns to identify test code directories
   * @return List of contributor statistics sorted by contribution size
   */
  List<ContributorStats> aggregateStatistics(
      List<GitCommit> commits,
      Map<String, ContributorIdentity> identities,
      Map<String, String> extensionMapping,
      Map<String, String> filenameMapping,
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
   * Get the language for a file based on its extension or filename.
   *
   * @param filePath File path
   * @param extensionMapping Map of extension to language
   * @param filenameMapping Map of filename to language
   * @return Language name or "Unknown"
   */
  default String getLanguageForFile(
      String filePath, Map<String, String> extensionMapping, Map<String, String> filenameMapping) {
    int lastDot = filePath.lastIndexOf('.');
    String fileName = filePath.substring(filePath.lastIndexOf('/') + 1).toLowerCase();

    // First check if the complete filename (without path) matches our filename mappings
    if (filenameMapping.containsKey(fileName)) {
      return filenameMapping.get(fileName);
    }

    // Handle files without extensions
    if (lastDot == -1) {
      return "Unknown";
    }

    // Check for extension mapping, but only if the extension doesn't contain slashes
    String extension = filePath.substring(lastDot + 1).toLowerCase();
    if (!extension.contains("/")) {
      return extensionMapping.getOrDefault(extension, "Unknown");
    }

    return "Unknown";
  }
}
