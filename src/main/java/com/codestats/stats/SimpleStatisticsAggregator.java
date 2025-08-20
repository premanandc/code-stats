package com.codestats.stats;

import java.util.*;
import java.util.stream.Collectors;

import com.codestats.model.*;

/**
 * Simple implementation of StatisticsAggregator using functional programming principles. Pure
 * functions, immutable data structures, no side effects.
 */
public class SimpleStatisticsAggregator implements StatisticsAggregator {

  @Override
  public List<ContributorStats> aggregateStatistics(
      List<GitCommit> commits,
      Map<String, ContributorIdentity> identities,
      Map<String, String> extensionMapping,
      Map<String, String> filenameMapping,
      List<String> productionPatterns,
      List<String> testPatterns) {
    if (commits == null || commits.isEmpty()) {
      return List.of();
    }

    // First resolve aliases to find canonical emails
    Map<String, ContributorIdentity> resolvedIdentities = identities;
    if (resolvedIdentities.isEmpty()) {
      // If no identities provided, create them from commits
      resolvedIdentities = createDefaultIdentities(commits);
    }

    // Create email to canonical email mapping
    Map<String, String> emailToCanonical = new HashMap<>();
    for (ContributorIdentity identity : resolvedIdentities.values()) {
      String canonical = identity.primaryEmail();
      for (String email : identity.allEmails()) {
        emailToCanonical.put(email, canonical);
      }
    }

    // Group commits by canonical email
    Map<String, List<GitCommit>> commitsByContributor =
        commits.stream()
            .collect(
                Collectors.groupingBy(
                    commit ->
                        emailToCanonical.getOrDefault(commit.authorEmail(), commit.authorEmail())));

    // Aggregate statistics for each contributor
    List<ContributorStats> allStats = new ArrayList<>();

    for (Map.Entry<String, List<GitCommit>> entry : commitsByContributor.entrySet()) {
      String email = entry.getKey();
      List<GitCommit> contributorCommits = entry.getValue();

      // Get identity or create default
      ContributorIdentity identity =
          identities.getOrDefault(email, createDefaultIdentity(email, contributorCommits));

      ContributorStats stats =
          aggregateContributorStats(
              identity,
              contributorCommits,
              extensionMapping,
              filenameMapping,
              productionPatterns,
              testPatterns);

      allStats.add(stats);
    }

    // Sort by contribution size (commit count descending, then by name)
    allStats.sort(
        (a, b) -> {
          int commitComparison = Integer.compare(b.commitCount(), a.commitCount());
          if (commitComparison != 0) {
            return commitComparison;
          }
          return a.name().compareTo(b.name());
        });

    return List.copyOf(allStats);
  }

  /** Aggregate statistics for a single contributor from their commits. */
  private ContributorStats aggregateContributorStats(
      ContributorIdentity identity,
      List<GitCommit> commits,
      Map<String, String> extensionMapping,
      Map<String, String> filenameMapping,
      List<String> productionPatterns,
      List<String> testPatterns) {
    int totalCommits = commits.size();
    int totalInsertions = commits.stream().mapToInt(GitCommit::insertions).sum();
    int totalDeletions = commits.stream().mapToInt(GitCommit::deletions).sum();
    int totalFilesChanged = commits.stream().mapToInt(GitCommit::filesChangedCount).sum();

    // Aggregate language statistics
    Map<String, LanguageStats> languageStats =
        aggregateLanguageStats(commits, extensionMapping, filenameMapping);

    // Aggregate production vs test lines
    Map<String, Integer> productionLines = new HashMap<>();
    Map<String, Integer> testLines = new HashMap<>();

    for (GitCommit commit : commits) {
      for (FileChange fileChange : commit.fileChanges()) {
        String language = getLanguageForFile(fileChange.path(), extensionMapping, filenameMapping);
        int netLines = fileChange.netLines();

        if (isProductionCode(fileChange.path(), productionPatterns, testPatterns)) {
          productionLines.merge(language, netLines, Integer::sum);
        } else if (isTestCode(fileChange.path(), productionPatterns, testPatterns)) {
          testLines.merge(language, netLines, Integer::sum);
        }
        // Files that are neither production nor test are not counted in code distribution
      }
    }

    return ContributorStats.builder()
        .name(identity.canonicalName())
        .primaryEmail(identity.primaryEmail())
        .allEmails(identity.allEmails())
        .commitCount(totalCommits)
        .filesChanged(totalFilesChanged)
        .insertions(totalInsertions)
        .deletions(totalDeletions)
        .languageStats(Map.copyOf(languageStats))
        .productionLines(Map.copyOf(productionLines))
        .testLines(Map.copyOf(testLines))
        .build();
  }

  /** Aggregate language statistics from commits. */
  private Map<String, LanguageStats> aggregateLanguageStats(
      List<GitCommit> commits,
      Map<String, String> extensionMapping,
      Map<String, String> filenameMapping) {
    Map<String, List<FileChange>> changesByLanguage = new HashMap<>();

    // Group all file changes by language
    for (GitCommit commit : commits) {
      for (FileChange fileChange : commit.fileChanges()) {
        String language = getLanguageForFile(fileChange.path(), extensionMapping, filenameMapping);
        changesByLanguage.computeIfAbsent(language, k -> new ArrayList<>()).add(fileChange);
      }
    }

    // Aggregate statistics for each language
    Map<String, LanguageStats> languageStats = new HashMap<>();

    for (Map.Entry<String, List<FileChange>> entry : changesByLanguage.entrySet()) {
      String language = entry.getKey();
      List<FileChange> changes = entry.getValue();

      int totalInsertions = changes.stream().mapToInt(FileChange::insertions).sum();
      int totalDeletions = changes.stream().mapToInt(FileChange::deletions).sum();
      int totalLinesChanged = changes.stream().mapToInt(FileChange::totalLinesChanged).sum();
      int filesChanged = changes.size();

      LanguageStats stats =
          new LanguageStats(
              language, totalLinesChanged, totalInsertions, totalDeletions, filesChanged);

      languageStats.put(language, stats);
    }

    return languageStats;
  }

  /** Create a default identity when none is provided. */
  private ContributorIdentity createDefaultIdentity(String email, List<GitCommit> commits) {
    // Find the most complete name from commits
    String name =
        commits.stream()
            .map(GitCommit::authorName)
            .max(Comparator.comparing(String::length))
            .orElse("Unknown");

    return ContributorIdentity.builder()
        .canonicalName(name)
        .primaryEmail(email)
        .allEmails(List.of(email))
        .allNames(List.of(name))
        .build();
  }

  /** Create default identities for all contributors when none provided. */
  private Map<String, ContributorIdentity> createDefaultIdentities(List<GitCommit> commits) {
    Map<String, List<GitCommit>> commitsByEmail =
        commits.stream().collect(Collectors.groupingBy(GitCommit::authorEmail));

    Map<String, ContributorIdentity> identities = new HashMap<>();
    for (Map.Entry<String, List<GitCommit>> entry : commitsByEmail.entrySet()) {
      String email = entry.getKey();
      List<GitCommit> emailCommits = entry.getValue();
      identities.put(email, createDefaultIdentity(email, emailCommits));
    }

    return identities;
  }
}
