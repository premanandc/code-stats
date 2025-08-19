package com.codestats.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.model.FileChange;
import com.codestats.model.GitCommit;

class GitLogParserTest {

  private GitLogParser parser;

  @BeforeEach
  void setUp() {
    // Will implement JGitLogParser later
    parser = new JGitLogParser();
  }

  @Test
  @DisplayName("Should parse single commit with basic information")
  void shouldParseSingleCommitBasicInfo() {
    String gitLogOutput =
        """
            commit abc123def456
            Author: John Doe <john.doe@example.com>
            Date:   2024-01-15 10:30:45 +0000

                Add user authentication feature

             src/main/java/User.java    | 25 +++++++++++++++++++++++++
             src/test/java/UserTest.java |  8 ++++++++
             2 files changed, 33 insertions(+)
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    assertThat(commit.hash()).isEqualTo("abc123def456");
    assertThat(commit.authorName()).isEqualTo("John Doe");
    assertThat(commit.authorEmail()).isEqualTo("john.doe@example.com");
    assertThat(commit.message()).contains("Add user authentication feature");
    assertThat(commit.insertions()).isEqualTo(33);
    assertThat(commit.deletions()).isEqualTo(0);
    assertThat(commit.filesChangedCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should parse file changes with insertions and deletions")
  void shouldParseFileChangesWithStats() {
    String gitLogOutput =
        """
            commit xyz789abc123
            Author: Jane Smith <jane@company.com>
            Date:   2024-01-16 14:20:30 +0000

                Refactor authentication logic

             src/main/java/Auth.java     | 15 ++++++++-------
             src/main/java/User.java     | 42 +++++++++++++++++++++++++---------
             src/test/java/AuthTest.java | 12 ++++++++++
             3 files changed, 53 insertions(+), 7 deletions(-)
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    assertThat(commit.insertions()).isEqualTo(53);
    assertThat(commit.deletions()).isEqualTo(7);
    assertThat(commit.totalLinesChanged()).isEqualTo(60);
    assertThat(commit.netLines()).isEqualTo(46);

    assertThat(commit.fileChanges()).hasSize(3);
    assertThat(commit.fileChanges())
        .extracting(FileChange::path)
        .contains(
            "src/main/java/Auth.java", "src/main/java/User.java", "src/test/java/AuthTest.java");
  }

