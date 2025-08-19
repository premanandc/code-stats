package com.codestats.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codestats.model.ContributorStats;
import com.codestats.model.LanguageStats;
import com.codestats.service.CodeStatsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonOutputFormatterTest {

  private JsonOutputFormatter formatter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    formatter = new JsonOutputFormatter();
    objectMapper = new ObjectMapper();
  }

  @Test
  void shouldFormatSuccessfulResult() throws Exception {
    // Given
    var contributor = createTestContributor();
    var result = createSuccessResult(List.of(contributor));

    // When
    String json = formatter.format(result);

    // Then
    assertThat(json).isNotEmpty();

    // Verify it's valid JSON
    JsonNode jsonNode = objectMapper.readTree(json);
    assertThat(jsonNode.get("success").asBoolean()).isTrue();
    assertThat(jsonNode.get("totalCommits").asInt()).isEqualTo(5);
    assertThat(jsonNode.get("contributorStats")).hasSize(1);

    // Verify contributor data
    JsonNode contributorNode = jsonNode.get("contributorStats").get(0);
    assertThat(contributorNode.get("name").asText()).isEqualTo("John Doe");
    assertThat(contributorNode.get("primaryEmail").asText()).isEqualTo("john@example.com");
    assertThat(contributorNode.get("commitCount").asInt()).isEqualTo(5);
    assertThat(contributorNode.get("insertions").asInt()).isEqualTo(100);
    assertThat(contributorNode.get("deletions").asInt()).isEqualTo(20);
    assertThat(contributorNode.get("filesChanged").asInt()).isEqualTo(3);
  }

  @Test
  void shouldFormatErrorResult() throws Exception {
    // Given
    var result = createErrorResult("Repository not found");

    // When
    String json = formatter.format(result);

    // Then
    assertThat(json).isNotEmpty();

    // Verify it's valid JSON
    JsonNode jsonNode = objectMapper.readTree(json);
    assertThat(jsonNode.get("success").asBoolean()).isFalse();
    assertThat(jsonNode.get("errorMessage").asText()).isEqualTo("Repository not found");
    assertThat(jsonNode.get("contributorStats")).hasSize(0);
    assertThat(jsonNode.get("totalCommits").asInt()).isEqualTo(0);
  }

  @Test
  void shouldFormatEmptyResult() throws Exception {
    // Given
    var result = createSuccessResult(List.of());

    // When
    String json = formatter.format(result);

    // Then
    assertThat(json).isNotEmpty();

    // Verify it's valid JSON
    JsonNode jsonNode = objectMapper.readTree(json);
    assertThat(jsonNode.get("success").asBoolean()).isTrue();
    assertThat(jsonNode.get("contributorStats")).hasSize(0);
    assertThat(jsonNode.get("totalCommits").asInt()).isEqualTo(0);
  }

  @Test
  void shouldFormatMultipleContributors() throws Exception {
    // Given
    var contributor1 = createTestContributor("Alice", "alice@example.com", 10, 200, 30, 5);
    var contributor2 = createTestContributor("Bob", "bob@example.com", 8, 150, 25, 4);
    var result = createSuccessResult(List.of(contributor1, contributor2));

    // When
    String json = formatter.format(result);

    // Then
    assertThat(json).isNotEmpty();

    // Verify it's valid JSON
    JsonNode jsonNode = objectMapper.readTree(json);
    assertThat(jsonNode.get("success").asBoolean()).isTrue();
    assertThat(jsonNode.get("contributorStats")).hasSize(2);

    // Verify first contributor
    JsonNode first = jsonNode.get("contributorStats").get(0);
    assertThat(first.get("name").asText()).isEqualTo("Alice");
    assertThat(first.get("commitCount").asInt()).isEqualTo(10);

    // Verify second contributor
    JsonNode second = jsonNode.get("contributorStats").get(1);
    assertThat(second.get("name").asText()).isEqualTo("Bob");
    assertThat(second.get("commitCount").asInt()).isEqualTo(8);
  }

  @Test
  void shouldIncludeLanguageStats() throws Exception {
    // Given
    var contributor = createContributorWithLanguages();
    var result = createSuccessResult(List.of(contributor));

    // When
    String json = formatter.format(result);

    // Then
    JsonNode jsonNode = objectMapper.readTree(json);
    JsonNode contributorNode = jsonNode.get("contributorStats").get(0);
    JsonNode languageStats = contributorNode.get("languageStats");

    assertThat(languageStats.has("Java")).isTrue();
    assertThat(languageStats.has("Python")).isTrue();
    assertThat(languageStats.get("Java").get("linesChanged").asInt()).isEqualTo(80);
    assertThat(languageStats.get("Python").get("linesChanged").asInt()).isEqualTo(40);
  }

  @Test
  void shouldIncludeProductionAndTestStats() throws Exception {
    // Given
    var contributor = createContributorWithCodeDistribution();
    var result = createSuccessResult(List.of(contributor));

    // When
    String json = formatter.format(result);

    // Then
    JsonNode jsonNode = objectMapper.readTree(json);
    JsonNode contributorNode = jsonNode.get("contributorStats").get(0);

    JsonNode prodLines = contributorNode.get("productionLines");
    JsonNode testLines = contributorNode.get("testLines");

    assertThat(prodLines.get("Java").asInt()).isEqualTo(60);
    assertThat(testLines.get("Java").asInt()).isEqualTo(20);
  }

  @Test
  void shouldIncludeRepositoryPathAndDates() throws Exception {
    // Given
    var contributor = createTestContributor();
    var result = createSuccessResult(List.of(contributor));

    // When
    String json = formatter.format(result);

    // Then
    JsonNode jsonNode = objectMapper.readTree(json);
    assertThat(jsonNode.has("repositoryPath")).isTrue();
    assertThat(jsonNode.has("oldestCommit")).isTrue();
    assertThat(jsonNode.has("newestCommit")).isTrue();
  }

  @Test
  void shouldHandleSpecialCharactersInNames() throws Exception {
    // Given
    var contributor = createTestContributor("José O'Malley-Smith", "josé@example.com", 1, 10, 0, 1);
    var result = createSuccessResult(List.of(contributor));

    // When
    String json = formatter.format(result);

    // Then
    JsonNode jsonNode = objectMapper.readTree(json);
    JsonNode contributorNode = jsonNode.get("contributorStats").get(0);
    assertThat(contributorNode.get("name").asText()).isEqualTo("José O'Malley-Smith");
    assertThat(contributorNode.get("primaryEmail").asText()).isEqualTo("josé@example.com");
  }

  @Test
  void shouldHandleMultipleEmailAliases() throws Exception {
    // Given
    var contributor =
        new ContributorStats(
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

    var result = createSuccessResult(List.of(contributor));

    // When
    String json = formatter.format(result);

    // Then
    JsonNode jsonNode = objectMapper.readTree(json);
    JsonNode contributorNode = jsonNode.get("contributorStats").get(0);
    JsonNode allEmails = contributorNode.get("allEmails");

    assertThat(allEmails).hasSize(3);
    assertThat(allEmails.toString()).contains("john@example.com");
    assertThat(allEmails.toString()).contains("john.doe@company.com");
    assertThat(allEmails.toString()).contains("jdoe@old-company.com");
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

  private CodeStatsService.CodeStatsResult createSuccessResult(
      List<ContributorStats> contributors) {
    return new CodeStatsService.CodeStatsResult(
        contributors,
        contributors.size() > 0
            ? contributors.stream().mapToInt(ContributorStats::commitCount).sum()
            : 0,
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
}
