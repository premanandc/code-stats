package com.codestats.analysis;

/** Code quality and development practice indicators. */
public record QualityMetrics(
    double testCoveragePercentage,
    int productionLines,
    int testLines,
    int documentationLines,
    QualityLevel testingCulture,
    QualityLevel documentationLevel,
    String qualityInsight) {

  public enum QualityLevel {
    EXCELLENT("✅ Excellent", "Industry best practices"),
    GOOD("🟢 Good", "Above average standards"),
    MODERATE("🟡 Moderate", "Room for improvement"),
    POOR("🔴 Poor", "Needs immediate attention");

    private final String indicator;
    private final String description;

    QualityLevel(String indicator, String description) {
      this.indicator = indicator;
      this.description = description;
    }

    public String getIndicator() {
      return indicator;
    }

    public String getDescription() {
      return description;
    }
  }

  public String getTestCoverageInsight() {
    if (testCoveragePercentage >= 80) {
      return "Exceptional testing discipline";
    } else if (testCoveragePercentage >= 60) {
      return "Strong testing culture";
    } else if (testCoveragePercentage >= 40) {
      return "Moderate test coverage";
    }
    return "Testing needs improvement";
  }

  public String getDocumentationInsight() {
    double docRatio = (double) documentationLines / (productionLines + testLines) * 100;
    if (docRatio >= 5) {
      return "Well documented codebase";
    } else if (docRatio >= 2) {
      return "Adequate documentation";
    }
    return "Documentation could be improved";
  }

  public int getTotalCodeLines() {
    return productionLines + testLines;
  }
}
