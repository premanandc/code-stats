package com.codestats.analysis;

import java.util.List;

/** Assessment of development risks and team dependencies. */
public record RiskAssessment(
    RiskLevel overallRisk,
    int busFactor,
    double knowledgeConcentration,
    List<String> criticalRisks,
    List<String> mediumRisks,
    List<String> mitigationStrategies) {

  public enum RiskLevel {
    LOW("🟢", "Low Risk"),
    MEDIUM("🟡", "Medium Risk"),
    HIGH("🟠", "High Risk"),
    CRITICAL("🔴", "Critical Risk");

    private final String emoji;
    private final String label;

    RiskLevel(String emoji, String label) {
      this.emoji = emoji;
      this.label = label;
    }

    public String getEmoji() {
      return emoji;
    }

    public String getLabel() {
      return label;
    }

    public String getDisplayName() {
      return emoji + " " + label;
    }
  }

  public String getBusFactorDescription() {
    return switch (busFactor) {
      case 1 -> "⚠️ Single point of failure";
      case 2 -> "⚠️ Limited backup coverage";
      case 3, 4 -> "✅ Adequate team coverage";
      default -> "✅ Strong team resilience";
    };
  }

  public String getConcentrationWarning() {
    if (knowledgeConcentration > 60) {
      return "Critical: "
          + String.format("%.0f%%", knowledgeConcentration)
          + " concentrated in top contributor";
    } else if (knowledgeConcentration > 40) {
      return "Moderate: "
          + String.format("%.0f%%", knowledgeConcentration)
          + " concentrated in top contributor";
    }
    return "Well distributed knowledge across team";
  }
}
