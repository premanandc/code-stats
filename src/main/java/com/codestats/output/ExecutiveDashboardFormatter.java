package com.codestats.output;

import java.util.List;

import com.codestats.analysis.BusinessAnalysis;
import com.codestats.analysis.BusinessAnalyzer;
import com.codestats.analysis.TeamComposition.ContributorProfile;
import com.codestats.analysis.TechnologyBreakdown.TechnologyEntry;
import com.codestats.service.CodeStatsService;

/**
 * Formats code statistics as an executive dashboard with business intelligence insights. Transforms
 * raw git metrics into strategic information for IT leadership.
 */
public class ExecutiveDashboardFormatter implements OutputFormatter {

  private final boolean useColors;

  public ExecutiveDashboardFormatter(boolean useColors) {
    this.useColors = useColors;
  }

  @Override
  public String format(CodeStatsService.CodeStatsResult result) {
    if (!result.success()) {
      return formatError(result);
    }

    if (result.contributorStats().isEmpty()) {
      return formatNoData(result);
    }

    BusinessAnalysis analysis = BusinessAnalyzer.analyze(result);

    StringBuilder dashboard = new StringBuilder();

    dashboard.append(formatHeader(analysis));
    dashboard.append(formatExecutiveSummary(analysis));
    dashboard.append(formatTeamAnalysis(analysis));
    dashboard.append(formatRiskAssessment(analysis));
    dashboard.append(formatQualityMetrics(analysis));
    dashboard.append(formatTechnologyPortfolio(analysis));
    dashboard.append(formatStrategicRecommendations(analysis));

    return dashboard.toString();
  }

  private String formatError(CodeStatsService.CodeStatsResult result) {
    return color("🔴 ANALYSIS ERROR", BRIGHT_RED)
        + "\n"
        + "════════════════════════════════════════════════════════════════════\n"
        + result.errorMessage()
        + "\n\n";
  }

  private String formatNoData(CodeStatsService.CodeStatsResult result) {
    return color("📊 NO DEVELOPMENT ACTIVITY", CYAN)
        + "\n"
        + "════════════════════════════════════════════════════════════════════\n"
        + "Repository: "
        + result.repositoryPath().getAbsolutePath()
        + "\n"
        + "Period: "
        + result.getAnalysisPeriod()
        + "\n"
        + "No commits found in the specified time period.\n\n";
  }

  private String formatHeader(BusinessAnalysis analysis) {
    String projectName = analysis.projectSummary().projectName();
    String velocity = analysis.projectSummary().getVelocityIndicator();

    return color("🏢 " + projectName + " - EXECUTIVE DEVELOPMENT DASHBOARD", BRIGHT_CYAN)
        + "\n"
        + "════════════════════════════════════════════════════════════════════\n"
        + color("Period: ", CYAN)
        + analysis.projectSummary().analysisPeriod()
        + "  "
        + velocity
        + "\n\n";
  }

  private String formatExecutiveSummary(BusinessAnalysis analysis) {
    var summary = analysis.projectSummary();
    var risk = analysis.riskAssessment();
    var quality = analysis.qualityMetrics();

    return color("📊 EXECUTIVE SUMMARY", BRIGHT_YELLOW)
        + "\n"
        + "────────────────────────────────────────────────────────────────────\n"
        + String.format(
            "%-25s %s commits (%s%% by %s)\n",
            "Development Velocity:",
            summary.totalCommits(),
            String.format("%.0f", summary.topPerformerPercentage()),
            summary.topPerformerName())
        + String.format(
            "%-25s %d developers (%s)\n",
            "Team Composition:",
            summary.activeContributors(),
            analysis.teamComposition().getTeamHealthIndicator())
        + String.format(
            "%-25s %s (%s)\n",
            "Risk Assessment:", risk.overallRisk().getDisplayName(), risk.getBusFactorDescription())
        + String.format(
            "%-25s %.0f%% test coverage (%s)\n",
            "Code Quality:", quality.testCoveragePercentage(), quality.getTestCoverageInsight())
        + String.format(
            "%-25s %s lines changed\n", "Code Impact:", summary.getFormattedLinesChanged())
        + "\n";
  }

