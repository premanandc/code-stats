package com.codestats.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.model.ContributorIdentity;
import com.codestats.model.ContributorStats;
import com.codestats.model.FileChange;
import com.codestats.model.GitCommit;

class SimpleStatisticsAggregatorTest {

  private SimpleStatisticsAggregator aggregator;

  @BeforeEach
  void setUp() {
    aggregator = new SimpleStatisticsAggregator();
  }

  @Test
  @DisplayName("Should handle null commits list")
  void shouldHandleNullCommitsList() {
    // Test the "commits == null" conditional branch
    Map<String, ContributorIdentity> identities = Map.of();
    Map<String, String> extensions = Map.of("java", "Java");
    Map<String, String> filenames = Map.of();
    List<String> productionDirs = List.of("src/main");
    List<String> testDirs = List.of("src/test");

    List<ContributorStats> result =
        aggregator.aggregateStatistics(
            null, identities, extensions, filenames, productionDirs, testDirs);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should handle empty commits list")
  void shouldHandleEmptyCommitsList() {
    // Test the "commits.isEmpty()" conditional branch
    List<GitCommit> commits = List.of();
    Map<String, ContributorIdentity> identities = Map.of();
    Map<String, String> extensions = Map.of("java", "Java");
    Map<String, String> filenames = Map.of();
    List<String> productionDirs = List.of("src/main");
    List<String> testDirs = List.of("src/test");

    List<ContributorStats> result =
        aggregator.aggregateStatistics(
            commits, identities, extensions, filenames, productionDirs, testDirs);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should handle empty identities map by creating defaults")
  void shouldHandleEmptyIdentitiesMapByCreatingDefaults() {
    // Test the "resolvedIdentities.isEmpty()" conditional branch - should create default identities
    List<GitCommit> commits = List.of(createTestCommit("alice@example.com"));
    Map<String, ContributorIdentity> identities = Map.of(); // Empty identities
    Map<String, String> extensions = Map.of("java", "Java");
    Map<String, String> filenames = Map.of();
    List<String> productionDirs = List.of("src/main");
    List<String> testDirs = List.of("src/test");

    List<ContributorStats> result =
        aggregator.aggregateStatistics(
            commits, identities, extensions, filenames, productionDirs, testDirs);

    // Should create default identity and return 1 result (not empty)
    assertThat(result).hasSize(1);
    ContributorStats stats = result.get(0);
    assertThat(stats.primaryEmail()).isEqualTo("alice@example.com");
    assertThat(stats.name()).isEqualTo("Test Author");
  }

  @Test
  @DisplayName("Should aggregate statistics for single contributor")
  void shouldAggregateStatisticsForSingleContributor() {
    // Test normal flow with single contributor
    List<GitCommit> commits = List.of(createTestCommit("alice@example.com"));
    Map<String, ContributorIdentity> identities =
        Map.of(
            "alice@example.com",
            new ContributorIdentity(
                "Alice Smith",
                "alice@example.com",
                List.of("alice@example.com"),
                List.of("Alice Smith")));
    Map<String, String> extensions = Map.of("java", "Java");
    Map<String, String> filenames = Map.of();
    List<String> productionDirs = List.of("src/main");
    List<String> testDirs = List.of("src/test");

    List<ContributorStats> result =
        aggregator.aggregateStatistics(
            commits, identities, extensions, filenames, productionDirs, testDirs);

    assertThat(result).hasSize(1);
    ContributorStats stats = result.get(0);
    assertThat(stats.name()).isEqualTo("Alice Smith");
    assertThat(stats.primaryEmail()).isEqualTo("alice@example.com");
    assertThat(stats.commitCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should sort contributors by commit count descending")
  void shouldSortContributorsByCommitCountDescending() {
    // Test sorting logic and "commitComparison != 0" conditional branches
    List<GitCommit> commits =
        List.of(
            createTestCommit("alice@example.com"),
            createTestCommit("bob@example.com"),
            createTestCommit("bob@example.com"), // Bob has 2 commits
            createTestCommit("charlie@example.com"),
            createTestCommit("charlie@example.com"),
            createTestCommit("charlie@example.com") // Charlie has 3 commits
            );

    Map<String, ContributorIdentity> identities =
        Map.of(
            "alice@example.com",
                new ContributorIdentity(
                    "Alice", "alice@example.com", List.of("alice@example.com"), List.of("Alice")),
            "bob@example.com",
                new ContributorIdentity(
                    "Bob", "bob@example.com", List.of("bob@example.com"), List.of("Bob")),
            "charlie@example.com",
                new ContributorIdentity(
                    "Charlie",
                    "charlie@example.com",
                    List.of("charlie@example.com"),
                    List.of("Charlie")));
    Map<String, String> extensions = Map.of("java", "Java");
    Map<String, String> filenames = Map.of();
    List<String> productionDirs = List.of("src/main");
    List<String> testDirs = List.of("src/test");

    List<ContributorStats> result =
        aggregator.aggregateStatistics(
            commits, identities, extensions, filenames, productionDirs, testDirs);

    assertThat(result).hasSize(3);
    // Should be sorted by commit count descending
    assertThat(result.get(0).name()).isEqualTo("Charlie"); // 3 commits
    assertThat(result.get(1).name()).isEqualTo("Bob"); // 2 commits
    assertThat(result.get(2).name()).isEqualTo("Alice"); // 1 commit

    assertThat(result.get(0).commitCount()).isEqualTo(3);
    assertThat(result.get(1).commitCount()).isEqualTo(2);
    assertThat(result.get(2).commitCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should sort by name when commit counts are equal")
  void shouldSortByNameWhenCommitCountsEqual() {
    // Test the "commitComparison == 0" branch (fallback to name sorting)
    List<GitCommit> commits =
        List.of(
            createTestCommit("zebra@example.com"), createTestCommit("alice@example.com")
            // Both have 1 commit each, should sort by name
            );

    Map<String, ContributorIdentity> identities =
        Map.of(
            "zebra@example.com",
                new ContributorIdentity(
                    "Zebra", "zebra@example.com", List.of("zebra@example.com"), List.of("Zebra")),
            "alice@example.com",
                new ContributorIdentity(
                    "Alice", "alice@example.com", List.of("alice@example.com"), List.of("Alice")));
    Map<String, String> extensions = Map.of("java", "Java");
    Map<String, String> filenames = Map.of();
    List<String> productionDirs = List.of("src/main");
    List<String> testDirs = List.of("src/test");

    List<ContributorStats> result =
        aggregator.aggregateStatistics(
            commits, identities, extensions, filenames, productionDirs, testDirs);

    assertThat(result).hasSize(2);
    // Both have same commit count (1), should sort by name alphabetically
    assertThat(result.get(0).name()).isEqualTo("Alice"); // "Alice" comes before "Zebra"
    assertThat(result.get(1).name()).isEqualTo("Zebra");

    assertThat(result.get(0).commitCount()).isEqualTo(1);
    assertThat(result.get(1).commitCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle production and test code classification")
  void shouldHandleProductionAndTestCodeClassification() {
    // Test file classification logic
    List<FileChange> fileChanges =
        List.of(
            new FileChange("src/main/java/App.java", 10, 2, FileChange.ChangeType.MODIFIED),
            new FileChange("src/test/java/AppTest.java", 5, 1, FileChange.ChangeType.MODIFIED),
            new FileChange("docs/README.md", 3, 0, FileChange.ChangeType.MODIFIED));

    GitCommit commit =
        new GitCommit(
            "abc123",
            "Alice",
            "alice@example.com",
            LocalDateTime.now(),
            "Test commit",
            fileChanges,
            18,
            3);

    List<GitCommit> commits = List.of(commit);
    Map<String, ContributorIdentity> identities =
        Map.of(
            "alice@example.com",
            new ContributorIdentity(
                "Alice", "alice@example.com", List.of("alice@example.com"), List.of("Alice")));
    Map<String, String> extensions = Map.of("java", "Java", "md", "Markdown");
    Map<String, String> filenames = Map.of();
    List<String> productionDirs = List.of("src/main");
    List<String> testDirs = List.of("src/test");

    List<ContributorStats> result =
        aggregator.aggregateStatistics(
            commits, identities, extensions, filenames, productionDirs, testDirs);

    assertThat(result).hasSize(1);
    ContributorStats stats = result.get(0);

    // Check production code
    assertThat(stats.productionLines()).containsKey("Java");
    assertThat(stats.productionLines().get("Java")).isEqualTo(8); // 10-2 net lines

    // Check test code
    assertThat(stats.testLines()).containsKey("Java");
    assertThat(stats.testLines().get("Java")).isEqualTo(4); // 5-1 net lines

    // Check other code
    assertThat(stats.otherLines()).containsKey("Markdown");
    assertThat(stats.otherLines().get("Markdown")).isEqualTo(3); // 3-0 net lines
  }

  // Helper method to create test commits
  private GitCommit createTestCommit(String authorEmail) {
    List<FileChange> fileChanges =
        List.of(new FileChange("src/main/java/Test.java", 10, 0, FileChange.ChangeType.MODIFIED));

    return new GitCommit(
        "hash123",
        "Test Author",
        authorEmail,
        LocalDateTime.now(),
        "Test commit",
        fileChanges,
        10,
        0);
  }
}
