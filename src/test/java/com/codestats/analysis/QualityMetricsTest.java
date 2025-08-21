package com.codestats.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.analysis.QualityMetrics.QualityLevel;

class QualityMetricsTest {

  @Test
  @DisplayName("Should provide test coverage insight for exceptional coverage")
  void shouldProvideTestCoverageInsightForExceptional() {
    QualityMetrics metrics =
        new QualityMetrics(
            85.0, 1000, 800, 100, QualityLevel.EXCELLENT, QualityLevel.GOOD, "Test insight");

    assertThat(metrics.getTestCoverageInsight()).isEqualTo("Exceptional testing discipline");
  }

  @Test
  @DisplayName("Should provide test coverage insight for strong coverage")
  void shouldProvideTestCoverageInsightForStrong() {
    QualityMetrics metrics =
        new QualityMetrics(
            70.0, 1000, 700, 100, QualityLevel.GOOD, QualityLevel.GOOD, "Test insight");

    assertThat(metrics.getTestCoverageInsight()).isEqualTo("Strong testing culture");
  }

  @Test
  @DisplayName("Should provide test coverage insight for moderate coverage")
  void shouldProvideTestCoverageInsightForModerate() {
    QualityMetrics metrics =
        new QualityMetrics(
            50.0, 1000, 500, 100, QualityLevel.MODERATE, QualityLevel.MODERATE, "Test insight");

    assertThat(metrics.getTestCoverageInsight()).isEqualTo("Moderate test coverage");
  }

  @Test
  @DisplayName("Should provide test coverage insight for needs improvement")
  void shouldProvideTestCoverageInsightForNeedsImprovement() {
    QualityMetrics metrics =
        new QualityMetrics(
            30.0, 1000, 300, 50, QualityLevel.POOR, QualityLevel.POOR, "Test insight");

    assertThat(metrics.getTestCoverageInsight()).isEqualTo("Testing needs improvement");
  }

  @Test
  @DisplayName("Should handle boundary values for test coverage insights")
  void shouldHandleBoundaryValuesForTestCoverageInsights() {
    // Test exactly 80% (should be exceptional)
    QualityMetrics exactly80 =
        new QualityMetrics(
            80.0, 1000, 800, 100, QualityLevel.EXCELLENT, QualityLevel.GOOD, "Test insight");
    assertThat(exactly80.getTestCoverageInsight()).isEqualTo("Exceptional testing discipline");

    // Test just under 80% (should be strong)
    QualityMetrics under80 =
        new QualityMetrics(
            79.9, 1000, 799, 100, QualityLevel.GOOD, QualityLevel.GOOD, "Test insight");
    assertThat(under80.getTestCoverageInsight()).isEqualTo("Strong testing culture");

    // Test exactly 60% (should be strong)
    QualityMetrics exactly60 =
        new QualityMetrics(
            60.0, 1000, 600, 100, QualityLevel.GOOD, QualityLevel.GOOD, "Test insight");
    assertThat(exactly60.getTestCoverageInsight()).isEqualTo("Strong testing culture");

    // Test just under 60% (should be moderate)
    QualityMetrics under60 =
        new QualityMetrics(
            59.9, 1000, 599, 100, QualityLevel.MODERATE, QualityLevel.MODERATE, "Test insight");
    assertThat(under60.getTestCoverageInsight()).isEqualTo("Moderate test coverage");

    // Test exactly 40% (should be moderate)
    QualityMetrics exactly40 =
        new QualityMetrics(
            40.0, 1000, 400, 100, QualityLevel.MODERATE, QualityLevel.MODERATE, "Test insight");
    assertThat(exactly40.getTestCoverageInsight()).isEqualTo("Moderate test coverage");

    // Test just under 40% (should need improvement)
    QualityMetrics under40 =
        new QualityMetrics(
            39.9, 1000, 399, 100, QualityLevel.POOR, QualityLevel.POOR, "Test insight");
    assertThat(under40.getTestCoverageInsight()).isEqualTo("Testing needs improvement");
  }

  @Test
  @DisplayName("Should provide documentation insight for well documented codebase")
  void shouldProvideDocumentationInsightForWellDocumented() {
    QualityMetrics metrics =
        new QualityMetrics(
            70.0,
            1000,
            700,
            100, // 100/(1000+700)*100 = 5.88% doc ratio
            QualityLevel.GOOD,
            QualityLevel.EXCELLENT,
            "Test insight");

    assertThat(metrics.getDocumentationInsight()).isEqualTo("Well documented codebase");
  }