  private String formatTeamAnalysis(BusinessAnalysis analysis) {
    var team = analysis.teamComposition();

    StringBuilder section = new StringBuilder();
    section.append(color("👥 TEAM COMPOSITION & IMPACT", BRIGHT_YELLOW)).append("\n");
    section.append("────────────────────────────────────────────────────────────────────\n");

    List<ContributorProfile> topContributors = team.contributors().stream().limit(5).toList();

    for (ContributorProfile contributor : topContributors) {
      String impactBar = generateImpactBar(contributor.commitPercentage(), 30);
      section.append(
          String.format(
              "%-20s %s %s %d commits\n",
              contributor.name(),
              contributor.impact().getLabel(),
              impactBar,
              contributor.commits()));

      section.append(
          String.format(
              "%-20s %s | %s\n",
              contributor.email(),
              contributor.role().getDisplayName(),
              String.join(", ", contributor.primaryTechnologies())));
      section.append("\n");
    }

    section
        .append(color("Team Assessment: ", CYAN))
        .append(team.getTeamHealthIndicator())
        .append("\n");
    section
        .append(color("Recommendation: ", CYAN))
        .append(team.teamSizeRecommendation())
        .append("\n\n");

    return section.toString();
  }

  private String formatRiskAssessment(BusinessAnalysis analysis) {
    var risk = analysis.riskAssessment();

    StringBuilder section = new StringBuilder();
    section.append(color("⚠️ RISK ASSESSMENT", BRIGHT_RED)).append("\n");
    section.append("────────────────────────────────────────────────────────────────────\n");

    section.append(String.format("Overall Risk Level: %s\n", risk.overallRisk().getDisplayName()));
    section.append(
        String.format("Bus Factor: %d (%s)\n", risk.busFactor(), risk.getBusFactorDescription()));
    section.append(String.format("Knowledge Concentration: %s\n", risk.getConcentrationWarning()));
    section.append("\n");

    if (!risk.criticalRisks().isEmpty()) {
      section.append(color("🔴 CRITICAL RISKS:", BRIGHT_RED)).append("\n");
      for (String criticalRisk : risk.criticalRisks()) {
        section.append("• ").append(criticalRisk).append("\n");
      }
      section.append("\n");
    }

    if (!risk.mediumRisks().isEmpty()) {
      section.append(color("🟡 MEDIUM RISKS:", YELLOW)).append("\n");
      for (String mediumRisk : risk.mediumRisks()) {
        section.append("• ").append(mediumRisk).append("\n");
      }
      section.append("\n");
    }

    section.append(color("🛡️ MITIGATION STRATEGIES:", CYAN)).append("\n");
    for (String strategy : risk.mitigationStrategies()) {
      section.append("• ").append(strategy).append("\n");
    }
    section.append("\n");

    return section.toString();
  }

  private String formatQualityMetrics(BusinessAnalysis analysis) {
    var quality = analysis.qualityMetrics();

    StringBuilder section = new StringBuilder();
    section.append(color("🧪 CODE QUALITY ANALYSIS", BRIGHT_GREEN)).append("\n");
    section.append("────────────────────────────────────────────────────────────────────\n");

    String testBar = generateProgressBar(quality.testCoveragePercentage(), 100, 40);
    section.append(
        String.format("Test Coverage:     %s %.0f%%\n", testBar, quality.testCoveragePercentage()));
    section.append(
        String.format("Testing Culture:   %s\n", quality.testingCulture().getIndicator()));
    section.append(
        String.format("Documentation:     %s\n", quality.documentationLevel().getIndicator()));
    section.append("\n");

    section.append(color("CODE DISTRIBUTION:", CYAN)).append("\n");
    section.append(String.format("Production Code:   %,d lines\n", quality.productionLines()));
    section.append(String.format("Test Code:         %,d lines\n", quality.testLines()));
    section.append(String.format("Documentation:     %,d lines\n", quality.documentationLines()));
    section.append("\n");

    section
        .append(color("Quality Insight: ", CYAN))
        .append(quality.qualityInsight())
        .append("\n\n");

    return section.toString();
  }

