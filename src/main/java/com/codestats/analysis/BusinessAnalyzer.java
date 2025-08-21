package com.codestats.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.codestats.analysis.QualityMetrics.QualityLevel;
import com.codestats.analysis.RiskAssessment.RiskLevel;
import com.codestats.analysis.TeamComposition.ContributorImpact;
import com.codestats.analysis.TeamComposition.ContributorProfile;
import com.codestats.analysis.TeamComposition.ContributorRole;
import com.codestats.analysis.TechnologyBreakdown.TechnologyCategory;
import com.codestats.analysis.TechnologyBreakdown.TechnologyEntry;
import com.codestats.model.ContributorStats;
import com.codestats.service.CodeStatsService;

/**
 * Transforms raw git statistics into business intelligence insights. Applies domain knowledge and
 * analytical rules to provide executive-level analysis.
 */
public class BusinessAnalyzer {

  public static BusinessAnalysis analyze(CodeStatsService.CodeStatsResult result) {
    String projectName = extractProjectName(result.repositoryPath());

    ProjectSummary projectSummary = analyzeProjectSummary(result, projectName);
    TeamComposition teamComposition = analyzeTeamComposition(result);
    RiskAssessment riskAssessment = analyzeRisks(result, teamComposition);
    QualityMetrics qualityMetrics = analyzeQuality(result);
    TechnologyBreakdown technologyBreakdown = analyzeTechnology(result);
    List<String> recommendations =
        generateStrategicRecommendations(
            teamComposition, riskAssessment, qualityMetrics, technologyBreakdown);

    return BusinessAnalysis.builder()
        .projectSummary(projectSummary)
        .teamComposition(teamComposition)
        .riskAssessment(riskAssessment)
        .qualityMetrics(qualityMetrics)
        .technologyBreakdown(technologyBreakdown)
        .strategicRecommendations(recommendations)
        .build();
  }

  private static String extractProjectName(File repositoryPath) {
    return repositoryPath.getName().toUpperCase();
  }

  private static ProjectSummary analyzeProjectSummary(
      CodeStatsService.CodeStatsResult result, String projectName) {
    List<ContributorStats> contributors = result.contributorStats();

    if (contributors.isEmpty()) {
      return new ProjectSummary(projectName, 0, 0, 0, "No data", null, null, "None", 0, 0.0);
    }

    ContributorStats topPerformer =
        contributors.stream()
            .max(Comparator.comparing(ContributorStats::commitCount))
            .orElse(contributors.get(0));

    int totalLinesChanged =
        contributors.stream().mapToInt(ContributorStats::totalLinesChanged).sum();

    double topPerformerPercentage =
        (double) topPerformer.commitCount() / result.totalCommits() * 100;

    return new ProjectSummary(
        projectName,
        result.totalCommits(),
        contributors.size(),
        totalLinesChanged,
        result.getAnalysisPeriod(),
        result.oldestCommit(),
        result.newestCommit(),
        topPerformer.name(),
        topPerformer.commitCount(),
        topPerformerPercentage);
  }

  private static TeamComposition analyzeTeamComposition(CodeStatsService.CodeStatsResult result) {
    int totalCommits = result.totalCommits();
    List<ContributorProfile> profiles =
        result.contributorStats().stream()
            .map(stats -> analyzeContributor(stats, totalCommits))
            .sorted((a, b) -> Integer.compare(b.commits(), a.commits()))
            .toList();

    int totalMembers = profiles.size();
    int activeMembers =
        (int)
            profiles.stream()
                .filter(p -> p.commitPercentage() >= 5.0) // 5% or more commits
                .count();

    String teamSizeRecommendation = generateTeamSizeRecommendation(totalMembers, profiles);

    return new TeamComposition(profiles, totalMembers, activeMembers, teamSizeRecommendation);
  }

