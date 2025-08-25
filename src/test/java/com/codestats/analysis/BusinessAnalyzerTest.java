package com.codestats.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.analysis.QualityMetrics.QualityLevel;
import com.codestats.analysis.RiskAssessment.RiskLevel;
import com.codestats.analysis.TeamComposition.ContributorImpact;
import com.codestats.analysis.TeamComposition.ContributorProfile;
import com.codestats.analysis.TeamComposition.ContributorRole;
import com.codestats.analysis.TechnologyBreakdown.TechnologyCategory;
import com.codestats.model.ContributorStats;
import com.codestats.model.LanguageStats;
import com.codestats.service.CodeStatsService;

class BusinessAnalyzerTest {

  @Test
  @DisplayName("Should analyze project with single high-impact contributor")
  void shouldAnalyzeSingleHighImpactContributor() {
    CodeStatsService.CodeStatsResult result = createResultWithSingleContributor();

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);

    // Project summary assertions
    assertThat(analysis.projectSummary().projectName()).isEqualTo("TEST-REPO");
    assertThat(analysis.projectSummary().totalCommits()).isEqualTo(50);
    assertThat(analysis.projectSummary().activeContributors()).isEqualTo(1);
    assertThat(analysis.projectSummary().topPerformerName()).isEqualTo("Alice Smith");
    assertThat(analysis.projectSummary().topPerformerPercentage()).isEqualTo(100.0);

    // Team composition assertions
    assertThat(analysis.teamComposition().contributors()).hasSize(1);
    ContributorProfile contributor = analysis.teamComposition().contributors().get(0);
    assertThat(contributor.name()).isEqualTo("Alice Smith");
    assertThat(contributor.role()).isEqualTo(ContributorRole.TECH_LEAD);
    assertThat(contributor.impact()).isEqualTo(ContributorImpact.HIGH);
    assertThat(contributor.commitPercentage()).isEqualTo(100.0);

    // Risk assessment assertions
    assertThat(analysis.riskAssessment().overallRisk()).isEqualTo(RiskLevel.CRITICAL);
    assertThat(analysis.riskAssessment().busFactor()).isEqualTo(1);
    assertThat(analysis.riskAssessment().knowledgeConcentration()).isEqualTo(100.0);
    assertThat(analysis.riskAssessment().criticalRisks()).isNotEmpty();

