package com.codestats.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ContributorStatsTest {

  @Test
  void shouldCalculateTotalLinesChanged() {
    var stats =
        ContributorStats.builder()
            .name("John Doe")
            .primaryEmail("john@example.com")
            .insertions(100)
            .deletions(50)
            .build();

    assertThat(stats.totalLinesChanged()).isEqualTo(150);
  }

  @Test
  void shouldCalculateNetLines() {
    var stats =
        ContributorStats.builder()
            .name("Jane Smith")
            .primaryEmail("jane@example.com")
            .insertions(100)
            .deletions(30)
            .build();

    assertThat(stats.netLines()).isEqualTo(70);
  }

  @Test
  void shouldCalculateCommitPercentage() {
    var stats =
        ContributorStats.builder()
            .name("Alice")
            .primaryEmail("alice@example.com")
            .commitCount(25)
            .build();

    assertThat(stats.getCommitPercentage(100)).isEqualTo(25.0);
    assertThat(stats.getCommitPercentage(0)).isEqualTo(0.0);
  }

  @Test
  void shouldBuildCompleteStats() {
    var languageStats =
        Map.of(
            "Java", new LanguageStats("Java", 200, 150, 50, 5),
            "Python", new LanguageStats("Python", 100, 80, 20, 3));

    var stats =
        ContributorStats.builder()
            .name("Bob Developer")
            .primaryEmail("bob@company.com")
            .allEmails(List.of("bob@company.com", "bob@personal.com"))
            .commitCount(15)
            .filesChanged(8)
            .insertions(230)
            .deletions(70)
            .languageStats(languageStats)
            .productionLines(Map.of("Java", 180, "Python", 90))
            .testLines(Map.of("Java", 20, "Python", 10))
            .build();

    assertThat(stats.name()).isEqualTo("Bob Developer");
    assertThat(stats.allEmails()).containsExactly("bob@company.com", "bob@personal.com");
    assertThat(stats.languageStats()).hasSize(2);
    assertThat(stats.totalLinesChanged()).isEqualTo(300);
    assertThat(stats.netLines()).isEqualTo(160);
  }
}
