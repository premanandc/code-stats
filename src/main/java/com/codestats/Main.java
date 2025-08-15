package com.codestats;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.codestats.alias.SimpleAliasResolver;
import com.codestats.config.CodeStatsConfig;
import com.codestats.git.JGitLogParser;
import com.codestats.output.JsonOutputFormatter;
import com.codestats.output.OutputFormatter;
import com.codestats.output.TextOutputFormatter;
import com.codestats.service.CodeStatsService;
import com.codestats.service.ProcessGitCommandExecutor;
import com.codestats.stats.SimpleStatisticsAggregator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "code-stats",
    description = "Analyze git repository contributor statistics",
    version = "1.0.0",
    mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "Repository path (default: current directory)",
      defaultValue = ".")
  private File repository;

  @Option(
      names = {"-d", "--days"},
      description = "Number of days in the past to analyze")
  private Integer days;

  @Option(
      names = {"--since"},
      description = "Start date (YYYY-MM-DD format)")
  private String since;

  @Option(
      names = {"--until"},
      description = "End date (YYYY-MM-DD format)")
  private String until;

  @Option(
      names = {"--include-users"},
      description = "Comma-separated list of user emails to include",
      split = ",")
  private String[] includeUsers;

  @Option(
      names = {"--exclude-users"},
      description = "Comma-separated list of user emails to exclude",
      split = ",")
  private String[] excludeUsers;

  @Option(
      names = {"--config"},
      description = "Configuration file path")
  private File configFile;

  @Option(
      names = {"--json"},
      description = "Output results in JSON format")
  private boolean jsonOutput;

  @Option(
      names = {"--ext"},
      description = "Custom file extensions mapping (format: ext1:Lang1,ext2:Lang2)")
  private String customExtensions;

  @Option(
      names = {"--no-color"},
      description = "Disable colored output")
  private boolean noColor;

  @Option(
      names = {"--init-config"},
      description = "Generate a sample configuration file")
  private boolean initConfig;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    try {
      // Handle init-config option
      if (initConfig) {
        return handleInitConfig();
      }

      // Initialize service components
      CodeStatsService service =
          new CodeStatsService(
              new JGitLogParser(),
              new SimpleAliasResolver(),
              new SimpleStatisticsAggregator(),
              new ProcessGitCommandExecutor());

      // Load configuration
      CodeStatsConfig config = loadConfiguration();

      // Parse date filters
      LocalDateTime sinceDate = parseDate(since);
      LocalDateTime untilDate = parseDate(until);

      // Parse user filters
      Set<String> includeUserSet = parseUserList(includeUsers);
      Set<String> excludeUserSet = parseUserList(excludeUsers);

      // Build request
      CodeStatsService.CodeStatsRequest request =
          CodeStatsService.CodeStatsRequest.builder()
              .repositoryPath(repository)
              .config(config)
              .days(days)
              .since(sinceDate)
              .until(untilDate)
              .includeUsers(includeUserSet)
              .excludeUsers(excludeUserSet)
              .build();

      // Execute analysis
      CodeStatsService.CodeStatsResult result = service.analyzeRepository(request);

      // Format and output results
      OutputFormatter formatter =
          jsonOutput ? new JsonOutputFormatter() : new TextOutputFormatter(!noColor);

      System.out.println(formatter.format(result));

      return result.isSuccess() ? 0 : 1;

    } catch (Exception e) {
      // Sanitize error message to avoid information exposure
      String sanitizedMessage = sanitizeErrorMessage(e.getMessage());
      System.err.println("Error: " + sanitizedMessage);
      if (System.getProperty("debug") != null) {
        e.printStackTrace();
      }
      return 1;
    }
  }

  private CodeStatsConfig loadConfiguration() {
    CodeStatsConfig config = CodeStatsConfig.getDefault();

    // Load from config file if specified
    if (configFile != null) {
      try {
        config = loadConfigFromFile(configFile);
      } catch (Exception e) {
        System.err.println(
            "Warning: Failed to load config file '" + configFile + "': " + e.getMessage());
        System.err.println("Using default configuration.");
      }
    }

    // Handle custom extensions override
    if (customExtensions != null) {
      Map<String, String> customLangMap = parseCustomExtensions(customExtensions);
      config =
          config.mergeWith(
              new CodeStatsConfig(
                  customLangMap,
                  config.productionDirectories(),
                  config.testDirectories(),
                  config.aliases()));
    }

    return config;
  }

  private CodeStatsConfig loadConfigFromFile(File configFile) throws Exception {
    if (!configFile.exists()) {
      throw new RuntimeException(
          "Configuration file does not exist: " + configFile.getAbsolutePath());
    }

    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    // Parse the YAML file into a generic map structure first
    @SuppressWarnings("unchecked")
    Map<String, Object> configMap = yamlMapper.readValue(configFile, Map.class);

    // Extract each section with proper type handling
    Map<String, String> languages = extractLanguages(configMap);
    List<String> productionDirs = extractStringList(configMap, "productionDirectories");
    List<String> testDirs = extractStringList(configMap, "testDirectories");
    Map<String, Set<String>> aliases = extractAliases(configMap);

    return new CodeStatsConfig(languages, productionDirs, testDirs, aliases);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractLanguages(Map<String, Object> configMap) {
    Object languagesObj = configMap.get("languages");
    if (languagesObj instanceof Map) {
      Map<String, Object> languagesMap = (Map<String, Object>) languagesObj;
      Map<String, String> result = new java.util.HashMap<>();
      for (Map.Entry<String, Object> entry : languagesMap.entrySet()) {
        result.put(entry.getKey(), String.valueOf(entry.getValue()));
      }
      return Map.copyOf(result);
    }
    return Map.of();
  }

  @SuppressWarnings("unchecked")
  private List<String> extractStringList(Map<String, Object> configMap, String key) {
    Object listObj = configMap.get(key);
    if (listObj instanceof List) {
      List<Object> list = (List<Object>) listObj;
      return list.stream().map(String::valueOf).toList();
    }
    return List.of();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> extractAliases(Map<String, Object> configMap) {
    Object aliasesObj = configMap.get("aliases");
    if (aliasesObj instanceof Map) {
      Map<String, Object> aliasesMap = (Map<String, Object>) aliasesObj;
      Map<String, Set<String>> result = new java.util.HashMap<>();

      for (Map.Entry<String, Object> entry : aliasesMap.entrySet()) {
        String canonicalEmail = entry.getKey();
        Object aliasListObj = entry.getValue();

        if (aliasListObj instanceof List) {
          List<Object> aliasList = (List<Object>) aliasListObj;
          Set<String> aliases =
              aliasList.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
          result.put(canonicalEmail, aliases);
        }
      }
      return Map.copyOf(result);
    }
    return Map.of();
  }

  private Map<String, String> parseCustomExtensions(String extensionsStr) {
    var customExtensions = new java.util.HashMap<String, String>();

    if (extensionsStr != null && !extensionsStr.trim().isEmpty()) {
      String[] pairs = extensionsStr.split(",");
      for (String pair : pairs) {
        String[] parts = pair.split(":");
        if (parts.length == 2) {
          customExtensions.put(parts[0].trim(), parts[1].trim());
        }
      }
    }

    return Map.copyOf(customExtensions);
  }

  private LocalDateTime parseDate(String dateStr) {
    if (dateStr == null || dateStr.trim().isEmpty()) {
      return null;
    }

    try {
      // Try parsing as date only (YYYY-MM-DD)
      LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
      return date.atStartOfDay();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid date format: " + dateStr + ". Use YYYY-MM-DD format.");
    }
  }

  private Set<String> parseUserList(String[] users) {
    if (users == null || users.length == 0) {
      return Set.of();
    }

    return Set.of(users);
  }

  private Integer handleInitConfig() {
    try {
      String configFileName = "code-stats-config.yaml";
      File configFile = new File(configFileName);

      if (configFile.exists()) {
        System.out.println("Configuration file '" + configFileName + "' already exists.");
        System.out.print("Overwrite? (y/N): ");

        // Simple yes/no confirmation - in a real app you might use Scanner
        // For now, we'll just warn and exit
        System.out.println(
            "Use --config to specify a different file name or remove the existing file.");
        return 1;
      }

      String sampleConfig = generateSampleConfig();

      java.nio.file.Files.writeString(configFile.toPath(), sampleConfig);

      System.out.println("Sample configuration file created: " + configFileName);
      System.out.println(
          "Edit this file to customize language mappings, directory patterns, and email aliases.");

      return 0;

    } catch (Exception e) {
      System.err.println("Failed to create configuration file: " + e.getMessage());
      return 1;
    }
  }

  private String generateSampleConfig() {
    return """
            # Code Stats Configuration File
            # This file controls how the code-stats tool analyzes your repository

            # Language Detection
            # Maps file extensions to language names for statistics
            languages:
              # Web Development
              js: JavaScript
              jsx: JavaScript
              ts: TypeScript
              tsx: TypeScript
              html: HTML
              css: CSS
              scss: SCSS
              vue: Vue
              svelte: Svelte

              # Backend Languages
              java: Java
              py: Python
              rb: Ruby
              php: PHP
              go: Go
              rs: Rust
              cpp: C++
              c: C
              cs: "C#"
              kt: Kotlin
              scala: Scala

              # Scripting & Shell
              sh: Shell
              bash: Bash
              ps1: PowerShell

              # Data & Config
              sql: SQL
              json: JSON
              yaml: YAML
              yml: YAML
              xml: XML
              toml: TOML
              md: Markdown
              markdown: Markdown

              # Mobile
              swift: Swift
              dart: Dart

              # Add your custom extensions here:
              # myext: "My Language"

            # Directory Classification
            # Files in these directories count as "production" code
            productionDirectories:
              - src
              - lib
              - app
              - source
              - main
              # Add your production directories here

            # Files in these directories count as "test" code
            testDirectories:
              - test
              - tests
              - __tests__
              - spec
              - specs
              - cypress
              - e2e
              # Add your test directories here

            # Email Aliases
            # Group multiple emails/names to the same contributor
            # Format: canonical_email -> list of aliases
            aliases:
              # Example: if "john.doe@company.com" is the canonical identity:
              # "john.doe@company.com":
              #   - "john@personal.com"
              #   - "j.doe@company.com"
              #   - "John Doe <john.doe@company.com>"

              # Add your email aliases here
            """;
  }

  /**
   * Sanitize error message to prevent information exposure through error messages. Removes
   * potentially sensitive file paths and internal details.
   */
  private String sanitizeErrorMessage(String message) {
    if (message == null) {
      return "An error occurred during processing";
    }

    // Remove absolute file paths that might expose system structure
    String sanitized = message.replaceAll("/[\\w/.-]+", "<path>");
    sanitized = sanitized.replaceAll("\\\\[\\w\\\\.-]+", "<path>");

    // Remove class names that might expose internal structure
    sanitized = sanitized.replaceAll("\\w+(\\.\\w+)+", "<internal>");

    // If message becomes too generic, provide a safe generic message
    if (sanitized.length() < 10 || sanitized.equals("<internal>")) {
      return "An error occurred during processing";
    }

    return sanitized;
  }
}
