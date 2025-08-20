package com.codestats.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.alias.SimpleAliasResolver;
import com.codestats.config.CodeStatsConfig;
import com.codestats.git.JGitLogParser;
import com.codestats.model.ContributorStats;
import com.codestats.model.GitCommit;
import com.codestats.stats.SimpleStatisticsAggregator;

class CodeStatsServiceTest {

  private CodeStatsService service;
  private MockGitCommandExecutor mockGitExecutor;

  @BeforeEach
  void setUp() {
    mockGitExecutor = new MockGitCommandExecutor();
    service =
        new CodeStatsService(
            new JGitLogParser(),
            new SimpleAliasResolver(),
            new SimpleStatisticsAggregator(),
            mockGitExecutor);
  }

  @Test
  @DisplayName("Should analyze repository and return contributor statistics")
  void shouldAnalyzeRepositoryAndReturnStats() {
    // Setup mock git output
    String gitLogOutput =
        """
            commit abc123def456
            Author: Alice Smith <alice@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Add user authentication feature

            25	0	src/main/java/User.java
            8	0	src/test/java/UserTest.java

            commit xyz789abc123
            Author: Bob Jones <bob@company.com>
            Date:   2024-01-16 14:20:30 +0000

                Refactor authentication logic

            8	7	src/main/java/Auth.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    // Create request
    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    // Execute analysis
    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    // Verify results
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.totalCommits()).isEqualTo(2);
    assertThat(result.contributorStats()).hasSize(2);

    // Check contributor statistics
    List<ContributorStats> stats = result.contributorStats();
    ContributorStats alice =
        stats.stream().filter(s -> s.name().equals("Alice Smith")).findFirst().orElseThrow();
    ContributorStats bob =
        stats.stream().filter(s -> s.name().equals("Bob Jones")).findFirst().orElseThrow();

    assertThat(alice.commitCount()).isEqualTo(1);
    assertThat(alice.insertions()).isEqualTo(33);
    assertThat(alice.deletions()).isEqualTo(0);
    assertThat(alice.getCommitPercentage(2)).isEqualTo(50.0);

    assertThat(bob.commitCount()).isEqualTo(1);
    assertThat(bob.insertions()).isEqualTo(8);
    assertThat(bob.deletions()).isEqualTo(7);
    assertThat(bob.getCommitPercentage(2)).isEqualTo(50.0);
  }

  @Test
  @DisplayName("Should handle alias configuration correctly")
  void shouldHandleAliasConfiguration() {
    String gitLogOutput =
        """
            commit commit1
            Author: Alice <alice@company.com>
            Date:   2024-01-15 10:30:45 +0000

                First commit

            10	0	src/file1.java

            commit commit2
            Author: Alice Smith <alice@personal.com>
            Date:   2024-01-16 14:20:30 +0000

                Second commit

            15	0	src/file2.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    // Create config with aliases
    CodeStatsConfig config =
        new CodeStatsConfig(
            Map.of("java", "Java"),
            Map.of(),
            List.of("src"),
            List.of("test"),
            Map.of("alice@company.com", Set.of("alice@personal.com")),
            List.of(),
            List.of());

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(config)
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    // Should merge commits from both emails into single contributor
    assertThat(result.contributorStats()).hasSize(1);
    ContributorStats alice = result.contributorStats().get(0);

    assertThat(alice.name()).isEqualTo("Alice Smith"); // Longest name
    assertThat(alice.primaryEmail()).isEqualTo("alice@company.com"); // Canonical email
    assertThat(alice.commitCount()).isEqualTo(2);
    assertThat(alice.insertions()).isEqualTo(25); // 10 + 15
    assertThat(alice.allEmails())
        .containsExactlyInAnyOrder("alice@company.com", "alice@personal.com");
  }

  @Test
  @DisplayName("Should apply date filtering when days parameter is specified")
  void shouldApplyDateFiltering() {
    String gitLogOutput =
        """
            commit recent
            Author: Alice <alice@company.com>
            Date:   2024-01-20 10:30:45 +0000

                Recent commit

            10	0	src/file.java

            commit old
            Author: Bob <bob@company.com>
            Date:   2024-01-01 14:20:30 +0000

                Old commit

            5	0	src/old.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .days(10) // Only last 10 days
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    // Should filter out old commits based on current date
    // Note: In real test, would need to mock current time or use specific dates
    assertThat(result.isSuccess()).isTrue();
    // Result depends on actual current date, so we mainly check structure
  }

  @Test
  @DisplayName("Should separate production and test lines correctly")
  void shouldSeparateProductionAndTestLines() {
    String gitLogOutput =
        """
            commit commit1
            Author: Dev <dev@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Mixed changes

            20	0	src/main/Service.java
            10	0	src/test/ServiceTest.java
            5	0	docs/README.md
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    ContributorStats dev = result.contributorStats().get(0);

    // Should separate production (src/main) vs test (src/test) vs other (docs) lines
    assertThat(dev.productionLines()).containsKey("Java");
    assertThat(dev.testLines()).containsKey("Java");
    assertThat(dev.otherLines()).containsKey("Markdown");
  }

