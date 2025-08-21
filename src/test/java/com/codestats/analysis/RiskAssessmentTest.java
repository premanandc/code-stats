package com.codestats.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.analysis.RiskAssessment.RiskLevel;

class RiskAssessmentTest {

  @Test
  @DisplayName("Should provide bus factor description for single point of failure")
  void shouldProvideBusFactorDescriptionForSinglePoint() {
    RiskAssessment assessment =
        new RiskAssessment(
            RiskLevel.CRITICAL,
            1,
            80.0,
            List.of("Critical risk"),
            List.of(),
            List.of("Mitigation"));

    assertThat(assessment.getBusFactorDescription()).isEqualTo("⚠️ Single point of failure");
  }

  @Test
  @DisplayName("Should provide bus factor description for limited backup")
  void shouldProvideBusFactorDescriptionForLimitedBackup() {
    RiskAssessment assessment =
        new RiskAssessment(
            RiskLevel.HIGH, 2, 60.0, List.of(), List.of("Medium risk"), List.of("Mitigation"));

    assertThat(assessment.getBusFactorDescription()).isEqualTo("⚠️ Limited backup coverage");
  }

  @Test
  @DisplayName("Should provide bus factor description for adequate coverage")
  void shouldProvideBusFactorDescriptionForAdequateCoverage() {
    RiskAssessment assessment =
        new RiskAssessment(
            RiskLevel.MEDIUM, 3, 40.0, List.of(), List.of("Medium risk"), List.of("Mitigation"));

    assertThat(assessment.getBusFactorDescription()).isEqualTo("✅ Adequate team coverage");

    // Test with 4 as well (should also be adequate)
    RiskAssessment assessment4 =
        new RiskAssessment(RiskLevel.MEDIUM, 4, 35.0, List.of(), List.of(), List.of("Mitigation"));

    assertThat(assessment4.getBusFactorDescription()).isEqualTo("✅ Adequate team coverage");
  }

  @Test
  @DisplayName("Should provide bus factor description for strong resilience")
  void shouldProvideBusFactorDescriptionForStrongResilience() {
    RiskAssessment assessment =
        new RiskAssessment(RiskLevel.LOW, 5, 25.0, List.of(), List.of(), List.of("Mitigation"));

    assertThat(assessment.getBusFactorDescription()).isEqualTo("✅ Strong team resilience");

    // Test with even higher bus factor
    RiskAssessment assessment10 =
        new RiskAssessment(RiskLevel.LOW, 10, 15.0, List.of(), List.of(), List.of("Mitigation"));

    assertThat(assessment10.getBusFactorDescription()).isEqualTo("✅ Strong team resilience");
  }

  @Test
  @DisplayName("Should provide concentration warning for critical levels")
  void shouldProvideConcentrationWarningForCritical() {
    RiskAssessment assessment =
        new RiskAssessment(
            RiskLevel.CRITICAL,
            1,
            75.0,
            List.of("Critical risk"),
            List.of(),
            List.of("Mitigation"));

    String warning = assessment.getConcentrationWarning();
    assertThat(warning).startsWith("Critical:");
    assertThat(warning).contains("75% concentrated in top contributor");
  }

  @Test
  @DisplayName("Should provide concentration warning for moderate levels")
  void shouldProvideConcentrationWarningForModerate() {
    RiskAssessment assessment =
        new RiskAssessment(
            RiskLevel.MEDIUM, 2, 50.0, List.of(), List.of("Medium risk"), List.of("Mitigation"));

    String warning = assessment.getConcentrationWarning();
    assertThat(warning).startsWith("Moderate:");
    assertThat(warning).contains("50% concentrated in top contributor");
  }

  @Test
  @DisplayName("Should provide concentration warning for well distributed")
  void shouldProvideConcentrationWarningForWellDistributed() {
    RiskAssessment assessment =
        new RiskAssessment(RiskLevel.LOW, 5, 25.0, List.of(), List.of(), List.of("Mitigation"));

    assertThat(assessment.getConcentrationWarning())
        .isEqualTo("Well distributed knowledge across team");
  }

  @Test
  @DisplayName("Should handle boundary values for concentration warnings")
  void shouldHandleBoundaryValuesForConcentrationWarnings() {
    // Test exactly 40% (should be well distributed)
    RiskAssessment exactly40 =
        new RiskAssessment(RiskLevel.LOW, 3, 40.0, List.of(), List.of(), List.of("Mitigation"));

    assertThat(exactly40.getConcentrationWarning())
        .isEqualTo("Well distributed knowledge across team");

    // Test just over 40% (should be moderate)
    RiskAssessment over40 =
        new RiskAssessment(RiskLevel.MEDIUM, 3, 40.1, List.of(), List.of(), List.of("Mitigation"));

    assertThat(over40.getConcentrationWarning()).startsWith("Moderate:");

    // Test exactly 60% (should be moderate)
    RiskAssessment exactly60 =
        new RiskAssessment(RiskLevel.MEDIUM, 2, 60.0, List.of(), List.of(), List.of("Mitigation"));

    assertThat(exactly60.getConcentrationWarning()).startsWith("Moderate:");

    // Test just over 60% (should be critical)
    RiskAssessment over60 =
        new RiskAssessment(
            RiskLevel.CRITICAL, 1, 60.1, List.of(), List.of(), List.of("Mitigation"));

    assertThat(over60.getConcentrationWarning()).startsWith("Critical:");
  }

  @Test
  @DisplayName("Should test RiskLevel enum functionality")
  void shouldTestRiskLevelEnum() {
    // Test all risk levels have proper display components
    assertThat(RiskLevel.LOW.getEmoji()).isEqualTo("🟢");
    assertThat(RiskLevel.LOW.getLabel()).isEqualTo("Low Risk");
    assertThat(RiskLevel.LOW.getDisplayName()).isEqualTo("🟢 Low Risk");

    assertThat(RiskLevel.MEDIUM.getEmoji()).isEqualTo("🟡");
    assertThat(RiskLevel.MEDIUM.getLabel()).isEqualTo("Medium Risk");
    assertThat(RiskLevel.MEDIUM.getDisplayName()).isEqualTo("🟡 Medium Risk");

    assertThat(RiskLevel.HIGH.getEmoji()).isEqualTo("🟠");
    assertThat(RiskLevel.HIGH.getLabel()).isEqualTo("High Risk");
    assertThat(RiskLevel.HIGH.getDisplayName()).isEqualTo("🟠 High Risk");

    assertThat(RiskLevel.CRITICAL.getEmoji()).isEqualTo("🔴");
    assertThat(RiskLevel.CRITICAL.getLabel()).isEqualTo("Critical Risk");
    assertThat(RiskLevel.CRITICAL.getDisplayName()).isEqualTo("🔴 Critical Risk");
  }

  @Test
  @DisplayName("Should handle edge case with zero concentration")
  void shouldHandleZeroConcentration() {
    RiskAssessment assessment =
        new RiskAssessment(RiskLevel.LOW, 5, 0.0, List.of(), List.of(), List.of("Mitigation"));

    assertThat(assessment.getConcentrationWarning())
        .isEqualTo("Well distributed knowledge across team");
  }
}
