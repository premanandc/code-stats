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

    assertThat(config.extensions()).containsKeys("java", "js", "py", "go", "rs");
    assertThat(config.extensions().get("java")).isEqualTo("Java");
    assertThat(config.extensions().get("ts")).isEqualTo("TypeScript");

    assertThat(config.filenames()).containsKeys("dockerfile", "makefile");
    assertThat(config.filenames().get("dockerfile")).isEqualTo("Docker");

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
            Map.of("dockerfile", "Docker"),
            List.of("src"),
            List.of("test"),
            Map.of("alice@company.com", Set.of("alice@personal.com")));

    CodeStatsConfig override =
        new CodeStatsConfig(
            Map.of("py", "Python", "js", "ECMAScript"), // Override js mapping
            Map.of("makefile", "Makefile"), // Override filename mapping
            List.of("lib", "app"), // Replace production dirs
            List.of(), // Keep base test dirs (empty override)
            Map.of("bob@company.com", Set.of("bob@contractor.com")) // Add new alias
            );

    CodeStatsConfig merged = base.mergeWith(override);

    // Extensions should be merged with override taking precedence
    assertThat(merged.extensions()).containsKeys("java", "js", "py");
    assertThat(merged.extensions().get("java")).isEqualTo("Java"); // From base
    assertThat(merged.extensions().get("js")).isEqualTo("ECMAScript"); // Overridden
    assertThat(merged.extensions().get("py")).isEqualTo("Python"); // From override

    // Filenames should be merged with override taking precedence
    assertThat(merged.filenames()).containsKeys("dockerfile", "makefile");
    assertThat(merged.filenames().get("dockerfile")).isEqualTo("Docker"); // From base
    assertThat(merged.filenames().get("makefile")).isEqualTo("Makefile"); // From override

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
    CodeStatsConfig empty = new CodeStatsConfig(Map.of(), Map.of(), List.of(), List.of(), Map.of());

    CodeStatsConfig merged = base.mergeWith(empty);

    // Should keep base values when override is empty
    assertThat(merged.extensions()).isEqualTo(base.extensions());
    assertThat(merged.filenames()).isEqualTo(base.filenames());
    assertThat(merged.productionDirectories()).isEqualTo(base.productionDirectories());
    assertThat(merged.testDirectories()).isEqualTo(base.testDirectories());
    assertThat(merged.aliases()).isEqualTo(base.aliases());
  }

  @Test
  @DisplayName("Should handle language extensions correctly")
  void shouldHandleLanguageExtensions() {
    CodeStatsConfig config = CodeStatsConfig.getDefault();

    // Common web extensions
    assertThat(config.extensions().get("js")).isEqualTo("JavaScript");
    assertThat(config.extensions().get("jsx")).isEqualTo("JavaScript");
    assertThat(config.extensions().get("ts")).isEqualTo("TypeScript");
    assertThat(config.extensions().get("tsx")).isEqualTo("TypeScript");

    // Backend languages
    assertThat(config.extensions().get("java")).isEqualTo("Java");
    assertThat(config.extensions().get("py")).isEqualTo("Python");
    assertThat(config.extensions().get("go")).isEqualTo("Go");
    assertThat(config.extensions().get("rs")).isEqualTo("Rust");

    // Config formats
    assertThat(config.extensions().get("yaml")).isEqualTo("YAML");
    assertThat(config.extensions().get("yml")).isEqualTo("YAML");
    assertThat(config.extensions().get("json")).isEqualTo("JSON");

    // Filename mappings
    assertThat(config.filenames().get("dockerfile")).isEqualTo("Docker");
    assertThat(config.filenames().get("makefile")).isEqualTo("Makefile");
    assertThat(config.filenames().get("jenkinsfile")).isEqualTo("Jenkins");
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