  private String formatTechnologyPortfolio(BusinessAnalysis analysis) {
    var tech = analysis.technologyBreakdown();

    StringBuilder section = new StringBuilder();
    section.append(color("🏗️ TECHNOLOGY PORTFOLIO", BRIGHT_BLUE)).append("\n");
    section.append("────────────────────────────────────────────────────────────────────\n");

    section.append(String.format("Primary Stack:     %s\n", tech.primaryStack()));
    section.append(String.format("Architecture:      %s\n", tech.architectureType()));
    section.append(String.format("Stack Maturity:    %s\n", tech.getStackMaturity()));
    section.append("\n");

    section.append(color("TECHNOLOGY BREAKDOWN:", CYAN)).append("\n");
    List<TechnologyEntry> topTech = tech.getTopTechnologies(6);

    for (TechnologyEntry entry : topTech) {
      String techBar = generateProgressBar(entry.percentage(), 100, 25);
      section.append(
          String.format(
              "%-12s %s %s %.0f%% (%,d lines)\n",
              entry.language(),
              entry.category().getEmoji(),
              techBar,
              entry.percentage(),
              entry.linesOfCode()));
    }
    section.append("\n");

    if (!tech.technologyRecommendations().isEmpty()) {
      section.append(color("💡 TECHNOLOGY RECOMMENDATIONS:", CYAN)).append("\n");
      for (String recommendation : tech.technologyRecommendations()) {
        section.append("• ").append(recommendation).append("\n");
      }
      section.append("\n");
    }

    return section.toString();
  }

  private String formatStrategicRecommendations(BusinessAnalysis analysis) {
    StringBuilder section = new StringBuilder();
    section.append(color("🎯 STRATEGIC RECOMMENDATIONS", BRIGHT_MAGENTA)).append("\n");
    section.append("────────────────────────────────────────────────────────────────────\n");

    List<String> recommendations = analysis.strategicRecommendations();
    for (int i = 0; i < recommendations.size(); i++) {
      section.append(String.format("%d. %s\n", i + 1, recommendations.get(i)));
    }
    section.append("\n");

    section.append(color("💼 BUSINESS IMPACT:", CYAN)).append("\n");
    section.append("• Reduced development risk through better team distribution\n");
    section.append("• Improved delivery predictability with stronger practices\n");
    section.append("• Enhanced scalability through architectural improvements\n");
    section.append("• Better ROI on development investment\n\n");

    return section.toString();
  }

  private String generateImpactBar(double percentage, int width) {
    int filled = (int) (percentage / 100.0 * width);
    return "█".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, width - filled));
  }

  private String generateProgressBar(double value, double max, int width) {
    int filled = (int) (value / max * width);
    String bar = "█".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, width - filled));

    if (value >= max * 0.8) {
      return color(bar, GREEN);
    } else if (value >= max * 0.6) {
      return color(bar, YELLOW);
    } else {
      return color(bar, RED);
    }
  }

  // Color constants and utility method (same as TextOutputFormatter)
  private static final String RESET = "\033[0m";
  private static final String BRIGHT_RED = "\033[91m";
  private static final String BRIGHT_GREEN = "\033[92m";
  private static final String BRIGHT_YELLOW = "\033[93m";
  private static final String BRIGHT_BLUE = "\033[94m";
  private static final String BRIGHT_MAGENTA = "\033[95m";
  private static final String BRIGHT_CYAN = "\033[96m";
  private static final String RED = "\033[31m";
  private static final String GREEN = "\033[32m";
  private static final String YELLOW = "\033[33m";
  private static final String CYAN = "\033[36m";

  private String color(String text, String colorCode) {
    if (!useColors) {
      return text;
    }
    return colorCode + text + RESET;
  }
}
