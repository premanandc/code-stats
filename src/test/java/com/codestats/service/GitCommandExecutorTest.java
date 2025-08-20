package com.codestats.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitCommandExecutorTest {

  private GitCommandExecutor gitCommandExecutor;

  @TempDir File tempDir;

  @BeforeEach
  void setUp() {
    gitCommandExecutor = new TestableGitCommandExecutor();
  }

  @Test
  void shouldDetectGitRepository() {
    // Given - Create a .git directory
    File gitDir = new File(tempDir, ".git");
    assertThat(gitDir.mkdir()).isTrue();

    // When
    boolean isGitRepo = gitCommandExecutor.isGitRepository(tempDir);

    // Then
    assertThat(isGitRepo).isTrue();
  }

  @Test
  void shouldDetectNonGitRepository() {
    // Given - No .git directory exists

    // When
    boolean isGitRepo = gitCommandExecutor.isGitRepository(tempDir);

    // Then
    assertThat(isGitRepo).isFalse();
  }

  @Test
  void shouldDetectNonGitRepositoryWhenGitFileExists() {
    // Given - Create a .git file instead of directory
    File gitFile = new File(tempDir, ".git");
    try {
      assertThat(gitFile.createNewFile()).isTrue();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // When
    boolean isGitRepo = gitCommandExecutor.isGitRepository(tempDir);

    // Then
    assertThat(isGitRepo).isFalse();
  }

  @Test
  void shouldFormatDateForGit() {
    // Given
    LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30, 45);

    // When
    String formatted = gitCommandExecutor.formatDateForGit(dateTime);

    // Then
    assertThat(formatted).isEqualTo("2024-03-15T14:30:45");
  }

  @Test
  void shouldBuildBasicGitLogCommand() {
    // Given - No filters

    // When
    String command = gitCommandExecutor.buildGitLogCommand(null, null, null, null, null);

    // Then
    assertThat(command).isEqualTo("git log --stat --pretty=fuller");
  }

  @Test
  void shouldBuildGitLogCommandWithSinceDate() {
    // Given
    LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0, 0);

    // When
    String command = gitCommandExecutor.buildGitLogCommand(since, null, null, null, null);

    // Then
    assertThat(command).isEqualTo("git log --stat --pretty=fuller --since=\"2024-01-01T00:00:00\"");
  }

  @Test
  void shouldBuildGitLogCommandWithUntilDate() {
    // Given
    LocalDateTime until = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

    // When
    String command = gitCommandExecutor.buildGitLogCommand(null, until, null, null, null);

    // Then
    assertThat(command).isEqualTo("git log --stat --pretty=fuller --until=\"2024-12-31T23:59:59\"");
  }

  @Test
  void shouldBuildGitLogCommandWithDateRange() {
    // Given
    LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    LocalDateTime until = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

    // When
    String command = gitCommandExecutor.buildGitLogCommand(since, until, null, null, null);

    // Then
    assertThat(command)
        .isEqualTo(
            "git log --stat --pretty=fuller --since=\"2024-01-01T00:00:00\" --until=\"2024-12-31T23:59:59\"");
  }

  @Test
  void shouldBuildGitLogCommandWithSingleIncludeUser() {
    // Given
    Set<String> includeUsers = Set.of("john@example.com");

    // When
    String command = gitCommandExecutor.buildGitLogCommand(null, null, null, includeUsers, null);

    // Then
    assertThat(command).isEqualTo("git log --stat --pretty=fuller --author=\"john@example.com\"");
  }

  @Test
  void shouldBuildGitLogCommandWithMultipleIncludeUsers() {
    // Given
    Set<String> includeUsers = Set.of("john@example.com", "jane@example.com");

    // When
    String command = gitCommandExecutor.buildGitLogCommand(null, null, null, includeUsers, null);

    // Then
    assertThat(command).contains("git log --stat --pretty=fuller");
    assertThat(command).contains("--author=\"john@example.com\"");
    assertThat(command).contains("--author=\"jane@example.com\"");
  }

  @Test
  void shouldBuildGitLogCommandWithEmptyIncludeUsers() {
    // Given
    Set<String> includeUsers = Set.of();

    // When
    String command = gitCommandExecutor.buildGitLogCommand(null, null, null, includeUsers, null);

    // Then
    assertThat(command).isEqualTo("git log --stat --pretty=fuller");
  }

  @Test
  void shouldBuildComplexGitLogCommand() {
    // Given
    LocalDateTime since = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    LocalDateTime until = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
    Set<String> includeUsers = Set.of("john@example.com", "jane@example.com");
    Set<String> excludeUsers = Set.of("bot@example.com");

    // When
    String command =
        gitCommandExecutor.buildGitLogCommand(since, until, null, includeUsers, excludeUsers);

    // Then
    assertThat(command).contains("git log --stat --pretty=fuller");
    assertThat(command).contains("--since=\"2024-01-01T00:00:00\"");
    assertThat(command).contains("--until=\"2024-12-31T23:59:59\"");
    assertThat(command).contains("--author=\"john@example.com\"");
    assertThat(command).contains("--author=\"jane@example.com\"");
    // Note: excludeUsers is handled in post-processing as per the comment
  }

  @Test
  void shouldHandleSpecialCharactersInUserNames() {
    // Given
    Set<String> includeUsers = Set.of("user.name+tag@domain.co.uk");

    // When
    String command = gitCommandExecutor.buildGitLogCommand(null, null, null, includeUsers, null);

    // Then
    assertThat(command).contains("--author=\"user.name+tag@domain.co.uk\"");
  }

  /** Testable implementation of GitCommandExecutor for testing the interface default methods. */
  private static class TestableGitCommandExecutor implements GitCommandExecutor {
    @Override
    public String executeGitLog(
        File repositoryPath,
        LocalDateTime since,
        LocalDateTime until,
        Integer maxCommits,
        Set<String> includeUsers,
        Set<String> excludeUsers) {
      return "mock git output";
    }
  }
}