  private static ContributorProfile analyzeContributor(ContributorStats stats, int totalCommits) {
    ContributorRole role = determineRole(stats);
    ContributorImpact impact = determineImpact(stats);
    List<String> primaryTechnologies = extractPrimaryTechnologies(stats);
    double commitPercentage =
        totalCommits > 0 ? (double) stats.commitCount() / totalCommits * 100 : 0.0;

    return new ContributorProfile(
        stats.name(),
        stats.primaryEmail(),
        stats.commitCount(),
        commitPercentage,
        stats.totalLinesChanged(),
        role,
        impact,
        primaryTechnologies);
  }

  private static ContributorRole determineRole(ContributorStats stats) {
    if (stats.commitCount() >= 50 && stats.totalLinesChanged() >= 10000) {
      return ContributorRole.TECH_LEAD;
    } else if (stats.commitCount() >= 30 && stats.totalLinesChanged() >= 5000) {
      return ContributorRole.SENIOR_DEVELOPER;
    } else if (stats.commitCount() >= 10) {
      return ContributorRole.DEVELOPER;
    } else if (stats.commitCount() >= 5) {
      return ContributorRole.JUNIOR_DEVELOPER;
    }
    return ContributorRole.CONTRIBUTOR;
  }

  private static ContributorImpact determineImpact(ContributorStats stats) {
    if (stats.commitCount() >= 30 && stats.totalLinesChanged() >= 8000) {
      return ContributorImpact.HIGH;
    } else if (stats.commitCount() >= 10 && stats.totalLinesChanged() >= 2000) {
      return ContributorImpact.MEDIUM;
    }
    return ContributorImpact.LOW;
  }