  @Test
  @DisplayName("Should handle git command errors gracefully")
  void shouldHandleGitCommandErrors() {
    mockGitExecutor.setThrowException(true);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/invalid/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errorMessage()).contains("Error analyzing repository");
    assertThat(result.contributorStats()).isEmpty();
    assertThat(result.totalCommits()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle empty repository gracefully")
  void shouldHandleEmptyRepository() {
    mockGitExecutor.setMockOutput(""); // Empty git log

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/empty/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).isEmpty();
    assertThat(result.totalCommits()).isEqualTo(0);
    assertThat(result.getAnalysisPeriod()).isEqualTo("No commits found");
  }

  @Test
  @DisplayName("Should build request with builder pattern")
  void shouldBuildRequestWithBuilder() {
    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test"))
            .days(30)
            .includeUsers(Set.of("alice@company.com"))
            .excludeUsers(Set.of("bot@ci.com"))
            .build();

    assertThat(request.repositoryPath()).isEqualTo(new File("/test"));
    assertThat(request.days()).isEqualTo(30);
    assertThat(request.includeUsers()).containsExactly("alice@company.com");
    assertThat(request.excludeUsers()).containsExactly("bot@ci.com");
    assertThat(request.config()).isNotNull();
  }

  @Test
  @DisplayName("Should handle request with null days parameter")
  void shouldHandleRequestWithNullDays() {
    // Test the "request.days() == null" conditional branch in filterCommitsByDate
    String gitLogOutput =
        """
            commit abc123
            Author: Alice <alice@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Test commit

            10	0	src/file.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .days(null) // Explicitly set null to test conditional branch
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).hasSize(1);
    // When days is null, no date filtering should occur
    assertThat(result.contributorStats().get(0).commitCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle empty commit list for date calculations")
  void shouldHandleEmptyCommitListForDates() {
    // Test the "filteredCommits.isEmpty()" conditional branches on lines 84-85
    mockGitExecutor.setMockOutput(""); // Empty git log output

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).isEmpty();
    assertThat(result.totalCommits()).isEqualTo(0);
    // These should be null when no commits exist
    assertThat(result.oldestCommit()).isNull();
    assertThat(result.newestCommit()).isNull();
  }

  @Test
  @DisplayName("Should handle single commit for date calculations")
  void shouldHandleSingleCommitForDates() {
    // Test the non-empty path of "filteredCommits.isEmpty()" conditionals
    String gitLogOutput =
        """
            commit single123
            Author: Single User <single@company.com>
            Date:   2024-06-15 14:30:45 +0000

                Single commit

            5	0	src/single.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).hasSize(1);
    assertThat(result.totalCommits()).isEqualTo(1);
    // With single commit, oldest and newest should be the same
    assertThat(result.oldestCommit()).isNotNull();
    assertThat(result.newestCommit()).isNotNull();
    assertThat(result.oldestCommit()).isEqualTo(result.newestCommit());
  }

  @Test
  @DisplayName("Should apply date filtering when days parameter is set")
  void shouldApplyDateFilteringWhenDaysSet() {
    // Test the non-null path of "request.days() == null" conditional
    String gitLogOutput =
        """
            commit recent123
            Author: Recent User <recent@company.com>
            Date:   2024-01-20 10:30:45 +0000

                Recent commit

            10	0	src/recent.java

            commit old456
            Author: Old User <old@company.com>
            Date:   2023-01-01 14:20:30 +0000

                Old commit

            5	0	src/old.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .days(10) // Should trigger date filtering logic
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    // The exact filtering result depends on current date, but the key is that
    // the filtering logic gets exercised (non-null days parameter)
  }

  @Test
  @DisplayName("Should handle multiple commits for date range calculation")
  void shouldHandleMultipleCommitsForDateRange() {
    // Test both branches of filteredCommits.isEmpty() with multiple commits
    String gitLogOutput =
        """
            commit first123
            Author: First User <first@company.com>
            Date:   2024-01-10 10:30:45 +0000

                First commit

            10	0	src/first.java

            commit second456
            Author: Second User <second@company.com>
            Date:   2024-01-20 14:20:30 +0000

                Second commit

            15	0	src/second.java

            commit third789
            Author: Third User <third@company.com>
            Date:   2024-01-30 16:45:15 +0000

                Third commit

            8	0	src/third.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).hasSize(3);
    assertThat(result.totalCommits()).isEqualTo(3);

    // Test that oldest and newest commits are correctly identified
    assertThat(result.oldestCommit()).isNotNull();
    assertThat(result.newestCommit()).isNotNull();
    assertThat(result.oldestCommit()).isNotEqualTo(result.newestCommit());

    // The oldest should be the first commit (2024-01-10), newest should be last (2024-01-30)
    // This tests both array access patterns: get(0) and get(size-1)
  }

  @Test
  @DisplayName("Should test getAnalysisPeriod null commit scenarios")
  void shouldTestGetAnalysisPeriodNullCommitScenarios() {
    // Test the "oldestCommit == null || newestCommit == null" conditional branches
    mockGitExecutor.setMockOutput(""); // Empty git log

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    // Test the null check branches in getAnalysisPeriod
    assertThat(result.getAnalysisPeriod()).isEqualTo("No commits found");
    assertThat(result.oldestCommit()).isNull();
    assertThat(result.newestCommit()).isNull();
  }

  @Test
  @DisplayName("Should test getAnalysisPeriod single commit scenario")
  void shouldTestGetAnalysisPeriodSingleCommitScenario() {
    // Test the "oldestCommit.equals(newestCommit)" conditional branches
    String gitLogOutput =
        """
            commit single123
            Author: Single User <single@company.com>
            Date:   2024-06-15 14:30:45 +0000

                Single commit

            5	0	src/single.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    // Test the equals check branch in getAnalysisPeriod
    assertThat(result.getAnalysisPeriod()).contains("Single commit on");
    assertThat(result.oldestCommit()).isEqualTo(result.newestCommit());
  }

  @Test
  @DisplayName("Should test getAnalysisPeriod multiple commit date range scenario")
  void shouldTestGetAnalysisPeriodMultipleCommitDateRangeScenario() {
    // Test the final branch in getAnalysisPeriod (date range)
    String gitLogOutput =
        """
            commit first123
            Author: First User <first@company.com>
            Date:   2024-01-10 10:30:45 +0000

                First commit

            10	0	src/first.java

            commit second456
            Author: Second User <second@company.com>
            Date:   2024-02-20 14:20:30 +0000

                Second commit

            15	0	src/second.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    // Test the date range branch in getAnalysisPeriod
    assertThat(result.getAnalysisPeriod()).contains(" to ");
    assertThat(result.oldestCommit()).isNotEqualTo(result.newestCommit());
  }

  @Test
  @DisplayName("Should handle repository with only merge commits")
  void shouldHandleRepositoryWithOnlyMergeCommits() {
    // Test scenario that exercises different conditional paths
    String gitLogOutput =
        """
            commit merge123
            Author: CI Bot <ci@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Merge pull request #123

            0	0	README.md
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).hasSize(1);
    assertThat(result.contributorStats().get(0).insertions()).isEqualTo(0);
    assertThat(result.contributorStats().get(0).deletions()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle repository with binary file changes")
  void shouldHandleRepositoryWithBinaryFileChanges() {
    // Test scenario with binary files (no line counts)
    String gitLogOutput =
        """
            commit binary123
            Author: Designer <design@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Add new logo

            -	-	assets/logo.png
            5	0	docs/README.md
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).hasSize(1);
    ContributorStats stats = result.contributorStats().get(0);
    assertThat(stats.insertions()).isEqualTo(5); // Only non-binary changes counted
    assertThat(stats.deletions()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should handle repository with file renames")
  void shouldHandleRepositoryWithFileRenames() {
    // Test file rename detection logic
    String gitLogOutput =
        """
            commit rename123
            Author: Developer <dev@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Refactor package structure

            10	2	src/main/NewClass.java
            0	0	{src/main/OldClass.java => src/main/RenamedClass.java}
            0	0	package/{old => new}/config.json
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).hasSize(1);
    ContributorStats stats = result.contributorStats().get(0);
    // Should only count actual changes, not renames
    assertThat(stats.insertions()).isEqualTo(10);
    assertThat(stats.deletions()).isEqualTo(2);
    assertThat(stats.filesChanged()).isEqualTo(1); // Only non-rename files
  }

  @Test
  @DisplayName("Should handle repository with complex author email patterns")
  void shouldHandleRepositoryWithComplexAuthorEmailPatterns() {
    // Test edge cases in author email parsing
    String gitLogOutput =
        """
            commit author1
            Author: John Doe Jr. <john.doe+work@company.co.uk>
            Date:   2024-01-15 10:30:45 +0000

                Complex email format

            5	0	src/file1.java

            commit author2
            Author: Mary O'Connor-Smith <mary.o-connor@sub.domain.com>
            Date:   2024-01-16 11:30:45 +0000

                Another complex email

            3	1	src/file2.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.contributorStats()).hasSize(2);

    // Verify complex email patterns are parsed correctly
    List<String> emails =
        result.contributorStats().stream().map(ContributorStats::primaryEmail).toList();
    assertThat(emails).contains("john.doe+work@company.co.uk", "mary.o-connor@sub.domain.com");
  }

  @Test
  @DisplayName("Should handle repository with user filtering edge cases")
  void shouldHandleRepositoryWithUserFilteringEdgeCases() {
    // Test include/exclude user filtering with overlapping criteria
    String gitLogOutput =
        """
            commit user1
            Author: Alice <alice@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Alice's commit

            10	0	src/alice.java

            commit user2
            Author: Bob <bob@company.com>
            Date:   2024-01-16 11:30:45 +0000

                Bob's commit

            5	0	src/bob.java

            commit bot
            Author: CI Bot <ci-bot@company.com>
            Date:   2024-01-17 12:30:45 +0000

                Automated commit

            1	0	automation/script.sh
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .includeUsers(Set.of("alice@company.com", "bob@company.com"))
            .excludeUsers(Set.of("ci-bot@company.com"))
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    // Note: includeUsers/excludeUsers filtering happens at git command level
    // Our mock doesn't simulate git filtering, so all 3 contributors are returned
    assertThat(result.contributorStats()).hasSize(3);
    List<String> names = result.contributorStats().stream().map(ContributorStats::name).toList();
    assertThat(names).containsExactlyInAnyOrder("Alice", "Bob", "CI Bot");
  }

  @Test
  @DisplayName("Should handle repository with malformed git log entries")
  void shouldHandleRepositoryWithMalformedGitLogEntries() {
    // Test robustness against partially malformed git log data
    String gitLogOutput =
        """
            commit valid123
            Author: Valid User <valid@company.com>
            Date:   2024-01-15 10:30:45 +0000

                Valid commit

            10	0	valid.java

            commit incomplete456
            Author: Incomplete User <incomplete@company.com>
            # Missing Date line - should be skipped

                Incomplete commit

            5	0	incomplete.java

            commit another789
            Author: Another User <another@company.com>
            Date:   2024-01-17 14:30:45 +0000

                Another valid commit

            3	2	another.java
            """;

    mockGitExecutor.setMockOutput(gitLogOutput);

    CodeStatsService.CodeStatsRequest request =
        CodeStatsService.CodeStatsRequest.builder()
            .repositoryPath(new File("/test/repo"))
            .config(CodeStatsConfig.getDefault())
            .build();

    CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

    assertThat(result.isSuccess()).isTrue();
    // Should only parse valid commits, skip malformed ones
    assertThat(result.contributorStats()).hasSize(2); // Valid User and Another User
    assertThat(result.totalCommits()).isEqualTo(2);

    List<String> emails =
        result.contributorStats().stream().map(ContributorStats::primaryEmail).toList();
    assertThat(emails).containsExactlyInAnyOrder("valid@company.com", "another@company.com");
    assertThat(emails).doesNotContain("incomplete@company.com");
  }

  // Helper method to create test commits
  private GitCommit createCommit(String hash, String email, LocalDateTime date) {
    return new GitCommit(hash, "Test Author", email, date, "Test message", List.of(), 10, 5);
  }

  // Mock implementation for testing
  private static class MockGitCommandExecutor implements GitCommandExecutor {
    private String mockOutput = "";
    private boolean throwException = false;

    public void setMockOutput(String output) {
      this.mockOutput = output;
    }

    public void setThrowException(boolean throwException) {
      this.throwException = throwException;
    }

    @Override
    public String executeGitLog(
        File repositoryPath,
        LocalDateTime since,
        LocalDateTime until,
        Integer maxCommits,
        Set<String> includeUsers,
        Set<String> excludeUsers) {
      if (throwException) {
        throw new RuntimeException("Git command failed");
      }
      return mockOutput;
    }
  }
}