  @Test
  @DisplayName("Should provide documentation insight for adequate documentation")
  void shouldProvideDocumentationInsightForAdequate() {
    QualityMetrics metrics =
        new QualityMetrics(
            70.0,
            1000,
            700,
            50, // 50/(1000+700)*100 = 2.94% doc ratio
            QualityLevel.GOOD,
            QualityLevel.GOOD,
            "Test insight");

    assertThat(metrics.getDocumentationInsight()).isEqualTo("Adequate documentation");
  }

  @Test
  @DisplayName("Should provide documentation insight for could be improved")
  void shouldProvideDocumentationInsightForCouldBeImproved() {
    QualityMetrics metrics =
        new QualityMetrics(
            70.0,
            1000,
            700,
            20, // 20/(1000+700)*100 = 1.18% doc ratio
            QualityLevel.GOOD,
            QualityLevel.POOR,
            "Test insight");

    assertThat(metrics.getDocumentationInsight()).isEqualTo("Documentation could be improved");
  }

  @Test
  @DisplayName("Should handle boundary values for documentation insights")
  void shouldHandleBoundaryValuesForDocumentationInsights() {
    // Test exactly 5% doc ratio (should be well documented)
    QualityMetrics exactly5Percent =
        new QualityMetrics(
            70.0,
            1000,
            700,
            85, // 85/(1000+700)*100 = 5.0%
            QualityLevel.GOOD,
            QualityLevel.EXCELLENT,
            "Test insight");
    assertThat(exactly5Percent.getDocumentationInsight()).isEqualTo("Well documented codebase");

    // Test just under 5% (should be adequate)
    QualityMetrics under5Percent =
        new QualityMetrics(
            70.0,
            1000,
            700,
            84, // 84/(1000+700)*100 = 4.94%
            QualityLevel.GOOD,
            QualityLevel.GOOD,
            "Test insight");
    assertThat(under5Percent.getDocumentationInsight()).isEqualTo("Adequate documentation");

    // Test exactly 2% (should be adequate)
    QualityMetrics exactly2Percent =
        new QualityMetrics(
            70.0,
            1000,
            700,
            34, // 34/(1000+700)*100 = 2.0%
            QualityLevel.GOOD,
            QualityLevel.GOOD,
            "Test insight");
    assertThat(exactly2Percent.getDocumentationInsight()).isEqualTo("Adequate documentation");

    // Test just under 2% (should need improvement)
    QualityMetrics under2Percent =
        new QualityMetrics(
            70.0,
            1000,
            700,
            33, // 33/(1000+700)*100 = 1.94%
            QualityLevel.GOOD,
            QualityLevel.POOR,
            "Test insight");
    assertThat(under2Percent.getDocumentationInsight())
        .isEqualTo("Documentation could be improved");
  }

  @Test
  @DisplayName("Should calculate total code lines correctly")
  void shouldCalculateTotalCodeLines() {
    QualityMetrics metrics =
        new QualityMetrics(
            70.0, 1500, 900, 200, QualityLevel.GOOD, QualityLevel.GOOD, "Test insight");

    assertThat(metrics.getTotalCodeLines()).isEqualTo(2400); // 1500 + 900
  }

  @Test
  @DisplayName("Should test QualityLevel enum functionality")
  void shouldTestQualityLevelEnum() {
    // Test all quality levels have proper indicators and descriptions
    assertThat(QualityLevel.EXCELLENT.getIndicator()).isEqualTo("✅ Excellent");
    assertThat(QualityLevel.EXCELLENT.getDescription()).isEqualTo("Industry best practices");

    assertThat(QualityLevel.GOOD.getIndicator()).isEqualTo("🟢 Good");
    assertThat(QualityLevel.GOOD.getDescription()).isEqualTo("Above average standards");

    assertThat(QualityLevel.MODERATE.getIndicator()).isEqualTo("🟡 Moderate");
    assertThat(QualityLevel.MODERATE.getDescription()).isEqualTo("Room for improvement");

    assertThat(QualityLevel.POOR.getIndicator()).isEqualTo("🔴 Poor");
    assertThat(QualityLevel.POOR.getDescription()).isEqualTo("Needs immediate attention");
  }

  @Test
  @DisplayName("Should handle edge case with zero code lines")
  void shouldHandleZeroCodeLines() {
    QualityMetrics metrics =
        new QualityMetrics(0.0, 0, 0, 0, QualityLevel.POOR, QualityLevel.POOR, "Empty project");

    assertThat(metrics.getTotalCodeLines()).isEqualTo(0);
    assertThat(metrics.getDocumentationInsight()).isEqualTo("Documentation could be improved");
    assertThat(metrics.getTestCoverageInsight()).isEqualTo("Testing needs improvement");
  }
}
