package com.codestats.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable configuration record for code statistics analysis. Loaded from YAML files and
 * command-line overrides.
 */
public record CodeStatsConfig(
    Map<String, String> extensions,
    Map<String, String> filenames,
    List<String> productionDirectories,
    List<String> testDirectories,
    Map<String, Set<String>> aliases,
    List<String> includeUsers,
    List<String> excludeUsers) {

  /** Get default configuration with sensible defaults. */
  public static CodeStatsConfig getDefault() {
    return new CodeStatsConfig(
        getDefaultExtensions(),
        getDefaultFilenames(),
        getDefaultProductionDirectories(),
        getDefaultTestDirectories(),
        Map.of(),
        List.of(),
        List.of());
  }

  /** Merge this config with another, preferring values from the other config. */
  public CodeStatsConfig mergeWith(CodeStatsConfig other) {
    return new CodeStatsConfig(
        mergeMaps(this.extensions, other.extensions),
        mergeMaps(this.filenames, other.filenames),
        other.productionDirectories.isEmpty()
            ? this.productionDirectories
            : other.productionDirectories,
        other.testDirectories.isEmpty() ? this.testDirectories : other.testDirectories,
        mergeAliases(this.aliases, other.aliases),
        other.includeUsers.isEmpty() ? this.includeUsers : other.includeUsers,
        other.excludeUsers.isEmpty() ? this.excludeUsers : other.excludeUsers);
  }

  private static Map<String, String> getDefaultExtensions() {
    var extensions = new java.util.HashMap<String, String>();

    // Web Development
    extensions.put("js", "JavaScript");
    extensions.put("mjs", "JavaScript");
    extensions.put("jsx", "JavaScript");
    extensions.put("ts", "TypeScript");
    extensions.put("tsx", "TypeScript");
    extensions.put("html", "HTML");
    extensions.put("css", "CSS");
    extensions.put("scss", "SCSS");
    extensions.put("vue", "Vue");
    extensions.put("svelte", "Svelte");

    // Backend Languages
    extensions.put("java", "Java");
    extensions.put("py", "Python");
    extensions.put("rb", "Ruby");
    extensions.put("php", "PHP");
    extensions.put("go", "Go");
    extensions.put("rs", "Rust");
    extensions.put("cpp", "C++");
    extensions.put("c", "C");
    extensions.put("cs", "C#");
    extensions.put("kt", "Kotlin");
    extensions.put("scala", "Scala");

    // Scripting
    extensions.put("sh", "Shell");
    extensions.put("bash", "Bash");
    extensions.put("ps1", "PowerShell");

    // Data & Config
    extensions.put("sql", "SQL");
    extensions.put("json", "JSON");
    extensions.put("yaml", "YAML");
    extensions.put("yml", "YAML");
    extensions.put("xml", "XML");
    extensions.put("toml", "TOML");
    extensions.put("md", "Markdown");
    extensions.put("mdc", "Markdown");
    extensions.put("markdown", "Markdown");

    // Mobile
    extensions.put("swift", "Swift");
    extensions.put("dart", "Dart");

    return Map.copyOf(extensions);
  }

  private static Map<String, String> getDefaultFilenames() {
    var filenames = new java.util.HashMap<String, String>();

    // DevOps and Build Files
    filenames.put("dockerfile", "Docker");
    filenames.put("makefile", "Makefile");
    filenames.put("jenkinsfile", "Jenkins");
    filenames.put("vagrantfile", "Vagrant");

    // Ruby ecosystem files
    filenames.put("rakefile", "Ruby");
    filenames.put("gemfile", "Ruby");
    filenames.put("podfile", "Ruby");
    filenames.put("fastfile", "Ruby");
    filenames.put("brewfile", "Ruby");

    return Map.copyOf(filenames);
  }

  private static List<String> getDefaultProductionDirectories() {
    return List.of("src", "lib", "app", "source", "main");
  }

  private static List<String> getDefaultTestDirectories() {
    return List.of("test", "tests", "__tests__", "spec", "specs", "cypress", "e2e");
  }

  private Map<String, String> mergeMaps(Map<String, String> base, Map<String, String> override) {
    var merged = Map.copyOf(base);
    var mutableMerged = new java.util.HashMap<>(merged);
    mutableMerged.putAll(override);
    return Map.copyOf(mutableMerged);
  }

  private Map<String, Set<String>> mergeAliases(
      Map<String, Set<String>> base, Map<String, Set<String>> override) {
    var merged = Map.copyOf(base);
    var mutableMerged = new java.util.HashMap<>(merged);
    mutableMerged.putAll(override);
    return Map.copyOf(mutableMerged);
  }
}