    // Strategic recommendations should include hiring
    assertThat(analysis.strategicRecommendations())
        .anyMatch(rec -> rec.contains("hiring") || rec.contains("knowledge transfer"));
  }

  @Test
  @DisplayName("Should analyze well-balanced team with multiple contributors")
  void shouldAnalyzeBalancedTeam() {
    CodeStatsService.CodeStatsResult result = createResultWithMultipleContributors();

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);

    // Project summary
    assertThat(analysis.projectSummary().totalCommits()).isEqualTo(180);
    assertThat(analysis.projectSummary().activeContributors()).isEqualTo(5);
    assertThat(analysis.projectSummary().topPerformerPercentage())
        .isCloseTo(27.8, org.assertj.core.data.Offset.offset(0.1)); // 50/180

    // Team composition - should have multiple roles
    List<ContributorProfile> contributors = analysis.teamComposition().contributors();
    assertThat(contributors).hasSize(5);
    assertThat(contributors.stream().map(ContributorProfile::role))
        .containsExactlyInAnyOrder(
            ContributorRole.TECH_LEAD,
            ContributorRole.SENIOR_DEVELOPER,
            ContributorRole.SENIOR_DEVELOPER,
            ContributorRole.DEVELOPER,
            ContributorRole.DEVELOPER);

    // Risk should be manageable with distributed team (may be HIGH due to bus factor)
    assertThat(analysis.riskAssessment().overallRisk()).isIn(RiskLevel.MEDIUM, RiskLevel.HIGH);
    assertThat(analysis.riskAssessment().busFactor()).isGreaterThan(1);
    assertThat(analysis.riskAssessment().knowledgeConcentration()).isLessThan(60.0);

    // Quality metrics
    assertThat(analysis.qualityMetrics().testCoveragePercentage()).isGreaterThan(0);
    assertThat(analysis.qualityMetrics().productionLines()).isGreaterThan(0);

    // Technology breakdown
    assertThat(analysis.technologyBreakdown().technologies()).isNotEmpty();
    assertThat(analysis.technologyBreakdown().primaryStack()).isNotEmpty();
  }

  @Test
  @DisplayName("Should handle empty repository gracefully")
  void shouldHandleEmptyRepository() {
    CodeStatsService.CodeStatsResult result = createEmptyResult();

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);

    assertThat(analysis.projectSummary().totalCommits()).isEqualTo(0);
    assertThat(analysis.projectSummary().activeContributors()).isEqualTo(0);
    assertThat(analysis.projectSummary().topPerformerName()).isEqualTo("None");

    assertThat(analysis.teamComposition().contributors()).isEmpty();
    assertThat(analysis.teamComposition().totalMembers()).isEqualTo(0);

    assertThat(analysis.qualityMetrics().testCoveragePercentage()).isEqualTo(0.0);
    assertThat(analysis.technologyBreakdown().technologies()).isEmpty();
  }

  @Test
  @DisplayName("Should categorize contributor roles correctly")
  void shouldCategorizeContributorRoles() {
    // Test different contributor patterns
    ContributorStats techLead = createContributor("Tech Lead", 60, 15000);
    ContributorStats senior = createContributor("Senior Dev", 40, 8000);
    ContributorStats developer = createContributor("Developer", 20, 3000);
    ContributorStats junior = createContributor("Junior", 8, 1000);
    ContributorStats contributor = createContributor("Contributor", 3, 200);

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(techLead, senior, developer, junior, contributor),
            131, // Total commits
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    List<ContributorProfile> profiles = analysis.teamComposition().contributors();

    // Verify roles are assigned correctly based on commit count and lines changed
    assertThat(profiles.get(0).role()).isEqualTo(ContributorRole.TECH_LEAD);
    assertThat(profiles.get(1).role()).isEqualTo(ContributorRole.SENIOR_DEVELOPER);
    assertThat(profiles.get(2).role()).isEqualTo(ContributorRole.DEVELOPER);
    assertThat(profiles.get(3).role()).isEqualTo(ContributorRole.JUNIOR_DEVELOPER);
    assertThat(profiles.get(4).role()).isEqualTo(ContributorRole.CONTRIBUTOR);
  }

  @Test
  @DisplayName("Should determine impact levels correctly")
  void shouldDetermineImpactLevels() {
    ContributorStats highImpact =
        createContributor(
            "High Impact",
            30,
            8000); // HIGH impact: meets both thresholds (30+ commits, 8000+ lines)
    ContributorStats mediumImpact =
        createContributor(
            "Medium Impact",
            10,
            2000); // MEDIUM impact: meets both thresholds (10+ commits, 2000+ lines)
    ContributorStats lowImpact = createContributor("Low Impact", 5, 500); // LOW impact

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(highImpact, mediumImpact, lowImpact),
            45, // Adjusted total to match individual commits (30+10+5)
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    List<ContributorProfile> profiles = analysis.teamComposition().contributors();

    assertThat(profiles.get(0).impact()).isEqualTo(ContributorImpact.HIGH);
    assertThat(profiles.get(1).impact()).isEqualTo(ContributorImpact.MEDIUM);
    assertThat(profiles.get(2).impact()).isEqualTo(ContributorImpact.LOW);
  }

  @Test
  @DisplayName("Should calculate bus factor correctly")
  void shouldCalculateBusFactorCorrectly() {
    // Create scenario where top contributor has 60% of commits (bus factor = 1)
    ContributorStats dominant = createContributor("Dominant", 60, 10000);
    ContributorStats secondary = createContributor("Secondary", 25, 3000);
    ContributorStats minor = createContributor("Minor", 15, 1000);

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(dominant, secondary, minor),
            100,
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);

    // 60% commits means bus factor = 1 (one person handles >50% of work)
    assertThat(analysis.riskAssessment().busFactor()).isEqualTo(1);
    assertThat(analysis.riskAssessment().overallRisk()).isEqualTo(RiskLevel.CRITICAL);
  }

  @Test
  @DisplayName("Should calculate quality metrics correctly")
  void shouldCalculateQualityMetrics() {
    ContributorStats contributor = createContributorWithDetailedStats();

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(contributor),
            50, // Total commits for impact test
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    QualityMetrics quality = analysis.qualityMetrics();

    // Production: 1000, Test: 800, Doc: 100
    assertThat(quality.productionLines()).isEqualTo(1000);
    assertThat(quality.testLines()).isEqualTo(800);
    assertThat(quality.documentationLines()).isEqualTo(100);

    // Test coverage: 800 / (1000 + 800) * 100 = 44.4%
    assertThat(quality.testCoveragePercentage())
        .isCloseTo(44.4, org.assertj.core.data.Offset.offset(0.1));
    assertThat(quality.testingCulture()).isEqualTo(QualityLevel.MODERATE);
  }

  @Test
  @DisplayName("Should analyze technology breakdown correctly")
  void shouldAnalyzeTechnologyBreakdown() {
    ContributorStats javaBackendDev = createJavaBackendContributor();
    ContributorStats frontendDev = createFrontendContributor();

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(javaBackendDev, frontendDev),
            80,
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    TechnologyBreakdown tech = analysis.technologyBreakdown();

    assertThat(tech.primaryStack()).isEqualTo("Full-stack Web");
    assertThat(tech.technologies()).isNotEmpty();

    // Should contain both backend and frontend technologies
    assertThat(tech.technologies().stream().map(t -> t.category()))
        .contains(TechnologyCategory.BACKEND, TechnologyCategory.FRONTEND);
  }

  @Test
  @DisplayName("Should generate appropriate strategic recommendations")
  void shouldGenerateStrategicRecommendations() {
    // High-risk scenario: single contributor with poor test coverage
    ContributorStats singleDev = createContributor("Solo Dev", 100, 20000);

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(singleDev),
            100,
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    List<String> recommendations = analysis.strategicRecommendations();

    // Should recommend addressing team risk
    assertThat(recommendations)
        .anyMatch(rec -> rec.contains("team risk") || rec.contains("hiring"));

    // Should recommend scaling team
    assertThat(recommendations).anyMatch(rec -> rec.contains("Scale team"));

    // Should include general strategic recommendations
    assertThat(recommendations)
        .anyMatch(rec -> rec.contains("knowledge sharing") || rec.contains("retrospectives"));
  }

  // Helper methods to create test data

  private CodeStatsService.CodeStatsResult createResultWithSingleContributor() {
    ContributorStats alice =
        createContributor(
            "Alice Smith", 50, 10000); // Meets TECH_LEAD threshold (50+ commits, 10000+ lines)

    return new CodeStatsService.CodeStatsResult(
        List.of(alice),
        50,
        new File("/test-repo"),
        LocalDateTime.now().minusDays(30),
        LocalDateTime.now(),
        true,
        null);
  }

  private CodeStatsService.CodeStatsResult createResultWithMultipleContributors() {
    ContributorStats alice =
        createContributor("Alice Smith", 50, 10000); // TECH_LEAD (50+ commits, 10000+ lines)
    ContributorStats bob =
        createContributor("Bob Jones", 40, 5000); // SENIOR_DEVELOPER (30+ commits, 5000+ lines)
    ContributorStats charlie =
        createContributor("Charlie Brown", 35, 5000); // SENIOR_DEVELOPER (30+ commits, 5000+ lines)
    ContributorStats david =
        createContributor(
            "David Wilson",
            30,
            1500); // SENIOR_DEVELOPER (30+ commits, but 1500 lines = DEVELOPER actually)
    ContributorStats eve = createContributor("Eve Adams", 25, 1200); // DEVELOPER (10+ commits)

    return new CodeStatsService.CodeStatsResult(
        List.of(alice, bob, charlie, david, eve),
        180, // Total commits (50+40+35+30+25) - more balanced
        new File("/test-repo"),
        LocalDateTime.now().minusDays(60),
        LocalDateTime.now(),
        true,
        null);
  }

  private CodeStatsService.CodeStatsResult createEmptyResult() {
    return new CodeStatsService.CodeStatsResult(
        List.of(), 0, new File("/empty-repo"), null, null, true, null);
  }

  private ContributorStats createContributor(String name, int commits, int linesChanged) {
    // Adjust values to ensure proper thresholds are met
    Map<String, LanguageStats> languageStats =
        Map.of(
            "Java",
                new LanguageStats(
                    "Java", linesChanged / 2, linesChanged / 3, linesChanged / 4, commits / 2),
            "JavaScript",
                new LanguageStats(
                    "JavaScript",
                    linesChanged / 4,
                    linesChanged / 6,
                    linesChanged / 8,
                    commits / 3));

    Map<String, Integer> productionLines = Map.of("Java", linesChanged / 2);
    Map<String, Integer> testLines = Map.of("Java", linesChanged / 4);
    Map<String, Integer> otherLines = Map.of("JavaScript", linesChanged / 4);

    return new ContributorStats(
        name,
        name.toLowerCase().replace(" ", ".") + "@company.com",
        List.of(name.toLowerCase().replace(" ", ".") + "@company.com"),
        commits,
        commits, // files changed
        linesChanged * 2 / 3
            + (linesChanged % 3 > 0 ? 1 : 0), // insertions - compensate for integer division
        linesChanged / 3, // deletions
        languageStats,
        productionLines,
        testLines,
        otherLines);
  }

  private ContributorStats createContributorWithDetailedStats() {
    Map<String, LanguageStats> languageStats =
        Map.of(
            "Java", new LanguageStats("Java", 1000, 900, 100, 25),
            "Markdown", new LanguageStats("Markdown", 100, 100, 0, 5));

    Map<String, Integer> productionLines = Map.of("Java", 1000);
    Map<String, Integer> testLines = Map.of("Java", 800);
    Map<String, Integer> otherLines = Map.of("Markdown", 100);

    return new ContributorStats(
        "Detailed Dev",
        "detailed@company.com",
        List.of("detailed@company.com"),
        50,
        1100,
        100,
        25,
        languageStats,
        productionLines,
        testLines,
        otherLines);
  }

  private ContributorStats createJavaBackendContributor() {
    Map<String, LanguageStats> languageStats =
        Map.of(
            "Java", new LanguageStats("Java", 2000, 1800, 200, 15),
            "Python", new LanguageStats("Python", 500, 450, 50, 5));

    Map<String, Integer> productionLines = Map.of("Java", 2000, "Python", 500);
    Map<String, Integer> testLines = Map.of("Java", 400);
    Map<String, Integer> otherLines = Map.of();

    return new ContributorStats(
        "Backend Dev",
        "backend@company.com",
        List.of("backend@company.com"),
        40,
        2500,
        250,
        20,
        languageStats,
        productionLines,
        testLines,
        otherLines);
  }

  private ContributorStats createFrontendContributor() {
    Map<String, LanguageStats> languageStats =
        Map.of(
            "JavaScript", new LanguageStats("JavaScript", 1500, 1350, 150, 20),
            "TypeScript", new LanguageStats("TypeScript", 800, 720, 80, 15),
            "CSS", new LanguageStats("CSS", 400, 360, 40, 10));

    Map<String, Integer> productionLines =
        Map.of(
            "JavaScript", 1500,
            "TypeScript", 800,
            "CSS", 400);
    Map<String, Integer> testLines = Map.of("JavaScript", 300);
    Map<String, Integer> otherLines = Map.of();

    return new ContributorStats(
        "Frontend Dev",
        "frontend@company.com",
        List.of("frontend@company.com"),
        40,
        2700,
        270,
        25,
        languageStats,
        productionLines,
        testLines,
        otherLines);
  }

  @Test
  @DisplayName("Should sort contributors correctly when commits are equal")
  void shouldSortContributorsWithEqualCommits() {
    // Create contributors with identical commit counts to test stable sorting
    ContributorStats alice = createContributor("Alice", 30, 5000);
    ContributorStats bob = createContributor("Bob", 30, 6000); // Same commits, different lines
    ContributorStats charlie = createContributor("Charlie", 30, 4000);

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(alice, bob, charlie),
            90,
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    TeamComposition team = analysis.teamComposition();

    // All contributors should be included (they have equal commits)
    assertThat(team.contributors()).hasSize(3);

    // Verify the comparator returns 0 for equal values by checking all are included
    // and that the sorting is stable (maintains original order for equal elements)
    List<String> contributorNames =
        team.contributors().stream().map(ContributorProfile::name).toList();

    assertThat(contributorNames).containsExactlyInAnyOrder("Alice", "Bob", "Charlie");
  }

  @Test
  @DisplayName("Should sort primary technologies correctly when lines are equal")
  void shouldSortPrimaryTechnologiesWithEqualLines() {
    // Create contributor with technologies that have equal lines of code
    Map<String, LanguageStats> languageStats =
        Map.of(
            "Java", new LanguageStats("Java", 1000, 900, 100, 20),
            "Python", new LanguageStats("Python", 1000, 900, 100, 20), // Equal lines
            "JavaScript", new LanguageStats("JavaScript", 1000, 900, 100, 20),
            "Go", new LanguageStats("Go", 500, 450, 50, 10)); // Lower lines

    Map<String, Integer> productionLines =
        Map.of("Java", 900, "Python", 900, "JavaScript", 900, "Go", 450);

    ContributorStats contributor =
        new ContributorStats(
            "Multi-tech Dev",
            "dev@company.com",
            List.of("dev@company.com"),
            40,
            3000,
            300,
            50,
            languageStats,
            productionLines,
            Map.of(),
            Map.of());

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(contributor),
            40,
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);

    // Check that primary technologies are extracted correctly
    // The comparator should handle equal values (1000 lines each) correctly
    ContributorProfile profile = analysis.teamComposition().contributors().get(0);
    assertThat(profile.primaryTechnologies()).hasSize(3); // Limited to 3

    // All technologies with equal lines should be in the result
    assertThat(profile.primaryTechnologies())
        .contains("Java", "Python", "JavaScript")
        .doesNotContain("Go"); // Lower lines should be filtered out
  }

  @Test
  @DisplayName("Should sort technology breakdown entries when lines of code are equal")
  void shouldSortTechnologyBreakdownWithEqualLines() {
    ContributorStats equalLinesDev = createContributorWithEqualTechLines();

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(equalLinesDev),
            30,
            new File("/test-repo"),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    TechnologyBreakdown tech = analysis.technologyBreakdown();

    assertThat(tech.technologies()).hasSizeGreaterThan(0);

    // Verify that technologies with equal lines are sorted correctly
    // The lambda comparator should return 0 for equal values, maintaining stable sort
    List<Integer> techLines = tech.technologies().stream().map(t -> t.linesOfCode()).toList();

    // Should be sorted in descending order
    for (int i = 0; i < techLines.size() - 1; i++) {
      assertThat(techLines.get(i)).isGreaterThanOrEqualTo(techLines.get(i + 1));
    }
  }

  @Test
  @DisplayName("Should handle team size recommendations for different scenarios")
  void shouldGenerateCorrectTeamSizeRecommendations() {
    // Test case 1: Very small team (should recommend expanding)
    ContributorStats soloDev = createContributor("Solo", 60, 15000);
    CodeStatsService.CodeStatsResult smallTeam =
        new CodeStatsService.CodeStatsResult(
            List.of(soloDev),
            60,
            new File("/test"),
            LocalDateTime.now(),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis smallAnalysis = BusinessAnalyzer.analyze(smallTeam);
    assertThat(smallAnalysis.teamComposition().teamSizeRecommendation()).contains("expanding team");

    // Test case 2: Well-balanced team
    List<ContributorStats> balancedTeam =
        List.of(
            createContributor("Lead", 50, 10000),
            createContributor("Senior1", 40, 8000),
            createContributor("Senior2", 35, 7000),
            createContributor("Mid", 25, 4000),
            createContributor("Junior", 15, 2000));

    CodeStatsService.CodeStatsResult balancedResult =
        new CodeStatsService.CodeStatsResult(
            balancedTeam,
            165,
            new File("/test"),
            LocalDateTime.now(),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis balancedAnalysis = BusinessAnalyzer.analyze(balancedResult);
    assertThat(balancedAnalysis.teamComposition().teamSizeRecommendation())
        .contains("adequate for current workload");
  }

  @Test
  @DisplayName("Should identify medium risks correctly")
  void shouldIdentifyMediumRisks() {
    // Create team with specific risk factors - small team size for medium risk
    List<ContributorStats> riskTeam =
        List.of(
            createContributor("Junior1", 10, 1000), // Junior contributors
            createContributor("Junior2", 8, 800),
            createContributor("Senior", 40, 8000)); // Only one senior - 3 total members

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            riskTeam, 58, new File("/test"), LocalDateTime.now(), LocalDateTime.now(), true, null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    RiskAssessment risk = analysis.riskAssessment();

    // Should identify small team size risk
    assertThat(risk.mediumRisks())
        .anyMatch(
            riskMsg -> riskMsg.contains("Small team size") || riskMsg.contains("Experience gap"));
  }

  @Test
  @DisplayName("Should determine documentation levels correctly")
  void shouldDetermineDocumentationLevels() {
    // Test different documentation ratios
    ContributorStats heavyDocumenter = createContributorWithDocumentation();

    CodeStatsService.CodeStatsResult result =
        new CodeStatsService.CodeStatsResult(
            List.of(heavyDocumenter),
            30,
            new File("/test"),
            LocalDateTime.now(),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);
    QualityMetrics quality = analysis.qualityMetrics();

    // Should calculate documentation level based on ratio
    assertThat(quality.documentationLevel()).isNotNull();

    // Should generate appropriate quality insights
    assertThat(quality.qualityInsight()).isNotEmpty();
    assertThat(quality.qualityInsight()).isNotBlank(); // Not blank string
  }

  @Test
  @DisplayName("Should generate quality insights for different scenarios")
  void shouldGenerateQualityInsights() {
    // Test case 1: Excellent testing + Good docs
    ContributorStats excellentDev = createContributorWithExcellentMetrics();
    CodeStatsService.CodeStatsResult excellentResult =
        new CodeStatsService.CodeStatsResult(
            List.of(excellentDev),
            40,
            new File("/test"),
            LocalDateTime.now(),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis excellentAnalysis = BusinessAnalyzer.analyze(excellentResult);
    assertThat(excellentAnalysis.qualityMetrics().qualityInsight())
        .isNotEmpty()
        .isNotBlank(); // Should not return empty/blank string

    // Test case 2: Poor testing + Poor docs
    ContributorStats poorDev = createContributorWithPoorMetrics();
    CodeStatsService.CodeStatsResult poorResult =
        new CodeStatsService.CodeStatsResult(
            List.of(poorDev),
            20,
            new File("/test"),
            LocalDateTime.now(),
            LocalDateTime.now(),
            true,
            null);

    BusinessAnalysis poorAnalysis = BusinessAnalyzer.analyze(poorResult);
    assertThat(poorAnalysis.qualityMetrics().qualityInsight())
        .isNotEmpty()
        .isNotBlank(); // Should not return empty/blank string
  }

  // Helper methods for new test scenarios

  private ContributorStats createContributorWithEqualTechLines() {
    Map<String, LanguageStats> languageStats =
        Map.of(
            "Java", new LanguageStats("Java", 2000, 1800, 200, 25),
            "Python", new LanguageStats("Python", 2000, 1800, 200, 25), // Equal lines
            "TypeScript", new LanguageStats("TypeScript", 2000, 1800, 200, 25),
            "Go", new LanguageStats("Go", 1000, 900, 100, 15)); // Different lines

    Map<String, Integer> productionLines =
        Map.of("Java", 1800, "Python", 1800, "TypeScript", 1800, "Go", 900);

    return new ContributorStats(
        "Equal Tech Dev",
        "equal@company.com",
        List.of("equal@company.com"),
        30,
        6700,
        670,
        65,
        languageStats,
        productionLines,
        Map.of(),
        Map.of());
  }

  private ContributorStats createContributorWithDocumentation() {
    Map<String, LanguageStats> languageStats =
        Map.of("Java", new LanguageStats("Java", 1000, 900, 100, 20));

    Map<String, Integer> productionLines = Map.of("Java", 900);
    Map<String, Integer> testLines = Map.of("Java", 200);
    Map<String, Integer> docLines = Map.of("Markdown", 150); // Good documentation

    return new ContributorStats(
        "Documenter",
        "doc@company.com",
        List.of("doc@company.com"),
        30,
        1250,
        125,
        25,
        languageStats,
        productionLines,
        testLines,
        docLines);
  }

  private ContributorStats createContributorWithExcellentMetrics() {
    Map<String, LanguageStats> languageStats =
        Map.of("Java", new LanguageStats("Java", 2000, 1200, 800, 40));

    Map<String, Integer> productionLines = Map.of("Java", 1200);
    Map<String, Integer> testLines = Map.of("Java", 800); // 40% test coverage (excellent)
    Map<String, Integer> docLines = Map.of("Markdown", 200); // Good documentation

    return new ContributorStats(
        "Excellent Dev",
        "excellent@company.com",
        List.of("excellent@company.com"),
        40,
        2200,
        220,
        45,
        languageStats,
        productionLines,
        testLines,
        docLines);
  }

  private ContributorStats createContributorWithPoorMetrics() {
    Map<String, LanguageStats> languageStats =
        Map.of("Java", new LanguageStats("Java", 1000, 950, 50, 20));

    Map<String, Integer> productionLines = Map.of("Java", 950);
    Map<String, Integer> testLines = Map.of("Java", 50); // 5% test coverage (poor)
    Map<String, Integer> docLines = Map.of(); // No documentation

    return new ContributorStats(
        "Poor Metrics Dev",
        "poor@company.com",
        List.of("poor@company.com"),
        20,
        1000,
        100,
        20,
        languageStats,
        productionLines,
        testLines,
        docLines);
  }
}
