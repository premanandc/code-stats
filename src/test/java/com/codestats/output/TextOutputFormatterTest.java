package com.codestats.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.codestats.model.ContributorStats;
import com.codestats.model.LanguageStats;
import com.codestats.service.CodeStatsService;

class TextOutputFormatterTest {

  private TextOutputFormatter formatterWithColors;
  private TextOutputFormatter formatterWithoutColors;

  @BeforeEach
  void setUp() {
    formatterWithColors = new TextOutputFormatter(true);
    formatterWithoutColors = new TextOutputFormatter(false);
  }

  @Test
  void shouldFormatSuccessfulResult() {
    // Given
    var contributor = createTestContributor();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithoutColors.format(result);

    // Then
    assertThat(output).isNotEmpty();
    assertThat(output).contains("📊 Code Statistics Report");
    assertThat(output).contains("Repository:");
    assertThat(output).contains("Period:");
    assertThat(output).contains("Contributors:");
    assertThat(output).contains("John Doe");
    assertThat(output).contains("john@example.com");
    assertThat(output).contains("5 (100.0%)");
    assertThat(output).contains("Files changed: 3");
    assertThat(output).contains("+100 -20");
  }

  @Test
  void shouldFormatErrorResult() {
    // Given
    var result = createErrorResult("Repository not found");

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).isNotEmpty();
    assertThat(output).contains("❌ Error: Repository not found");
  }

  @Test
  void shouldFormatEmptyResult() {
    // Given
    var result = createSuccessResult(List.of());

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).isNotEmpty();
    assertThat(output).contains("📊 No commits found in the specified repository or time range.");
  }

  @Test
  void shouldFormatMultipleContributors() {
    // Given
    var contributor1 = createTestContributor("Alice", "alice@example.com", 10, 200, 30, 5);
    var contributor2 = createTestContributor("Bob", "bob@example.com", 8, 150, 25, 4);
    var result = createSuccessResult(List.of(contributor1, contributor2));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("Alice");
    assertThat(output).contains("alice@example.com");
    assertThat(output).contains("Bob");
    assertThat(output).contains("bob@example.com");
  }

  @Test
  void shouldShowLanguageStats() {
    // Given
    var contributor = createContributorWithLanguages();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("Languages:");
    assertThat(output).contains("Java (80)");
    assertThat(output).contains("Python (40)");
  }

  @Test
  void shouldLimitLanguagesToTopThree() {
    // Given
    var contributor = createContributorWithManyLanguages();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    // Should show top 3 languages by lines changed
    assertThat(output).contains("Java (100)");
    assertThat(output).contains("Python (80)");
    assertThat(output).contains("JavaScript (60)");
    // Should not show the 4th language
    assertThat(output).doesNotContain("Go (40)");
  }

  @Test
  void shouldShowProductionAndTestCodeDistribution() {
    // Given
    var contributor = createContributorWithCodeDistribution();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("Code distribution:");
    assertThat(output).contains("Production: 60");
    assertThat(output).contains("Test: 20");
  }

  @Test
  void shouldShowOnlyProductionWhenNoTest() {
    // Given
    var contributor = createContributorWithOnlyProduction();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("Code distribution:");
    assertThat(output).contains("Production: 50");
    assertThat(output).doesNotContain("Test:");
  }

  @Test
  void shouldShowOnlyTestWhenNoProduction() {
    // Given
    var contributor = createContributorWithOnlyTest();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("Code distribution:");
    assertThat(output).contains("Test: 30");
    assertThat(output).doesNotContain("Production:");
  }

  @Test
  void shouldShowMultipleEmailAliases() {
    // Given
    var contributor = createContributorWithAliases();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("john@example.com");
    assertThat(output).contains("Also commits as:");
    assertThat(output).contains("john.doe@company.com");
    assertThat(output).contains("jdoe@old-company.com");
  }

  @Test
  void shouldCalculateCorrectPercentages() {
    // Given
    var contributor1 = createTestContributor("Alice", "alice@example.com", 7, 100, 10, 3);
    var contributor2 = createTestContributor("Bob", "bob@example.com", 3, 50, 5, 2);
    var result = createSuccessResult(List.of(contributor1, contributor2)); // Total: 10 commits

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("Alice");
    assertThat(output).contains("Bob");
    assertThat(output).contains("70.0%");
    assertThat(output).contains("30.0%");
  }

  @Test
  void shouldFormatNetChangeCorrectly() {
    // Given - contributor with net positive changes
    var contributor1 = createTestContributor("Adder", "add@example.com", 1, 100, 20, 1);
    // Given - contributor with net negative changes
    var contributor2 = createTestContributor("Deleter", "del@example.com", 1, 10, 50, 1);
    // Given - contributor with neutral changes
    var contributor3 = createTestContributor("Neutral", "neu@example.com", 1, 30, 30, 1);

    var result1 = createSuccessResult(List.of(contributor1));
    var result2 = createSuccessResult(List.of(contributor2));
    var result3 = createSuccessResult(List.of(contributor3));

    // When
    String output1 = formatterWithColors.format(result1);
    String output2 = formatterWithColors.format(result2);
    String output3 = formatterWithColors.format(result3);

    // Then
    assertThat(output1).contains("+80");
    assertThat(output2).contains("-40");
    assertThat(output3).contains("30");
  }

  @Test
  void shouldWorkWithoutColors() {
    // Given
    var contributor = createTestContributor();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithoutColors.format(result);

    // Then
    assertThat(output).isNotEmpty();
    assertThat(output).contains("📊 Code Statistics Report");
    assertThat(output).contains("John Doe");
    assertThat(output).contains("john@example.com");
    // Should not contain ANSI color codes
    assertThat(output).doesNotContain("\u001B[");
  }

  @Test
  void shouldHandleSpecialCharactersInNames() {
    // Given
    var contributor = createTestContributor("José O'Malley-Smith", "josé@example.com", 1, 10, 0, 1);
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("José O'Malley-Smith");
    assertThat(output).contains("josé@example.com");
  }

  @Test
  void shouldIncludeRepositoryPath() {
    // Given
    var contributor = createTestContributor();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("/test/repo");
  }

  @Test
  void shouldIncludeDatePeriod() {
    // Given
    var contributor = createTestContributor();
    var result = createSuccessResult(List.of(contributor));

    // When
    String output = formatterWithColors.format(result);

    // Then
    assertThat(output).contains("Period:");
    // The exact format depends on the getAnalysisPeriod() method implementation
  }

  private ContributorStats createTestContributor() {
    return createTestContributor("John Doe", "john@example.com", 5, 100, 20, 3);
  }

  private ContributorStats createTestContributor(
      String name, String email, int commits, int insertions, int deletions, int files) {
    return new ContributorStats(
        name,
        email,
        List.of(email),
        commits,
        files,
        insertions,
        deletions,
        Map.of(),
        Map.of(),
        Map.of());
  }

  private ContributorStats createContributorWithLanguages() {
    var languageStats =
        Map.of(
            "Java", new LanguageStats("Java", 80, 50, 30, 4),
            "Python", new LanguageStats("Python", 40, 25, 15, 4));

    return new ContributorStats(
        "Developer",
        "dev@example.com",
        List.of("dev@example.com"),
        5,
        8,
        75,
        45,
        languageStats,
        Map.of(),
        Map.of());
  }

  private ContributorStats createContributorWithManyLanguages() {
    var languageStats =
        Map.of(
            "Java", new LanguageStats("Java", 100, 60, 40, 4),
            "Python", new LanguageStats("Python", 80, 50, 30, 4),
            "JavaScript", new LanguageStats("JavaScript", 60, 40, 20, 4),
            "Go", new LanguageStats("Go", 40, 25, 15, 3));

    return new ContributorStats(
        "Polyglot",
        "poly@example.com",
        List.of("poly@example.com"),
        10,
        15,
        175,
        105,
        languageStats,
        Map.of(),
        Map.of());
  }

  private ContributorStats createContributorWithCodeDistribution() {
    return new ContributorStats(
        "Developer",
        "dev@example.com",
        List.of("dev@example.com"),
        3,
        5,
        80,
        0,
        Map.of(),
        Map.of("Java", 60),
        Map.of("Java", 20));
  }

  private ContributorStats createContributorWithOnlyProduction() {
    return new ContributorStats(
        "Prod Developer",
        "prod@example.com",
        List.of("prod@example.com"),
        2,
        3,
        50,
        0,
        Map.of(),
        Map.of("Java", 50),
        Map.of());
  }

  private ContributorStats createContributorWithOnlyTest() {
    return new ContributorStats(
        "Test Developer",
        "test@example.com",
        List.of("test@example.com"),
        2,
        2,
        30,
        0,
        Map.of(),
        Map.of(),
        Map.of("Java", 30));
  }

  private ContributorStats createContributorWithAliases() {
    return new ContributorStats(
        "John Doe",
        "john@example.com",
        List.of("john@example.com", "john.doe@company.com", "jdoe@old-company.com"),
        5,
        3,
        100,
        20,
        Map.of(),
        Map.of(),
        Map.of());
  }

  private CodeStatsService.CodeStatsResult createSuccessResult(
      List<ContributorStats> contributors) {
    return new CodeStatsService.CodeStatsResult(
        contributors,
        contributors.stream().mapToInt(ContributorStats::commitCount).sum(),
        new File("/test/repo"),
        LocalDateTime.of(2024, 1, 1, 0, 0),
        LocalDateTime.of(2024, 12, 31, 23, 59),
        true,
        null);
  }

  private CodeStatsService.CodeStatsResult createErrorResult(String errorMessage) {
    return new CodeStatsService.CodeStatsResult(
        List.of(), 0, new File("/test/repo"), null, null, false, errorMessage);
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1, 0, 1, '100.0%', 'Single contribution'",
    "0, 0, 0, 0, '0.0%', 'Zero contribution'",
    "1, 5, 5, 1, '100.0%', 'Zero net changes'",
    "10, 100, 0, 5, '100.0%', 'Addition only'",
    "5, 0, 50, 3, '100.0%', 'Deletion only'",
    "999, 999, 999, 999, '100.0%', 'Maximum values'"
  })
  @DisplayName("Should handle boundary conditions for single contributor")
  void shouldHandleBoundaryConditionsSingleContributor(
      int commits,
      int insertions,
      int deletions,
      int files,
      String expectedPercentage,
      String description) {

    var contributor =
        createTestContributor(
            "Test Dev", "test@example.com", commits, insertions, deletions, files);
    var result = createSuccessResult(List.of(contributor));

    String output = formatterWithoutColors.format(result);

    assertThat(output).contains("Test Dev");
    assertThat(output).contains(expectedPercentage);
    assertThat(output).contains("Files changed: " + files);
    assertThat(output).contains("+" + insertions + " -" + deletions);

    // Test net calculation boundary
    int netChange = insertions - deletions;
    if (netChange > 0) {
      assertThat(output).contains("net: +" + netChange);
    } else if (netChange < 0) {
      assertThat(output).contains("net: " + netChange);
    } else {
      assertThat(output).contains("net: 0");
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 5, 10, 50, 100})
  @DisplayName("Should handle boundary conditions for contributor count")
  void shouldHandleBoundaryContributorCount(int contributorCount) {
    if (contributorCount == 0) {
      // Test empty result
      var emptyResult = createSuccessResult(List.of());
      String output = formatterWithoutColors.format(emptyResult);
      assertThat(output).contains("📊 No commits found");
      return;
    }

    // Create specified number of contributors
    var contributors = new java.util.ArrayList<ContributorStats>();
    for (int i = 1; i <= contributorCount; i++) {
      contributors.add(
          createTestContributor(
              "Dev" + i,
              "dev" + i + "@example.com",
              contributorCount - i + 1, // Decreasing commits for percentage variety
              (contributorCount - i + 1) * 10,
              i,
              i));
    }

    var result = createSuccessResult(contributors);
    String output = formatterWithoutColors.format(result);

    // Test boundary: all contributors should be listed
    for (int i = 1; i <= contributorCount; i++) {
      assertThat(output).contains("Dev" + i);
    }

    // Test that percentage calculations work for any count
    if (contributorCount == 1) {
      assertThat(output).contains("100.0%");
    } else {
      // Multiple contributors should have various percentages
      assertThat(output).contains("%");
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0, 0, 'No changes'",
    "1, 0, 'Single addition'",
    "0, 1, 'Single deletion'",
    "50, 50, 'Equal changes'",
    "1000, 1, 'Large addition'",
    "1, 1000, 'Large deletion'",
    "9999, 9999, 'Maximum changes'"
  })
  @DisplayName("Should handle boundary conditions for change calculations")
  void shouldHandleBoundaryChangeCalculations(int insertions, int deletions, String description) {
    var contributor =
        createTestContributor("Test Dev", "test@example.com", 1, insertions, deletions, 1);
    var result = createSuccessResult(List.of(contributor));

    String output = formatterWithoutColors.format(result);

    // Test that large numbers are formatted correctly
    assertThat(output).contains("+" + insertions + " -" + deletions);

    // Test net calculation boundary conditions
    int net = insertions - deletions;
    if (net == 0) {
      assertThat(output).contains("net: 0");
    } else if (net > 0) {
      assertThat(output).contains("net: +" + net);
    } else {
      assertThat(output).contains("net: " + net);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 10, 100})
  @DisplayName("Should handle boundary conditions for email alias count")
  void shouldHandleBoundaryEmailAliasCount(int aliasCount) {
    if (aliasCount == 0) {
      return; // Skip 0 case as it's not valid for this test
    }

    // Create contributor with specified number of email aliases
    var emails = new java.util.ArrayList<String>();
    emails.add("primary@example.com");
    for (int i = 1; i < aliasCount; i++) {
      emails.add("alias" + i + "@example.com");
    }

    var contributor =
        new ContributorStats(
            "Multi Email Dev",
            "primary@example.com",
            emails,
            5,
            3,
            50,
            10,
            Map.of(),
            Map.of(),
            Map.of());

    var result = createSuccessResult(List.of(contributor));
    String output = formatterWithoutColors.format(result);

    assertThat(output).contains("Multi Email Dev");
    assertThat(output).contains("primary@example.com");

    if (aliasCount > 1) {
      assertThat(output).contains("Also commits as:");
      // Should show additional aliases
      for (int i = 1; i < aliasCount; i++) {
        assertThat(output).contains("alias" + i + "@example.com");
      }
    }
  }
}