  @Test
  @DisplayName("Should parse multiple commits")
  void shouldParseMultipleCommits() {
    String gitLogOutput =
        """
            commit commit1hash
            Author: Alice <alice@dev.com>
            Date:   2024-01-17 09:15:00 +0000

                Fix bug in validation

             src/Validator.java | 5 +++--
             1 file changed, 3 insertions(+), 2 deletions(-)

            commit commit2hash
            Author: Bob <bob@dev.com>
            Date:   2024-01-16 16:45:30 +0000

                Add new feature

             src/Feature.java     | 20 ++++++++++++++++++++
             test/FeatureTest.java |  8 ++++++++
             2 files changed, 28 insertions(+)
            """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(2);

    // First commit
    GitCommit commit1 = commits.get(0);
    assertThat(commit1.authorName()).isEqualTo("Alice");
    assertThat(commit1.authorEmail()).isEqualTo("alice@dev.com");
    assertThat(commit1.insertions()).isEqualTo(3);
    assertThat(commit1.deletions()).isEqualTo(2);

    // Second commit
    GitCommit commit2 = commits.get(1);
    assertThat(commit2.authorName()).isEqualTo("Bob");
    assertThat(commit2.authorEmail()).isEqualTo("bob@dev.com");
    assertThat(commit2.insertions()).isEqualTo(28);
    assertThat(commit2.deletions()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should filter commits by date range")
  void shouldFilterCommitsByDateRange() {
    LocalDateTime jan15 = LocalDateTime.of(2024, 1, 15, 10, 0);
    LocalDateTime jan16 = LocalDateTime.of(2024, 1, 16, 10, 0);
    LocalDateTime jan17 = LocalDateTime.of(2024, 1, 17, 10, 0);

    List<GitCommit> commits =
        List.of(
            createCommit("hash1", "alice@dev.com", jan15),
            createCommit("hash2", "bob@dev.com", jan16),
            createCommit("hash3", "charlie@dev.com", jan17));

    // Filter from jan16 onwards
    List<GitCommit> filtered = parser.filterByDateRange(commits, jan16, null);

    assertThat(filtered).hasSize(2);
    assertThat(filtered).extracting(GitCommit::hash).containsExactly("hash2", "hash3");
  }

  @Test
  @DisplayName("Should filter commits by author emails")
  void shouldFilterCommitsByAuthorEmails() {
    List<GitCommit> commits =
        List.of(
            createCommit("hash1", "alice@dev.com", LocalDateTime.now()),
            createCommit("hash2", "bob@dev.com", LocalDateTime.now()),
            createCommit("hash3", "charlie@dev.com", LocalDateTime.now()));

    Set<String> targetEmails = Set.of("alice@dev.com", "charlie@dev.com");
    List<GitCommit> filtered = parser.filterByAuthors(commits, targetEmails);

    assertThat(filtered).hasSize(2);
    assertThat(filtered)
        .extracting(GitCommit::authorEmail)
        .containsExactly("alice@dev.com", "charlie@dev.com");
  }

  @Test
  @DisplayName("Should group file changes by language")
  void shouldGroupFileChangesByLanguage() {
    List<FileChange> fileChanges =
        List.of(
            new FileChange("src/User.java", 10, 2, FileChange.ChangeType.MODIFIED),
            new FileChange("src/Auth.java", 15, 5, FileChange.ChangeType.MODIFIED),
            new FileChange("script.py", 8, 1, FileChange.ChangeType.ADDED),
            new FileChange("README.md", 5, 0, FileChange.ChangeType.MODIFIED));

    GitCommit commit =
        new GitCommit(
            "hash123",
            "dev",
            "dev@example.com",
            LocalDateTime.now(),
            "Test commit",
            fileChanges,
            38,
            8);

    Map<String, String> langMapping =
        Map.of(
            "java", "Java",
            "py", "Python",
            "md", "Markdown");

    Map<String, List<FileChange>> grouped = commit.getChangesByLanguage(langMapping, Map.of());

    assertThat(grouped).containsKeys("Java", "Python", "Markdown");
    assertThat(grouped.get("Java")).hasSize(2);
    assertThat(grouped.get("Python")).hasSize(1);
    assertThat(grouped.get("Markdown")).hasSize(1);
  }

  @Test
  @DisplayName("Should handle empty git log output")
  void shouldHandleEmptyGitLogOutput() {
    String emptyOutput = "";

    List<GitCommit> commits = parser.parseCommits(emptyOutput);

    assertThat(commits).isEmpty();
  }

  @Test
  @DisplayName("Should exclude file renames from changes")
  void shouldExcludeFileRenames() {
    String gitLogOutput =
        """
        commit abc123
        Author: Test User <test@example.com>
        Date: 2024-01-15 10:30:45 +0000

            Rename files and modify code

         src/Main.java                     |  5 +++++
         {old_file.py => new_file.py}      |  0
         app/{config.js => settings.js}    |  0
         test/Test.java                    | 10 +++-------
         3 files changed, 8 insertions(+), 7 deletions(-)
        """;

    List<GitCommit> commits = parser.parseCommits(gitLogOutput);

    assertThat(commits).hasSize(1);
    GitCommit commit = commits.get(0);

    // Should only include actual file changes, not renames
    assertThat(commit.fileChanges()).hasSize(2);

    List<String> filePaths = commit.fileChanges().stream().map(FileChange::path).toList();

    assertThat(filePaths).containsOnly("src/Main.java", "test/Test.java");
    assertThat(filePaths).doesNotContain("{old_file.py => new_file.py}");
    assertThat(filePaths).doesNotContain("app/{config.js => settings.js}");
  }

  @Test
  @DisplayName("Should handle malformed git log output gracefully")
  void shouldHandleMalformedGitLogOutput() {
    String malformedOutput =
        """
            This is not a valid git log format
            Random text here
            """;

    List<GitCommit> commits = parser.parseCommits(malformedOutput);

    assertThat(commits).isEmpty();
  }

  // Helper method to create test commits
  private GitCommit createCommit(String hash, String email, LocalDateTime date) {
    return new GitCommit(hash, "Test Author", email, date, "Test message", List.of(), 10, 5);
  }
}
