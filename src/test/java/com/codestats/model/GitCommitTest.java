package com.codestats.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GitCommitTest {

  @Test
  void testLanguageDetectionWithExtensions() {
    var commit = createCommit();
    var extensionToLanguage = Map.of("java", "Java", "py", "Python");

    var changesByLanguage = commit.getChangesByLanguage(extensionToLanguage, Map.of());

    assertThat(changesByLanguage).containsKey("Java");
    assertThat(changesByLanguage.get("Java")).hasSize(1);
    assertThat(changesByLanguage.get("Java").get(0).path()).isEqualTo("src/Main.java");
  }

  @Test
  void testLanguageDetectionWithoutExtensions() {
    var fileChanges =
        List.of(
            new FileChange("Dockerfile", 10, 5, FileChange.ChangeType.MODIFIED),
            new FileChange("Makefile", 3, 1, FileChange.ChangeType.MODIFIED),
            new FileChange("Jenkinsfile", 2, 0, FileChange.ChangeType.ADDED),
            new FileChange("Gemfile", 1, 0, FileChange.ChangeType.MODIFIED),
            new FileChange("unknown-file", 1, 1, FileChange.ChangeType.MODIFIED));

    var commit =
        new GitCommit(
            "abc123",
            "Test Author",
            "test@example.com",
            LocalDateTime.now(),
            "Test commit",
            fileChanges,
            17,
            7);

    var filenameToLanguage =
        Map.of(
            "dockerfile",
            "Docker",
            "makefile",
            "Makefile",
            "jenkinsfile",
            "Jenkins",
            "gemfile",
            "Ruby");
    var changesByLanguage = commit.getChangesByLanguage(Map.of(), filenameToLanguage);

    assertThat(changesByLanguage).containsKeys("Docker", "Makefile", "Jenkins", "Ruby", "Unknown");
    assertThat(changesByLanguage.get("Docker").get(0).path()).isEqualTo("Dockerfile");
    assertThat(changesByLanguage.get("Makefile").get(0).path()).isEqualTo("Makefile");
    assertThat(changesByLanguage.get("Jenkins").get(0).path()).isEqualTo("Jenkinsfile");
    assertThat(changesByLanguage.get("Ruby").get(0).path()).isEqualTo("Gemfile");
    assertThat(changesByLanguage.get("Unknown").get(0).path()).isEqualTo("unknown-file");
  }

  @Test
  void testTotalLinesChanged() {
    var commit = createCommit();
    assertThat(commit.totalLinesChanged()).isEqualTo(15); // 10 + 5
  }

  @Test
  void testNetLines() {
    var commit = createCommit();
    assertThat(commit.netLines()).isEqualTo(5); // 10 - 5
  }

  @Test
  void testFilesChangedCount() {
    var commit = createCommit();
    assertThat(commit.filesChangedCount()).isEqualTo(1);
  }

  private GitCommit createCommit() {
    var fileChanges =
        List.of(new FileChange("src/Main.java", 10, 5, FileChange.ChangeType.MODIFIED));
    return new GitCommit(
        "abc123",
        "Test Author",
        "test@example.com",
        LocalDateTime.now(),
        "Test commit",
        fileChanges,
        10,
        5);
  }
}