  private static List<String> extractPrimaryTechnologies(ContributorStats stats) {
    return stats.languageStats().entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue().linesChanged(), a.getValue().linesChanged()))
        .limit(3)
        .map(Map.Entry::getKey)
        .filter(lang -> !lang.equals("Lock File") && !lang.equals("Unknown"))
        .toList();
  }

  private static String generateTeamSizeRecommendation(
      int totalMembers, List<ContributorProfile> profiles) {
    long highImpactCount =
        profiles.stream().mapToLong(p -> p.impact() == ContributorImpact.HIGH ? 1 : 0).sum();

    if (totalMembers < 3) {
      return "🔄 Consider expanding team to 4-6 members for better coverage";
    } else if (highImpactCount == 1 && totalMembers < 6) {
      return "⚖️ Add senior developers to reduce single-person dependencies";
    } else if (totalMembers > 10) {
      return "🏗️ Large team - ensure clear module ownership and communication";
    }
    return "✅ Team size appears adequate for current workload";
  }

  private static RiskAssessment analyzeRisks(
      CodeStatsService.CodeStatsResult result, TeamComposition team) {
    int busFactor = calculateBusFactor(result.contributorStats());
    double knowledgeConcentration =
        calculateKnowledgeConcentration(result.contributorStats(), result.totalCommits());

    RiskLevel overallRisk =
        determineOverallRisk(busFactor, knowledgeConcentration, team.totalMembers());

    List<String> criticalRisks = identifyCriticalRisks(busFactor, knowledgeConcentration, team);
    List<String> mediumRisks = identifyMediumRisks(team, result.contributorStats());
    List<String> mitigationStrategies = generateMitigationStrategies(criticalRisks, mediumRisks);

    return new RiskAssessment(
        overallRisk,
        busFactor,
        knowledgeConcentration,
        criticalRisks,
        mediumRisks,
        mitigationStrategies);
  }

  private static int calculateBusFactor(List<ContributorStats> contributors) {
    if (contributors.isEmpty()) return 0;

    int totalCommits = contributors.stream().mapToInt(ContributorStats::commitCount).sum();
    int cumulativeCommits = 0;

    for (int i = 0; i < contributors.size(); i++) {
      cumulativeCommits += contributors.get(i).commitCount();
      if (cumulativeCommits >= totalCommits * 0.5) { // 50% of work
        return i + 1;
      }
    }
    return contributors.size();
  }

  private static double calculateKnowledgeConcentration(
      List<ContributorStats> contributors, int totalCommits) {
    if (contributors.isEmpty() || totalCommits == 0) return 0.0;

    ContributorStats topContributor =
        contributors.stream()
            .max(Comparator.comparing(ContributorStats::commitCount))
            .orElse(contributors.get(0));

    return (double) topContributor.commitCount() / totalCommits * 100;
  }

  private static RiskLevel determineOverallRisk(int busFactor, double concentration, int teamSize) {
    if (busFactor == 1 || concentration > 70) {
      return RiskLevel.CRITICAL;
    } else if (busFactor == 2 || concentration > 50 || teamSize < 3) {
      return RiskLevel.HIGH;
    } else if (concentration > 35 || teamSize < 5) {
      return RiskLevel.MEDIUM;
    }
    return RiskLevel.LOW;
  }

  private static List<String> identifyCriticalRisks(
      int busFactor, double concentration, TeamComposition team) {
    List<String> risks = new ArrayList<>();

    if (busFactor == 1) {
      risks.add("Single point of failure: One person handles 50%+ of development");
    }
    if (concentration > 60) {
      risks.add(
          "Knowledge concentration: "
              + String.format("%.0f%%", concentration)
              + " of commits by one person");
    }
    if (team.getHighImpactContributors().size() == 1) {
      risks.add("Limited senior coverage: Only one high-impact contributor");
    }

    return risks;
  }

  private static List<String> identifyMediumRisks(
      TeamComposition team, List<ContributorStats> contributors) {
    List<String> risks = new ArrayList<>();

    if (team.totalMembers() < 4) {
      risks.add("Small team size: Limited backup for knowledge areas");
    }

    long juniorCount =
        team.contributors().stream()
            .mapToLong(
                c ->
                    c.role() == ContributorRole.JUNIOR_DEVELOPER
                            || c.role() == ContributorRole.CONTRIBUTOR
                        ? 1
                        : 0)
            .sum();

    if (juniorCount > team.totalMembers() / 2) {
      risks.add("Experience gap: High ratio of junior contributors");
    }

    return risks;
  }

  private static List<String> generateMitigationStrategies(
      List<String> criticalRisks, List<String> mediumRisks) {
    List<String> strategies = new ArrayList<>();

    if (!criticalRisks.isEmpty()) {
      strategies.add("Implement pair programming for critical modules");
      strategies.add("Cross-train team members on key technologies");
      strategies.add("Document architectural decisions and domain knowledge");
    }

    if (!mediumRisks.isEmpty()) {
      strategies.add("Schedule regular knowledge sharing sessions");
      strategies.add("Rotate code review assignments across team");
    }

    strategies.add("Consider hiring senior developer to reduce dependencies");

    return strategies;
  }

  private static QualityMetrics analyzeQuality(CodeStatsService.CodeStatsResult result) {
    int totalProdLines =
        result.contributorStats().stream()
            .flatMap(c -> c.productionLines().values().stream())
            .mapToInt(Integer::intValue)
            .sum();

    int totalTestLines =
        result.contributorStats().stream()
            .flatMap(c -> c.testLines().values().stream())
            .mapToInt(Integer::intValue)
            .sum();

    int totalDocLines =
        result.contributorStats().stream()
            .flatMap(c -> c.languageStats().entrySet().stream())
            .filter(e -> e.getKey().equals("Markdown"))
            .mapToInt(e -> e.getValue().linesChanged())
            .sum();

    double testCoverage =
        totalProdLines + totalTestLines > 0
            ? (double) totalTestLines / (totalProdLines + totalTestLines) * 100
            : 0.0;

    QualityLevel testingCulture = determineTestingCulture(testCoverage);
    QualityLevel documentationLevel =
        determineDocumentationLevel(totalDocLines, totalProdLines + totalTestLines);

    String qualityInsight =
        generateQualityInsight(testCoverage, testingCulture, documentationLevel);

    return new QualityMetrics(
        testCoverage,
        totalProdLines,
        totalTestLines,
        totalDocLines,
        testingCulture,
        documentationLevel,
        qualityInsight);
  }

  private static QualityLevel determineTestingCulture(double testCoverage) {
    if (testCoverage >= 80) return QualityLevel.EXCELLENT;
    if (testCoverage >= 60) return QualityLevel.GOOD;
    if (testCoverage >= 40) return QualityLevel.MODERATE;
    return QualityLevel.POOR;
  }

  private static QualityLevel determineDocumentationLevel(int docLines, int totalCodeLines) {
    if (totalCodeLines == 0) return QualityLevel.POOR;

    double docRatio = (double) docLines / totalCodeLines * 100;
    if (docRatio >= 5) return QualityLevel.EXCELLENT;
    if (docRatio >= 2) return QualityLevel.GOOD;
    if (docRatio >= 1) return QualityLevel.MODERATE;
    return QualityLevel.POOR;
  }

  private static String generateQualityInsight(
      double testCoverage, QualityLevel testingCulture, QualityLevel docLevel) {
    if (testingCulture == QualityLevel.EXCELLENT && docLevel == QualityLevel.GOOD) {
      return "Exceptional engineering practices with strong testing and documentation";
    } else if (testingCulture == QualityLevel.GOOD) {
      return "Solid testing culture - " + String.format("%.0f%% test coverage", testCoverage);
    } else if (testingCulture == QualityLevel.MODERATE) {
      return "Developing testing practices - opportunity for improvement";
    }
    return "Testing and documentation practices need attention";
  }

  private static TechnologyBreakdown analyzeTechnology(CodeStatsService.CodeStatsResult result) {
    Map<String, Integer> languageDistribution =
        result.contributorStats().stream()
            .flatMap(c -> c.languageStats().entrySet().stream())
            .collect(
                Collectors.groupingBy(
                    Map.Entry::getKey, Collectors.summingInt(e -> e.getValue().linesChanged())));

    List<TechnologyEntry> technologies =
        languageDistribution.entrySet().stream()
            .filter(e -> !e.getKey().equals("Lock File") && !e.getKey().equals("Unknown"))
            .map(e -> createTechnologyEntry(e, languageDistribution))
            .sorted((a, b) -> Integer.compare(b.linesOfCode(), a.linesOfCode()))
            .toList();

    String primaryStack = determinePrimaryStack(technologies);
    String architectureType = determineArchitectureType(technologies);
    List<String> recommendations = generateTechnologyRecommendations(technologies);

    return new TechnologyBreakdown(
        technologies, languageDistribution, primaryStack, architectureType, recommendations);
  }

  private static TechnologyEntry createTechnologyEntry(
      Map.Entry<String, Integer> entry, Map<String, Integer> total) {
    String language = entry.getKey();
    int lines = entry.getValue();
    int totalLines = total.values().stream().mapToInt(Integer::intValue).sum();
    double percentage = (double) lines / totalLines * 100;

    TechnologyCategory category = categorizeTechnology(language);
    String businessValue = determineBusinessValue(language, category);

    return new TechnologyEntry(language, lines, percentage, category, businessValue);
  }

  private static TechnologyCategory categorizeTechnology(String language) {
    return switch (language.toLowerCase()) {
      case "python", "java", "go", "rust", "c#" -> TechnologyCategory.BACKEND;
      case "javascript", "typescript", "html", "css", "react", "vue" -> TechnologyCategory.FRONTEND;
      case "swift", "kotlin", "dart" -> TechnologyCategory.MOBILE;
      case "shell", "bash", "docker", "yaml" -> TechnologyCategory.DEVOPS;
      case "sql", "json" -> TechnologyCategory.DATA;
      case "markdown", "text" -> TechnologyCategory.DOCUMENTATION;
      default -> TechnologyCategory.CONFIG;
    };
  }

  private static String determineBusinessValue(String language, TechnologyCategory category) {
    return switch (category) {
      case BACKEND -> "Core business logic and data processing";
      case FRONTEND -> "User experience and interface";
      case MOBILE -> "Mobile user engagement";
      case DEVOPS -> "Infrastructure efficiency and reliability";
      case DATA -> "Data insights and analytics";
      case TESTING -> "Quality assurance and reliability";
      case DOCUMENTATION -> "Knowledge management and onboarding";
      default -> "Configuration and maintenance";
    };
  }

  private static String determinePrimaryStack(List<TechnologyEntry> technologies) {
    boolean hasBackend =
        technologies.stream().anyMatch(t -> t.category() == TechnologyCategory.BACKEND);
    boolean hasFrontend =
        technologies.stream().anyMatch(t -> t.category() == TechnologyCategory.FRONTEND);
    boolean hasMobile =
        technologies.stream().anyMatch(t -> t.category() == TechnologyCategory.MOBILE);

    if (hasBackend && hasFrontend && hasMobile) {
      return "Full-stack + Mobile";
    } else if (hasBackend && hasFrontend) {
      return "Full-stack Web";
    } else if (hasBackend) {
      return "Backend/API";
    } else if (hasFrontend) {
      return "Frontend/UI";
    }
    return "Specialized/Other";
  }

  private static String determineArchitectureType(List<TechnologyEntry> technologies) {
    boolean hasPython = technologies.stream().anyMatch(t -> t.language().equals("Python"));
    boolean hasJavaScript = technologies.stream().anyMatch(t -> t.language().equals("JavaScript"));
    boolean hasTypeScript = technologies.stream().anyMatch(t -> t.language().equals("TypeScript"));

    if (hasPython && (hasJavaScript || hasTypeScript)) {
      return "Modern Web Application (Python + JS/TS)";
    } else if (hasPython) {
      return "Python-based Backend";
    } else if (hasTypeScript) {
      return "TypeScript Frontend Application";
    }
    return "Mixed Technology Stack";
  }

  private static List<String> generateTechnologyRecommendations(
      List<TechnologyEntry> technologies) {
    List<String> recommendations = new ArrayList<>();

    boolean hasTypeScript = technologies.stream().anyMatch(t -> t.language().equals("TypeScript"));
    boolean hasJavaScript = technologies.stream().anyMatch(t -> t.language().equals("JavaScript"));

    if (hasJavaScript && !hasTypeScript) {
      recommendations.add("Consider migrating to TypeScript for better type safety");
    }

    boolean hasTestingFramework =
        technologies.stream().anyMatch(t -> t.category() == TechnologyCategory.TESTING);
    if (!hasTestingFramework) {
      recommendations.add("Implement automated testing framework");
    }

    recommendations.add("Maintain technology stack currency with regular updates");

    return recommendations;
  }

  private static List<String> generateStrategicRecommendations(
      TeamComposition team, RiskAssessment risk, QualityMetrics quality, TechnologyBreakdown tech) {

    List<String> recommendations = new ArrayList<>();

    // Team-based recommendations
    if (risk.overallRisk() == RiskLevel.CRITICAL || risk.overallRisk() == RiskLevel.HIGH) {
      recommendations.add("🚨 Priority: Address team risk through hiring or knowledge transfer");
    }

    if (team.totalMembers() < 4) {
      recommendations.add("📈 Scale team to 4-6 developers for better coverage and velocity");
    }

    // Quality-based recommendations
    if (quality.testCoveragePercentage() < 60) {
      recommendations.add("🧪 Improve test coverage to reach 70%+ for better reliability");
    }

    // Technology-based recommendations
    if (tech.primaryStack().contains("Full-stack")) {
      recommendations.add("🏗️ Consider architectural review to ensure scalability");
    }

    // General strategic recommendations
    recommendations.add("📊 Establish regular team retrospectives and knowledge sharing");
    recommendations.add("🎯 Define clear module ownership to reduce dependencies");

    return recommendations;
  }
}
