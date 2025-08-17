package com.codestats.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable configuration record for code statistics analysis. Loaded from YAML files and
 * command-line overrides.
 */
public record CodeStatsConfig(
    Map<String, String> languages,
    List<String> productionDirectories,
    List<String> testDirectories,
    Map<String, Set<String>> aliases) {

  /** Get default configuration with sensible defaults. */
  public static CodeStatsConfig getDefault() {
    return new CodeStatsConfig(
        getDefaultLanguages(),
        getDefaultProductionDirectories(),
        getDefaultTestDirectories(),
        Map.of());
  }

  /** Merge this config with another, preferring values from the other config. */
  public CodeStatsConfig mergeWith(CodeStatsConfig other) {
    return new CodeStatsConfig(
        mergeLanguages(this.languages, other.languages),
        other.productionDirectories.isEmpty()
            ? this.productionDirectories
            : other.productionDirectories,
        other.testDirectories.isEmpty() ? this.testDirectories : other.testDirectories,
        mergeAliases(this.aliases, other.aliases));
  }

  private static Map<String, String> getDefaultLanguages() {
    var languages = new java.util.HashMap<String, String>();

    // Web Development
    languages.put("js", "JavaScript");
    languages.put("mjs", "JavaScript");
    languages.put("jsx", "JavaScript");
    languages.put("ts", "TypeScript");
    languages.put("tsx", "TypeScript");
    languages.put("html", "HTML");
    languages.put("css", "CSS");
    languages.put("scss", "SCSS");
    languages.put("vue", "Vue");
    languages.put("svelte", "Svelte");

    // Backend Languages
    languages.put("java", "Java");
    languages.put("py", "Python");
    languages.put("rb", "Ruby");
    languages.put("php", "PHP");
    languages.put("go", "Go");
    languages.put("rs", "Rust");
    languages.put("cpp", "C++");
    languages.put("c", "C");
    languages.put("cs", "C#");
    languages.put("kt", "Kotlin");
    languages.put("scala", "Scala");

    // Scripting
    languages.put("sh", "Shell");
    languages.put("bash", "Bash");
    languages.put("ps1", "PowerShell");

    // Data & Config
    languages.put("sql", "SQL");
    languages.put("json", "JSON");
    languages.put("yaml", "YAML");
    languages.put("yml", "YAML");
    languages.put("xml", "XML");
    languages.put("toml", "TOML");
    languages.put("md", "Markdown");
    languages.put("mdc", "Markdown");
    languages.put("markdown", "Markdown");

    // Mobile
    languages.put("swift", "Swift");
    languages.put("dart", "Dart");

    return Map.copyOf(languages);
  }

  private static List<String> getDefaultProductionDirectories() {
    return List.of("src", "lib", "app", "source", "main");
  }

  private static List<String> getDefaultTestDirectories() {
    return List.of("test", "tests", "__tests__", "spec", "specs", "cypress", "e2e");
  }

  private Map<String, String> mergeLanguages(
      Map<String, String> base, Map<String, String> override) {
    var merged = Map.<String, String>copyOf(base);
    var mutableMerged = new java.util.HashMap<>(merged);
    mutableMerged.putAll(override);
    return Map.copyOf(mutableMerged);
  }

  private Map<String, Set<String>> mergeAliases(
      Map<String, Set<String>> base, Map<String, Set<String>> override) {
    var merged = Map.<String, Set<String>>copyOf(base);
    var mutableMerged = new java.util.HashMap<>(merged);
    mutableMerged.putAll(override);
    return Map.copyOf(mutableMerged);
  }
}
