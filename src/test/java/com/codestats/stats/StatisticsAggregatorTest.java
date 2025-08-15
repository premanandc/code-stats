package com.codestats.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.model.*;

class StatisticsAggregatorTest {

  private StatisticsAggregator aggregator;

  @BeforeEach
  void setUp() {
    aggregator = new SimpleStatisticsAggregator();
  }

  @Test
  @DisplayName("Should aggregate statistics for single contributor")
  void shouldAggregateStatsForSingleContributor() {
    // Create test commits
    List<GitCommit> commits =
        List.of(
            createCommit(
                "Alice",
                "alice@company.com",
                List.of(
                    new FileChange("src/User.java", 10, 2, FileChange.ChangeType.MODIFIED),
                    new FileChange("test/UserTest.java", 5, 1, FileChange.ChangeType.MODIFIED)),
                15,
                3),
            createCommit(
                "Alice",
                "alice@company.com",
                List.of(new FileChange("src/Auth.java", 8, 0, FileChange.ChangeType.ADDED)),
                8,
                0));

    // Create identity mapping
    Map<String, ContributorIdentity> identities =
        Map.of(
            "alice@company.com",
            ContributorIdentity.builder()
                .canonicalName("Alice Smith")
                .primaryEmail("alice@company.com")
                .allEmails(List.of("alice@company.com"))
                .allNames(List.of("Alice"))
                .build());

    // Language mapping
    Map<String, String> languageMapping = Map.of("java", "Java");

    // Directory patterns
    List<String> productionPatterns = List.of("src");
    List<String> testPatterns = List.of("test");

    List<ContributorStats> stats =
        aggregator.aggregateStatistics(
            commits, identities, languageMapping, productionPatterns, testPatterns);

    assertThat(stats).hasSize(1);

    ContributorStats aliceStats = stats.get(0);
    assertThat(aliceStats.name()).isEqualTo("Alice Smith");
    assertThat(aliceStats.primaryEmail()).isEqualTo("alice@company.com");
    assertThat(aliceStats.commitCount()).isEqualTo(2);
    assertThat(aliceStats.filesChanged()).isEqualTo(3); // 2 + 1
    assertThat(aliceStats.insertions()).isEqualTo(23); // 15 + 8
    assertThat(aliceStats.deletions()).isEqualTo(3); // 3 + 0
    assertThat(aliceStats.totalLinesChanged()).isEqualTo(26);
    assertThat(aliceStats.getCommitPercentage(2)).isEqualTo(100.0);
  }

  @Test
  @DisplayName("Should separate production and test lines")
  void shouldSeparateProductionAndTestLines() {
    List<GitCommit> commits =
        List.of(
            createCommit(
                "Bob",
                "bob@dev.com",
                List.of(
                    new FileChange(
                        "src/main/java/Service.java", 20, 5, FileChange.ChangeType.MODIFIED),
                    new FileChange(
                        "src/test/java/ServiceTest.java", 10, 2, FileChange.ChangeType.MODIFIED),
                    new FileChange("docs/README.md", 5, 0, FileChange.ChangeType.MODIFIED)),
                35,
                7));

    Map<String, ContributorIdentity> identities =
        Map.of(
            "bob@dev.com",
            ContributorIdentity.builder()
                .canonicalName("Bob")
                .primaryEmail("bob@dev.com")
                .allEmails(List.of("bob@dev.com"))
                .allNames(List.of("Bob"))
                .build());

    Map<String, String> languageMapping = Map.of("java", "Java", "md", "Markdown");
    List<String> productionPatterns = List.of("src/main", "docs");
    List<String> testPatterns = List.of("src/test", "test");

    List<ContributorStats> stats =
        aggregator.aggregateStatistics(
            commits, identities, languageMapping, productionPatterns, testPatterns);

    ContributorStats bobStats = stats.get(0);

    // Production lines: Service.java (25 total) + README.md (5 total) = 30
    assertThat(bobStats.productionLines()).containsEntry("Java", 25);
    assertThat(bobStats.productionLines()).containsEntry("Markdown", 5);

    // Test lines: ServiceTest.java (12 total) = 12
    assertThat(bobStats.testLines()).containsEntry("Java", 12);
  }

  @Test
  @DisplayName("Should handle multiple contributors and calculate percentages")
  void shouldHandleMultipleContributors() {
    List<GitCommit> commits =
        List.of(
            createCommit("Alice", "alice@company.com", List.of(), 10, 2),
            createCommit("Alice", "alice@company.com", List.of(), 15, 3),
            createCommit("Bob", "bob@company.com", List.of(), 5, 1),
            createCommit("Charlie", "charlie@company.com", List.of(), 8, 0));

    Map<String, ContributorIdentity> identities =
        Map.of(
            "alice@company.com", createIdentity("Alice", "alice@company.com"),
            "bob@company.com", createIdentity("Bob", "bob@company.com"),
            "charlie@company.com", createIdentity("Charlie", "charlie@company.com"));

    List<ContributorStats> stats =
        aggregator.aggregateStatistics(commits, identities, Map.of(), List.of(), List.of());

    assertThat(stats).hasSize(3);

    // Should be sorted by total contribution (commit count)
    assertThat(stats.get(0).name()).isEqualTo("Alice"); // 2 commits
    assertThat(stats.get(1).name()).isIn("Bob", "Charlie"); // 1 commit each
    assertThat(stats.get(2).name()).isIn("Bob", "Charlie");

    // Check percentages (4 total commits)
    ContributorStats alice =
        stats.stream().filter(s -> s.name().equals("Alice")).findFirst().orElseThrow();
    assertThat(alice.getCommitPercentage(4)).isEqualTo(50.0); // 2/4 = 50%
  }

