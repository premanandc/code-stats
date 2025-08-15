package com.codestats.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodeStatsConfigTest {

  @Test
  @DisplayName("Should provide sensible default configuration")
  void shouldProvideDefaultConfiguration() {
    CodeStatsConfig config = CodeStatsConfig.getDefault();

    assertThat(config.languages()).containsKeys("java", "js", "py", "go", "rs");
    assertThat(config.languages().get("java")).isEqualTo("Java");
    assertThat(config.languages().get("ts")).isEqualTo("TypeScript");

    assertThat(config.productionDirectories()).contains("src", "lib", "app");
    assertThat(config.testDirectories()).contains("test", "tests", "__tests__", "spec");

    assertThat(config.aliases()).isEmpty();
  }

  @Test
  @DisplayName("Should merge configurations correctly")
  void shouldMergeConfigurations() {
    CodeStatsConfig base =
        new CodeStatsConfig(
            Map.of("java", "Java", "js", "JavaScript"),
            List.of("src"),
            List.of("test"),
            Map.of("alice@company.com", Set.of("alice@personal.com")));

    CodeStatsConfig override =
        new CodeStatsConfig(
            Map.of("py", "Python", "js", "ECMAScript"), // Override js mapping
            List.of("lib", "app"), // Replace production dirs
            List.of(), // Keep base test dirs (empty override)
            Map.of("bob@company.com", Set.of("bob@contractor.com")) // Add new alias
            );

    CodeStatsConfig merged = base.mergeWith(override);

    // Languages should be merged with override taking precedence
    assertThat(merged.languages()).containsKeys("java", "js", "py");
    assertThat(merged.languages().get("java")).isEqualTo("Java"); // From base
    assertThat(merged.languages().get("js")).isEqualTo("ECMAScript"); // Overridden
    assertThat(merged.languages().get("py")).isEqualTo("Python"); // From override

    // Production directories replaced (non-empty override)
    assertThat(merged.productionDirectories()).containsExactly("lib", "app");

    // Test directories kept from base (empty override)
    assertThat(merged.testDirectories()).containsExactly("test");

    // Aliases merged
    assertThat(merged.aliases()).hasSize(2);
    assertThat(merged.aliases()).containsKeys("alice@company.com", "bob@company.com");
  }

  @Test
  @DisplayName("Should handle empty configurations in merge")
  void shouldHandleEmptyConfigurationsMerge() {
    CodeStatsConfig base = CodeStatsConfig.getDefault();
    CodeStatsConfig empty = new CodeStatsConfig(Map.of(), List.of(), List.of(), Map.of());

    CodeStatsConfig merged = base.mergeWith(empty);

    // Should keep base values when override is empty
    assertThat(merged.languages()).isEqualTo(base.languages());
    assertThat(merged.productionDirectories()).isEqualTo(base.productionDirectories());
    assertThat(merged.testDirectories()).isEqualTo(base.testDirectories());
    assertThat(merged.aliases()).isEqualTo(base.aliases());
  }

  @Test
  @DisplayName("Should handle language extensions correctly")
  void shouldHandleLanguageExtensions() {
    CodeStatsConfig config = CodeStatsConfig.getDefault();

    // Common web extensions
    assertThat(config.languages().get("js")).isEqualTo("JavaScript");
    assertThat(config.languages().get("jsx")).isEqualTo("JavaScript");
    assertThat(config.languages().get("ts")).isEqualTo("TypeScript");
    assertThat(config.languages().get("tsx")).isEqualTo("TypeScript");

    // Backend languages
    assertThat(config.languages().get("java")).isEqualTo("Java");
    assertThat(config.languages().get("py")).isEqualTo("Python");
    assertThat(config.languages().get("go")).isEqualTo("Go");
    assertThat(config.languages().get("rs")).isEqualTo("Rust");

    // Config formats
    assertThat(config.languages().get("yaml")).isEqualTo("YAML");
    assertThat(config.languages().get("yml")).isEqualTo("YAML");
    assertThat(config.languages().get("json")).isEqualTo("JSON");
  }

  @Test
  @DisplayName("Should include comprehensive directory patterns")
  void shouldIncludeComprehensiveDirectoryPatterns() {
    CodeStatsConfig config = CodeStatsConfig.getDefault();

    // Production patterns should cover common structures
    assertThat(config.productionDirectories()).contains("src", "lib", "app", "source", "main");

    // Test patterns should cover various testing frameworks
    assertThat(config.testDirectories())
        .contains("test", "tests", "__tests__", "spec", "specs", "cypress", "e2e");
  }
}
