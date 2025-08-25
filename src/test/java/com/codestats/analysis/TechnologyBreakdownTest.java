package com.codestats.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codestats.analysis.TechnologyBreakdown.TechnologyCategory;
import com.codestats.analysis.TechnologyBreakdown.TechnologyEntry;

class TechnologyBreakdownTest {

  @Test
  @DisplayName("Should get top technologies correctly sorted by lines of code")
  void shouldGetTopTechnologiesSorted() {
    List<TechnologyEntry> technologies =
        List.of(
            new TechnologyEntry("Java", 5000, 50.0, TechnologyCategory.BACKEND, "Backend value"),
            new TechnologyEntry(
                "JavaScript", 3000, 30.0, TechnologyCategory.FRONTEND, "Frontend value"),
            new TechnologyEntry("Python", 1500, 15.0, TechnologyCategory.BACKEND, "Backend value"),
            new TechnologyEntry("CSS", 500, 5.0, TechnologyCategory.FRONTEND, "Frontend value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(technologies, Map.of(), "Full-stack", "Modern", List.of());

    List<TechnologyEntry> top3 = breakdown.getTopTechnologies(3);

    assertThat(top3).hasSize(3);
    assertThat(top3.get(0).language()).isEqualTo("Java");
    assertThat(top3.get(1).language()).isEqualTo("JavaScript");
    assertThat(top3.get(2).language()).isEqualTo("Python");
  }

  @Test
  @DisplayName("Should get top technologies with limit larger than available")
  void shouldGetTopTechnologiesWithLargeLimit() {
    List<TechnologyEntry> technologies =
        List.of(
            new TechnologyEntry("Java", 5000, 100.0, TechnologyCategory.BACKEND, "Backend value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(technologies, Map.of(), "Backend", "Java-based", List.of());

    List<TechnologyEntry> top10 = breakdown.getTopTechnologies(10);

    assertThat(top10).hasSize(1);
    assertThat(top10.get(0).language()).isEqualTo("Java");
  }

  @Test
  @DisplayName("Should provide stack maturity for modern well-architected stack")
  void shouldProvideStackMaturityForModernStack() {
    List<TechnologyEntry> modernStack =
        List.of(
            new TechnologyEntry(
                "TypeScript", 3000, 40.0, TechnologyCategory.FRONTEND, "Frontend value"),
            new TechnologyEntry("Java", 3000, 40.0, TechnologyCategory.BACKEND, "Backend value"),
            new TechnologyEntry("JUnit", 1500, 20.0, TechnologyCategory.TESTING, "Testing value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(modernStack, Map.of(), "Full-stack", "Modern", List.of());

    assertThat(breakdown.getStackMaturity()).isEqualTo("🚀 Modern, well-architected stack");
  }

  @Test
  @DisplayName("Should provide stack maturity for solid foundation with React frontend")
  void shouldProvideStackMaturityForSolidFoundationWithReact() {
    List<TechnologyEntry> reactStack =
        List.of(
            new TechnologyEntry("React", 3000, 50.0, TechnologyCategory.FRONTEND, "Frontend value"),
            new TechnologyEntry("Python", 2000, 33.33, TechnologyCategory.BACKEND, "Backend value"),
            new TechnologyEntry(
                "pytest", 1000, 16.67, TechnologyCategory.TESTING, "Testing value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(reactStack, Map.of(), "Full-stack", "Modern", List.of());

    assertThat(breakdown.getStackMaturity()).isEqualTo("🚀 Modern, well-architected stack");
  }

  @Test
  @DisplayName("Should provide stack maturity for solid foundation needing frontend modernization")
  void shouldProvideStackMaturityForSolidFoundation() {
    List<TechnologyEntry> backendStack =
        List.of(
            new TechnologyEntry("Java", 4000, 66.67, TechnologyCategory.BACKEND, "Backend value"),
            new TechnologyEntry("JUnit", 2000, 33.33, TechnologyCategory.TESTING, "Testing value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(backendStack, Map.of(), "Backend", "Java-based", List.of());

    assertThat(breakdown.getStackMaturity())
        .isEqualTo("🏗️ Solid foundation with room for frontend modernization");
  }

  @Test
  @DisplayName("Should provide stack maturity for developing stack")
  void shouldProvideStackMaturityForDevelopingStack() {
    List<TechnologyEntry> basicStack =
        List.of(
            new TechnologyEntry(
                "JavaScript", 3000, 75.0, TechnologyCategory.FRONTEND, "Frontend value"),
            new TechnologyEntry("CSS", 1000, 25.0, TechnologyCategory.FRONTEND, "Frontend value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(basicStack, Map.of(), "Frontend", "JavaScript-based", List.of());

    assertThat(breakdown.getStackMaturity())
        .isEqualTo("📊 Developing stack - opportunities for modernization");
  }

  @Test
  @DisplayName("Should test TechnologyCategory enum functionality")
  void shouldTestTechnologyCategoryEnum() {
    assertThat(TechnologyCategory.BACKEND.getName()).isEqualTo("Backend");
    assertThat(TechnologyCategory.BACKEND.getEmoji()).isEqualTo("🔧");
    assertThat(TechnologyCategory.BACKEND.getDescription()).isEqualTo("Server-side logic and APIs");
    assertThat(TechnologyCategory.BACKEND.getDisplayName()).isEqualTo("🔧 Backend");

    assertThat(TechnologyCategory.FRONTEND.getName()).isEqualTo("Frontend");
    assertThat(TechnologyCategory.FRONTEND.getEmoji()).isEqualTo("🎨");
    assertThat(TechnologyCategory.FRONTEND.getDescription())
        .isEqualTo("User interface and experience");
    assertThat(TechnologyCategory.FRONTEND.getDisplayName()).isEqualTo("🎨 Frontend");

    assertThat(TechnologyCategory.MOBILE.getName()).isEqualTo("Mobile");
    assertThat(TechnologyCategory.MOBILE.getEmoji()).isEqualTo("📱");
    assertThat(TechnologyCategory.MOBILE.getDescription())
        .isEqualTo("Mobile application development");
    assertThat(TechnologyCategory.MOBILE.getDisplayName()).isEqualTo("📱 Mobile");

    assertThat(TechnologyCategory.DEVOPS.getName()).isEqualTo("DevOps");
    assertThat(TechnologyCategory.DEVOPS.getEmoji()).isEqualTo("⚙️");
    assertThat(TechnologyCategory.DEVOPS.getDescription())
        .isEqualTo("Infrastructure and deployment");
    assertThat(TechnologyCategory.DEVOPS.getDisplayName()).isEqualTo("⚙️ DevOps");

    assertThat(TechnologyCategory.DATA.getName()).isEqualTo("Data");
    assertThat(TechnologyCategory.DATA.getEmoji()).isEqualTo("📊");
    assertThat(TechnologyCategory.DATA.getDescription()).isEqualTo("Data processing and analytics");
    assertThat(TechnologyCategory.DATA.getDisplayName()).isEqualTo("📊 Data");

    assertThat(TechnologyCategory.TESTING.getName()).isEqualTo("Testing");
    assertThat(TechnologyCategory.TESTING.getEmoji()).isEqualTo("🧪");
    assertThat(TechnologyCategory.TESTING.getDescription())
        .isEqualTo("Quality assurance and testing");
    assertThat(TechnologyCategory.TESTING.getDisplayName()).isEqualTo("🧪 Testing");

    assertThat(TechnologyCategory.CONFIG.getName()).isEqualTo("Configuration");
    assertThat(TechnologyCategory.CONFIG.getEmoji()).isEqualTo("⚙️");
    assertThat(TechnologyCategory.CONFIG.getDescription())
        .isEqualTo("Setup and configuration files");
    assertThat(TechnologyCategory.CONFIG.getDisplayName()).isEqualTo("⚙️ Configuration");

    assertThat(TechnologyCategory.DOCUMENTATION.getName()).isEqualTo("Documentation");
    assertThat(TechnologyCategory.DOCUMENTATION.getEmoji()).isEqualTo("📚");
    assertThat(TechnologyCategory.DOCUMENTATION.getDescription())
        .isEqualTo("Project documentation");
    assertThat(TechnologyCategory.DOCUMENTATION.getDisplayName()).isEqualTo("📚 Documentation");
  }

  @Test
  @DisplayName("Should handle empty technology list")
  void shouldHandleEmptyTechnologyList() {
    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(List.of(), Map.of(), "Unknown", "Unknown", List.of());

    assertThat(breakdown.getTopTechnologies(5)).isEmpty();
    assertThat(breakdown.getStackMaturity())
        .isEqualTo("📊 Developing stack - opportunities for modernization");
  }

  @Test
  @DisplayName("Should handle edge case with only testing framework")
  void shouldHandleOnlyTestingFramework() {
    List<TechnologyEntry> testingOnly =
        List.of(
            new TechnologyEntry("JUnit", 1000, 100.0, TechnologyCategory.TESTING, "Testing value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(testingOnly, Map.of(), "Testing", "Test-focused", List.of());

    // Has testing framework but no robust backend or modern frontend
    assertThat(breakdown.getStackMaturity())
        .isEqualTo("📊 Developing stack - opportunities for modernization");
  }

  @Test
  @DisplayName("Should handle stack maturity edge cases")
  void shouldHandleStackMaturityEdgeCases() {
    // Test case: modern frontend + robust backend but no testing
    List<TechnologyEntry> noTestingStack =
        List.of(
            new TechnologyEntry(
                "TypeScript", 3000, 60.0, TechnologyCategory.FRONTEND, "Frontend value"),
            new TechnologyEntry("Java", 2000, 40.0, TechnologyCategory.BACKEND, "Backend value"));

    TechnologyBreakdown noTestingBreakdown =
        new TechnologyBreakdown(noTestingStack, Map.of(), "Full-stack", "Modern", List.of());

    // Should not be considered "well-architected" without testing - defaults to developing
    assertThat(noTestingBreakdown.getStackMaturity())
        .isEqualTo("📊 Developing stack - opportunities for modernization");
  }

  @Test
  @DisplayName("Should sort technologies correctly with equal lines of code")
  void shouldSortTechnologiesWithEqualLines() {
    List<TechnologyEntry> mixedTechnologies =
        List.of(
            new TechnologyEntry("Python", 2000, 40.0, TechnologyCategory.BACKEND, "Backend value"),
            new TechnologyEntry("Java", 1000, 20.0, TechnologyCategory.BACKEND, "Backend value"),
            new TechnologyEntry(
                "JavaScript", 1000, 20.0, TechnologyCategory.FRONTEND, "Frontend value"),
            new TechnologyEntry("CSS", 1000, 20.0, TechnologyCategory.FRONTEND, "Frontend value"));

    TechnologyBreakdown breakdown =
        new TechnologyBreakdown(mixedTechnologies, Map.of(), "Full-stack", "Mixed", List.of());

    List<TechnologyEntry> sorted = breakdown.getTopTechnologies(4);

    // Should maintain stable sort order: highest lines first, then original order for equal values
    assertThat(sorted).hasSize(4);
    assertThat(sorted.get(0).language()).isEqualTo("Python"); // 2000 lines (highest)
    // The remaining three (Java, JavaScript, CSS) all have 1000 lines
    // The comparator should return 0 for equal values, maintaining stable sort
    List<String> equalLineLanguages =
        sorted.subList(1, 4).stream().map(TechnologyEntry::language).toList();
    assertThat(equalLineLanguages).containsExactly("Java", "JavaScript", "CSS");
  }
}
