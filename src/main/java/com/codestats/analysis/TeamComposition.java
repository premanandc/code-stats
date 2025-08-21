package com.codestats.analysis;

import java.util.List;

/** Analysis of team structure and contribution patterns. */
public record TeamComposition(
    List<ContributorProfile> contributors,
    int totalMembers,
    int activeMembers,
    String teamSizeRecommendation) {

  public record ContributorProfile(
      String name,
      String email,
      int commits,
      double commitPercentage,
      int linesChanged,
      ContributorRole role,
      ContributorImpact impact,
      List<String> primaryTechnologies) {}

  public enum ContributorRole {
    TECH_LEAD("Tech Lead"),
    SENIOR_DEVELOPER("Senior Developer"),
    DEVELOPER("Developer"),
    JUNIOR_DEVELOPER("Junior Developer"),
    SPECIALIST("Specialist"),
    CONTRIBUTOR("Contributor");

    private final String displayName;

    ContributorRole(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  public enum ContributorImpact {
    HIGH("⭐ High Impact", "Drives major features and architecture"),
    MEDIUM("📊 Medium Impact", "Contributes to features and maintenance"),
    LOW("📝 Low Impact", "Supports specific areas or occasional contributions");

    private final String label;
    private final String description;

    ContributorImpact(String label, String description) {
      this.label = label;
      this.description = description;
    }

    public String getLabel() {
      return label;
    }

    public String getDescription() {
      return description;
    }
  }

  public List<ContributorProfile> getHighImpactContributors() {
    return contributors.stream().filter(c -> c.impact() == ContributorImpact.HIGH).toList();
  }

  public String getTeamHealthIndicator() {
    long highImpact =
        contributors.stream().mapToLong(c -> c.impact() == ContributorImpact.HIGH ? 1 : 0).sum();

    if (highImpact == 1 && totalMembers < 5) {
      return "⚠️ Team at risk - single high-impact contributor";
    } else if (highImpact >= 2) {
      return "✅ Healthy team structure";
    }
    return "📊 Developing team - needs senior leadership";
  }
}