  @Test
  @DisplayName("Should group file changes by language")
  void shouldGroupFileChangesByLanguage() {
    List<GitCommit> commits =
        List.of(
            createCommit(
                "Dev",
                "dev@example.com",
                List.of(
                    new FileChange("app.js", 10, 2, FileChange.ChangeType.MODIFIED),
                    new FileChange("utils.js", 5, 1, FileChange.ChangeType.MODIFIED),
                    new FileChange("styles.css", 8, 0, FileChange.ChangeType.ADDED),
                    new FileChange("script.py", 15, 3, FileChange.ChangeType.MODIFIED)),
                38,
                6));

    Map<String, ContributorIdentity> identities =
        Map.of("dev@example.com", createIdentity("Dev", "dev@example.com"));

    Map<String, String> languageMapping =
        Map.of(
            "js", "JavaScript",
            "css", "CSS",
            "py", "Python");

    List<ContributorStats> stats =
        aggregator.aggregateStatistics(commits, identities, languageMapping, List.of(), List.of());

    ContributorStats devStats = stats.get(0);

    assertThat(devStats.languageStats()).containsKeys("JavaScript", "CSS", "Python");

    LanguageStats jsStats = devStats.languageStats().get("JavaScript");
    assertThat(jsStats.linesChanged()).isEqualTo(18); // 12 + 6 from both JS files
    assertThat(jsStats.filesChanged()).isEqualTo(2);

    LanguageStats cssStats = devStats.languageStats().get("CSS");
    assertThat(cssStats.linesChanged()).isEqualTo(8);
    assertThat(cssStats.filesChanged()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should identify production vs test code correctly")
  void shouldIdentifyProductionVsTestCode() {
    List<String> productionPatterns = List.of("src/main", "lib", "app");
    List<String> testPatterns = List.of("test", "spec", "__tests__");

    assertThat(
            aggregator.isProductionCode(
                "src/main/java/Service.java", productionPatterns, testPatterns))
        .isTrue();
    assertThat(
            aggregator.isProductionCode(
                "src/test/java/ServiceTest.java", productionPatterns, testPatterns))
        .isFalse();
    assertThat(aggregator.isProductionCode("lib/utils.js", productionPatterns, testPatterns))
        .isTrue();
    assertThat(
            aggregator.isProductionCode(
                "__tests__/component.spec.js", productionPatterns, testPatterns))
        .isFalse();
    assertThat(aggregator.isProductionCode("random/file.txt", productionPatterns, testPatterns))
        .isTrue(); // Default to production
  }

  @Test
  @DisplayName("Should get language from file extension")
  void shouldGetLanguageFromFileExtension() {
    Map<String, String> mapping =
        Map.of(
            "java", "Java",
            "js", "JavaScript",
            "py", "Python");

    assertThat(aggregator.getLanguageForFile("Service.java", mapping)).isEqualTo("Java");
    assertThat(aggregator.getLanguageForFile("app.js", mapping)).isEqualTo("JavaScript");
    assertThat(aggregator.getLanguageForFile("script.unknown", mapping)).isEqualTo("Unknown");
    assertThat(aggregator.getLanguageForFile("README", mapping)).isEqualTo("Unknown");
  }

  @Test
  @DisplayName("Should handle empty commits gracefully")
  void shouldHandleEmptyCommitsGracefully() {
    List<GitCommit> commits = List.of();
    Map<String, ContributorIdentity> identities = Map.of();

    List<ContributorStats> stats =
        aggregator.aggregateStatistics(commits, identities, Map.of(), List.of(), List.of());

    assertThat(stats).isEmpty();
  }

  @Test
  @DisplayName("Should calculate total commits correctly")
  void shouldCalculateTotalCommitsCorrectly() {
    List<GitCommit> commits =
        List.of(
            createCommit("A", "a@test.com", List.of(), 1, 0),
            createCommit("B", "b@test.com", List.of(), 1, 0),
            createCommit("C", "c@test.com", List.of(), 1, 0));

    int total = aggregator.calculateTotalCommits(commits);
    assertThat(total).isEqualTo(3);
  }

  // Helper methods
  private GitCommit createCommit(
      String author, String email, List<FileChange> changes, int insertions, int deletions) {
    return new GitCommit(
        "hash123",
        author,
        email,
        LocalDateTime.now(),
        "Test commit",
        changes,
        insertions,
        deletions);
  }

  private ContributorIdentity createIdentity(String name, String email) {
    return ContributorIdentity.builder()
        .canonicalName(name)
        .primaryEmail(email)
        .allEmails(List.of(email))
        .allNames(List.of(name))
        .build();
  }
}
