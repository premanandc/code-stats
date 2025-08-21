package com.codestats.analysis;

import java.util.List;

/**
 * Business intelligence analysis of development team metrics. Transforms raw git statistics into
 * executive insights.
 */
public record BusinessAnalysis(
    ProjectSummary projectSummary,
    TeamComposition teamComposition,
    RiskAssessment riskAssessment,
    QualityMetrics qualityMetrics,
    TechnologyBreakdown technologyBreakdown,
    List<String> strategicRecommendations) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ProjectSummary projectSummary;
    private TeamComposition teamComposition;
    private RiskAssessment riskAssessment;
    private QualityMetrics qualityMetrics;
    private TechnologyBreakdown technologyBreakdown;
    private List<String> strategicRecommendations;

    public Builder projectSummary(ProjectSummary projectSummary) {
      this.projectSummary = projectSummary;
      return this;
    }

    public Builder teamComposition(TeamComposition teamComposition) {
      this.teamComposition = teamComposition;
      return this;
    }

    public Builder riskAssessment(RiskAssessment riskAssessment) {
      this.riskAssessment = riskAssessment;
      return this;
    }

    public Builder qualityMetrics(QualityMetrics qualityMetrics) {
      this.qualityMetrics = qualityMetrics;
      return this;
    }

    public Builder technologyBreakdown(TechnologyBreakdown technologyBreakdown) {
      this.technologyBreakdown = technologyBreakdown;
      return this;
    }

    public Builder strategicRecommendations(List<String> strategicRecommendations) {
      this.strategicRecommendations = strategicRecommendations;
      return this;
    }

    public BusinessAnalysis build() {
      return new BusinessAnalysis(
          projectSummary,
          teamComposition,
          riskAssessment,
          qualityMetrics,
          technologyBreakdown,
          strategicRecommendations);
    }
  }
}
