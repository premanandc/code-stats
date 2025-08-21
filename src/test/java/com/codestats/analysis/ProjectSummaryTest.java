package com.codestats.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectSummaryTest {

  @Test
  @DisplayName("Should format lines changed for small values")
  void shouldFormatLinesChangedForSmallValues() {
    ProjectSummary summary =
        new ProjectSummary(
            "TEST",
            10,
            5,
            500,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Test User",
            10,
            100.0);

    assertThat(summary.getFormattedLinesChanged()).isEqualTo("500");
  }

  @Test
  @DisplayName("Should format lines changed for large values in K")
  void shouldFormatLinesChangedForLargeValues() {
    ProjectSummary summary =
        new ProjectSummary(
            "TEST",
            100,
            10,
            15000,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Test User",
            100,
            100.0);

    assertThat(summary.getFormattedLinesChanged()).isEqualTo("15.0K");
  }

  @Test
  @DisplayName("Should format lines changed for exactly 1000")
  void shouldFormatLinesChangedForExactly1000() {
    ProjectSummary summary =
        new ProjectSummary(
            "TEST",
            50,
            5,
            1000,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Test User",
            50,
            100.0);

    assertThat(summary.getFormattedLinesChanged()).isEqualTo("1.0K");
  }

  @Test
  @DisplayName("Should provide velocity indicator for high activity")
  void shouldProvideVelocityIndicatorForHighActivity() {
    ProjectSummary summary =
        new ProjectSummary(
            "HIGH_VELOCITY",
            150,
            10,
            20000,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Active User",
            150,
            100.0);

    assertThat(summary.getVelocityIndicator()).isEqualTo("🚀 High");
  }

  @Test
  @DisplayName("Should provide velocity indicator for medium activity")
  void shouldProvideVelocityIndicatorForMediumActivity() {
    ProjectSummary summary =
        new ProjectSummary(
            "MEDIUM_VELOCITY",
            75,
            8,
            10000,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Medium User",
            75,
            100.0);

    assertThat(summary.getVelocityIndicator()).isEqualTo("📈 Medium");
  }

  @Test
  @DisplayName("Should provide velocity indicator for steady activity")
  void shouldProvideVelocityIndicatorForSteadyActivity() {
    ProjectSummary summary =
        new ProjectSummary(
            "STEADY_VELOCITY",
            25,
            3,
            2000,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Steady User",
            25,
            100.0);

    assertThat(summary.getVelocityIndicator()).isEqualTo("📊 Steady");
  }

  @Test
  @DisplayName("Should handle edge case with zero commits")
  void shouldHandleZeroCommits() {
    ProjectSummary summary =
        new ProjectSummary(
            "EMPTY",
            0,
            0,
            0,
            "empty period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "None",
            0,
            0.0);

    assertThat(summary.getVelocityIndicator()).isEqualTo("📊 Steady");
    assertThat(summary.getFormattedLinesChanged()).isEqualTo("0");
  }

  @Test
  @DisplayName("Should handle boundary values correctly")
  void shouldHandleBoundaryValues() {
    // Test boundary at 50 commits (medium threshold)
    ProjectSummary exactly50 =
        new ProjectSummary(
            "BOUNDARY_50",
            50,
            5,
            5000,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Boundary User",
            50,
            100.0);

    assertThat(exactly50.getVelocityIndicator()).isEqualTo("📊 Steady");

    // Test boundary at 51 commits (should be medium)
    ProjectSummary moreThan50 =
        new ProjectSummary(
            "BOUNDARY_51",
            51,
            5,
            5100,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Boundary User",
            51,
            100.0);

    assertThat(moreThan50.getVelocityIndicator()).isEqualTo("📈 Medium");

    // Test boundary at 100 commits (high threshold)
    ProjectSummary exactly100 =
        new ProjectSummary(
            "BOUNDARY_100",
            100,
            10,
            10000,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Boundary User",
            100,
            100.0);

    assertThat(exactly100.getVelocityIndicator()).isEqualTo("📈 Medium");

    // Test boundary at 101 commits (should be high)
    ProjectSummary moreThan100 =
        new ProjectSummary(
            "BOUNDARY_101",
            101,
            10,
            10100,
            "test period",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "Boundary User",
            101,
            100.0);

    assertThat(moreThan100.getVelocityIndicator()).isEqualTo("🚀 High");
  }
}
